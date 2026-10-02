import {
  test as base,
  expect,
  request,
  type APIRequestContext,
} from '@playwright/test';
import { CONFIG } from '../config/env';
import { LandingPage } from '../pages/landing.page';
import { LoginPage } from '../pages/login.page';
import { RegisterPage } from '../pages/register.page';
import { OnboardingPage } from '../pages/onboarding.page';
import { RoadmapPage } from '../pages/roadmap.page';
import { LearningSessionPage } from '../pages/learning-session.page';
import { DashboardPage } from '../pages/dashboard.page';

type TestFixtures = {
  landingPage: LandingPage;
  loginPage: LoginPage;
  registerPage: RegisterPage;
  onboardingPage: OnboardingPage;
  roadmapPage: RoadmapPage;
  learningSessionPage: LearningSessionPage;
  dashboardPage: DashboardPage;
};

type WorkerFixtures = {
  /** HTTP client bound to the gateway base URL (reused across a worker). */
  gatewayApi: APIRequestContext;
  /** HTTP client bound to the Eureka base URL. */
  eurekaApi: APIRequestContext;
  /** Probed once per worker: is the backend gateway up? Gates backend specs. */
  backendAvailable: boolean;
};

export const test = base.extend<TestFixtures, WorkerFixtures>({
  gatewayApi: [
    async ({}, use) => {
      const ctx = await request.newContext({ baseURL: CONFIG.gatewayUrl });
      await use(ctx);
      await Promise.race([ctx.dispose(), new Promise((r) => setTimeout(r, 5_000))]);
    },
    { scope: 'worker' },
  ],

  eurekaApi: [
    async ({}, use) => {
      const ctx = await request.newContext({ baseURL: CONFIG.eurekaUrl });
      await use(ctx);
      await Promise.race([ctx.dispose(), new Promise((r) => setTimeout(r, 5_000))]);
    },
    { scope: 'worker' },
  ],

  backendAvailable: [
    async ({ gatewayApi }, use) => {
      let up = false;
      try {
        const res = await gatewayApi.get('/actuator/health', {
          timeout: CONFIG.backendProbeTimeoutMs,
          failOnStatusCode: false,
        });
        up = res.ok();
      } catch {
        up = false;
      }
      await use(up);
    },
    { scope: 'worker' },
  ],

  landingPage: async ({ page }, use) => {
    await use(new LandingPage(page));
  },

  loginPage: async ({ page }, use) => {
    await use(new LoginPage(page));
  },

  registerPage: async ({ page }, use) => {
    await use(new RegisterPage(page));
  },

  onboardingPage: async ({ page }, use) => {
    await use(new OnboardingPage(page));
  },

  roadmapPage: async ({ page }, use) => {
    await use(new RoadmapPage(page));
  },

  learningSessionPage: async ({ page }, use) => {
    await use(new LearningSessionPage(page));
  },

  dashboardPage: async ({ page }, use) => {
    await use(new DashboardPage(page));
  },
});

// WebKit on Windows can keep a worker alive after stop if page JS (Angular
// intervals / pollers) is still running. Blank the page so teardown can exit.
test.afterEach(async ({ page }) => {
  await page.goto('about:blank').catch(() => undefined);
});

export { expect };
