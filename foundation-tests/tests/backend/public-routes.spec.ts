import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';

const GATEWAY_MISSING_AUTH_MESSAGE = 'Missing or malformed Authorization header';

/**
 * Gateway security posture at the edge (no browser needed).
 *   FND-GW-008 — public auth routes need no bearer token.
 *   FND-GW-004 — protected routes without a token are rejected at the gateway.
 */
test.describe('Foundation · Gateway routing & auth', () => {
  test('FND-GW-008 — public /auth/login is NOT blocked by the JWT filter', async ({ gatewayApi, backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);

    const res = await gatewayApi.post('/auth/login', {
      data: { login: 'no-such-user', password: 'wrong-password' },
      failOnStatusCode: false,
    });

    // The request must reach auth-service business logic, not be rejected by the
    // gateway for a missing bearer. Distinguish by the gateway's signature body.
    const body = await res.text();
    expect(body).not.toContain(GATEWAY_MISSING_AUTH_MESSAGE);
  });

  test('FND-GW-004 — protected route without token → 401 from the gateway', async ({ gatewayApi, backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);

    const res = await gatewayApi.get('/users/me', { failOnStatusCode: false });
    expect(res.status()).toBe(401);

    const body = await res.json();
    expect(body.error).toBe('Unauthorized');
    expect(body.message).toContain(GATEWAY_MISSING_AUTH_MESSAGE);
  });
});
