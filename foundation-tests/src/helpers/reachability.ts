import { request } from '@playwright/test';
import { CONFIG } from '../config/env';

/**
 * True if `url` answers any HTTP status within the timeout. Used to gate
 * backend-dependent specs deterministically (no arbitrary sleeps): if the stack
 * is down the probe fails fast and dependent specs skip with a clear reason.
 */
export async function isReachable(
  url: string,
  timeoutMs: number = CONFIG.backendProbeTimeoutMs,
): Promise<boolean> {
  const ctx = await request.newContext();
  try {
    const res = await ctx.get(url, { timeout: timeoutMs, failOnStatusCode: false });
    return res.status() > 0;
  } catch {
    return false;
  } finally {
    await ctx.dispose();
  }
}
