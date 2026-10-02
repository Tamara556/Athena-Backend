import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi, readStoredToken } from '../../src/helpers/auth-helper';

/**
 * Login in the browser (LoginComponent): happy-path sign-in + redirect,
 * invalid credentials, client-side empty guard, and loading state.
 */
test.describe('Auth · Login UI', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('AUTH-LOGINUI-001 — valid login signs in and redirects to /roadmap', async ({ loginPage, page, gatewayApi }) => {
    const { user } = await registerViaApi(gatewayApi);

    await loginPage.goto();
    await loginPage.login(user.username, user.password);

    await page.waitForURL('**/roadmap');
    expect(await readStoredToken(page)).toBeTruthy();
  });

  test('AUTH-LOGINUI-002 — invalid credentials show the server error and stay on /login', async ({ loginPage, page, gatewayApi }) => {
    const { user } = await registerViaApi(gatewayApi);

    await loginPage.goto();
    await loginPage.login(user.username, 'Wrong1!Pass');

    await expect(loginPage.errorAlert).toBeVisible();
    await expect(loginPage.errorAlert).toContainText('Invalid login or password');
    await expect(page).toHaveURL(/\/login$/);
  });

  test('AUTH-LOGINUI-003 — empty credentials are blocked client-side (no backend call)', async ({ loginPage, page }) => {
    let loginCalls = 0;
    page.on('request', (r) => {
      if (r.url().includes('/auth/login')) loginCalls++;
    });

    await loginPage.goto();
    await loginPage.submit();

    await expect(loginPage.errorAlert).toBeVisible();
    await expect(loginPage.errorAlert).toContainText('Enter your email or username and password');
    await expect(page).toHaveURL(/\/login$/);
    expect(loginCalls).toBe(0);
  });

  test('AUTH-LOGINUI-004 — submit shows a loading state while the request is in flight', async ({ loginPage, page, gatewayApi }) => {
    const { user } = await registerViaApi(gatewayApi);

    let release!: () => void;
    const gate = new Promise<void>((resolve) => (release = resolve));
    await page.route('**/auth/login', async (route) => {
      await gate;
      await route.continue();
    });

    await loginPage.goto();
    await loginPage.login(user.username, user.password);

    await expect(loginPage.submitButton).toHaveClass(/loading/);
    release();
    await page.waitForURL('**/roadmap');
  });
});
