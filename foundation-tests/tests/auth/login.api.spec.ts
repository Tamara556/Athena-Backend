import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { loginViaApi, registerViaApi } from '../../src/helpers/auth-helper';

/**
 * Login contract (POST /auth/login, JSON {login, password}).
 */
test.describe('Auth · Login API', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('AUTH-LOGIN-001 — valid credentials (by username) → 200 with tokens', async ({ gatewayApi }) => {
    const { user } = await registerViaApi(gatewayApi);
    const auth = await loginViaApi(gatewayApi, user.username, user.password);

    expect(auth.accessToken).toBeTruthy();
    expect(auth.refreshToken).toBeTruthy();
    expect(auth.roles).toEqual(['USER']);
  });

  test('AUTH-LOGIN-002 — login by email also works', async ({ gatewayApi }) => {
    const { user } = await registerViaApi(gatewayApi);
    const auth = await loginViaApi(gatewayApi, user.email, user.password);
    expect(auth.userId).toBeTruthy();
  });

  test('AUTH-LOGIN-003 — wrong password → 401 Invalid login or password', async ({ gatewayApi }) => {
    const { user } = await registerViaApi(gatewayApi);

    const res = await gatewayApi.post('/auth/login', {
      data: { login: user.username, password: 'Wrong1!Pass' },
      failOnStatusCode: false,
    });

    expect(res.status()).toBe(401);
    const body = await res.json();
    expect(body.message).toBe('Invalid login or password');
  });

  test('AUTH-LOGIN-004 — unknown user → 401 (no account enumeration)', async ({ gatewayApi }) => {
    const res = await gatewayApi.post('/auth/login', {
      data: { login: 'definitely-no-such-user-xyz', password: 'Whatever1!' },
      failOnStatusCode: false,
    });

    expect(res.status()).toBe(401);
    const body = await res.json();
    // Same generic message as wrong-password: does not reveal whether the user exists.
    expect(body.message).toBe('Invalid login or password');
  });

  test('AUTH-LOGIN-005 — empty credentials → 400 validation', async ({ gatewayApi }) => {
    const res = await gatewayApi.post('/auth/login', {
      data: { login: '', password: '' },
      failOnStatusCode: false,
    });

    expect(res.status()).toBe(400);
    const body = await res.json();
    expect(body.message).toBe('Validation failed for one or more fields');
  });
});
