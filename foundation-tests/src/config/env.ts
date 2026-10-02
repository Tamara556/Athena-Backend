import path from 'node:path';

/**
 * Single source of truth for environment endpoints. Values mirror
 * FOUNDATION_TEST_CASES.md §0.1–0.2 and are overridable via env vars so the same
 * suite runs locally, in Docker, or in CI without code changes.
 */
const val = (key: string, fallback: string): string => {
  const v = process.env[key];
  return v && v.trim().length > 0 ? v.trim() : fallback;
};

// foundation-tests/src/config -> repo root is three levels up.
const repoRoot = path.resolve(__dirname, '..', '..', '..');
const frontendDirDefault = path.resolve(repoRoot, '..', 'Athena-Frontend');

export const CONFIG = {
  /** Angular dev server — Playwright owns its lifecycle via `webServer`. */
  frontendUrl: val('ATHENA_FRONTEND_URL', 'http://localhost:4200'),
  /** Spring Cloud Gateway — the only externally-called backend port. */
  gatewayUrl: val('ATHENA_GATEWAY_URL', 'http://localhost:8080'),
  /** Eureka registry. */
  eurekaUrl: val('ATHENA_EUREKA_URL', 'http://localhost:8761'),
  /** Working directory the `webServer` runs `npm start` in. */
  frontendDir: process.env.ATHENA_FRONTEND_DIR
    ? path.resolve(process.env.ATHENA_FRONTEND_DIR)
    : frontendDirDefault,
  /** Reachability probe timeout for the backend gate. */
  backendProbeTimeoutMs: Number(val('ATHENA_BACKEND_PROBE_TIMEOUT_MS', '4000')),
  /**
   * Local dev JWT secret + issuer (from application.yml / docker-compose). Used
   * ONLY to mint expired/invalid tokens for deterministic negative auth tests —
   * never to forge a valid identity against a real deployment. Override per env.
   */
  jwtSecret: val('ATHENA_JWT_SECRET', 'local-dev-secret-change-me-please-0123456789abcdef'),
  jwtIssuer: val('ATHENA_JWT_ISSUER', 'athena-auth'),
  /** Compose container that holds generated_roadmaps / learning_sessions / daily plans. */
  aiDbContainer: val('ATHENA_AI_DB_CONTAINER', 'athena-ai-db'),
} as const;

/**
 * Reason surfaced when a backend-dependent spec is skipped. The backend stack
 * (gateway + microservices + infra) is Integration/Docker scope and is NOT
 * started by Phase 1 — these specs run only when the stack is already up.
 */
export const BACKEND_SKIP_REASON =
  `Backend gateway not reachable at ${CONFIG.gatewayUrl}. ` +
  `Start the stack with 'docker compose up' before running backend smoke checks ` +
  `(Integration/Docker orchestration is out of Phase 1 scope).`;
