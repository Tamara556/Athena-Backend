import { execFile } from 'node:child_process';
import { randomUUID } from 'node:crypto';
import { promisify } from 'node:util';
import { type APIRequestContext, type Page, type Route, expect } from '@playwright/test';
import { CONFIG } from '../config/env';
import { fulfillJson, type RouteHandler } from './onboarding-helper';
import { registerViaApi, seedSession, sessionStorage, type AuthTokens } from './auth-helper';

const execFileAsync = promisify(execFile);

/** Phase statuses persisted by ai-service (RoadmapServiceImpl / AiGenerationServiceImpl). */
export type PhaseStatus = 'COMPLETED' | 'CURRENT' | 'AVAILABLE' | 'LOCKED';

export interface PhaseFixture {
  name: string;
  description: string;
  durationWeeks: number;
  objectives: string[];
  status: PhaseStatus;
}

export interface RoadmapFixture {
  id: string;
  goal: string;
  level: string;
  phases: PhaseFixture[];
  createdAt: string;
}

export interface DailyPlanFixture {
  id: string;
  date: string;
  items: { type: string; title: string; description: string; estimatedMinutes: number }[];
  createdAt: string;
}

export interface LearningSessionFixture {
  id: string;
  roadmapId: string;
  roadmapNodeId: string;
  nodeIndex: number;
  title: string;
  status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED';
  estimatedMinutes: number;
  generatedAt: string;
  updatedAt: string;
  readings: { id: string; title: string; content: string; estimatedMinutes: number; orderIndex: number }[];
  watchings: {
    id: string;
    title: string;
    description: string;
    videoQuery: string;
    videoId: string | null;
    estimatedMinutes: number;
    orderIndex: number;
  }[];
  practices: {
    id: string;
    title: string;
    description: string;
    practiceType: string;
    instructions: string;
    starterContent: string;
    estimatedMinutes: number;
    orderIndex: number;
  }[];
  quizzes: {
    id: string;
    question: string;
    type: string;
    options: string[];
    correctAnswer: string;
    explanation: string;
    orderIndex: number;
  }[];
}

const NOW = '2026-08-13T12:00:00Z';

export const SAMPLE_PHASES: PhaseFixture[] = [
  {
    name: 'Java Foundations',
    description: 'Core syntax and OOP',
    durationWeeks: 4,
    objectives: ['Variables & types', 'Classes & objects'],
    status: 'CURRENT',
  },
  {
    name: 'Spring Boot Basics',
    description: 'REST and dependency injection',
    durationWeeks: 4,
    objectives: ['Controllers', 'Services'],
    status: 'AVAILABLE',
  },
  {
    name: 'Databases & JPA',
    description: 'Persistence with JPA',
    durationWeeks: 4,
    objectives: ['SQL basics', 'Repositories'],
    status: 'LOCKED',
  },
];

export const SAMPLE_ROADMAP: RoadmapFixture = {
  id: '00000000-0000-0000-0000-0000000000aa',
  goal: 'Become a backend engineer',
  level: 'beginner',
  phases: SAMPLE_PHASES,
  createdAt: NOW,
};

export const SAMPLE_DAILY_PLAN: DailyPlanFixture = {
  id: '00000000-0000-0000-0000-0000000000bb',
  date: '2026-08-13',
  items: [
    {
      type: 'READING',
      title: 'Java types in 15 minutes',
      description: 'Skim primitives vs objects',
      estimatedMinutes: 15,
    },
    {
      type: 'PRACTICE',
      title: 'Write a record',
      description: 'Model a User with a Java record',
      estimatedMinutes: 20,
    },
  ],
  createdAt: NOW,
};

