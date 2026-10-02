import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi, seedSession, sessionStorage } from '../../src/helpers/auth-helper';
import { fulfillJson } from '../../src/helpers/onboarding-helper';
import {
  SAMPLE_ROADMAP,
  installRoadmapRoutes,
  getMyRoadmap,
} from '../../src/helpers/roadmap-helper';

test.describe('Roadmap · Empty states', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('RM-EMPTY-001 — user with no roadmap sees empty state + onboarding link (real 404)', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await roadmapPage.goto();

    await expect(roadmapPage.emptySection).toBeVisible();
    await expect(roadmapPage.emptySection.getByRole('heading', { name: 'No roadmap yet' })).toBeVisible();
    await expect(roadmapPage.startOnboardingLink).toBeVisible();
    await expect(roadmapPage.stops).toHaveCount(0);
    await expect(roadmapPage.stats).toHaveCount(0);

    const res = await getMyRoadmap(gatewayApi, auth.accessToken);
    expect(res.status()).toBe(404);
  });

  test('RM-EMPTY-002 — empty-state Start onboarding navigates to /onboarding', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await roadmapPage.goto();
    await roadmapPage.clickStartOnboarding();
    await expect(page).toHaveURL(/\/onboarding$/);
  });

  test('RM-EMPTY-003 — refresh of an empty roadmap stays empty', async ({ page, gatewayApi, roadmapPage }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await roadmapPage.goto();
    await expect(roadmapPage.emptySection).toBeVisible();
    await page.reload();
    await expect(roadmapPage.emptySection).toBeVisible();
    await expect(roadmapPage.stops).toHaveCount(0);
  });

  test('RM-EMPTY-004 — roadmap with zero phases: journey exists, no stops, stats are 0', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, {
      roadmap: (route) =>
        fulfillJson(route, 200, { ...SAMPLE_ROADMAP, phases: [] }),
    });
    await roadmapPage.goto();

    await expect(roadmapPage.emptySection).toHaveCount(0);
    await expect(roadmapPage.stats).toBeVisible();
    await expect(roadmapPage.phasesStat).toHaveText('0');
    await expect(roadmapPage.milestonesStat).toHaveText('0');
    await expect(roadmapPage.stops).toHaveCount(0);
    await expect(roadmapPage.completePhaseButton).toHaveCount(0);
  });

  test('RM-EMPTY-005 — current phase with zero objectives hides the objectives list', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));
    await installRoadmapRoutes(page, {
      roadmap: (route) =>
        fulfillJson(route, 200, {
          ...SAMPLE_ROADMAP,
          phases: [
            {
              name: 'Empty phase',
              description: 'No lessons',
              durationWeeks: 1,
              objectives: [],
              status: 'CURRENT',
            },
          ],
        }),
    });
    await roadmapPage.goto();

    await expect(roadmapPage.heading('Empty phase')).toBeVisible();
    await expect(roadmapPage.objectives).toHaveCount(0);
    await expect(roadmapPage.milestonesStat).toHaveText('0');
    await expect(roadmapPage.currentPhase.locator('.skills')).toHaveCount(0);
  });

  test('RM-EMPTY-006 — loading then empty: delayed 404 never flashes a journey', async ({
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
        await fulfillJson(route, 404, { status: 404, error: 'Not Found' });
      },
    });

    const pending = roadmapPage.goto();
    await expect(roadmapPage.emptySection).toHaveCount(0);
    await expect(roadmapPage.stops).toHaveCount(0);
    release();
    await pending;
    await expect(roadmapPage.emptySection).toBeVisible();
  });
});
