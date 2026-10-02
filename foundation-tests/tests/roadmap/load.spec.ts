import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi, seedSession, sessionStorage } from '../../src/helpers/auth-helper';
import { fulfillJson, abortResponder, jsonResponder } from '../../src/helpers/onboarding-helper';
import {
  SAMPLE_ROADMAP,
  SAMPLE_PHASES,
  installRoadmapRoutes,
  openRoadmap,
  getMyRoadmap,
  openSeededRoadmap,
  seedRoadmap,
  aiDbAvailable,
  AI_DB_SKIP_REASON,
} from '../../src/helpers/roadmap-helper';

test.describe('Roadmap · Load', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('RM-LOAD-001 — authenticated user opens /roadmap and the journey renders', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    await openRoadmap(page, gatewayApi);

    await expect(page).toHaveURL(/\/roadmap$/);
    await expect(roadmapPage.stops).toHaveCount(SAMPLE_PHASES.length);
    await expect(roadmapPage.heading('Java Foundations')).toBeVisible();
    // Available/locked titles live on nodes (hover card / aria-label), not as visible headings.
    await expect(page.locator('.node[aria-label="Spring Boot Basics — avail"]')).toBeVisible();
    await expect(page.locator('.node[aria-label="Databases & JPA — locked"]')).toBeVisible();
    await expect(roadmapPage.lockedStops).toHaveCount(1);
  });

  test('RM-LOAD-002 — title, goal, level and stats match the API payload', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    await openRoadmap(page, gatewayApi);

    await expect(roadmapPage.goalText).toHaveText(SAMPLE_ROADMAP.goal);
    await expect(roadmapPage.levelStat).toHaveText(SAMPLE_ROADMAP.level);
    await expect(roadmapPage.phasesStat).toHaveText(String(SAMPLE_PHASES.length));
    await expect(roadmapPage.milestonesStat).toHaveText('6'); // 2 objectives × 3 phases
    await expect(roadmapPage.weeksStat).toContainText('12');
    await expect(roadmapPage.insight).toContainText(SAMPLE_ROADMAP.goal);
  });

  test('RM-LOAD-003 — current phase is identified and objectives are listed', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    await openRoadmap(page, gatewayApi);

    await expect(roadmapPage.currentPhase).toBeVisible();
    await expect(roadmapPage.currentPill).toHaveText('You are here');
    await expect(roadmapPage.objectives).toHaveCount(2);
    await expect(roadmapPage.objectives.nth(0)).toContainText('Variables & types');
    await expect(roadmapPage.continueLearningLink).toBeVisible();
  });

  test('RM-LOAD-004 — greeting uses the authenticated user name', async ({ page, gatewayApi, roadmapPage }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page);
    await roadmapPage.goto();

    await expect(roadmapPage.greeting).toContainText(auth.firstName);
  });

  test('RM-LOAD-005 — loading: empty and journey stay hidden until GET /ai/roadmaps/me resolves', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    let release!: () => void;
    const gate = new Promise<void>((resolve) => (release = resolve));
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, {
      roadmap: async (route) => {
        await gate;
        await fulfillJson(route, 200, SAMPLE_ROADMAP);
      },
    });

    const pending = roadmapPage.goto();
    await expect(roadmapPage.emptySection).toHaveCount(0);
    await expect(roadmapPage.stops).toHaveCount(0);

    release();
    await pending;
    await expect(roadmapPage.stops).toHaveCount(SAMPLE_PHASES.length);
  });

  test('RM-LOAD-006 — backend 500 is rendered as the empty state (no journey)', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    await openRoadmap(page, gatewayApi, { roadmap: jsonResponder(500, { status: 500, error: 'Error' }) });

    await expect(roadmapPage.emptySection).toBeVisible();
    await expect(roadmapPage.stops).toHaveCount(0);
  });

  test('RM-LOAD-007 — network failure is rendered as the empty state', async ({ page, gatewayApi, roadmapPage }) => {
    await openRoadmap(page, gatewayApi, { roadmap: abortResponder('failed') });

    await expect(roadmapPage.emptySection).toBeVisible();
    await expect(roadmapPage.stops).toHaveCount(0);
  });

  test('RM-LOAD-008 — unauthenticated direct navigation to /roadmap redirects to /login', async ({
    page,
    roadmapPage,
  }) => {
    await roadmapPage.goto();
    await expect(page).toHaveURL(/\/login$/);
  });

  test('RM-LOAD-009 — refresh re-GETs /ai/roadmaps/me and keeps the same journey', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    let gets = 0;
    page.on('request', (r) => {
      if (r.method() === 'GET' && /\/ai\/roadmaps\/me$/.test(r.url())) gets++;
    });
    await openRoadmap(page, gatewayApi);
    await expect(roadmapPage.stops).toHaveCount(SAMPLE_PHASES.length);
    const before = gets;

    await page.reload();
    await expect(roadmapPage.stops).toHaveCount(SAMPLE_PHASES.length);
    expect(gets).toBeGreaterThan(before);
  });

  test('RM-LOAD-010 — GET /ai/roadmaps/me without a token is 401', async ({ gatewayApi }) => {
    const res = await gatewayApi.get('/ai/roadmaps/me', { failOnStatusCode: false });
    expect(res.status()).toBe(401);
  });

  test('RM-LOAD-011 — a freshly registered user has no roadmap (real 404)', async ({ gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);
    const res = await getMyRoadmap(gatewayApi, auth.accessToken);
    expect(res.status()).toBe(404);
  });

  test('RM-LOAD-012 — seeded roadmap GET /me returns that user\'s goal and phases', async ({ gatewayApi }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth } = await registerViaApi(gatewayApi);
    await seedRoadmap(auth.userId);

    const res = await getMyRoadmap(gatewayApi, auth.accessToken);
    expect(res.status(), await res.text()).toBe(200);
    const body = await res.json();
    expect(body.goal).toBe(SAMPLE_ROADMAP.goal);
    expect(body.level).toBe(SAMPLE_ROADMAP.level);
    expect(body.phases).toHaveLength(3);
    expect(body.phases[0].status).toBe('CURRENT');
    expect(body.phases[1].status).toBe('AVAILABLE');
    expect(body.phases[2].status).toBe('LOCKED');
  });

  test('RM-LOAD-013 — UI of a seeded roadmap matches the API body after refresh', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth, seeded } = await openSeededRoadmap(page, gatewayApi);
    await expect(roadmapPage.stops).toHaveCount(seeded.phases.length);
    await expect(roadmapPage.goalText).toHaveText(seeded.goal);

    const res = await getMyRoadmap(gatewayApi, auth.accessToken);
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.goal).toBe(seeded.goal);

    await page.reload();
    await expect(roadmapPage.goalText).toHaveText(body.goal);
    await expect(roadmapPage.stops).toHaveCount(body.phases.length);
  });
});
