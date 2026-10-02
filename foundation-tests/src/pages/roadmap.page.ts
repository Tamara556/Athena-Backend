import { type Page, type Locator } from '@playwright/test';

/**
 * Page Object for the generated roadmap (route `/roadmap`, RoadmapComponent).
 * Renders either the roadmap journey (when the backend has one) or an empty
 * "start onboarding" state (404 from /ai/roadmaps/me).
 *
 * Actual UI (do not invent expand/collapse): the CURRENT phase is always an
 * expanded feature card; other phases are nodes with hover cards (locked has none).
 */
export class RoadmapPage {
  readonly page: Page;
  readonly emptySection: Locator;
  readonly startOnboardingLink: Locator;
  readonly stops: Locator;
  readonly currentPhase: Locator;
  readonly currentPill: Locator;
  readonly continueLearningLink: Locator;
  readonly completePhaseButton: Locator;
  readonly featureCard: Locator;
  readonly objectives: Locator;
  readonly lockedStops: Locator;
  readonly doneStops: Locator;
  readonly availableStops: Locator;
  readonly pager: Locator;
  readonly pagerPrev: Locator;
  readonly pagerNext: Locator;
  readonly pagerInfo: Locator;
  readonly greeting: Locator;
  readonly goalText: Locator;
  readonly phasesStat: Locator;
  readonly milestonesStat: Locator;
  readonly weeksStat: Locator;
  readonly levelStat: Locator;
  readonly insight: Locator;
  readonly stats: Locator;

  constructor(page: Page) {
    this.page = page;
    this.emptySection = page.locator('.empty');
    this.startOnboardingLink = page.locator('.empty a[href="/onboarding"]');
    this.stops = page.locator('.journey > .stop');
    this.currentPhase = page.locator('.stop.is-current');
    this.currentPill = page.locator('.live-pill');
    this.continueLearningLink = page.locator('a[href="/learning/current"]').first();
    this.completePhaseButton = page.locator('button.rm-complete');
    this.featureCard = page.locator('.feature-card');
    this.objectives = page.locator('.obj-block li');
    this.lockedStops = page.locator('.stop.is-locked');
    this.doneStops = page.locator('.stop.is-done');
    this.availableStops = page.locator('.stop.has-card:not(.is-done):not(.is-current)');
    this.pager = page.locator('.pager');
    this.pagerPrev = page.getByRole('button', { name: 'Previous phases' });
    this.pagerNext = page.getByRole('button', { name: 'Next phases' });
    this.pagerInfo = page.locator('.pager-info');
    this.greeting = page.locator('.greet h1');
    this.goalText = page.locator('.greet p b');
    this.stats = page.locator('.stats');
    this.phasesStat = page.locator('.stats .stat').nth(0).locator('.stat-num');
    this.milestonesStat = page.locator('.stats .stat').nth(1).locator('.stat-num');
    this.weeksStat = page.locator('.stats .stat').nth(2).locator('.stat-num');
    this.levelStat = page.locator('.stats .stat').nth(3).locator('.stat-num');
    this.insight = page.locator('.insight');
  }

  async goto(): Promise<void> {
    await this.page.goto('/roadmap');
  }

  heading(name: string): Locator {
    return this.page.getByRole('heading', { name });
  }

  stopByName(name: string): Locator {
    return this.page.locator('.stop', { has: this.page.getByRole('heading', { name }) });
  }

  regenerateControl(): Locator {
    return this.page.getByRole('button', { name: /regenerat/i });
  }

  /** Hover a non-current stop. Force avoids the expanded current card intercepting WebKit. */
  async hoverStop(stop: Locator): Promise<void> {
    await stop.hover({ force: true });
  }

  /**
   * WebKit keeps many Athena controls in a CSS-transition "unstable" state.
   * Force skips actionability so the click still lands on the real control.
   */
  async forceClick(locator: Locator): Promise<void> {
    await locator.waitFor({ state: 'visible' });
    await locator.click({ force: true });
  }

  async clickCompletePhase(): Promise<void> {
    await this.forceClick(this.completePhaseButton);
  }

  async clickContinueLearning(): Promise<void> {
    await this.forceClick(this.continueLearningLink);
  }

  async clickStartOnboarding(): Promise<void> {
    await this.forceClick(this.startOnboardingLink);
  }

  async clickPagerNext(): Promise<void> {
    await this.forceClick(this.pagerNext);
  }

  async clickNav(href: string): Promise<void> {
    await this.forceClick(this.page.locator(`a.nav-item[href="${href}"]`));
  }
}
