import { type FullConfig } from '@playwright/test';
import { CONFIG } from './src/config/env';
import { isReachable } from './src/helpers/reachability';

/**
 * Probes backend reachability once and prints an environment banner so the
 * report makes clear WHY backend specs run or skip. Does not start the backend
 * (Integration/Docker scope); the frontend is started by Playwright's webServer.
 */
async function globalSetup(_config: FullConfig): Promise<void> {
  const gatewayUp = await isReachable(`${CONFIG.gatewayUrl}/actuator/health`);
  const eurekaUp = await isReachable(`${CONFIG.eurekaUrl}/actuator/health`);

  console.log('\n──────── Athena Foundation smoke — environment ────────');
  console.log(`  Frontend (webServer) : ${CONFIG.frontendUrl}`);
  console.log(`  Frontend dir         : ${CONFIG.frontendDir}`);
  console.log(`  Gateway              : ${CONFIG.gatewayUrl}  →  ${gatewayUp ? 'UP' : 'DOWN — backend specs will SKIP'}`);
  console.log(`  Eureka               : ${CONFIG.eurekaUrl}  →  ${eurekaUp ? 'UP' : 'DOWN'}`);
  console.log('───────────────────────────────────────────────────────\n');
}

export default globalSetup;