export const SAMPLE_SESSION: LearningSessionFixture = {
  id: '00000000-0000-0000-0000-0000000000cc',
  roadmapId: SAMPLE_ROADMAP.id,
  roadmapNodeId: '00000000-0000-0000-0000-0000000000ee',
  nodeIndex: 0,
  title: 'Java Foundations — Lesson 1',
  status: 'NOT_STARTED',
  estimatedMinutes: 25,
  generatedAt: NOW,
  updatedAt: NOW,
  readings: [
    {
      id: '00000000-0000-0000-0000-0000000000f1',
      title: 'Variables',
      content: 'A variable holds a value. Java is statically typed.',
      estimatedMinutes: 5,
      orderIndex: 0,
    },
  ],
  watchings: [
    {
      id: '00000000-0000-0000-0000-0000000000f2',
      title: 'OOP in 5 minutes',
      description: 'Classes and objects',
      videoQuery: 'java oop',
      videoId: null,
      estimatedMinutes: 5,
      orderIndex: 0,
    },
  ],
  practices: [
    {
      id: '00000000-0000-0000-0000-0000000000f3',
      title: 'Write a class',
      description: 'Create a Person class',
      practiceType: 'CODE_EDITOR',
      instructions: 'Try it',
      starterContent: 'class Person {}',
      estimatedMinutes: 10,
      orderIndex: 0,
    },
  ],
  quizzes: [
    {
      id: '00000000-0000-0000-0000-0000000000f4',
      question: 'Is Java statically typed?',
      type: 'TRUE_FALSE',
      options: ['True', 'False'],
      correctAnswer: 'True',
      explanation: 'Yes — types are checked at compile time.',
      orderIndex: 0,
    },
  ],
};

/** Mirrors RoadmapServiceImpl.recompute — the real progress formula. */
export function recomputePhases(phases: PhaseFixture[], completedCount: number): PhaseFixture[] {
  return phases.map((p, i) => {
    const status: PhaseStatus =
      i < completedCount ? 'COMPLETED'
      : i === completedCount ? 'CURRENT'
      : i === completedCount + 1 ? 'AVAILABLE'
      : 'LOCKED';
    return { ...p, status };
  });
}

export function completedCountOf(phases: PhaseFixture[]): number {
  return phases.filter((p) => p.status === 'COMPLETED').length;
}

export function applyCompletePhase(roadmap: RoadmapFixture, phaseIndex: number): RoadmapFixture {
  const phases = roadmap.phases;
  if (phaseIndex < 0 || phaseIndex >= phases.length) {
    throw new Error(`Phase index out of range: ${phaseIndex}`);
  }
  const completed = completedCountOf(phases);
  if (phaseIndex > completed) {
    throw new Error(`Phase ${phaseIndex + 1} is locked and cannot be completed yet`);
  }
  return {
    ...roadmap,
    phases: recomputePhases(phases, Math.max(completed, phaseIndex + 1)),
  };
}

export function withStatuses(phases: Omit<PhaseFixture, 'status'>[], completedCount = 0): PhaseFixture[] {
  return recomputePhases(
    phases.map((p) => ({ ...p, status: 'LOCKED' as PhaseStatus })),
    completedCount,
  );
}

export function authHeader(token: string): { Authorization: string } {
  return { Authorization: `Bearer ${token}` };
}

export async function getMyRoadmap(api: APIRequestContext, token: string) {
  return api.get('/ai/roadmaps/me', { headers: authHeader(token), failOnStatusCode: false });
}

export async function getRoadmapById(api: APIRequestContext, token: string, id: string) {
  return api.get(`/ai/roadmaps/${id}`, { headers: authHeader(token), failOnStatusCode: false });
}

export async function completePhaseApi(api: APIRequestContext, token: string, index: number) {
  return api.post(`/ai/roadmaps/me/phases/${index}/complete`, {
    headers: authHeader(token),
    data: {},
    failOnStatusCode: false,
  });
}

export async function getDailyPlan(api: APIRequestContext, token: string) {
  return api.get('/ai/daily-plans/me', { headers: authHeader(token), failOnStatusCode: false });
}

export async function getCurrentSession(api: APIRequestContext, token: string) {
  return api.get('/learning-sessions/current', { headers: authHeader(token), failOnStatusCode: false });
}

export async function getSessionById(api: APIRequestContext, token: string, id: string) {
  return api.get(`/learning-sessions/${id}`, { headers: authHeader(token), failOnStatusCode: false });
}

export async function completeSessionApi(api: APIRequestContext, token: string, id: string) {
  return api.post(`/learning-sessions/${id}/complete`, {
    headers: authHeader(token),
    data: {},
    failOnStatusCode: false,
  });
}

export async function startSessionApi(api: APIRequestContext, token: string, id: string) {
  return api.post(`/learning-sessions/${id}/start`, {
    headers: authHeader(token),
    data: {},
    failOnStatusCode: false,
  });
}

