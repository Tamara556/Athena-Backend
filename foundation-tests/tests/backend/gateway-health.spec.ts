import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';

/**
 * FND-HEALTH-007 / FND-ENV-004 (HTTP part) — the gateway is reachable and its
 * public actuator health reports UP. Skips when the stack is not running.
 */
test.describe('Foundation · Gateway health (public)', () => {
  test('FND-HEALTH-007 — public /actuator/health via gateway returns UP', async ({ gatewayApi, backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);

    const res = await gatewayApi.get('/actuator/health');
    expect(res.status()).toBe(200);

    const body = await res.json();
    expect(body.status).toBe('UP');
  });
});
