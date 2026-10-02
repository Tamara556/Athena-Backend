import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { openOnboarding, CANNED, fulfillJson } from '../../src/helpers/onboarding-helper';

/**
 * AI request behaviour: exactly-once submission, loading indicator, and the fact
 * that entering the busy state removes the submit control (duplicate prevention).
 */
test.describe('Onboarding · AI request', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('ONB-AI-001 — the goal request is sent exactly once', async ({ page, gatewayApi, onboardingPage }) => {
    let goalCalls = 0;
    page.on('request', (r) => {
      if (r.method() === 'POST' && r.url().includes('/ai/onboarding/goal')) goalCalls++;
    });

    await openOnboarding(page, gatewayApi);
    await onboardingPage.enterGoal('Become a backend engineer');
    await onboardingPage.submitGoal();

    await expect(onboardingPage.questions).toHaveCount(CANNED.questions.length);
    expect(goalCalls).toBe(1);
  });

  test('ONB-AI-002 — the loading state shows while the AI request is in flight', async ({ page, gatewayApi, onboardingPage }) => {
    let release!: () => void;
    const gate = new Promise<void>((resolve) => (release = resolve));

    await openOnboarding(page, gatewayApi, {
      goal: async (route) => {
        await gate; // hold the response so the busy loader is observable deterministically
        await fulfillJson(route, 200, { questions: CANNED.questions });
      },
    });

    await onboardingPage.enterGoal('Become a backend engineer');
    await onboardingPage.submitGoal();

    await expect(onboardingPage.busyLoader).toBeVisible();
    await expect(onboardingPage.busyText).toHaveText('Athena is thinking…');

    release();
    await expect(onboardingPage.questions).toHaveCount(CANNED.questions.length);
  });

  test('ONB-AI-003 — the submit control is removed during generation (no duplicate submit)', async ({ page, gatewayApi, onboardingPage }) => {
    let goalCalls = 0;
    let release!: () => void;
    const gate = new Promise<void>((resolve) => (release = resolve));

    await openOnboarding(page, gatewayApi, {
      goal: async (route) => {
        goalCalls++;
        await gate;
        await fulfillJson(route, 200, { questions: CANNED.questions });
      },
    });

    await onboardingPage.enterGoal('Become a backend engineer');
    await onboardingPage.submitGoal();

    // Entering the busy step replaces the goal card, so the submit button is gone.
    await expect(onboardingPage.busyLoader).toBeVisible();
    await expect(onboardingPage.goalSubmit).toHaveCount(0);

    release();
    await expect(onboardingPage.questions).toHaveCount(CANNED.questions.length);
    expect(goalCalls).toBe(1);
  });

  test('ONB-AI-004 — the assessment request is sent exactly once on generate', async ({ page, gatewayApi, onboardingPage }) => {
    let assessmentCalls = 0;
    page.on('request', (r) => {
      if (r.method() === 'POST' && r.url().includes('/ai/onboarding/assessment')) assessmentCalls++;
    });

    await openOnboarding(page, gatewayApi);
    await onboardingPage.enterGoal('Become a backend engineer');
    await onboardingPage.submitGoal();
    await expect(onboardingPage.questions).toHaveCount(CANNED.questions.length);
    await onboardingPage.answerAll('My answer');
    await onboardingPage.generate();

    await page.waitForURL('**/roadmap');
    expect(assessmentCalls).toBe(1);
  });
});
