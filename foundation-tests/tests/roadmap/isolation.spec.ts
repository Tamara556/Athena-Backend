import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi, seedSession, sessionStorage } from '../../src/helpers/auth-helper';
import {
  SAMPLE_ROADMAP,
  SAMPLE_PHASES,
  getMyRoadmap,
  getRoadmapById,
  completePhaseApi,
  completeSessionApi,
  getSessionById,
  seedRoadmap,
  seedLearningSession,
  aiDbAvailable,
  AI_DB_SKIP_REASON,
  withStatuses,
  authHeader,
} from '../../src/helpers/roadmap-helper';

test.describe('Roadmap · Data isolation', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('RM-ISO-001 — GET /me never returns another user\'s roadmap', async ({ gatewayApi }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth: a } = await registerViaApi(gatewayApi);
    const { auth: b } = await registerViaApi(gatewayApi);
    await seedRoadmap(a.userId, { ...SAMPLE_ROADMAP, goal: 'User A goal' });
    await seedRoadmap(b.userId, { ...SAMPLE_ROADMAP, goal: 'User B goal' });

    const aBody = await (await getMyRoadmap(gatewayApi, a.accessToken)).json();
    const bBody = await (await getMyRoadmap(gatewayApi, b.accessToken)).json();
    expect(aBody.goal).toBe('User A goal');
    expect(bBody.goal).toBe('User B goal');
    expect(aBody.id).not.toBe(bBody.id);
  });

  test('RM-ISO-002 — User A completing a phase does not change User B\'s roadmap', async ({ gatewayApi }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth: a } = await registerViaApi(gatewayApi);
    const { auth: b } = await registerViaApi(gatewayApi);
    await seedRoadmap(a.userId);
    await seedRoadmap(b.userId);

    expect((await completePhaseApi(gatewayApi, a.accessToken, 0)).status()).toBe(200);

    const bBody = await (await getMyRoadmap(gatewayApi, b.accessToken)).json();
    expect(bBody.phases.map((p: { status: string }) => p.status)).toEqual(['CURRENT', 'AVAILABLE', 'LOCKED']);
  });

  test('RM-ISO-003 — User B cannot complete User A\'s learning session', async ({ gatewayApi }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth: a } = await registerViaApi(gatewayApi);
    const { auth: b } = await registerViaApi(gatewayApi);
    const roadmap = await seedRoadmap(a.userId);
    const session = await seedLearningSession(a.userId, roadmap.id);

    expect((await completeSessionApi(gatewayApi, b.accessToken, session.id)).status()).toBe(404);
    expect((await getSessionById(gatewayApi, b.accessToken, session.id)).status()).toBe(404);
    expect((await getSessionById(gatewayApi, a.accessToken, session.id)).status()).toBe(200);
  });

  test('RM-ISO-004 — User B cannot GET User A\'s roadmap by id (gateway ownership)', async ({ gatewayApi }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth: a } = await registerViaApi(gatewayApi);
    const { auth: b } = await registerViaApi(gatewayApi);
    const aRoadmap = await seedRoadmap(a.userId, { ...SAMPLE_ROADMAP, goal: 'Secret A path' });

    const asOwner = await getRoadmapById(gatewayApi, a.accessToken, aRoadmap.id);
    expect(asOwner.status(), await asOwner.text()).toBe(200);
    expect((await asOwner.json()).goal).toBe('Secret A path');

    const asOther = await getRoadmapById(gatewayApi, b.accessToken, aRoadmap.id);
    expect(asOther.status(), await asOther.text()).toBe(403);
    const body = await asOther.json();
    expect(body.message).toMatch(/own roadmap/i);
  });

  test('RM-ISO-005 — forged X-User-Id cannot read or complete another user\'s roadmap', async ({ gatewayApi }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth: a } = await registerViaApi(gatewayApi);
    const { auth: b } = await registerViaApi(gatewayApi);
    await seedRoadmap(a.userId, { ...SAMPLE_ROADMAP, goal: 'Only A' });
    await seedRoadmap(b.userId, { ...SAMPLE_ROADMAP, goal: 'Only B' });

    const spoofed = await gatewayApi.get('/ai/roadmaps/me', {
      headers: {
        ...authHeader(b.accessToken),
        'X-User-Id': a.userId,
        'X-User-Roles': 'ADMIN',
      },
      failOnStatusCode: false,
    });
    expect(spoofed.status()).toBe(200);
    expect((await spoofed.json()).goal).toBe('Only B');

    const spoofComplete = await gatewayApi.post('/ai/roadmaps/me/phases/0/complete', {
      headers: {
        ...authHeader(b.accessToken),
        'X-User-Id': a.userId,
      },
      data: {},
      failOnStatusCode: false,
    });
    expect(spoofComplete.status()).toBe(200);
    expect((await spoofComplete.json()).goal).toBe('Only B');

    const aStill = await (await getMyRoadmap(gatewayApi, a.accessToken)).json();
    expect(aStill.phases[0].status).toBe('CURRENT');
  });

  test('RM-ISO-006 — UI for User A never renders User B\'s goal', async ({
    page,
    browser,
    gatewayApi,
    roadmapPage,
  }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth: a } = await registerViaApi(gatewayApi);
    const { auth: b } = await registerViaApi(gatewayApi);
    await seedRoadmap(a.userId, { ...SAMPLE_ROADMAP, goal: 'Alpha goal' });
    await seedRoadmap(b.userId, {
      ...SAMPLE_ROADMAP,
      goal: 'Bravo goal',
      phases: withStatuses(SAMPLE_PHASES, 0),
    });

    await seedSession(page, sessionStorage(a));
    await roadmapPage.goto();
    await expect(roadmapPage.goalText).toHaveText('Alpha goal');
    await expect(page.getByText('Bravo goal')).toHaveCount(0);

    const bContext = await browser.newContext();
    const other = await bContext.newPage();
    await seedSession(other, sessionStorage(b));
    await other.goto('/roadmap');
    await expect(other.locator('.greet p b')).toHaveText('Bravo goal');
    await expect(other.getByText('Alpha goal')).toHaveCount(0);
    await bContext.close();
  });
});
