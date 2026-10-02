import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi, uniqueUser, readStoredToken } from '../../src/helpers/auth-helper';

/**
 * Registration in the browser (RegisterComponent): client-side validation,
 * happy-path sign-in + redirect, server error surfacing, and loading state.
 */
test.describe('Auth · Registration UI', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('AUTH-REGUI-001 — valid registration signs in and redirects to /onboarding', async ({ registerPage, page }) => {
    await registerPage.goto();
    await registerPage.register(uniqueUser());

    await expect(registerPage.successAlert).toBeVisible();
    await page.waitForURL('**/onboarding');
    expect(await readStoredToken(page)).toBeTruthy();
  });

  test('AUTH-REGUI-002 — empty submit shows client error and calls no backend', async ({ registerPage, page }) => {
    let registerCalls = 0;
    page.on('request', (r) => {
      if (r.url().includes('/auth/register')) registerCalls++;
    });

    await registerPage.goto();
    // Accept terms first so the client-side error is the "complete the fields"
    // path (the terms gate is reported ahead of empty fields).
    await registerPage.acceptTerms();
    await registerPage.submit();

    await expect(registerPage.errorAlert).toBeVisible();
    await expect(registerPage.errorAlert).toContainText('complete the highlighted fields');
    await expect(page).toHaveURL(/\/register$/);
    expect(registerCalls).toBe(0);
  });

  test('AUTH-REGUI-003 — valid fields but terms unchecked is blocked client-side', async ({ registerPage, page }) => {
    await registerPage.goto();
    await registerPage.fillForm(uniqueUser());
    await registerPage.submit(); // terms NOT accepted

    await expect(registerPage.errorAlert).toBeVisible();
    await expect(registerPage.errorAlert).toContainText('Terms of Service');
    await expect(page).toHaveURL(/\/register$/);
  });

  test('AUTH-REGUI-004 — invalid email is rejected client-side', async ({ registerPage, page }) => {
    await registerPage.goto();
    await registerPage.fillForm(uniqueUser({ email: 'not-an-email' }));
    await registerPage.acceptTerms();
    await registerPage.submit();

    await expect(registerPage.errorAlert).toBeVisible();
    await expect(page).toHaveURL(/\/register$/);
  });

  test('AUTH-REGUI-005 — mismatched confirm password is rejected client-side', async ({ registerPage, page }) => {
    const user = uniqueUser();
    await registerPage.goto();
    await registerPage.fillForm(user, 'Different1!Pass');
    await registerPage.acceptTerms();
    await registerPage.submit();

    await expect(registerPage.confirmMsg).toBeVisible();
    await expect(registerPage.errorAlert).toBeVisible();
    await expect(page).toHaveURL(/\/register$/);
  });

  test('AUTH-REGUI-006 — duplicate email surfaces the server error', async ({ registerPage, gatewayApi }) => {
    const { user } = await registerViaApi(gatewayApi);

    await registerPage.goto();
    await registerPage.register(uniqueUser({ email: user.email }));

    await expect(registerPage.errorAlert).toBeVisible();
    await expect(registerPage.errorAlert).toContainText('account with this email already exists');
  });

  test('AUTH-REGUI-007 — submit shows a loading state while the request is in flight', async ({ registerPage, page }) => {
    let release!: () => void;
    const gate = new Promise<void>((resolve) => (release = resolve));
    await page.route('**/auth/register', async (route) => {
      await gate; // hold the response so the loading state is observable deterministically
      await route.continue();
    });

    await registerPage.goto();
    await registerPage.register(uniqueUser());

    await expect(registerPage.submitButton).toHaveClass(/loading/);
    release();
    await page.waitForURL('**/onboarding');
  });
});
