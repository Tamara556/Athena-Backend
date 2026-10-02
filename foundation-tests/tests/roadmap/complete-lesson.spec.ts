import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi, seedSession, sessionStorage } from '../../src/helpers/auth-helper';
import { mintExpiredToken } from '../../src/helpers/jwt';
import { fulfillJson, abortResponder, jsonResponder } from '../../src/helpers/onboarding-helper';
import {
  SAMPLE_ROADMAP,
  SAMPLE_SESSION,
  installRoadmapRoutes,
  openRoadmap,
  completeSessionApi,
  getSessionById,
  getMyRoadmap,
  seedRoadmap,
  seedLearningSession,
  aiDbAvailable,
  AI_DB_SKIP_REASON,
  applyCompletePhase,
} from '../../src/helpers/roadmap-helper';

test.describe('Roadmap · Complete lesson (learning session)', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('RM-LESSON-001 — current phase opens /learning/current and lesson content loads', async ({
    page,
    gatewayApi,
    roadmapPage,
    learningSessionPage,
  }) => {
    await openRoadmap(page, gatewayApi);
    await roadmapPage.clickContinueLearning();

    await expect(page).toHaveURL(/\/learning\/current$/);
    await expect(learningSessionPage.title).toHaveText(SAMPLE_SESSION.title);
    await expect(learningSessionPage.readingStage).toBeVisible();
  });

  test('RM-LESSON-002 — completing all four stages POSTs /complete and shows the overlay', async ({
    page,
    gatewayApi,
    learningSessionPage,
  }) => {
    let completeCalls = 0;
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, {
      completeSession: async (route) => {
        completeCalls++;
        await fulfillJson(route, 200, { ...SAMPLE_SESSION, status: 'COMPLETED' });
      },
    });

    await learningSessionPage.gotoCurrent();
    await expect(learningSessionPage.startCta).toBeVisible();
    await learningSessionPage.completeAllStages();

    await expect(learningSessionPage.completionOverlay).toHaveClass(/show/);
    expect(completeCalls).toBe(1);
  });

  test('RM-LESSON-003 — returning to the roadmap does not auto-complete the phase', async ({
    page,
    gatewayApi,
    roadmapPage,
    learningSessionPage,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, {
      completeSession: (route) => fulfillJson(route, 200, { ...SAMPLE_SESSION, status: 'COMPLETED' }),
    });

    await learningSessionPage.gotoCurrent();
    await learningSessionPage.completeAllStages();
    await expect(learningSessionPage.completionOverlay).toHaveClass(/show/);
    await learningSessionPage.returnToRoadmap.click({ force: true });

    await expect(page).toHaveURL(/\/roadmap$/);
    await expect(roadmapPage.currentPhase).toBeVisible();
    await expect(roadmapPage.completePhaseButton).toBeVisible();
    await expect(roadmapPage.doneStops).toHaveCount(0);
  });

  test('RM-LESSON-004 — a completed session stays completed after refresh', async ({
    page,
    gatewayApi,
    learningSessionPage,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, {
      currentSession: (route) => fulfillJson(route, 200, { ...SAMPLE_SESSION, status: 'COMPLETED' }),
    });

    await learningSessionPage.gotoCurrent();
    await expect(learningSessionPage.completionOverlay).toHaveClass(/show/);
    await expect(learningSessionPage.statusText).toHaveText('Complete');

    await page.reload();
    await expect(learningSessionPage.completionOverlay).toHaveClass(/show/);
  });

  test('RM-LESSON-005 — duplicate complete is a single POST (already-completed overlay path)', async ({
    page,
    gatewayApi,
    learningSessionPage,
  }) => {
    let completeCalls = 0;
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, {
      currentSession: (route) => fulfillJson(route, 200, { ...SAMPLE_SESSION, status: 'COMPLETED' }),
      completeSession: async (route) => {
        completeCalls++;
        await fulfillJson(route, 200, { ...SAMPLE_SESSION, status: 'COMPLETED' });
      },
    });

    await learningSessionPage.gotoCurrent();
    await expect(learningSessionPage.completionOverlay).toHaveClass(/show/);
    // applyStatus for COMPLETED does not re-POST complete — overlay is local.
    expect(completeCalls).toBe(0);
  });

  test('RM-LESSON-006 — complete with expired token redirects to /login', async ({
    page,
    gatewayApi,
    learningSessionPage,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, {
      ...sessionStorage(auth),
      athena_token: mintExpiredToken({ subject: auth.userId }),
    });
    await learningSessionPage.gotoCurrent();
    await expect(page).toHaveURL(/\/login$/);
  });

  test('RM-LESSON-007 — backend 500 during complete surfaces the error empty-state', async ({
    page,
    gatewayApi,
    learningSessionPage,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, {
      completeSession: jsonResponder(500, { status: 500, error: 'Error', message: 'complete failed' }),
    });

    await learningSessionPage.gotoCurrent();
    const posted = page.waitForResponse(
      (r) => r.request().method() === 'POST' && /\/learning-sessions\/[^/]+\/complete$/.test(r.url()),
    );
    await learningSessionPage.completeAllStages();
    expect((await posted).status()).toBe(500);

    await expect(learningSessionPage.errorState).toBeVisible();
    await expect(page.getByText('complete failed')).toBeVisible();
    await expect(learningSessionPage.completionOverlay).not.toHaveClass(/show/);
  });

  test('RM-LESSON-008 — network failure during complete is handled', async ({
    page,
    gatewayApi,
    learningSessionPage,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, { completeSession: abortResponder('failed') });

    await learningSessionPage.gotoCurrent();
    await learningSessionPage.completeAllStages();

    await expect(page.getByText(/Cannot reach the gateway/i)).toBeVisible();
  });

  test('RM-LESSON-009 — API complete without a session is 404; without a token is 401', async ({
    gatewayApi,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    const missing = await completeSessionApi(gatewayApi, auth.accessToken, '00000000-0000-0000-0000-000000000099');
    expect(missing.status()).toBe(404);

    const anon = await gatewayApi.post('/learning-sessions/00000000-0000-0000-0000-000000000099/complete', {
      data: {},
      failOnStatusCode: false,
    });
    expect(anon.status()).toBe(401);
  });

  test('RM-LESSON-010 — seeded session complete is persisted and idempotent; roadmap phase is unchanged', async ({
    gatewayApi,
  }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth } = await registerViaApi(gatewayApi);
    const roadmap = await seedRoadmap(auth.userId);
    const session = await seedLearningSession(auth.userId, roadmap.id);

    const first = await completeSessionApi(gatewayApi, auth.accessToken, session.id);
    expect(first.status(), await first.text()).toBe(200);
    expect((await first.json()).status).toBe('COMPLETED');

    const second = await completeSessionApi(gatewayApi, auth.accessToken, session.id);
    expect(second.status()).toBe(200);
    expect((await second.json()).status).toBe('COMPLETED');

    const after = await getSessionById(gatewayApi, auth.accessToken, session.id);
    expect((await after.json()).status).toBe('COMPLETED');

    const rm = await getMyRoadmap(gatewayApi, auth.accessToken);
    const body = await rm.json();
    // Completing the lesson must NOT have advanced the phase (separate API).
    expect(body.phases.map((p: { status: string }) => p.status)).toEqual(['CURRENT', 'AVAILABLE', 'LOCKED']);
    expect(applyCompletePhase(SAMPLE_ROADMAP, 0).phases[0].status).toBe('COMPLETED');
  });
});
