import { defineConfig, devices } from '@playwright/test';
import { CONFIG } from './src/config/env';

/**
 * Athena Foundation smoke suite — Phase 1.1.
 *
 * Playwright owns ONLY the frontend dev-server lifecycle (via `webServer`).
 * The backend stack (gateway, microservices, Kafka, Redis, Postgres, Eureka)
 * is Integration/Docker scope and is expected to be started separately; specs
 * that need it skip gracefully when it is unreachable (see BACKEND_SKIP_REASON).
 */
export default defineConfig({
  testDir: './tests',
  outputDir: './test-results',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  // Two retries absorb transient infra variance in real-backend E2E — the
  // slowest engine (WebKit) can exceed a click/nav timeout when the shared dev
  // server + LLM-backed stack are under load. A genuinely broken test fails all
  // attempts; a flaky-passed test is reported as such.
  retries: 2,
  // A single `ng serve` dev server compiles lazy chunks on demand and cannot
  // absorb one worker per core across three browsers — cap concurrency so page
  // loads stay fast and deterministic instead of timing out under contention.
  workers: process.env.CI ? 2 : 4,
  timeout: 90_000,
  expect: { timeout: 12_000 },

  reporter: [
    ['list'],
    ['html', { outputFolder: 'playwright-report', open: 'never' }],
    ['json', { outputFile: 'test-results/results.json' }],
  ],

  globalSetup: require.resolve('./global-setup'),
  globalTeardown: require.resolve('./global-teardown'),

  use: {
    baseURL: CONFIG.frontendUrl,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 20_000,
    navigationTimeout: 45_000,
    contextOptions: { reducedMotion: 'reduce' },
  },

  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
  ],

  // Frontend lifecycle only. Reuses an already-running dev server locally.
  webServer: {
    command: 'npm start',
    cwd: CONFIG.frontendDir,
    url: CONFIG.frontendUrl,
    reuseExistingServer: !process.env.CI,
    timeout: 240_000,
    stdout: 'pipe',
    stderr: 'pipe',
    env: { NG_CLI_ANALYTICS: 'false' },
  },
});
