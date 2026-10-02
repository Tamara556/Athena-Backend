import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import {
  openOnboarding,
  installOnboardingRoutes,
  fulfillJson,
  jsonResponder,
  abortResponder,
  CANNED,
} from '../../src/helpers/onboarding-helper';
import { registerViaApi, seedSession, sessionStorage } from '../../src/helpers/auth-helper';

/**
 * Negative & resilience scenarios for onboarding. AI failure modes are injected
 * deterministically via route interception; the assertions verify the real
 * graceful-error UI and step recovery.
 */
test.describe('Onboarding · Negative & resilience', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('ONB-NEG-001 — AI unavailable (503) surfaces the graceful message and returns to the goal step', async ({ page, gatewayApi, onboardingPage }) => {
    await openOnboarding(page, gatewayApi, { goal: jsonResponder(503, CANNED.unavailable) });

    await onboardingPage.enterGoal('Become a backend engineer');
    await onboardingPage.submitGoal();

    await expect(onboardingPage.error).toContainText('temporarily unavailable');
    await expect(onboardingPage.goalInput).toBeVisible(); // recovered to goal step
  });

  test('ONB-NEG-002 — network/gateway failure shows the "cannot reach gateway" error', async ({ page, gatewayApi, onboardingPage }) => {
    await openOnboarding(page, gatewayApi, { goal: abortResponder('failed') });

    await onboardingPage.enterGoal('Become a backend engineer');
    await onboardingPage.submitGoal();

    await expect(onboardingPage.error).toContainText('Cannot reach the gateway');
    await expect(onboardingPage.goalInput).toBeVisible();
  });

  test('ONB-NEG-003 — backend validation failure (400) is surfaced', async ({ page, gatewayApi, onboardingPage }) => {
    await openOnboarding(page, gatewayApi, {
      goal: jsonResponder(400, { status: 400, error: 'Bad Request', message: 'Validation failed for one or more fields', details: [] }),
    });

    await onboardingPage.enterGoal('x');
    await onboardingPage.submitGoal();

    await expect(onboardingPage.error).toContainText('Validation failed');
    await expect(onboardingPage.goalInput).toBeVisible();
  });

  test('ONB-NEG-004 — malformed AI response is handled without crashing', async ({ page, gatewayApi, onboardingPage }) => {
    // questions is not an array — the component defaults to an empty list.
    await openOnboarding(page, gatewayApi, { goal: jsonResponder(200, { questions: 'oops-not-an-array' }) });

    await onboardingPage.enterGoal('Become a backend engineer');
    await onboardingPage.submitGoal();

    // Transitions to the questions step but renders zero questions (graceful).
    await expect(onboardingPage.generateButton).toBeVisible();
    await expect(onboardingPage.questions).toHaveCount(0);
  });

  test('ONB-NEG-005 — retry after an AI failure succeeds on resubmit', async ({ page, gatewayApi, onboardingPage }) => {
    let attempts = 0;
    await openOnboarding(page, gatewayApi, {
      goal: async (route) => {
        attempts++;
        if (attempts === 1) return fulfillJson(route, 503, CANNED.unavailable);
        return fulfillJson(route, 200, { questions: CANNED.questions });
      },
    });

    await onboardingPage.enterGoal('Become a backend engineer');
    await onboardingPage.submitGoal(); // 1st → 503
    await expect(onboardingPage.error).toContainText('temporarily unavailable');

    await onboardingPage.submitGoal(); // retry (goal text is preserved) → success
    await expect(onboardingPage.questions).toHaveCount(CANNED.questions.length);
    expect(attempts).toBe(2);
  });

  test('ONB-NEG-006 — assessment failure keeps the user on the questions step', async ({ page, gatewayApi, onboardingPage }) => {
    await openOnboarding(page, gatewayApi, { assessment: jsonResponder(503, CANNED.unavailable) });

    await onboardingPage.enterGoal('Become a backend engineer');
    await onboardingPage.submitGoal();
    await expect(onboardingPage.questions).toHaveCount(CANNED.questions.length);
    await onboardingPage.answerAll('My answer');
    await onboardingPage.generate();

    await expect(onboardingPage.error).toContainText('temporarily unavailable');
    await expect(onboardingPage.questions.first()).toBeVisible(); // still on questions
    await expect(page).toHaveURL(/\/onboarding$/);
  });

  test('ONB-NEG-007 — refreshing during generation restarts onboarding at the goal step', async ({ page, gatewayApi, onboardingPage }) => {
    let release!: () => void;
    const gate = new Promise<void>((resolve) => (release = resolve));
    await openOnboarding(page, gatewayApi, {
      assessment: async (route) => {
        await gate; // hold generation so we can refresh mid-flight
        await fulfillJson(route, 200, {});
      },
    });

    await onboardingPage.enterGoal('Become a backend engineer');
    await onboardingPage.submitGoal();
    await expect(onboardingPage.questions).toHaveCount(CANNED.questions.length);
    await onboardingPage.answerAll('My answer');
    await onboardingPage.generate();
    await expect(onboardingPage.busyLoader).toBeVisible();

    await page.reload();

    // No client-side resume: onboarding starts over at the goal step.
    await expect(onboardingPage.goalInput).toBeVisible();
    await expect(onboardingPage.busyLoader).toHaveCount(0);
    release();
  });

  test('ONB-NEG-008 — browser Back during onboarding exits the flow (steps are not history entries)', async ({ page, gatewayApi, onboardingPage, roadmapPage }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installOnboardingRoutes(page);

    await roadmapPage.goto(); // establish a prior history entry
    await page.goto('/onboarding');
    await onboardingPage.enterGoal('Become a backend engineer');
    await onboardingPage.submitGoal();
    await expect(onboardingPage.questions).toHaveCount(CANNED.questions.length);

    await page.goBack();

    // Steps are not history entries: Back leaves /onboarding entirely.
    // Chromium/Firefox return to /roadmap; WebKit may surface the SPA bootstrap `/`.
    await expect(page).not.toHaveURL(/\/onboarding/);
    await expect(onboardingPage.questions).toHaveCount(0);
  });

  test('ONB-NEG-009 — direct navigation to /onboarding while logged out redirects to /login', async ({ page }) => {
    await page.goto('/onboarding');
    await expect(page).toHaveURL(/\/login$/);
  });
});