export interface RoadmapMocks {
  roadmap?: RouteHandler;
  completePhase?: RouteHandler;
  dailyPlan?: RouteHandler;
  currentSession?: RouteHandler;
  sessionById?: RouteHandler;
  startSession?: RouteHandler;
  completeSession?: RouteHandler;
  generateSession?: RouteHandler;
}

/**
 * Deterministic browser-side stubs for roadmap / daily-plan / learning-session
 * HTTP. Only the network boundary is stubbed; Angular behaviour is real.
 */
export async function installRoadmapRoutes(page: Page, mocks: RoadmapMocks = {}): Promise<void> {
  await page.route('**/ai/roadmaps/me/phases/*/complete', (route) =>
    mocks.completePhase ? mocks.completePhase(route) : fulfillJson(route, 200, applyCompletePhase(SAMPLE_ROADMAP, 0)),
  );
  await page.route('**/ai/roadmaps/me', (route) => {
    if (route.request().method() !== 'GET') return route.fallback();
    return mocks.roadmap ? mocks.roadmap(route) : fulfillJson(route, 200, SAMPLE_ROADMAP);
  });
  await page.route('**/ai/daily-plans/me', (route) =>
    mocks.dailyPlan ? mocks.dailyPlan(route) : fulfillJson(route, 200, SAMPLE_DAILY_PLAN),
  );
  await page.route('**/learning-sessions/current', (route) =>
    mocks.currentSession ? mocks.currentSession(route) : fulfillJson(route, 200, SAMPLE_SESSION),
  );
  await page.route('**/learning-sessions/generate', (route) =>
    mocks.generateSession ? mocks.generateSession(route) : fulfillJson(route, 200, SAMPLE_SESSION),
  );
  await page.route('**/learning-sessions/*/start', (route) =>
    mocks.startSession
      ? mocks.startSession(route)
      : fulfillJson(route, 200, { ...SAMPLE_SESSION, status: 'IN_PROGRESS' }),
  );
  await page.route('**/learning-sessions/*/complete', (route) =>
    mocks.completeSession
      ? mocks.completeSession(route)
      : fulfillJson(route, 200, { ...SAMPLE_SESSION, status: 'COMPLETED' }),
  );
  await page.route(/\/learning-sessions\/[0-9a-f-]{36}$/i, (route) => {
    if (route.request().method() !== 'GET') return route.fallback();
    return mocks.sessionById ? mocks.sessionById(route) : fulfillJson(route, 200, SAMPLE_SESSION);
  });
}

/** Stateful GET/POST pair that mirrors completePhase persistence for UI tests. */
export function statefulRoadmapHandlers(initial: RoadmapFixture = structuredClone(SAMPLE_ROADMAP)): {
  state: { current: RoadmapFixture };
  get: RouteHandler;
  complete: RouteHandler;
} {
  const state = { current: structuredClone(initial) };
  return {
    state,
    get: (route: Route) => fulfillJson(route, 200, state.current),
    complete: async (route: Route) => {
      const match = route.request().url().match(/\/phases\/(\d+)\/complete/);
      const index = match ? Number(match[1]) : 0;
      try {
        state.current = applyCompletePhase(state.current, index);
        await fulfillJson(route, 200, state.current);
      } catch (err) {
        const message = err instanceof Error ? err.message : 'locked';
        const status = message.includes('out of range') || message.includes('locked') ? 400 : 500;
        await fulfillJson(route, status, { status, error: 'Bad Request', message });
      }
    },
  };
}

export async function openRoadmap(
  page: Page,
  api: APIRequestContext,
  mocks: RoadmapMocks = {},
): Promise<AuthTokens> {
  const { auth } = await registerViaApi(api);
  await seedSession(page, sessionStorage(auth));
  await installRoadmapRoutes(page, mocks);
  await page.goto('/roadmap');
  return auth;
}

// ─── Deterministic DB seed (docker exec → athena-ai-db) ──────────────────────

let aiDbCached: boolean | undefined;

export async function aiDbAvailable(): Promise<boolean> {
  if (aiDbCached !== undefined) return aiDbCached;
  try {
    await aiPsql('SELECT 1');
    aiDbCached = true;
  } catch {
    aiDbCached = false;
  }
  return aiDbCached;
}

export const AI_DB_SKIP_REASON =
  `ai-db container '${CONFIG.aiDbContainer}' is not reachable via docker exec. ` +
  `Seeded persistence tests need the compose stack (athena-ai-db on port 5438).`;

