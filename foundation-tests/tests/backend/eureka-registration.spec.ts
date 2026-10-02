import { test, expect } from '../../src/fixtures/test-fixtures';
import { BACKEND_SKIP_REASON } from '../../src/config/env';

/**
 * FND-EUREKA-001 / FND-EUREKA-002 — core services are registered in Eureka and
 * the registry does not self-register. Read-only HTTP against /eureka/apps.
 */
const EXPECTED_APPS = [
  'API-GATEWAY',
  'AUTH-SERVICE',
  'USER-SERVICE',
  'PROGRESS-SERVICE',
  'LEARNING-SERVICE',
  'BADGE-SERVICE',
  'AI-SERVICE',
  'INTERVIEW-SERVICE',
  'RAG-SERVICE',
];

test.describe('Foundation · Eureka registration', () => {
  test('FND-EUREKA-001/002 — all core services registered; discovery not self-registered', async ({ eurekaApi, backendAvailable }) => {
    test.skip(!backendAvailable, BACKEND_SKIP_REASON);

    const res = await eurekaApi.get('/eureka/apps', {
      headers: { Accept: 'application/json' },
    });
    expect(res.status()).toBe(200);

    const json = await res.json();
    const registered: string[] = (json.applications?.application ?? []).map(
      (a: { name: string }) => a.name.toUpperCase(),
    );

    for (const app of EXPECTED_APPS) {
      expect(registered, `expected ${app} registered in Eureka`).toContain(app);
    }
    // Discovery server has register-with-eureka=false.
    expect(registered).not.toContain('DISCOVERY-SERVER');
  });
});
