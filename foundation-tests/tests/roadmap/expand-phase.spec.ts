import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { fulfillJson } from '../../src/helpers/onboarding-helper';
import {
  SAMPLE_ROADMAP,
  SAMPLE_PHASES,
  openRoadmap,
  withStatuses,
} from '../../src/helpers/roadmap-helper';

/**
 * The roadmap does NOT implement click-to-expand / collapse. The CURRENT phase
 * is always an expanded feature card; COMPLETED/AVAILABLE nodes expose a hover
 * card; LOCKED nodes have no card. Tests assert that real UI only.
 */
test.describe('Roadmap · Phase presentation (expand)', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('RM-EXP-001 — the current phase is expanded with objectives and actions', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    await openRoadmap(page, gatewayApi);

    await expect(roadmapPage.currentPhase).toBeVisible();
    await expect(roadmapPage.featureCard).toBeVisible();
    await expect(roadmapPage.objectives).toHaveCount(SAMPLE_PHASES[0].objectives.length);
    await expect(roadmapPage.completePhaseButton).toBeVisible();
    await expect(roadmapPage.continueLearningLink).toBeVisible();
  });

  test('RM-EXP-002 — non-current phases are nodes, not feature cards', async ({ page, gatewayApi, roadmapPage }) => {
    await openRoadmap(page, gatewayApi);

    await expect(roadmapPage.stops).toHaveCount(3);
    await expect(page.locator('.stop.stop--feature')).toHaveCount(1);
    await expect(roadmapPage.doneStops).toHaveCount(0);
    await expect(roadmapPage.lockedStops).toHaveCount(1);
  });

  test('RM-EXP-003 — locked phase has no hover card', async ({ page, gatewayApi, roadmapPage }) => {
    await openRoadmap(page, gatewayApi);

    const locked = roadmapPage.lockedStops.first();
    await expect(locked).toBeVisible();
    await expect(locked).not.toHaveClass(/has-card/);
    await expect(locked.locator('.stop__card')).toHaveCount(0);
  });

  test('RM-EXP-004 — available phase hover card lists the correct name and 0% progress', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    await openRoadmap(page, gatewayApi);

    const avail = page.locator('.stop.has-card').first();
    await roadmapPage.hoverStop(avail);
    await expect(avail.getByRole('heading', { name: 'Spring Boot Basics' })).toBeVisible();
    await expect(avail.locator('.pct')).toHaveText('0%');
    await expect(avail.locator('.card-status')).toContainText('Available');
  });

  test('RM-EXP-005 — completed phase hover card keeps Completed + 100%', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    const completed = {
      ...SAMPLE_ROADMAP,
      phases: withStatuses(SAMPLE_PHASES, 1),
    };
    await openRoadmap(page, gatewayApi, { roadmap: (route) => fulfillJson(route, 200, completed) });

    await expect(roadmapPage.doneStops).toHaveCount(1);
    const done = roadmapPage.doneStops.first();
    await roadmapPage.hoverStop(done);
    await expect(done.locator('.card-status')).toContainText('Completed');
    await expect(done.locator('.pct')).toHaveText('100%');
    await expect(done.getByRole('heading', { name: 'Java Foundations' })).toBeVisible();
  });

  test('RM-EXP-006 — there is no collapse control on the current phase', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    await openRoadmap(page, gatewayApi);
    await expect(roadmapPage.featureCard).toBeVisible();
    await expect(page.getByRole('button', { name: /collapse|expand/i })).toHaveCount(0);
  });

  test('RM-EXP-007 — more than 5 phases paginate; page is not persisted across refresh', async ({
    page,
    gatewayApi,
    roadmapPage,
  }) => {
    const many = {
      ...SAMPLE_ROADMAP,
      phases: Array.from({ length: 6 }, (_, i) => ({
        name: `Phase ${i + 1}`,
        description: `d${i}`,
        durationWeeks: 1,
        objectives: [`obj-${i}`],
        status: (i === 0 ? 'CURRENT' : i === 1 ? 'AVAILABLE' : 'LOCKED') as const,
      })),
    };
    await openRoadmap(page, gatewayApi, { roadmap: (route) => fulfillJson(route, 200, many) });

    await expect(roadmapPage.pager).toBeVisible();
    await expect(roadmapPage.stops).toHaveCount(5);
    await expect(roadmapPage.pagerInfo).toHaveText('1 / 2');

    await roadmapPage.clickPagerNext();
    await expect(roadmapPage.pagerInfo).toHaveText('2 / 2');
    await expect(roadmapPage.stops).toHaveCount(1);
    await expect(roadmapPage.lockedStops).toHaveCount(1);
    await expect(page.locator('.node--locked[aria-label="Phase 6 — locked"]')).toBeVisible();

    await page.reload();
    await expect(roadmapPage.pagerInfo).toHaveText('1 / 2');
    await expect(roadmapPage.heading('Phase 1')).toBeVisible();
  });

  test('RM-EXP-008 — only one current phase is expanded at a time', async ({ page, gatewayApi, roadmapPage }) => {
    await openRoadmap(page, gatewayApi);
    await expect(roadmapPage.currentPhase).toHaveCount(1);
    await expect(roadmapPage.featureCard).toHaveCount(1);
  });
});
