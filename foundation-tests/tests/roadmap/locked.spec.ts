import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi, seedSession, sessionStorage } from '../../src/helpers/auth-helper';
import { fulfillJson } from '../../src/helpers/onboarding-helper';
import {
  SAMPLE_ROADMAP,
  SAMPLE_PHASES,
  openRoadmap,
  completePhaseApi,
  seedRoadmap,
  seedLearningSession,
  completeSessionApi,
  getSessionById,
  aiDbAvailable,
  AI_DB_SKIP_REASON,
  withStatuses,
} from '../../src/helpers/roadmap-helper';

test.describe('Roadmap · Locked content', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('RM-LOCK-001 — locked phase has lock styling and no complete / continue actions', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    await openRoadmap(page, gatewayApi);

    await expect(roadmapPage.lockedStops).toHaveCount(1);
    const locked = roadmapPage.lockedStops.first();
    await expect(locked.locator('.node--locked')).toBeVisible();
    await expect(locked.locator('a[href="/learning/current"]')).toHaveCount(0);
    await expect(locked.locator('button.rm-complete')).toHaveCount(0);
    await expect(roadmapPage.completePhaseButton).toHaveCount(1); // only on current
  });

  test('RM-LOCK-002 — completing the current phase unlocks the next (AVAILABLE → CURRENT)', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    const { statefulRoadmapHandlers } = await import('../../src/helpers/roadmap-helper');
    const handlers = statefulRoadmapHandlers();
    await openRoadmap(page, gatewayApi, { roadmap: handlers.get, completePhase: handlers.complete });

    await expect(roadmapPage.lockedStops).toHaveCount(1);
    await roadmapPage.clickCompletePhase();

    await expect(roadmapPage.lockedStops).toHaveCount(0);
    await expect(roadmapPage.currentPhase.getByRole('heading', { name: 'Spring Boot Basics' })).toBeVisible();
    await expect(roadmapPage.doneStops).toHaveCount(1);
  });

  test('RM-LOCK-003 — API rejects completing a locked phase; current is unchanged', async ({ gatewayApi }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth } = await registerViaApi(gatewayApi);
    await seedRoadmap(auth.userId);

    const locked = await completePhaseApi(gatewayApi, auth.accessToken, 2);
    expect(locked.status()).toBe(400);

    const still = await completePhaseApi(gatewayApi, auth.accessToken, 0);
    expect(still.status()).toBe(200);
    expect((await still.json()).phases.map((p: { status: string }) => p.status)).toEqual([
      'COMPLETED',
      'CURRENT',
      'AVAILABLE',
    ]);
  });

  test('RM-LOCK-004 — there is no per-phase URL; /roadmap is the only roadmap route', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    await openRoadmap(page, gatewayApi);
    await page.goto('/roadmap/2');
    // Angular wildcard redirects unknown paths to `/`.
    await expect(page).not.toHaveURL(/\/roadmap\/2/);
    await roadmapPage.goto();
    await expect(roadmapPage.lockedStops).toHaveCount(1);
  });

  test('RM-LOCK-005 — learning-session complete is ownership-scoped (404 for a foreign id)', async ({
    gatewayApi,
  }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth: a } = await registerViaApi(gatewayApi);
    const { auth: b } = await registerViaApi(gatewayApi);
    const roadmap = await seedRoadmap(a.userId);
    const session = await seedLearningSession(a.userId, roadmap.id);

    const stolen = await completeSessionApi(gatewayApi, b.accessToken, session.id);
    expect(stolen.status()).toBe(404);

    const read = await getSessionById(gatewayApi, b.accessToken, session.id);
    expect(read.status()).toBe(404);
  });

  test('RM-LOCK-006 — a later-node session can be completed via API (no phase-lock on lessons)', async ({
    gatewayApi,
  }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth } = await registerViaApi(gatewayApi);
    const roadmap = await seedRoadmap(auth.userId);
    const later = await seedLearningSession(auth.userId, roadmap.id, { nodeIndex: 2, title: 'Locked-phase lesson' });

    const res = await completeSessionApi(gatewayApi, auth.accessToken, later.id);
    expect(res.status(), await res.text()).toBe(200);
    expect((await res.json()).status).toBe('COMPLETED');
  });

  test('RM-LOCK-007 — fully locked later phases stay locked until the prerequisite current is completed', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    await openRoadmap(page, gatewayApi, {
      roadmap: (route) =>
        fulfillJson(route, 200, {
          ...SAMPLE_ROADMAP,
          phases: withStatuses(SAMPLE_PHASES, 0),
        }),
    });
    await expect(roadmapPage.lockedStops).toHaveCount(1);
    await expect(page.locator('.stop.has-card')).toHaveCount(1); // available only
  });
});
