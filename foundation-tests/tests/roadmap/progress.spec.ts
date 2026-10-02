import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi, seedSession, sessionStorage } from '../../src/helpers/auth-helper';
import { fulfillJson } from '../../src/helpers/onboarding-helper';
import {
  SAMPLE_ROADMAP,
  SAMPLE_PHASES,
  openRoadmap,
  statefulRoadmapHandlers,
  completePhaseApi,
  getMyRoadmap,
  seedRoadmap,
  aiDbAvailable,
  AI_DB_SKIP_REASON,
  withStatuses,
} from '../../src/helpers/roadmap-helper';

/**
 * Real progress formula (RoadmapComponent.build + RoadmapServiceImpl.recompute):
 *   COMPLETED → 100%, every other status → 0%.
 * There is no overall percentage on the roadmap page; stats (phase/objective/week
 * counts) do not change when a phase is completed.
 */
test.describe('Roadmap · Progress', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('RM-PROG-001 — current phase shows 0% in progress; completed shows 100%', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    await openRoadmap(page, gatewayApi, {
      roadmap: (route) => fulfillJson(route, 200, { ...SAMPLE_ROADMAP, phases: withStatuses(SAMPLE_PHASES, 1) }),
    });

    await expect(roadmapPage.currentPhase.locator('.feature-ring-label b')).toHaveText('0%');
    const done = roadmapPage.doneStops.first();
    await roadmapPage.hoverStop(done);
    await expect(done.locator('.pct')).toHaveText('100%');
  });

  test('RM-PROG-002 — completing a phase updates that node to 100% and the next to 0%', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    const handlers = statefulRoadmapHandlers();
    await openRoadmap(page, gatewayApi, { roadmap: handlers.get, completePhase: handlers.complete });

    await expect(roadmapPage.currentPhase.locator('.feature-ring-label b')).toHaveText('0%');
    await roadmapPage.clickCompletePhase();

    const done = roadmapPage.doneStops.first();
    await roadmapPage.hoverStop(done);
    await expect(done.locator('.pct')).toHaveText('100%');
    await expect(roadmapPage.currentPhase.locator('.feature-ring-label b')).toHaveText('0%');
  });

  test('RM-PROG-003 — stats do not change on phase complete (not a derived overall %)', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    const handlers = statefulRoadmapHandlers();
    await openRoadmap(page, gatewayApi, { roadmap: handlers.get, completePhase: handlers.complete });

    await expect(roadmapPage.phasesStat).toHaveText('3');
    await expect(roadmapPage.milestonesStat).toHaveText('6');
    await expect(roadmapPage.weeksStat).toContainText('12');

    await roadmapPage.clickCompletePhase();
    await expect(roadmapPage.doneStops).toHaveCount(1);

    await expect(roadmapPage.phasesStat).toHaveText('3');
    await expect(roadmapPage.milestonesStat).toHaveText('6');
    await expect(roadmapPage.weeksStat).toContainText('12');
  });

  test('RM-PROG-004 — duplicate complete of an already-completed index does not increase progress', async ({
    gatewayApi,
  }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth } = await registerViaApi(gatewayApi);
    await seedRoadmap(auth.userId);
    await completePhaseApi(gatewayApi, auth.accessToken, 0);
    await completePhaseApi(gatewayApi, auth.accessToken, 0);

    const body = await (await getMyRoadmap(gatewayApi, auth.accessToken)).json();
    const completed = body.phases.filter((p: { status: string }) => p.status === 'COMPLETED');
    expect(completed).toHaveLength(1);
  });

  test('RM-PROG-005 — UI 100%/0% matches API statuses after a real complete + refresh', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth } = await registerViaApi(gatewayApi);
    await seedRoadmap(auth.userId);
    await seedSession(page, sessionStorage(auth));
    await roadmapPage.goto();
    await roadmapPage.clickCompletePhase();
    await expect(roadmapPage.doneStops).toHaveCount(1);

    const body = await (await getMyRoadmap(gatewayApi, auth.accessToken)).json();
    expect(body.phases[0].status).toBe('COMPLETED');
    expect(body.phases[1].status).toBe('CURRENT');

    await page.reload();
    const done = roadmapPage.doneStops.first();
    await roadmapPage.hoverStop(done);
    await expect(done.locator('.pct')).toHaveText('100%');
    await expect(roadmapPage.currentPhase.locator('.feature-ring-label b')).toHaveText('0%');
  });
});
