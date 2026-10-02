import { type APIRequestContext, type Page, expect } from '@playwright/test';

/** A valid registration payload (server rules: username ^[A-Za-z0-9._-]{3,50}$,
 *  password 8-72 with upper+lower+digit+symbol). */
export interface TestUser {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
}

/** Shape of AuthResponse returned by /auth/register, /auth/login, /auth/refresh. */
export interface AuthTokens {
  userId: string;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  roles: string[];
  tokenType: string;
  accessToken: string;
  refreshToken: string;
  expiresIn: number;
  imageName: string | null;
  twoFactorRequired: boolean;
  challengeToken: string | null;
  sessionId: string | null;
}

let counter = 0;

/** Collision-resistant, valid user. Override any field for negative cases. */
export function uniqueUser(overrides: Partial<TestUser> = {}): TestUser {
  const tag = `${Date.now().toString(36)}${(counter++).toString(36)}${Math.floor(
    Math.random() * 1_000_000,
  ).toString(36)}`;
  return {
    firstName: 'Test',
    lastName: 'User',
    username: `pw_${tag}`.slice(0, 50),
    email: `pw_${tag}@example.com`,
    password: 'Str0ng!Pass',
    ...overrides,
  };
}

/** Registers a fresh user via the API (multipart) and returns the tokens. */
export async function registerViaApi(
  api: APIRequestContext,
  user: TestUser = uniqueUser(),
): Promise<{ user: TestUser; auth: AuthTokens }> {
  const res = await api.post('/auth/register', {
    multipart: {
      firstName: user.firstName,
      lastName: user.lastName,
      username: user.username,
      email: user.email,
      password: user.password,
    },
  });
  expect(res.status(), await res.text()).toBe(201);
  return { user, auth: (await res.json()) as AuthTokens };
}

/** Logs in via the API and returns the tokens. */
export async function loginViaApi(
  api: APIRequestContext,
  login: string,
  password: string,
): Promise<AuthTokens> {
  const res = await api.post('/auth/login', { data: { login, password } });
  expect(res.status(), await res.text()).toBe(200);
  return (await res.json()) as AuthTokens;
}

/**
 * The localStorage entries the frontend Session writes on sign-in. Mirrors
 * Athena-Frontend/src/app/core/session.ts (only the access token is persisted —
 * the refresh token is intentionally NOT stored client-side).
 */
export function sessionStorage(auth: AuthTokens): Record<string, string> {
  const entries: Record<string, string> = {
    athena_token: auth.accessToken,
    athena_userId: auth.userId,
  };
  if (auth.sessionId) entries.athena_sessionId = auth.sessionId;
  const name =
    [auth.firstName, auth.lastName].filter(Boolean).join(' ').trim() || auth.username;
  if (name) entries.athena_name = name;
  return entries;
}

/**
 * Seeds a signed-in session BEFORE the Angular app bootstraps. `addInitScript`
 * runs on every navigation in the page, so the Session signals read the seeded
 * localStorage at construction — no reload race.
 */
export async function seedSession(
  page: Page,
  entries: Record<string, string>,
): Promise<void> {
  await page.addInitScript((kv: Record<string, string>) => {
    for (const [key, value] of Object.entries(kv)) {
      window.localStorage.setItem(key, value);
    }
  }, entries);
}

/** Reads the persisted session token straight from the browser. */
export async function readStoredToken(page: Page): Promise<string | null> {
  return page.evaluate(() => window.localStorage.getItem('athena_token'));
}
