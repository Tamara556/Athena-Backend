import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { openOnboarding, CANNED } from '../../src/helpers/onboarding-helper';

/**
 * The multi-step questionnaire: opening, step transitions, client validation,
 * the AI response rendering into questions, and the "start from zero" path.
 * AI endpoints are stubbed deterministically; the component behaviour is real.
 */
test.describe('Onboarding · Questionnaire', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('ONB-Q-001 — opens on the goal step for an authenticated user', async ({ page, gatewayApi, onboardingPage }) => {
    await openOnboarding(page, gatewayApi);

    await expect(page).toHaveURL(/\/onboarding$/);
    await expect(onboardingPage.goalInput).toBeVisible();
    await expect(onboardingPage.goalTitle).toBeVisible();
  });

  test('ONB-Q-002 — greeting from the AI start response is rendered', async ({ page, gatewayApi, onboardingPage }) => {
    await openOnboarding(page, gatewayApi);
    await expect(onboardingPage.greeting).toHaveText(CANNED.start.greeting);
  });

  test('ONB-Q-003 — empty goal is blocked client-side and calls no AI', async ({ page, gatewayApi, onboardingPage }) => {
    let goalCalls = 0;
    page.on('request', (r) => {
      if (r.url().includes('/ai/onboarding/goal')) goalCalls++;
    });
    await openOnboarding(page, gatewayApi);

    await onboardingPage.submitGoal(); // nothing typed

    await expect(onboardingPage.error).toBeVisible();
    await expect(onboardingPage.error).toContainText("Tell Athena what you'd like to learn");
    await expect(onboardingPage.goalInput).toBeVisible(); // still on goal step
    expect(goalCalls).toBe(0);
  });

  test('ONB-Q-004 — submitting a goal advances to the questions step with AI questions', async ({ page, gatewayApi, onboardingPage }) => {
    await openOnboarding(page, gatewayApi);

    await onboardingPage.enterGoal('Become a backend engineer');
    await onboardingPage.submitGoal();

    await expect(onboardingPage.questions).toHaveCount(CANNED.questions.length);
    await expect(onboardingPage.questions.first()).toBeVisible();
    await expect(onboardingPage.generateButton).toBeVisible();
  });

  test('ONB-Q-005 — a partially answered questionnaire is rejected client-side', async ({ page, gatewayApi, onboardingPage }) => {
    let assessmentCalls = 0;
    page.on('request', (r) => {
      if (r.url().includes('/ai/onboarding/assessment')) assessmentCalls++;
    });
    await openOnboarding(page, gatewayApi);

    await onboardingPage.enterGoal('Learn Spring');
    await onboardingPage.submitGoal();
    await expect(onboardingPage.questions).toHaveCount(CANNED.questions.length);

    await onboardingPage.answer(0, 'Some Java experience'); // leave the rest empty
    await onboardingPage.generate();

    await expect(onboardingPage.error).toBeVisible();
    await expect(onboardingPage.error).toContainText('answer every question');
    expect(assessmentCalls).toBe(0);
  });

  test('ONB-Q-006 — every question must be answered to submit', async ({ page, gatewayApi, onboardingPage }) => {
    await openOnboarding(page, gatewayApi);
    await onboardingPage.enterGoal('Learn Spring');
    await onboardingPage.submitGoal();
    await expect(onboardingPage.questions).toHaveCount(CANNED.questions.length);

    await onboardingPage.answerAll('My detailed answer');
    await onboardingPage.generate();

    // Proceeds to generation (busy → /roadmap); no validation error.
    await expect(onboardingPage.error).toHaveCount(0);
    await page.waitForURL('**/roadmap');
  });

  test('ONB-Q-007 — "Start all from 0" auto-answers and generates without manual input', async ({ page, gatewayApi, onboardingPage }) => {
    await openOnboarding(page, gatewayApi);
    await onboardingPage.enterGoal('Learn Spring');
    await onboardingPage.submitGoal();
    await expect(onboardingPage.questions).toHaveCount(CANNED.questions.length);

    await onboardingPage.startFromZero(); // no answers typed

    await page.waitForURL('**/roadmap'); // generation proceeds from the zero baseline
  });
});
