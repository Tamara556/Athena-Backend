import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi, seedSession, sessionStorage, readStoredToken } from '../../src/helpers/auth-helper';
import { mintExpiredToken } from '../../src/helpers/jwt';

/**
 * Session management & browser behaviour: guards, persistence, logout,
 * multi-tab sharing, back/forward, deep links, and the 401 interceptor.
 */
test.describe('Auth · Session & browser behaviour', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('AUTH-SESSION-001 — session persists across a full page reload', async ({ page, gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));

    await page.goto('/roadmap');
    await expect(page).toHaveURL(/\/roadmap$/);

    await page.reload();
    await expect(page).toHaveURL(/\/roadmap$/);
    expect(await readStoredToken(page)).toBe(auth.accessToken);
  });

  test('AUTH-SESSION-002 — guest-only route redirects a signed-in user to /roadmap', async ({ page, gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));

    await page.goto('/login');
    await expect(page).toHaveURL(/\/roadmap$/);
  });

  test('AUTH-SESSION-003 — protected route redirects a guest to /login', async ({ page }) => {
    await page.goto('/roadmap');
    await expect(page).toHaveURL(/\/login$/);
  });

  test('AUTH-SESSION-004 — logout clears the session and returns to /login', async ({ page, gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));

    await page.goto('/roadmap');
    await page.locator('button.logout').click();

    await expect(page).toHaveURL(/\/login$/);
    expect(await readStoredToken(page)).toBeNull();
  });

  test('AUTH-SESSION-005 — a second tab in the same context shares the session', async ({ page, context, gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));

    // First tab establishes the shared localStorage for this origin.
    await page.goto('/roadmap');
    await expect(page).toHaveURL(/\/roadmap$/);

    const secondTab = await context.newPage();
    await secondTab.goto('/roadmap');
    await expect(secondTab).toHaveURL(/\/roadmap$/); // authenticated via shared storage
    await secondTab.close();
  });

  test('AUTH-SESSION-006 — back/forward navigation keeps the user signed in', async ({ page, gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);
    await seedSession(page, sessionStorage(auth));

    await page.goto('/roadmap');
    await page.goto('/progress');
    await expect(page).toHaveURL(/\/progress$/);

    await page.goBack();
    await expect(page).toHaveURL(/\/roadmap$/);
    await page.goForward();
    await expect(page).toHaveURL(/\/progress$/);

    expect(await readStoredToken(page)).toBe(auth.accessToken);
  });

  test('AUTH-SESSION-007 — deep link to a protected route while logged out → /login', async ({ page }) => {
    await page.goto('/settings');
    await expect(page).toHaveURL(/\/login$/);
  });

  test('AUTH-SESSION-008 — a 401 from the API clears the session and redirects to /login', async ({ page, gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);
    // Seed a real identity but with an EXPIRED access token: the guard admits the
    // route (token present), then the roadmap's API call gets 401 → the auth
    // interceptor clears the session and redirects.
    await seedSession(page, { ...sessionStorage(auth), athena_token: mintExpiredToken({ subject: auth.userId }) });

    await page.goto('/roadmap');

    await expect(page).toHaveURL(/\/login$/);
    expect(await readStoredToken(page)).toBeNull();
  });
});
