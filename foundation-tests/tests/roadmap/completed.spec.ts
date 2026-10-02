import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi, seedSession, sessionStorage } from '../../src/helpers/auth-helper';
import { fulfillJson } from '../../src/helpers/onboarding-helper';
import {
  SAMPLE_ROADMAP,
  SAMPLE_PHASES,
  SAMPLE_DAILY_PLAN,
  installRoadmapRoutes,
  completePhaseApi,
  getMyRoadmap,
  getDailyPlan,
  seedRoadmap,
  seedDailyPlan,
  aiDbAvailable,
  AI_DB_SKIP_REASON,
  withStatuses,
} from '../../src/helpers/roadmap-helper';

test.describe('Roadmap · Completed', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('RM-DONE-001 — all phases COMPLETED: every node is done, no current, no complete button', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, {
      roadmap: (route) =>
        fulfillJson(route, 200, { ...SAMPLE_ROADMAP, phases: withStatuses(SAMPLE_PHASES, 3) }),
    });
    await roadmapPage.goto();

    await expect(roadmapPage.stops).toHaveCount(3);
    await expect(roadmapPage.doneStops).toHaveCount(3);
    await expect(roadmapPage.currentPhase).toHaveCount(0);
    await expect(roadmapPage.completePhaseButton).toHaveCount(0);
    await expect(roadmapPage.continueLearningLink).toHaveCount(0);
    await expect(roadmapPage.lockedStops).toHaveCount(0);
  });

  test('RM-DONE-002 — completed nodes show 100% after hover; refresh keeps them done', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, {
      roadmap: (route) =>
        fulfillJson(route, 200, { ...SAMPLE_ROADMAP, phases: withStatuses(SAMPLE_PHASES, 3) }),
    });
    await roadmapPage.goto();

    await roadmapPage.hoverStop(roadmapPage.doneStops.nth(0));
    await expect(roadmapPage.doneStops.nth(0).locator('.pct')).toHaveText('100%');
    await roadmapPage.hoverStop(roadmapPage.doneStops.nth(1));
    await expect(roadmapPage.doneStops.nth(1).locator('.pct')).toHaveText('100%');

    await page.reload();
    await expect(roadmapPage.doneStops).toHaveCount(3);
    await expect(roadmapPage.currentPhase).toHaveCount(0);
  });

  test('RM-DONE-003 — sidebar navigation still works from a completed roadmap', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, {
      roadmap: (route) =>
        fulfillJson(route, 200, { ...SAMPLE_ROADMAP, phases: withStatuses(SAMPLE_PHASES, 3) }),
    });
    await roadmapPage.goto();
    await roadmapPage.clickNav('/daily-journey');
    await expect(page).toHaveURL(/\/daily-journey$/);
    await roadmapPage.clickNav('/roadmap');
    await expect(page).toHaveURL(/\/roadmap$/);
    await expect(roadmapPage.doneStops).toHaveCount(3);
  });

  test('RM-DONE-004 — no regenerate control on a completed roadmap', async ({ page, gatewayApi, roadmapPage }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, {
      roadmap: (route) =>
        fulfillJson(route, 200, { ...SAMPLE_ROADMAP, phases: withStatuses(SAMPLE_PHASES, 3) }),
    });
    await roadmapPage.goto();
    await expect(roadmapPage.regenerateControl()).toHaveCount(0);
  });

  test('RM-DONE-005 — completing every phase via API reaches all COMPLETED; daily plan unchanged', async ({
    gatewayApi,
  }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth } = await registerViaApi(gatewayApi);
    const roadmap = await seedRoadmap(auth.userId);
    const plan = await seedDailyPlan(auth.userId, roadmap.id);

    expect((await completePhaseApi(gatewayApi, auth.accessToken, 0)).status()).toBe(200);
    expect((await completePhaseApi(gatewayApi, auth.accessToken, 1)).status()).toBe(200);
    expect((await completePhaseApi(gatewayApi, auth.accessToken, 2)).status()).toBe(200);

    const body = await (await getMyRoadmap(gatewayApi, auth.accessToken)).json();
    expect(body.phases.map((p: { status: string }) => p.status)).toEqual([
      'COMPLETED',
      'COMPLETED',
      'COMPLETED',
    ]);

    const afterPlan = await (await getDailyPlan(gatewayApi, auth.accessToken)).json();
    expect(afterPlan.id).toBe(plan.id);
    expect(afterPlan.items[0].title).toBe(SAMPLE_DAILY_PLAN.items[0].title);
  });

  test('RM-DONE-006 — UI of a fully completed seeded roadmap matches the API after refresh', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth } = await registerViaApi(gatewayApi);
    await seedRoadmap(auth.userId, { ...SAMPLE_ROADMAP, phases: withStatuses(SAMPLE_PHASES, 3) });
    await seedSession(page, sessionStorage(auth));
    await roadmapPage.goto();

    await expect(roadmapPage.doneStops).toHaveCount(3);
    await expect(roadmapPage.currentPhase).toHaveCount(0);

    await page.reload();
    await expect(roadmapPage.doneStops).toHaveCount(3);
    const body = await (await getMyRoadmap(gatewayApi, auth.accessToken)).json();
    expect(body.phases.every((p: { status: string }) => p.status === 'COMPLETED')).toBe(true);
  });
});
