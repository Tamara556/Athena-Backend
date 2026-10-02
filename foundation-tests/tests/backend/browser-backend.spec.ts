import { test, expect } from '../../src/fixtures/test-fixtures';
import { CONFIG, BACKEND_SKIP_REASON } from '../../src/config/env';

/**
 * Browser ↔ backend integration at the edge (needs both frontend origin + stack).
 *   FND-START-006 — the browser can reach the gateway from the app.
 *   FND-GW-010   — CORS allows the app origin (http://localhost:4200) to read
 *                  gateway responses via a real cross-origin fetch.
 */
test.describe('Foundation · Browser ↔ backend (CORS)', () => {
  test('FND-START-006 / FND-GW-010 — app origin can fetch the gateway (CORS allowed)', async ({ page, backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);

    // Establish the real browser origin (http://localhost:4200) before fetching.
    await page.goto('/');

    const result = await page.evaluate(async (gatewayUrl) => {
      // A cross-origin fetch that the browser will only allow the page to read
      // if the gateway returns matching CORS headers for this origin.
      const r = await fetch(`${gatewayUrl}/actuator/health`, { method: 'GET' });
      return { status: r.status, body: await r.text() };
    }, CONFIG.gatewayUrl);

    expect(result.status).toBe(200);
    expect(result.body).toContain('UP');
  });
});
