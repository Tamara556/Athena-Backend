import { type APIRequestContext, type Page, type Route } from '@playwright/test';
import { registerViaApi, seedSession, sessionStorage, type AuthTokens } from './auth-helper';

/**
 * Deterministic canned AI responses. The onboarding flow calls a local LLM
 * (via ai-service) which is slow and nondeterministic — even returning a real
 * 503 "temporarily unavailable" — so UI-flow tests isolate it by intercepting
 * the AI endpoints with `page.route`. The frontend behaviour under test is real;
 * only the model's output is stubbed.
 */
export const CANNED = {
  start: {
    sessionId: '00000000-0000-0000-0000-0000000000aa',
    greeting: "Hello 👋 Welcome to Athena",
    firstQuestion: 'What would you like to learn?',
  },
  questions: [
    'What is your current experience with Java?',
    'Have you built REST APIs before?',
    'How comfortable are you with SQL databases?',
  ],
  roadmap: {
    goal: 'Become a backend engineer',
    level: 'beginner',
    phases: [
      { name: 'Java Foundations', description: 'Core syntax and OOP', durationWeeks: 4, objectives: ['Variables & types', 'Classes & objects'] },
      { name: 'Spring Boot Basics', description: 'REST and dependency injection', durationWeeks: 4, objectives: ['Controllers', 'Services'] },
      { name: 'Databases & JPA', description: 'Persistence with JPA', durationWeeks: 4, objectives: ['SQL basics', 'Repositories'] },
    ],
  },
  /** The real 503 contract observed from /ai/onboarding/goal. */
  unavailable: {
    status: 'TEMPORARILY_UNAVAILABLE',
    message: 'Athena is temporarily unavailable. Please try again shortly.',
    retryAvailable: true,
    retryId: '00000000-0000-0000-0000-0000000000bb',
  },
};

export type RouteHandler = (route: Route) => Promise<void> | void;

export interface OnboardingMocks {
  start?: RouteHandler;
  goal?: RouteHandler;
  assessment?: RouteHandler;
  roadmap?: RouteHandler;
}

export async function fulfillJson(route: Route, status: number, body: unknown): Promise<void> {
  await route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
}

/** A handler that responds with a fixed status + JSON body. */
export const jsonResponder =
  (status: number, body: unknown): RouteHandler =>
  (route) =>
    fulfillJson(route, status, body);

/** A handler that simulates a network failure (no response). */
export const abortResponder =
  (errorCode: string = 'failed'): RouteHandler =>
  (route) =>
    route.abort(errorCode);

/**
 * Installs deterministic routes for all four onboarding/roadmap AI endpoints.
 * Any endpoint can be overridden per-test for negative scenarios.
 */
export async function installOnboardingRoutes(page: Page, mocks: OnboardingMocks = {}): Promise<void> {
  await page.route('**/ai/onboarding/start', (route) =>
    mocks.start ? mocks.start(route) : fulfillJson(route, 201, CANNED.start),
  );
  await page.route('**/ai/onboarding/goal', (route) =>
    mocks.goal ? mocks.goal(route) : fulfillJson(route, 200, { questions: CANNED.questions }),
  );
  await page.route('**/ai/onboarding/assessment', (route) =>
    mocks.assessment ? mocks.assessment(route) : fulfillJson(route, 200, {}),
  );
  await page.route('**/ai/roadmaps/me', (route) =>
    mocks.roadmap ? mocks.roadmap(route) : fulfillJson(route, 200, CANNED.roadmap),
  );
}

/**
 * Registers a fresh user, seeds their session, installs onboarding routes (unless
 * disabled), and navigates to /onboarding. Returns the auth tokens.
 */
export async function openOnboarding(
  page: Page,
  api: APIRequestContext,
  mocks: OnboardingMocks = {},
): Promise<AuthTokens> {
  const { auth } = await registerViaApi(api);
  await seedSession(page, sessionStorage(auth));
  await installOnboardingRoutes(page, mocks);
  await page.goto('/onboarding');
  return auth;
}
