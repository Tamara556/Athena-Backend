import { type Page, type Locator } from '@playwright/test';

/**
 * Page Object for `/dashboard` (DashboardComponent) — the only page that
 * renders both the latest roadmap and the read-only daily plan together.
 */
export class DashboardPage {
  readonly page: Page;
  readonly heading: Locator;
  readonly roadmapCard: Locator;
  readonly planCard: Locator;
  readonly roadmapMessage: Locator;
  readonly planMessage: Locator;
  readonly planItems: Locator;
  readonly phaseCards: Locator;

  constructor(page: Page) {
    this.page = page;
    this.heading = page.getByRole('heading', { name: 'Dashboard' });
    this.roadmapCard = page.locator('.card', { has: page.getByRole('heading', { name: /Roadmap/ }) });
    this.planCard = page.locator('.card', { has: page.getByRole('heading', { name: /Today's plan/ }) });
    this.roadmapMessage = this.roadmapCard.locator('p.muted');
    this.planMessage = this.planCard.locator('p.muted').first();
    this.planItems = this.planCard.locator('.sub-card');
    this.phaseCards = this.roadmapCard.locator('.sub-card');
  }

  async goto(): Promise<void> {
    await this.page.goto('/dashboard');
  }
}
