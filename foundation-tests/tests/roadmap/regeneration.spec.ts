import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi, seedSession, sessionStorage } from '../../src/helpers/auth-helper';
import { mintExpiredToken } from '../../src/helpers/jwt';
import {
  SAMPLE_ROADMAP,
  SAMPLE_PHASES,
  openRoadmap,
  getMyRoadmap,
  seedRoadmap,
  aiDbAvailable,
  AI_DB_SKIP_REASON,
  authHeader,
  withStatuses,
} from '../../src/helpers/roadmap-helper';

/**
 * There is no dedicated "regenerate roadmap" API or UI control.
 * A new GeneratedRoadmap row is created only by completing onboarding
 * assessment (LLM). GET /me returns the latest by createdAt desc.
 */
test.describe('Roadmap · AI regeneration (not implemented as a dedicated flow)', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('RM-REGEN-001 — the roadmap page has no regenerate control', async ({ page, gatewayApi, roadmapPage }) => {
    await openRoadmap(page, gatewayApi);
    await expect(roadmapPage.stops).toHaveCount(SAMPLE_PHASES.length);
    await expect(roadmapPage.regenerateControl()).toHaveCount(0);
    await expect(page.getByText(/regenerat/i)).toHaveCount(0);
  });

  test('RM-REGEN-002 — POST /ai/roadmaps/me/regenerate is not a real endpoint', async ({ gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);
    const res = await gatewayApi.post('/ai/roadmaps/me/regenerate', {
      headers: authHeader(auth.accessToken),
      data: {},
      failOnStatusCode: false,
    });
    expect([404, 405, 400, 500]).toContain(res.status());
    expect(res.status()).not.toBe(200);
    expect(res.status()).not.toBe(201);
  });

  test('RM-REGEN-003 — unauthenticated regenerate-shaped request is 401 (gateway)', async ({ gatewayApi }) => {
    const res = await gatewayApi.post('/ai/roadmaps/me/regenerate', { data: {}, failOnStatusCode: false });
    expect(res.status()).toBe(401);
  });

  test('RM-REGEN-004 — expired token cannot call a protected /ai/roadmaps path', async ({ gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);
    const res = await gatewayApi.get('/ai/roadmaps/me', {
      headers: authHeader(mintExpiredToken({ subject: auth.userId })),
      failOnStatusCode: false,
    });
    expect(res.status()).toBe(401);
  });

  test('RM-REGEN-005 — GET /me returns the latest seeded roadmap (previous row is superseded)', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth } = await registerViaApi(gatewayApi);
    await seedRoadmap(auth.userId, SAMPLE_ROADMAP, '2026-01-01T00:00:00Z');
    const newer = await seedRoadmap(
      auth.userId,
      {
        ...SAMPLE_ROADMAP,
        goal: 'Become a data engineer',
        level: 'intermediate',
        phases: withStatuses(
          SAMPLE_PHASES.map((p) => ({ ...p, name: `New ${p.name}` })),
          0,
        ),
      },
      '2026-08-13T18:00:00Z',
    );

    const body = await (await getMyRoadmap(gatewayApi, auth.accessToken)).json();
    expect(body.id).toBe(newer.id);
    expect(body.goal).toBe('Become a data engineer');
    expect(body.phases[0].name).toBe('New Java Foundations');

    await seedSession(page, sessionStorage(auth));
    await roadmapPage.goto();
    await expect(roadmapPage.goalText).toHaveText('Become a data engineer');
    await expect(roadmapPage.heading('New Java Foundations')).toBeVisible();

    await page.reload();
    await expect(roadmapPage.goalText).toHaveText('Become a data engineer');
  });
});
