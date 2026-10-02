import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import {
  openOnboarding,
  installOnboardingRoutes,
  CANNED,
} from '../../src/helpers/onboarding-helper';
import { registerViaApi, seedSession, sessionStorage } from '../../src/helpers/auth-helper';

/**
 * End-to-end generation and roadmap rendering, plus refresh idempotency and the
 * pre-onboarding empty state (real backend 404).
 */
test.describe('Onboarding · Roadmap generation', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('ONB-RM-001 — full flow: goal → questions → generate → roadmap renders', async ({ page, gatewayApi, onboardingPage, roadmapPage }) => {
    await openOnboarding(page, gatewayApi);

    await onboardingPage.enterGoal('Become a backend engineer');
    await onboardingPage.submitGoal();
    await expect(onboardingPage.questions).toHaveCount(CANNED.questions.length);
    await onboardingPage.answerAll('My detailed answer');
    await onboardingPage.generate();

    await page.waitForURL('**/roadmap');
    await expect(roadmapPage.stops).toHaveCount(CANNED.roadmap.phases.length);
    await expect(roadmapPage.heading('Java Foundations')).toBeVisible();
  });

  test('ONB-RM-002 — roadmap stats and metadata are computed correctly', async ({ page, gatewayApi, roadmapPage }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installOnboardingRoutes(page);

    await roadmapPage.goto();

    // 3 phases; first is CURRENT (status not provided → derived).
    await expect(roadmapPage.phasesStat).toHaveText('3');
    await expect(roadmapPage.currentPhase).toBeVisible();
    await expect(roadmapPage.currentPill).toBeVisible();
    await expect(page.getByText(CANNED.roadmap.goal).first()).toBeVisible();
  });

  test('ONB-RM-003 — the first learning task is reachable from the current phase', async ({ page, gatewayApi, roadmapPage }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installOnboardingRoutes(page);

    await roadmapPage.goto();
    await expect(roadmapPage.continueLearningLink).toBeVisible();
  });

  test('ONB-RM-004 — refreshing the roadmap does not create a new roadmap (read-only)', async ({ page, gatewayApi, roadmapPage }) => {
    let assessmentPosts = 0;
    page.on('request', (r) => {
      if (r.method() === 'POST' && r.url().includes('/ai/onboarding/assessment')) assessmentPosts++;
    });

    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installOnboardingRoutes(page);

    await roadmapPage.goto();
    await expect(roadmapPage.stops).toHaveCount(CANNED.roadmap.phases.length);

    await page.reload();
    await expect(roadmapPage.stops).toHaveCount(CANNED.roadmap.phases.length);
    expect(assessmentPosts).toBe(0); // refresh only reads the roadmap
  });

  test('ONB-RM-005 — a user with no roadmap sees the empty state that links to onboarding', async ({ page, gatewayApi, roadmapPage }) => {
    // No roadmap mock: a freshly-registered user has no roadmap → real 404.
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));

    await roadmapPage.goto();

    await expect(roadmapPage.emptySection).toBeVisible();
    await expect(roadmapPage.startOnboardingLink).toBeVisible();
    await expect(roadmapPage.stops).toHaveCount(0);
  });
});
