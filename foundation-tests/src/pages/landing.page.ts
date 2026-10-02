import { type Page, type Locator, expect } from '@playwright/test';

/**
 * Page Object for the public landing page (route `/`, HomeComponent).
 * Selectors are anchored to stable, user-visible content in
 * Athena-Frontend/src/app/pages/home/home.component.html.
 */
export class LandingPage {
  readonly page: Page;
  readonly nav: Locator;
  readonly brandWordmark: Locator;
  readonly heroHeading: Locator;
  readonly heroSubtitle: Locator;
  readonly loginLink: Locator;
  readonly getStartedCta: Locator;
  readonly featuresNavLink: Locator;

  constructor(page: Page) {
    this.page = page;
    this.nav = page.locator('#nav');
    this.brandWordmark = this.nav.locator('.brand .wordmark');
    this.heroHeading = page.getByRole('heading', { level: 1 });
    this.heroSubtitle = page.locator('.hero-sub');
    this.loginLink = this.nav.getByRole('link', { name: 'Log in' });
    this.getStartedCta = this.nav.getByRole('link', { name: 'Get Started' });
    this.featuresNavLink = page.locator('.nav-links').getByRole('link', { name: 'Features' });
  }

  async goto(): Promise<void> {
    await this.page.goto('/');
  }

  /** Asserts Angular bootstrapped and rendered the landing content. */
  async expectRendered(): Promise<void> {
    await expect(this.page).toHaveTitle(/Athena/i);
    await expect(this.nav).toBeVisible();
    await expect(this.brandWordmark).toHaveText(/athen/i);
    await expect(this.heroHeading).toContainText('Learn Anything');
    await expect(this.heroSubtitle).toContainText(/learning operating system/i);
    await expect(this.loginLink).toBeVisible();
    await expect(this.getStartedCta).toBeVisible();
    await expect(this.featuresNavLink).toBeVisible();
  }
}
