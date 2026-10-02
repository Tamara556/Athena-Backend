import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';
import { registerViaApi } from '../../src/helpers/auth-helper';

/**
 * Authorization & identity propagation at the gateway edge.
 */
test.describe('Auth · Authorization', () => {
  test.beforeEach(({ backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);
  });

  test('AUTH-AUTHZ-001 — protected route without token → 401', async ({ gatewayApi }) => {
    const res = await gatewayApi.get('/account/me', { failOnStatusCode: false });
    expect(res.status()).toBe(401);
    expect((await res.json()).error).toBe('Unauthorized');
  });

  test('AUTH-AUTHZ-002 — protected route with valid token → 200', async ({ gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);
    const res = await gatewayApi.get('/account/me', {
      headers: { Authorization: `Bearer ${auth.accessToken}` },
    });
    expect(res.status()).toBe(200);
  });

  test('AUTH-AUTHZ-003 — public route is reachable without a token', async ({ gatewayApi }) => {
    const res = await gatewayApi.post('/auth/login', {
      data: { login: 'nobody', password: 'wrong' },
      failOnStatusCode: false,
    });
    // Reaches auth-service (401 invalid creds) — NOT blocked by the JWT filter.
    const body = await res.text();
    expect(body).not.toContain('Missing or malformed Authorization header');
  });

  test('AUTH-AUTHZ-004 — every registered user carries exactly the USER role', async ({ gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);
    expect(auth.roles).toEqual(['USER']);
  });

  test('AUTH-AUTHZ-005 — client cannot spoof identity: gateway overwrites X-User-Id from the token', async ({ gatewayApi }) => {
    const { auth } = await registerViaApi(gatewayApi);

    const res = await gatewayApi.get('/account/me', {
      headers: {
        Authorization: `Bearer ${auth.accessToken}`,
        // Forged identity headers — the gateway must replace these from the JWT.
        'X-User-Id': '00000000-0000-0000-0000-000000000000',
        'X-User-Roles': 'ADMIN',
      },
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.userId).toBe(auth.userId); // resolved from token, not the forged header
  });

  // Role-gated authorization (403 Forbidden) is NOT implemented: the Role enum has
  // ADMIN, but registration only ever assigns USER and no endpoint requires a role.
  // Documented as unreachable rather than invented.
  test('AUTH-AUTHZ-006 — role-based 403 Forbidden (no role-gated endpoint exists)', async () => {
    test.skip(true, 'No role-gated endpoint in the codebase; ADMIN is unreachable via registration.');
  });
});
