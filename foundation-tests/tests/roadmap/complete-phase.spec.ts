import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi, seedSession, sessionStorage } from '../../src/helpers/auth-helper';
import { mintExpiredToken } from '../../src/helpers/jwt';
import { fulfillJson, abortResponder, jsonResponder } from '../../src/helpers/onboarding-helper';
import {
  SAMPLE_ROADMAP,
  SAMPLE_PHASES,
  installRoadmapRoutes,
  openRoadmap,
  statefulRoadmapHandlers,
  completePhaseApi,
  getMyRoadmap,
  seedRoadmap,
  aiDbAvailable,
  AI_DB_SKIP_REASON,
  withStatuses,
} from '../../src/helpers/roadmap-helper';

test.describe('Roadmap · Complete phase', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('RM-PHASE-001 — Mark phase complete sends POST and advances CURRENT → next', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    const handlers = statefulRoadmapHandlers();
    let posts = 0;
    page.on('request', (r) => {
      if (r.method() === 'POST' && r.url().includes('/phases/') && r.url().includes('/complete')) posts++;
    });
    await openRoadmap(page, gatewayApi, { roadmap: handlers.get, completePhase: handlers.complete });

    await expect(roadmapPage.heading('Java Foundations')).toBeVisible();
    await roadmapPage.clickCompletePhase();

    await expect(roadmapPage.doneStops).toHaveCount(1);
    await expect(roadmapPage.heading('Spring Boot Basics')).toBeVisible();
    await expect(roadmapPage.currentPhase.getByRole('heading', { name: 'Spring Boot Basics' })).toBeVisible();
    expect(posts).toBe(1);
    expect(handlers.state.current.phases.map((p) => p.status)).toEqual(['COMPLETED', 'CURRENT', 'AVAILABLE']);
  });

  test('RM-PHASE-002 — completing flag disables the button (no duplicate submit)', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    let release!: () => void;
    const gate = new Promise<void>((resolve) => (release = resolve));
    let posts = 0;
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, {
      completePhase: async (route) => {
        posts++;
        await gate;
        await fulfillJson(route, 200, {
          ...SAMPLE_ROADMAP,
          phases: withStatuses(SAMPLE_PHASES, 1),
        });
      },
    });
    await page.goto('/roadmap');
    await expect(roadmapPage.completePhaseButton).toBeEnabled();

    await roadmapPage.clickCompletePhase();
    await expect(roadmapPage.completePhaseButton).toBeDisabled();
    await expect(roadmapPage.completePhaseButton).toHaveText(/Saving/);
    await roadmapPage.completePhaseButton.click({ force: true }).catch(() => undefined);

    release();
    await expect(roadmapPage.doneStops).toHaveCount(1);
    expect(posts).toBe(1);
  });

  test('RM-PHASE-003 — refresh after complete keeps the new statuses (stateful mock)', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    const handlers = statefulRoadmapHandlers();
    await openRoadmap(page, gatewayApi, { roadmap: handlers.get, completePhase: handlers.complete });
    await roadmapPage.clickCompletePhase();
    await expect(roadmapPage.doneStops).toHaveCount(1);

    await page.reload();
    await expect(roadmapPage.doneStops).toHaveCount(1);
    await expect(roadmapPage.currentPhase.getByRole('heading', { name: 'Spring Boot Basics' })).toBeVisible();
  });

  test('RM-PHASE-004 — complete-phase 400 (locked) is silent: button re-enables, current stays', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    await openRoadmap(page, gatewayApi, {
      completePhase: jsonResponder(400, {
        status: 400,
        error: 'Bad Request',
        message: 'Phase 1 is locked and cannot be completed yet',
      }),
    });

    await roadmapPage.clickCompletePhase();
    await expect(roadmapPage.completePhaseButton).toBeEnabled();
    await expect(roadmapPage.currentPhase.getByRole('heading', { name: 'Java Foundations' })).toBeVisible();
    await expect(roadmapPage.doneStops).toHaveCount(0);
  });

  test('RM-PHASE-005 — complete-phase network failure is silent', async ({ page, gatewayApi, roadmapPage }) => {
    await openRoadmap(page, gatewayApi, { completePhase: abortResponder('failed') });
    await roadmapPage.clickCompletePhase();
    await expect(roadmapPage.completePhaseButton).toBeEnabled();
    await expect(roadmapPage.currentPhase).toBeVisible();
  });

  test('RM-PHASE-006 — expired token on complete redirects to /login', async ({ page, gatewayApi, roadmapPage }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, {
      ...sessionStorage(auth),
      athena_token: mintExpiredToken({ subject: auth.userId }),
    });
    await installRoadmapRoutes(page);
    await roadmapPage.goto();
    await expect(page).toHaveURL(/\/login$/);
  });

  test('RM-PHASE-007 — API: complete current phase persists; duplicate of that index is idempotent', async ({
    gatewayApi,
  }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth } = await registerViaApi(gatewayApi);
    await seedRoadmap(auth.userId);

    const first = await completePhaseApi(gatewayApi, auth.accessToken, 0);
    expect(first.status(), await first.text()).toBe(200);
    expect((await first.json()).phases.map((p: { status: string }) => p.status)).toEqual([
      'COMPLETED',
      'CURRENT',
      'AVAILABLE',
    ]);

    const dup = await completePhaseApi(gatewayApi, auth.accessToken, 0);
    expect(dup.status()).toBe(200);
    expect((await dup.json()).phases.map((p: { status: string }) => p.status)).toEqual([
      'COMPLETED',
      'CURRENT',
      'AVAILABLE',
    ]);

    const read = await getMyRoadmap(gatewayApi, auth.accessToken);
    expect((await read.json()).phases.map((p: { status: string }) => p.status)).toEqual([
      'COMPLETED',
      'CURRENT',
      'AVAILABLE',
    ]);
  });

  test('RM-PHASE-008 — API: locked/available-ahead phase is 400; out of range is 400', async ({ gatewayApi }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth } = await registerViaApi(gatewayApi);
    await seedRoadmap(auth.userId);

    const locked = await completePhaseApi(gatewayApi, auth.accessToken, 2);
    expect(locked.status()).toBe(400);
    expect((await locked.json()).message).toMatch(/locked/i);

    const avail = await completePhaseApi(gatewayApi, auth.accessToken, 1);
    expect(avail.status()).toBe(400);
    expect((await avail.json()).message).toMatch(/locked/i);

    const oor = await completePhaseApi(gatewayApi, auth.accessToken, 9);
    expect(oor.status()).toBe(400);
    expect((await oor.json()).message).toMatch(/out of range/i);
  });

  test('RM-PHASE-009 — API: no roadmap → 404; no token → 401', async ({ gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);
    const missing = await completePhaseApi(gatewayApi, auth.accessToken, 0);
    expect(missing.status()).toBe(404);

    const anon = await gatewayApi.post('/ai/roadmaps/me/phases/0/complete', { data: {}, failOnStatusCode: false });
    expect(anon.status()).toBe(401);
  });

  test('RM-PHASE-010 — UI + real backend: complete → API → UI → refresh', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    test.skip(!(await aiDbAvailable()), AI_DB_SKIP_REASON);
    const { auth } = await registerViaApi(gatewayApi);
    await seedRoadmap(auth.userId);
    await seedSession(page, sessionStorage(auth));
    await roadmapPage.goto();

    await expect(roadmapPage.completePhaseButton).toBeVisible();
    const posted = page.waitForResponse(
      (r) => r.request().method() === 'POST' && r.url().includes('/phases/0/complete'),
    );
    await roadmapPage.clickCompletePhase();
    const res = await posted;
    expect(res.status()).toBe(200);

    await expect(roadmapPage.doneStops).toHaveCount(1);
    await expect(roadmapPage.currentPhase.getByRole('heading', { name: 'Spring Boot Basics' })).toBeVisible();

    const api = await getMyRoadmap(gatewayApi, auth.accessToken);
    expect((await api.json()).phases.map((p: { status: string }) => p.status)).toEqual([
      'COMPLETED',
      'CURRENT',
      'AVAILABLE',
    ]);

    await page.reload();
    await expect(roadmapPage.doneStops).toHaveCount(1);
    await expect(roadmapPage.currentPhase.getByRole('heading', { name: 'Spring Boot Basics' })).toBeVisible();
  });
});
