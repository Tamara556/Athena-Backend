import { test, expect } from '../../src/fixtures/test-fixtures';

/**
 * Frontend shell smoke checks — run for real against the ng-serve dev server.
 * Covers FND-START-005 (frontend serves shell) and "landing page renders".
 */
test.describe('Foundation · Frontend shell', () => {
  test('FND-START-005 — landing page loads and Angular bootstraps', async ({ landingPage, page }) => {
    await landingPage.goto();

    await expect(page).toHaveTitle(/Athena/i);
    // app-root exists AND Angular rendered real content into it (not a blank shell).
    await expect(page.locator('app-root')).toBeAttached();
    await expect(landingPage.nav).toBeVisible();
  });

  test('Landing page renders hero, nav brand and primary CTAs', async ({ landingPage }) => {
    await landingPage.goto();
    await landingPage.expectRendered();
  });

  test('Landing → "Log in" navigates to the login route', async ({ landingPage, loginPage, page }) => {
    await landingPage.goto();
    await landingPage.loginLink.click();

    await expect(page).toHaveURL(/\/login$/);
    await expect(loginPage.heading).toBeVisible();
    await expect(loginPage.loginInput).toBeVisible();
    await expect(loginPage.passwordInput).toBeVisible();
  });
});
