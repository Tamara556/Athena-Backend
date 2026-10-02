import { test, expect } from '../../src/fixtures/test-fixtures';
import { CONFIG } from '../../src/config/env';

/**
 * FND-CONF-007 (runtime) — proves the built `environment.apiBase` is wired into
 * the running app: submitting login dispatches the HTTP call to the configured
 * gateway origin. Independent of backend availability — we inspect the outgoing
 * request URL, not its response, so this runs even when the stack is down.
 */
test.describe('Foundation · Runtime configuration', () => {
  test('FND-CONF-007 — app targets the configured gateway base URL for API calls', async ({ loginPage, page }) => {
    await loginPage.goto();
    await loginPage.fillCredentials('smoke-check@example.com', 'placeholder-password');

    const [request] = await Promise.all([
      page.waitForRequest(
        (r) => r.url().includes('/auth/login') && r.method() === 'POST',
      ),
      loginPage.submit(),
    ]);

    expect(request.url()).toBe(`${CONFIG.gatewayUrl}/auth/login`);
  });
});