export async function aiPsql(sql: string): Promise<string> {
  const { stdout, stderr } = await execFileAsync(
    'docker',
    [
      'exec',
      '-i',
      CONFIG.aiDbContainer,
      'psql',
      '-U',
      'athena',
      '-d',
      'athena_ai',
      '-v',
      'ON_ERROR_STOP=1',
      '-t',
      '-A',
      '-c',
      sql,
    ],
    { timeout: 15_000, windowsHide: true, maxBuffer: 2_000_000 },
  );
  if (stderr && /ERROR/i.test(stderr)) {
    throw new Error(stderr);
  }
  return stdout.trim();
}

function sqlStr(value: string): string {
  return `'${value.replace(/'/g, "''")}'`;
}

export async function seedRoadmap(
  userId: string,
  roadmap: RoadmapFixture = SAMPLE_ROADMAP,
  createdAt: string = NOW,
): Promise<RoadmapFixture> {
  const id = randomUUID();
  const content = JSON.stringify({
    phases: roadmap.phases.map((p) => ({
      name: p.name,
      description: p.description,
      durationWeeks: p.durationWeeks,
      objectives: p.objectives,
      status: p.status,
    })),
  });
  await aiPsql(
    `INSERT INTO generated_roadmaps (id, user_id, session_id, goal, level, content_json, created_at)
     VALUES (${sqlStr(id)}::uuid, ${sqlStr(userId)}::uuid, NULL, ${sqlStr(roadmap.goal)},
             ${sqlStr(roadmap.level)}, ${sqlStr(content)}, ${sqlStr(createdAt)}::timestamptz)`,
  );
  return { ...roadmap, id, createdAt };
}

export async function seedDailyPlan(
  userId: string,
  roadmapId: string,
  plan: DailyPlanFixture = SAMPLE_DAILY_PLAN,
): Promise<DailyPlanFixture> {
  const id = randomUUID();
  const content = JSON.stringify({ items: plan.items });
  await aiPsql(
    `INSERT INTO generated_daily_plans (id, user_id, roadmap_id, plan_date, content_json, created_at)
     VALUES (${sqlStr(id)}::uuid, ${sqlStr(userId)}::uuid, ${sqlStr(roadmapId)}::uuid,
             ${sqlStr(plan.date)}::date, ${sqlStr(content)}, ${sqlStr(plan.createdAt)}::timestamptz)`,
  );
  return { ...plan, id };
}

export async function seedLearningSession(
  userId: string,
  roadmapId: string,
  overrides: Partial<LearningSessionFixture> = {},
): Promise<LearningSessionFixture> {
  const id = randomUUID();
  const nodeId = randomUUID();
  const status = overrides.status ?? 'NOT_STARTED';
  const title = overrides.title ?? SAMPLE_SESSION.title;
  const nodeIndex = overrides.nodeIndex ?? 0;
  const minutes = overrides.estimatedMinutes ?? SAMPLE_SESSION.estimatedMinutes;
  await aiPsql(
    `INSERT INTO learning_sessions
       (id, user_id, roadmap_id, roadmap_node_id, node_index, title, status, estimated_minutes, generated_at, updated_at)
     VALUES (${sqlStr(id)}::uuid, ${sqlStr(userId)}::uuid, ${sqlStr(roadmapId)}::uuid, ${sqlStr(nodeId)}::uuid,
             ${nodeIndex}, ${sqlStr(title)}, ${sqlStr(status)}, ${minutes}, NOW(), NOW())`,
  );
  return {
    ...SAMPLE_SESSION,
    ...overrides,
    id,
    roadmapId,
    roadmapNodeId: nodeId,
    nodeIndex,
    title,
    status,
    estimatedMinutes: minutes,
  };
}

export async function expectJsonOk(res: { status: () => number; text: () => Promise<string> }, expected = 200) {
  expect(res.status(), await res.text()).toBe(expected);
}

export async function openSeededRoadmap(
  page: Page,
  api: APIRequestContext,
  roadmap: RoadmapFixture = SAMPLE_ROADMAP,
): Promise<{ auth: AuthTokens; seeded: RoadmapFixture }> {
  const { auth } = await registerViaApi(api);
  const seeded = await seedRoadmap(auth.userId, roadmap);
  await seedSession(page, sessionStorage(auth));
  await page.goto('/roadmap');
  return { auth, seeded };
}
