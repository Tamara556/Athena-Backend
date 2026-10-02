import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi } from '../../src/helpers/auth-helper';
import { mintExpiredToken, mintWrongSignatureToken } from '../../src/helpers/jwt';

const bearer = (token: string) => ({ headers: { Authorization: `Bearer ${token}` } });
const GATEWAY_REJECTION = 'Invalid or expired access token';
const MISSING_AUTH = 'Missing or malformed Authorization header';

/**
 * JWT lifecycle at the edge: access-token acceptance, refresh rotation, and the
 * four rejection modes (expired, wrong-signature, wrong-type, missing).
 */
test.describe('Auth · JWT & refresh', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('AUTH-JWT-001 — valid access token reaches a protected resource', async ({ gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);

    const res = await gatewayApi.get('/account/me', bearer(auth.accessToken));
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.userId).toBe(auth.userId);
  });

  test('AUTH-JWT-002 — refresh returns a new, working access token', async ({ gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);

    const res = await gatewayApi.post('/auth/refresh', {
      data: { refreshToken: auth.refreshToken },
    });
    expect(res.status()).toBe(200);
    const refreshed = await res.json();
    expect(refreshed.accessToken).toBeTruthy();

    const check = await gatewayApi.get('/account/me', bearer(refreshed.accessToken));
    expect(check.status()).toBe(200);
  });

  test('AUTH-JWT-003 — refresh with an invalid token → 401', async ({ gatewayApi }) => {
    const res = await gatewayApi.post('/auth/refresh', {
      data: { refreshToken: 'not.a.valid.refresh.token' },
      failOnStatusCode: false,
    });
    expect(res.status()).toBe(401);
    const body = await res.json();
    expect(body.message).toBe('Refresh token is invalid or expired');
  });

  test('AUTH-JWT-004 — refresh with missing token → 400 validation', async ({ gatewayApi }) => {
    const res = await gatewayApi.post('/auth/refresh', {
      data: {},
      failOnStatusCode: false,
    });
    expect(res.status()).toBe(400);
  });

  test('AUTH-JWT-005 — expired access token → 401 at the gateway', async ({ gatewayApi }) => {
    const res = await gatewayApi.get('/account/me', {
      ...bearer(mintExpiredToken()),
      failOnStatusCode: false,
    });
    expect(res.status()).toBe(401);
    const body = await res.json();
    expect(body.message).toBe(GATEWAY_REJECTION);
  });

  test('AUTH-JWT-006 — token with wrong signature → 401', async ({ gatewayApi }) => {
    const res = await gatewayApi.get('/account/me', {
      ...bearer(mintWrongSignatureToken()),
      failOnStatusCode: false,
    });
    expect(res.status()).toBe(401);
  });

  test('AUTH-JWT-007 — refresh token used as access token → 401 (wrong type)', async ({ gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);

    const res = await gatewayApi.get('/account/me', {
      ...bearer(auth.refreshToken),
      failOnStatusCode: false,
    });
    expect(res.status()).toBe(401);
    const body = await res.json();
    expect(body.message).toBe(GATEWAY_REJECTION);
  });

  test('AUTH-JWT-008 — missing token → 401 with distinct gateway message', async ({ gatewayApi }) => {
    const res = await gatewayApi.get('/account/me', { failOnStatusCode: false });
    expect(res.status()).toBe(401);
    const body = await res.json();
    expect(body.message).toContain(MISSING_AUTH);
  });
});
