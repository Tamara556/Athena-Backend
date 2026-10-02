import { type FullConfig } from '@playwright/test';

/**
 * The frontend dev-server lifecycle is owned by Playwright's `webServer`, and
 * the backend/infra are Integration scope — so there is nothing to tear down
 * here. Hook retained as the documented seam for future artifact aggregation.
 */
async function globalTeardown(_config: FullConfig): Promise<void> {
  // no-op
}

export default globalTeardown;
