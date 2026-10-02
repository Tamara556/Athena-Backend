import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi, seedSession, sessionStorage } from '../../src/helpers/auth-helper';
import { jsonResponder } from '../../src/helpers/onboarding-helper';
import {
  SAMPLE_DAILY_PLAN,
  SAMPLE_ROADMAP,
  installRoadmapRoutes,
  getDailyPlan,
  completePhaseApi,
  seedRoadmap,
  seedDailyPlan,
  aiDbAvailable,
  AI_DB_SKIP_REASON,
} from '../../src/helpers/roadmap-helper';

/**
 * Daily Plan (`GET /ai/daily-plans/me`) is a read-only snapshot created during
 * onboarding. Completing a roadmap phase or a learning session does NOT update
 * it. Daily Journey (`/daily-journey`) is a separate feature.
 *
 * `getLatestForUser` is ordered by createdAt desc — there is no "today only"
 * date filter on the read API.
 */
test.describe('Roadmap · Daily Plan (read-only snapshot)', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('RM-DP-001 — dashboard renders roadmap and daily plan independently', async ({
    page,
    gatewayApi,
    dashboardPage,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page);
    await dashboardPage.goto();

    await expect(dashboardPage.heading).toBeVisible();
    await expect(dashboardPage.roadmapCard).toContainText(SAMPLE_ROADMAP.goal);
    await expect(dashboardPage.phaseCards).toHaveCount(SAMPLE_ROADMAP.phases.length);
    await expect(dashboardPage.planCard).toContainText(SAMPLE_DAILY_PLAN.items[0].title);
    await expect(dashboardPage.planItems).toHaveCount(SAMPLE_DAILY_PLAN.items.length);
  });

  test('RM-DP-002 — missing daily plan shows the onboarding prompt; roadmap can still render', async ({
    page,
    gatewayApi,
    dashboardPage,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, { dailyPlan: jsonResponder(404, { status: 404, error: 'Not Found' }) });
    await dashboardPage.goto();

    await expect(dashboardPage.roadmapCard).toContainText(SAMPLE_ROADMAP.goal);
    await expect(dashboardPage.planMessage).toContainText('No daily plan yet');
  });

  test('RM-DP-003 — a user with no daily plan gets 404 from GET /ai/daily-plans/me', async ({ gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);
    const res = await getDailyPlan(gatewayApi, auth.accessToken);
    expect(res.status()).toBe(404);
  });

  test('RM-DP-004 — completing a roadmap phase does not change the daily plan', async ({ gatewayApi }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth } = await registerViaApi(gatewayApi);
    const roadmap = await seedRoadmap(auth.userId);
    const plan = await seedDailyPlan(auth.userId, roadmap.id);

    const before = await (await getDailyPlan(gatewayApi, auth.accessToken)).json();
    expect(before.id).toBe(plan.id);
    expect(before.items).toHaveLength(SAMPLE_DAILY_PLAN.items.length);

    const complete = await completePhaseApi(gatewayApi, auth.accessToken, 0);
    expect(complete.status()).toBe(200);

    const after = await (await getDailyPlan(gatewayApi, auth.accessToken)).json();
    expect(after.id).toBe(before.id);
    expect(after.date).toBe(before.date);
    expect(after.items).toEqual(before.items);
  });

  test('RM-DP-005 — latest daily plan is returned regardless of plan_date (no day-boundary filter)', async ({
    gatewayApi,
  }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth } = await registerViaApi(gatewayApi);
    const roadmap = await seedRoadmap(auth.userId);
    await seedDailyPlan(auth.userId, roadmap.id, {
      ...SAMPLE_DAILY_PLAN,
      date: '2020-01-01',
      createdAt: '2020-01-01T00:00:00Z',
      items: [{ type: 'READING', title: 'Old plan', description: 'stale', estimatedMinutes: 5 }],
    });
    const newer = await seedDailyPlan(auth.userId, roadmap.id, {
      ...SAMPLE_DAILY_PLAN,
      date: '2020-01-02',
      createdAt: '2020-01-02T00:00:00Z',
      items: [{ type: 'PRACTICE', title: 'Newer plan', description: 'latest', estimatedMinutes: 10 }],
    });

    const body = await (await getDailyPlan(gatewayApi, auth.accessToken)).json();
    expect(body.id).toBe(newer.id);
    expect(body.items[0].title).toBe('Newer plan');
  });

  test('RM-DP-006 — daily-plan backend failure on dashboard is the empty message', async ({
    page,
    gatewayApi,
    dashboardPage,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, { dailyPlan: jsonResponder(500, { status: 500, error: 'Error' }) });
    await dashboardPage.goto();
    await expect(dashboardPage.planMessage).toContainText('No daily plan yet');
  });

  test('RM-DP-007 — unauthenticated GET /ai/daily-plans/me is 401', async ({ gatewayApi }) => {
    const res = await gatewayApi.get('/ai/daily-plans/me', { failOnStatusCode: false });
    expect(res.status()).toBe(401);
  });
});
