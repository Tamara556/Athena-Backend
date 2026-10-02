# Athena — Frontend Functional Specification

> Source of truth for Playwright E2E test planning. Documents **only implemented** functionality, reverse-engineered from the Angular codebase (`C:\Users\muraz\IdeaProjects\Athena-Frontend`). Companion document: `FUNCTIONAL_SPECIFICATION_BACKEND.md`.

---

## 0. Global Architecture & Conventions

| Aspect | Detail |
|---|---|
| Framework | Angular 20, standalone components, new control flow (`@if`/`@for`), Signals for state (no NgRx). Custom CSS design system (no Material/Tailwind). |
| API base URL | `http://localhost:8080` (the API gateway). Configured in `src/environments/environment.ts` as `apiBase`. |
| HTTP client | Single `authInterceptor` registered globally. |
| Router | Lazy-loaded standalone components; `{ path: '**', redirectTo: '' }`. |

### 0.1 Session & Authentication (client-side)

Session state lives in `Session` (signals + `localStorage`). Keys:

- `athena_token` — JWT access token (Bearer)
- `athena_userId` — user UUID
- `athena_sessionId` — device-session id (used by "Manage devices")
- `athena_name` — display name (first + last, else username)
- `athena_image` — avatar URL (`{apiBase}/auth/users/{userId}/image`) or absent

`isLoggedIn` = truthy token. **There is NO refresh-token handling on the frontend** — only the access token is stored/used.

### 0.2 Auth interceptor (`auth.interceptor.ts`)

- If a token exists **and** the request URL starts with `apiBase`, adds `Authorization: Bearer <token>`.
- On response error `401` (for authed requests): clears the session and navigates to `/login`.
- All other errors pass through.

**Edge-case for tests:** External calls (e.g., YouTube Data API) do NOT get the Bearer header (URL doesn't start with apiBase).

### 0.3 Route guards (`auth.guard.ts`)

- `authGuard`: allow if logged in, else redirect to `/login`.
- `guestGuard`: if logged in, redirect to `/roadmap`; else allow.

### 0.4 Route table

| Route | Guard | Component | Notes |
|---|---|---|---|
| `/` | none (public) | HomeComponent | Landing page |
| `/login` | guestGuard | LoginComponent | |
| `/register` | guestGuard | RegisterComponent | |
| `/onboarding` | authGuard | OnboardingComponent | |
| `/roadmap` | authGuard | RoadmapComponent | Post-login home |
| `/dashboard` | authGuard | DashboardComponent | |
| `/daily-journey` | authGuard | DailyJourneyComponent | |
| `/knowledge-graph` | authGuard | KnowledgeGraphComponent | |
| `/interviews` | authGuard | InterviewsComponent | |
| `/achievements` | authGuard | AchievementsComponent | |
| `/streaks` | authGuard | StreaksComponent | |
| `/progress` | authGuard | ProgressComponent | |
| `/athena-insights` | authGuard | AthenaInsightsComponent | |
| `/profile` | authGuard | ProfileComponent | |
| `/settings` | authGuard | SettingsComponent | |
| `/learning/current` | authGuard | DailyLearningSessionComponent | Current lesson |
| `/learning/:id` | authGuard | DailyLearningSessionComponent | Specific lesson |
| `**` | — | redirect to `/` | |

### 0.5 Data-layer status: REAL backend vs MOCK/STUB

Critical for testing — several feature pages render from in-memory mock data or empty stubs, so their network calls will NOT appear in Playwright request interception.

| Feature / page | Data source |
|---|---|
| Register, Login, 2FA | **REAL** (`/auth/*`) |
| Onboarding (start/goal/assessment) | **REAL** (`/ai/onboarding/*`) |
| Roadmap | **REAL** (`/ai/roadmaps/me`, complete phase) |
| Dashboard | **REAL** (`/ai/roadmaps/me`, `/ai/daily-plans/me`) |
| Daily Journey | **REAL** (`/daily-journey/*`) |
| Learning Session | **REAL** (`/learning-sessions/*`) + external YouTube API (optional) |
| Knowledge Graph — graph itself | **REAL** (`/ai/knowledge-graph/me/visualization`) |
| Knowledge Graph — evolution, opportunities, accelerators, apply | **MOCK/STUB** (return `null`/`[]`/no-op) |
| Achievements — catalog + my badges | **REAL** (`/badges`, `/badges/me`) |
| Achievements — AI suggestions, progress | **STUB** (return `[]`) |
| Streaks | **REAL** (`/progress/streaks`) |
| Settings — all sections | **REAL** (`/users/me/settings`, `/account/*`) |
| Profile — identity/preferences/avatar | **REAL** (via Settings API); narrative/direction/insights/milestones **MOCK** |
| Interviews (entire page) | **MOCK** (all data hard-coded / `of(...)`; "start" only sets a signal, no live interview UI) |
| Progress (entire page) | **MOCK/empty** (all `of([])`/empty; prompts are static; reflection save is fake) |
| Athena Insights | **REAL** (`/ai/insights/me`) with empty-profile fallback on error |

### 0.6 Shared / cross-cutting UI

- **Sidebar** (`app-sidebar`) — wraps most in-app pages via `<ng-content>`. Contains the nav (see §Navigation), the current user (avatar/initials + name), a **Logout** button, collapse/expand (desktop), and a drawer (mobile). Shows a `🔥 <n>` streak badge on the Streaks link when `currentStreak > 0`.
- **Theme toggle** (`app-theme-toggle`) — cycles light / dark / baby-pink; writes `data-theme` on `<html>`. Present in every in-app page header.
- **Language select** (`app-lang-select`) — English / Armenian / Russian / Korean. `| t` translate pipe. Sidebar + chrome translated; long page prose largely English.
- **First-run tour** (`TourService`) — after registration, a guided spotlight tour auto-starts once when `/roadmap` first loads. Uses `localStorage` keys `athena_tour_pending` / `athena_tour_done`. Anchors are `data-tour="..."` attributes on nav items.
- **Athena loader** — animated loader used during long AI generations.

### Navigation (sidebar links)

Section **Learning**: Roadmap (`/roadmap`), Daily Journey (`/daily-journey`, static badge "2"), Knowledge Graph (`/knowledge-graph`), Interviews (`/interviews`).
Section **Progress**: Achievements (`/achievements`), Streaks (`/streaks`, live flame badge), Progress (`/progress`), Insights (`/athena-insights`).
Section **Account**: Profile (`/profile`), Settings (`/settings`).
Footer: user card + **Logout** (clears session → `/login`).

---

# Page: Landing / Home

**Purpose:** Public marketing landing page for Athena.
**Route:** `/`
**Required authentication:** None (public; a logged-in user is NOT auto-redirected away).

**Visible UI components:** Top nav bar (scroll-state aware), hero section with animated ring, scroll-reveal feature sections, mobile hamburger menu, CTA links.
**Available buttons / actions:** Nav links to `/login` and `/register`; mobile menu open/close; scroll triggers reveal animations and the "big ring" animation (IntersectionObserver).
**Filters / dialogs / forms:** None.
**Validation:** None.
**Success states:** Sections reveal on scroll; ring animates when in view.
**Failure states:** None (static content).
**Backend APIs / Kafka / AI / RAG:** None.

---

# Page: Register

**Purpose:** Create an Athena account (auto sign-in on success). Optional profile picture.
**Route:** `/register`
**Required authentication:** Guest only (`guestGuard` → logged-in users bounced to `/roadmap`).

**Visible UI components:** Form fields (First name, Last name, Email, Username, Password, Confirm password), password-visibility toggle, live password-strength meter (5 rules), username-availability indicator, optional avatar picker with preview, Terms checkbox, submit button, error/success alerts, social buttons (Google/GitHub — demo only).

**Available buttons:**
- **Create Account** (submit) — label changes to "Account created" on success.
- **Show/Hide password** toggle.
- **Choose image / Remove image** (avatar).
- **Google / GitHub** (social) — cosmetic: spin for ~1.6s, do nothing.
- Link to `/login`.

**Available actions:** Live per-field validation on input; debounced (700ms) username availability check; password rules recompute live; confirm-match check.

**Filters:** None.
**Dialogs:** None.

**Forms & validation rules (client-side):**
- First / Last name: non-empty (trimmed length ≥ 1). Backend max 100 chars.
- Email: regex `^[^\s@]+@[^\s@]+\.[^\s@]+$`.
- Username: ≥ 3 chars to trigger check. **Availability is MOCKED** — a hard-coded taken list (`athena, admin, test, user, root, demo, support`) marks "taken"; anything else "available". No backend call for the check.
- Password: must satisfy all 5 rules — length ≥ 8, uppercase, lowercase, number, special char (score 5/5 required).
- Confirm: must equal password.
- Terms checkbox: must be checked.
- Avatar (optional): type ∈ {png, jpeg, jpg, gif}; ≤ 5 MB. Invalid → inline `imageError`, input cleared.

**Submit payload:** `multipart/form-data` → `POST /auth/register` with `firstName, lastName, username, email, password` (+ optional `image`).

**Success states:** Success alert shown; session stored; `TourService.markPending()`; after ~900ms navigate to `/onboarding`.

**Failure states:**
- Client validation fail → red-highlighted fields + error alert ("Please complete the highlighted fields…" or "Please accept the Terms…"). No network call.
- Backend `409 Conflict` → error alert with message; if message matches `username` → username marked taken; if `email` → email marked invalid.
- Other backend error → generic error alert; loading reset.

**Backend APIs used:** `POST /auth/register` (multipart).
**Kafka:** Backend publishes `UserRegisteredEvent` (async; not observable from UI).
**AI / RAG:** None directly (AI onboarding session is created async server-side from the event).

---

# Page: Login

**Purpose:** Sign in with email OR username + password; optional SMS 2FA second step.
**Route:** `/login`
**Required authentication:** Guest only.

**Visible UI components:** Login field (email or username), password field + visibility toggle, submit button, error/success alerts, social buttons (demo), link to `/register`. **2FA step:** a 6-digit code input replaces the form when a challenge is required, with **Verify** and **Cancel**.

**Available buttons / actions:**
- **Sign in** (submit) → `POST /auth/login`.
- **Verify** (2FA step) → `POST /auth/2fa/verify`.
- **Cancel** (2FA step) → back to credentials.
- Show/hide password; Google/GitHub (demo, no-op); link to Register.

**Forms & validation rules:**
- Login + password: only non-empty checks (no format validation — backend accepts email or username).
- 2FA code: client requires exactly 6 digits (`^\d{6}$`).

**Success states:**
- No 2FA: session stored; success alert; navigate to `/roadmap`.
- 2FA required (`twoFactorRequired && challengeToken`): switch to code-entry step; submit label → "Verify".
- 2FA verified: session stored; navigate to `/roadmap`.

**Failure states:**
- Empty fields → "Enter your email or username and password." (no call).
- Bad credentials / server error → error alert (message from server) + both fields marked invalid.
- 2FA: invalid code format → local error; wrong/expired code → server error alert.

**Backend APIs used:** `POST /auth/login`, `POST /auth/2fa/verify`.
**Kafka / AI / RAG:** None.

---

# Page: Onboarding

**Purpose:** Guided AI onboarding — user states a learning goal, answers an AI-generated adaptive assessment, and the AI generates the roadmap (+ daily plan, KG seed).
**Route:** `/onboarding`
**Required authentication:** Signed-in (`authGuard`; also re-checks in `ngOnInit`, redirecting to `/login` if not).

**Steps (state machine):** `goal` → `busy` (loader) → `questions` → `busy` → navigate `/roadmap`.

**Visible UI components:**
- **Goal step:** greeting text, a goal question, a goal textarea/input, submit.
- **Busy step:** Athena animated loader with a rotating status line (messages cycle every 15s; last message persists). Long single request (local model reasons for minutes).
- **Questions step:** dynamic list of AI questions each with an answer field, a **Start from zero** shortcut, submit.

**Available buttons / actions:**
- Submit goal → `POST /ai/onboarding/goal`.
- Answer each question (input).
- **Start from zero** — auto-fills every answer with "I don't know — I'm starting from scratch." and submits.
- Submit assessment → `POST /ai/onboarding/assessment`.

**On init:** `POST /ai/onboarding/start` (idempotent) to fetch greeting + first question (failure → keep sensible defaults, goal step still works).

**Forms & validation:**
- Goal: must be non-empty (trimmed) → else "Tell Athena what you'd like to learn to continue."
- Assessment: every question must have a non-empty answer → else highlight first empty + "Please answer every question…".

**Success states:** After goal → questions rendered. After assessment → navigate `/roadmap`.

**Failure states:**
- Goal submit error → back to `goal` step, error message shown. (Server may schedule an AI retry → surfaces as an error to the user here.)
- Assessment submit error → back to `questions` step, error message.

**Backend APIs used:** `POST /ai/onboarding/start`, `POST /ai/onboarding/goal`, `POST /ai/onboarding/assessment`.
**Kafka:** server publishes onboarding/goal/assessment/roadmap/daily-plan events (not UI-observable).
**AI:** Heavy — assessment generation, goal analysis, roadmap generation, daily-plan generation (local LM Studio). **Expect very long latencies (minutes).**
**RAG:** Indirect (generation may use user memory server-side).

---

# Page: Roadmap

**Purpose:** Visualize the learner's AI-generated roadmap as a journey of phase nodes; complete phases; jump into today's lesson.
**Route:** `/roadmap`
**Required authentication:** Signed-in.

**Visible UI components:** Header (name, goal, level), summary stats (total phases / weeks / objectives), a node journey with per-node status styling, hover cards, pagination (5 nodes/page), the sidebar, theme toggle, language select. First-run **tour** overlay may appear here once for new users.

**Node statuses:** `done` (COMPLETED), `current` (CURRENT, expanded card w/ objectives), `avail` (AVAILABLE), `locked` (LOCKED). If server status missing, defaults: index 0 → CURRENT, 1 → AVAILABLE, else LOCKED.

**Available buttons / actions:**
- **Complete phase** (on current node) → `POST /ai/roadmaps/me/phases/{index}/complete`; re-renders roadmap, stays on the same page. Guarded by `completing` flag (no double submit).
- **Go to session** (keyboard activation on current node) → navigate `/learning/current`.
- Pagination **← / →** (`prev`/`next`), shown only when > 5 phases.

**Filters / dialogs / forms:** None (hover cards only).
**Validation:** None.

**Success states:** Roadmap loads → journey rendered, `hasRoadmap=true`. Tour auto-starts after 600ms if pending. Complete phase → statuses shift (this→done, next→current, next+1→available).

**Failure states:** Load error → `hasRoadmap=false`, loading cleared (empty/"no roadmap" state — user should complete onboarding). Complete-phase error → `completing` reset (silent).

**Backend APIs used:** `GET /ai/roadmaps/me`, `POST /ai/roadmaps/me/phases/{index}/complete`.
**Kafka / AI / RAG:** None at read time (roadmap is pre-generated).

---

# Page: Dashboard

**Purpose:** Minimal overview combining roadmap + today's daily plan.
**Route:** `/dashboard`
**Required authentication:** Signed-in.

**Visible UI components:** Roadmap summary block, daily-plan block, links (RouterLink).
**Available actions:** Loads roadmap + daily plan on init.
**Forms / dialogs / filters:** None.
**Success states:** Roadmap and plan rendered when available.
**Failure states:**
- Roadmap error → message "No roadmap yet — complete onboarding first."
- Daily plan error → message "No daily plan yet — complete onboarding first."
**Backend APIs used:** `GET /ai/roadmaps/me`, `GET /ai/daily-plans/me`.
**Kafka / AI / RAG:** None at read time.

---

# Page: Daily Journey

**Purpose:** Today's adaptive mission — a set of learning "blocks" the user starts/completes/skips, with AI mentor check-ins, plan adjustments, weakness drills, and an end-of-day reflection.
**Route:** `/daily-journey`
**Required authentication:** Signed-in.

**Visible UI components:** Header (localized date), mission card, progress ring (completed/total, %), block list (typed icons: READING 📖, PRACTICE 💻, VIDEO 🎥, QUIZ 🧠, SPEAKING 🎤, REVIEW 🗂, DRILL ⚡; difficulty color EASY/MODERATE/CHALLENGING), "Why this plan" reasoning panel, adjustments list, weaknesses list, mentor check-in selector (CONFIDENT / UNSURE / NEED_HELP), reflection form, toast notifications.

**Available buttons / actions (all POST to `/daily-journey/*`, re-render journey):**
- **Begin / Start day** → `POST /today/start`.
- **Adjust plan** menu (toggle) → `POST /today/adjust` with action (e.g., `REGENERATE`). Toast "Plan updated".
- **Edit time / Save time** → `POST /today/time` `{availableMinutes}` (must be ≥ 15, else ignored).
- **Start block** → `POST /blocks/{id}/start`.
- **Complete block** → `POST /blocks/{id}/complete`.
- **Skip block** → `POST /blocks/{id}/skip` (optional reason).
- **Relink block** (add back a skipped block) → `POST /blocks/{id}/relink`.
- **Strengthen weakness** → `POST /weaknesses/{knowledgeNodeId}/strengthen` (generates a drill).
- **Mentor check-in** (pick confidence) → `POST /checkin` → returns AI mentor reply.
- **Save reflection** → `POST /reflection` `{hardestPart, whatClicked, adjustRequest}`.
- **Skip reflection** → `POST /reflection/skip`.
- **Continue mission** — smooth-scrolls to blocks (no network).

**Forms & validation:** Time edit: parsed int, ignored if `< 15`. Reflection: free text (no strict validation). Confidence: enum.

**Success states:** Each action updates the journey signal; success toasts. Progress ring/percent recompute. Mentor reply appears after check-in.

**Failure states:**
- Initial load error → error message (`errMsg`).
- Any action error → toast "Something went wrong" (or specific) + message; busy flag reset. Mentor check-in error → "Could not save" toast.

**Backend APIs used:** `/daily-journey/today`, `/today/why`, `/today/start`, `/today/adjust`, `/today/time`, `/blocks/{id}/start|progress|complete|skip|relink`, `/weaknesses/{id}/strengthen`, `/checkin`, `/reflection`, `/reflection/skip`.
**Kafka:** server emits daily.* events (block completed/skipped, mission adjusted/generated, checkin, reflection).
**AI:** mission generation, mentor replies, weakness drills, "why" reasoning (local model — latency).
**RAG:** indirect via generation.

---

# Page: Learning Session (Daily Learning Session)

**Purpose:** Deliver one AI-generated lesson for the current roadmap node, staged Reading → Watching → Practice → Quiz, then completion.
**Route:** `/learning/current` and `/learning/:id`
**Required authentication:** Signed-in.

**Visible UI components:** Header (title, status, total/left minutes), stage progress (4 stages with per-stage meta: reading min, watching min, practice min, quiz question count), stage sections (Reading, Watching w/ embedded YouTube, Practice, Quiz), CTA button, completion section, empty state, Athena loader with rotating status messages, toasts.

**Loading behavior (important for tests):**
- `/learning/current`: `GET /learning-sessions/current`. On `404`, kicks off `POST /learning-sessions/generate` **and** polls `getCurrent` every 5s (up to 60 tries ≈ 5 min). First to resolve wins. If still nothing after max polls → empty state.
- `/learning/:id`: `GET /learning-sessions/{id}`. On `404` → poll current (generation path). Other error → error state.

**Available buttons / actions:**
- **CTA**: "Start Learning" (NOT_STARTED → `POST /{id}/start`, go to stage 0), "Continue Learning" (IN_PROGRESS → scroll to active stage), or hidden when COMPLETED.
- **Prepare** (empty state) → `POST /learning-sessions/generate`.
- **Select stage** (jump), **Back**.
- **Complete stage** (per section) → marks stage done, toast, advances; when all 4 done → `POST /{id}/complete` → completion screen.
- Completion actions: **Return to roadmap** (→ `/roadmap`), **Continue tomorrow** (close completion).

**Forms & validation:** Quiz answers selected in-section (client-side); no strict server validation surfaced here.

**Success states:** Session rendered; stage-by-stage completion; final completion screen after `complete`. A COMPLETED session on load shows all stages done + completion screen.

**Failure states:** Non-404 load error → error state (message). Generate error → loader stops, error shown. Complete/start error → error message.

**Backend APIs used:** `GET /learning-sessions/current`, `GET /{id}`, `POST /{id}/start`, `POST /{id}/complete`, `POST /generate`, `GET /upcoming`.
**External:** YouTube Data API v3 (`googleapis.com`) — only if `environment.youtubeApiKey` is set; resolves a real embeddable video id. Without a key, falls back to the AI-provided `videoId`/`videoQuery`.
**Kafka:** server emits learning.session.* events (generated/started/completed, node buffer refilled).
**AI:** lesson content generation (readings/watchings/practices/quizzes) — long latency on first generation.
**RAG:** indirect via generation.

---

# Page: Knowledge Graph

**Purpose:** Visual map of the learner's skills (nodes) and relationships (edges) with mastery/confidence, plus mentor insights & recommendations.
**Route:** `/knowledge-graph`
**Required authentication:** Signed-in.

**Visible UI components:** SVG graph (nodes positioned by computed layout, sized by mastery; edges with relationship labels), search box (focusable with Cmd/Ctrl+K), selected-node detail panel (status, mastery, confidence, connected skills), "why it matters" panel, mentor insights, exploration list, evolution chart, opportunities & accelerators cards. Summary (strongest/weakest skills, average mastery, total skills), insights list.

**Available actions / buttons:**
- **Search** (type) → filters/focuses nodes.
- **Select node** (click) → detail + highlight connected edges/nodes.
- **Select skill** (from connected list) → selects the matching node.
- **Apply recommendation** (opportunity/accelerator card) → **STUB no-op** (`of(void 0)`).

**Data source:** `GET /ai/knowledge-graph/me/visualization` (**REAL**; maps to nodes/edges/summary/insights; error → `null` → empty). Evolution, opportunities, accelerators, apply = **MOCK/STUB**.

**Forms / dialogs / filters:** Search box (filter). No dialogs/forms.
**Validation:** None.
**Success states:** Graph renders once ready; reveal animations; node selection highlights.
**Failure states:** Visualization error → graph is empty/null (page renders shell). Apply recommendation always "succeeds" silently (stub).

**Backend APIs used:** `GET /ai/knowledge-graph/me/visualization`.
**Kafka / AI / RAG:** Visualization is generated server-side (may involve AI/KG). Read-only from UI.

---

# Page: Interviews

**Purpose:** Overview of AI mock/scheduled interviews — readiness, confidence trend, history, review topics, tips, level selection, "start interview".
**Route:** `/interviews`
**Required authentication:** Signed-in.

> **ENTIRELY MOCK.** All data returned from `InterviewsApi` is hard-coded or `of(null)/of([])` with a simulated ~420ms latency. There is **no live interview UI/route** — "start" merely stores a fabricated `activeInterview` signal; submit returns a fabricated result. **No real network requests are issued.**

**Visible UI components:** Next-interview card (null in mock), confidence trend chart (null), readiness ring (null), history "talk" cards (empty, expandable), observations (empty), review topics (empty), tips (4 static), impacts (4 static), level selector (Gentle/Balanced/Challenge — beginner/intermediate/advanced), start buttons.

**Available buttons / actions:**
- **Pick level** (mock levels 0/1/2; default selected id = 1).
- **Start (scheduled)** — only when a next interview exists (never in mock) → `store.start(...)` (mock).
- **Start (mock level)** → `store.start(...)` (mock; sets `starting` then `activeInterview`).
- **Toggle talk** (expand history card).
- **Review** → navigate `/knowledge-graph`.

**Forms / validation / dialogs:** None.
**Success states:** Tips/impacts/levels appear after simulated latency; "start" sets `activeInterview` (no navigation).
**Failure states:** `start` error → `starting` reset (mock never errors).

**Backend APIs used:** **None** (all mock). *(Real backend endpoints exist — see backend spec `/interviews/*` and `/ai/interviews/*` — but the UI is not wired to them.)*
**Kafka / AI / RAG:** None from UI.

---

# Page: Achievements

**Purpose:** Badge gallery — earned + locked badges with rarity, category, progress, XP.
**Route:** `/achievements`
**Required authentication:** Signed-in.

**Visible UI components:** Hero (stats: total earned, "since" month), status filter chips (all / earned / locked), sort dropdown (recent / rarity / progress / title), badge grid (per-badge: icon, title, description, rarity, category, progress bar, earned date, XP, optional reward, AI-generated flag), badge detail (Esc closes), empty-state preview, count-up animations.

**Available buttons / actions:**
- **Filter by status** (all/earned/locked).
- **Sort** (recent/rarity/progress/title).
- **Open badge detail** (click) / **close** (Esc).

**Data source:** `forkJoin` of catalog `GET /badges` (**REAL**), my badges `GET /badges/me` (**REAL**), AI suggestions (**STUB `[]`**), progress (**STUB `[]`**). Merged with a client-side presentation map (rarity/color/animation/maxProgress/reward) and XP-by-rarity. Both real calls catch errors → `[]`.

**Forms / validation / dialogs:** No forms; badge detail is a panel.
**Success states:** Grid renders; stats computed; empty state when total = 0.
**Failure states:** API errors caught → treated as empty lists (page still renders, possibly empty).

**Backend APIs used:** `GET /badges`, `GET /badges/me`.
**Kafka / AI / RAG:** Badges are awarded server-side via events; UI is read-only.

---

# Page: Streaks

**Purpose:** Daily learning streak calendar and stats.
**Route:** `/streaks`
**Required authentication:** Signed-in.

**Visible UI components:** Current/longest streak, learning hours (this period + yearly), month calendar grid (localized weekday headers, active-day highlighting), day-detail panel (selected day), rotating consistency messages, count-up numbers, month prev/next switcher, empty state.

**Available buttons / actions:**
- **Prev/Next month** (`switchMonth(±1)`) — clears selection, animates.
- **Select day** (toggle detail).
- Defaults selection to today on init.

**Data source:** `GET /progress/streaks` (**REAL**) → `StreakActivity` (activity by date, summary, yearly, first-active date).
**Forms / validation / dialogs:** None.
**Success states:** Calendar renders; `loaded = !loading`.
**Failure states:** Load failure → empty/loading state (store-driven).

**Backend APIs used:** `GET /progress/streaks`.
**Kafka / AI / RAG:** Streaks updated server-side via `StreakUpdatedEvent`; read-only.

---

# Page: Progress

**Purpose:** Qualitative growth-story: transformation, highlights, confidence journey, observations, milestones, future predictions, and a reflection journal.
**Route:** `/progress`
**Required authentication:** Signed-in.

> **MOCK/EMPTY.** All content lists come back empty (`of([])` / empty transform); reflection **prompts are 3 static items**; `saveReflection` is fabricated (no persistence).

**Visible UI components:** Transformation view (start vs now rows), highlights, journey quotes, observations, milestones, future predictions, reflection journal (prompt selector + textarea + save), empty state, toast.

**Available buttons / actions:**
- **Select prompt** (from static prompts).
- **Edit draft** (textarea).
- **Save reflection** → mock save + toast. (No backend.)

**Forms & validation:** Reflection textarea (no strict validation).
**Success states:** Reflection "saved" toast. `emptyState` true for a new learner (no data).
**Failure states:** None meaningful (mock).

**Backend APIs used:** **None** (mock). *(Proposed real endpoints: `/progress` + AI narrative — not wired.)*
**Kafka / AI / RAG:** None from UI.

---

# Page: Athena Insights

**Purpose:** AI-authored "learner model" narrative — a letter, learning style, patterns, strengths, explorations, evolution, potential, future letter.
**Route:** `/athena-insights`
**Required authentication:** Signed-in.

**Visible UI components:** Greeting with name, letter blocks (eyebrow/title/paragraphs/sign-off), learning-style (best/harder), patterns, strengths, explorations, evolution segments, potential, future letter. Reusable `InsightTextComponent` renderer. Empty state for new learners.

**Available actions:** Read-only (scroll reveal). Sidebar drawer.
**Data source:** `GET /ai/insights/me` (**REAL**), with `EMPTY_PROFILE` fallback on error (`catchError → of(EMPTY_PROFILE)`).
**Forms / validation / dialogs / filters:** None.
**Success states:** Profile renders; `emptyState` when new learner or forced.
**Failure states:** Error → empty profile (page renders empty shell, no error surfaced).

**Backend APIs used:** `GET /ai/insights/me`.
**AI:** Insights are AI-generated server-side. **RAG:** indirect.

---

# Page: Profile

**Purpose:** View & edit the learner's identity, learning direction, and preferences; upload avatar; retake onboarding.
**Route:** `/profile`
**Required authentication:** Signed-in.

**Visible UI components:** Identity card (name, initials/avatar, joined label), narrative/direction/preferences panels, milestones, Athena insights snippets, edit dialogs, avatar picker, toast.

**Available buttons / actions:**
- **Edit profile** dialog → `PATCH /account/profile` (via Settings API).
- **Edit goal/direction** dialog → **MOCK** update (`of(direction)` delayed).
- **Edit availability** dialog → `PUT /users/me/settings` (real; merges learning.availability).
- **Pick avatar / upload** → `POST /account/image` (real).
- **Retake onboarding** → navigate `/onboarding`.
- Esc closes dialog.

**Data source:** identity/preferences derived from **REAL** Settings/Account APIs; narrative/direction/insights/milestones = **MOCK**.

**Forms & validation:**
- Profile: `validateName` (first/last: required, ≤ 100).
- Goal: `validateGoal` + `validateFocus` (focus/next milestone).
- Availability: must choose a value (30m/1h/2h/4h) → else "choose time" error.
- Avatar: png/jpg/gif ≤ 5 MB.

**Success states:** Dialog closes + toast on save; avatar refreshes.
**Failure states:** Validation → inline `formError`. API error → `formError` (server message) or toast for avatar.

**Backend APIs used:** `GET /account/me`, `PATCH /account/profile`, `GET/PUT /users/me/settings`, `POST /account/image`.
**Kafka / AI / RAG:** None.

---

# Page: Settings

**Purpose:** Manage learning preferences, experience, notifications, privacy toggles; account (profile/email/password/avatar); security (login activity, 2FA, devices); data export.
**Route:** `/settings`
**Required authentication:** Signed-in.

**Visible UI components:** Segmented controls & toggles for four groups (Learning: availability/difficulty/style; Experience: tone + motivational/reflection/adaptive; Notifications: dailyReminder/weeklySummary/interviewReminders/milestones; Privacy: personalize/shareAnon), Save/Discard bar (dirty-aware), account card (name/email/initials/avatar), dialogs, modals, toasts.

**Settings model (defaults):**
- Segs: availability `2h`, difficulty `balanced`, style `practical`, tone `encouraging`.
- Togs: motivational/reflection/adaptive on; dailyReminder/weeklySummary/milestones on; interviewReminders off; personalize on; shareAnon off.

**Available buttons / actions & dialogs:**
- **Select segment / Toggle** (dirty state).
- **Save** → `PUT /users/me/settings` (toast saved/error). **Discard** → revert.
- **Edit profile** dialog → `PATCH /account/profile`.
- **Change email** dialog → `POST /account/email` `{newEmail, currentPassword}`.
- **Change password** dialog → `POST /account/password` `{currentPassword, newPassword}`.
- **Upload avatar** → `POST /account/image`.
- **Login activity** modal → `GET /account/login-activity`.
- **Two-factor** modal → `GET /account/2fa`; enable flow: `POST /account/2fa/setup {phoneNumber}` → SMS code → `POST /account/2fa/enable {code}`; disable flow: `POST /account/2fa/send-code` → `POST /account/2fa/disable {code}`.
- **Manage devices** modal → `GET /account/devices?current={sessionId}`; **Revoke device** → `POST /account/devices/{id}/revoke` (if current device: clears session + hard-redirect `/login`); **Sign out other devices** → `POST /account/devices/revoke-others?current={sessionId}`.
- **Export data** → `GET /account/export` (Blob) → downloads `athena-data-export.json`.
- **Clear history** modal (confirm) → **cosmetic toast only** (no backend).
- Esc closes any open modal/dialog.

**Forms & validation:**
- Profile: name required ≤ 100.
- Email: valid email + password required.
- Password: current required; new must match `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{8,72}$`.
- Avatar: png/jpg/gif ≤ 5 MB.
- 2FA phone: `^\+?[0-9]{8,15}$`; 2FA code: exactly 6 digits.

**Success states:** Toasts per action; modals load lists; 2FA enable/disable close modal + toast; export downloads file.
**Failure states:** Validation → inline `formError` / toast. Load errors → per-modal error flags (activity/devices/2FA). Save error → error toast.

**Backend APIs used:** `GET/PUT /users/me/settings`; `GET /account/me`, `PATCH /account/profile`, `POST /account/email`, `POST /account/password`, `POST /account/image`, `GET /account/login-activity`, `GET /account/2fa`, `POST /account/2fa/setup|enable|send-code|disable`, `GET /account/devices`, `POST /account/devices/{id}/revoke`, `POST /account/devices/revoke-others`, `GET /account/export`.
**Kafka / AI / RAG:** None.

---

# User Flows (end-to-end journeys)

Each flow: **Start page → user actions → backend interactions → expected UI → failure scenarios.**

### Flow 1 — Registration → Onboarding → Roadmap (happy path)
1. `/register`: fill valid fields, accept terms, (optional avatar), **Create Account**.
   - `POST /auth/register` → session stored; success alert; tour pending; → `/onboarding` (~900ms).
2. `/onboarding`: `POST /ai/onboarding/start` fetches greeting. Enter goal → **Submit** (`POST /ai/onboarding/goal`) → loader → questions.
3. Answer questions (or **Start from zero**) → **Submit** (`POST /ai/onboarding/assessment`) → long AI generation → `/roadmap`.
4. `/roadmap`: `GET /ai/roadmaps/me` renders; first-run tour auto-starts once.
- **Failures:** 409 on register (dup email/username); AI errors on goal/assessment (stay on step, error shown, possible server-side retry); roadmap load empty if generation not finished.

### Flow 2 — Login (no 2FA)
`/login` → enter login+password → `POST /auth/login` → session → `/roadmap`. Failure → error alert; fields invalid.

### Flow 3 — Login with 2FA
`/login` → credentials → `POST /auth/login` returns `twoFactorRequired` + `challengeToken` → code step → enter 6 digits → `POST /auth/2fa/verify` → session → `/roadmap`. **Cancel** returns to credentials. Failures: bad/expired code → error; malformed code → local error.

### Flow 4 — Daily Learning (lesson completion)
`/roadmap` current node → **Go to session** → `/learning/current` → `GET /learning-sessions/current` (or generate+poll on 404) → **Start Learning** (`POST /{id}/start`) → complete Reading→Watching→Practice→Quiz → all done → `POST /{id}/complete` → completion screen → **Return to roadmap**. Failures: generation timeout → empty state; stage/complete errors → error message.

### Flow 5 — Daily Journey day
`/daily-journey` → `GET /today` (+`/today/why`) → **Begin** (`/today/start`) → per block **Start/Complete/Skip** → optional **Adjust plan** / **Edit time** / **Strengthen weakness** / **Mentor check-in** → **Save/Skip reflection**. Failures: action error → toast, journey unchanged.

### Flow 6 — Roadmap phase completion
`/roadmap` current node → **Complete phase** → `POST /ai/roadmaps/me/phases/{index}/complete` → statuses shift, re-render. Failure → silent reset.

### Flow 7 — Profile update
`/profile` → **Edit profile/goal/availability** or **upload avatar** → respective API → toast + refresh. (Goal/direction is mock.)

### Flow 8 — Settings: change password / email / 2FA / devices / export
See Settings page. Each is a dialog/modal with its own validation and API. 2FA enable requires SMS code round-trip. Revoking the current device logs you out.

### Flow 9 — Logout
Sidebar **Logout** → `Session.clear()` → `/login`. (No backend call; access token simply discarded client-side.)

### Flow 10 — Achievements / Streaks / Insights / KG (read flows)
Navigate via sidebar → data loads (real for badges/streaks/insights/KG-viz) → render. Empty/loading states when data absent or errors caught.

### Flow 11 — Theme & Language
Any in-app page → theme toggle cycles light/dark/baby-pink (`data-theme` on `<html>`); language select switches en/hy/ru/ko (translated chrome).

---

# Edge Cases (test checklist)

| Category | Case & expected behavior |
|---|---|
| Invalid form input | Register/login/settings/profile: highlighted fields + inline messages; no network call until valid. |
| Duplicate account | Register `409` → error alert; username/email field flagged from message. |
| Expired/invalid JWT | Any authed request `401` → interceptor clears session → redirect `/login`. |
| Guest visiting protected route | `authGuard` → redirect `/login`. |
| Logged-in visiting `/login`,`/register` | `guestGuard` → redirect `/roadmap`. |
| Unknown route | `**` → redirect `/`. |
| AI unavailable / slow | Onboarding, learning-session generation, daily-journey generation: long loaders (minutes); learning-session polls up to ~5 min then empty state; onboarding errors return to step. |
| Empty roadmap | `/roadmap` & `/dashboard` show "complete onboarding first" states. |
| Empty achievements/streaks/progress/insights | Empty states rendered; badge/streak API errors caught → empty. |
| 2FA edge | Wrong code, expired challenge (5 min TTL server-side), malformed code (client requires 6 digits). |
| Revoke current device | Logs the user out and hard-redirects to `/login`. |
| Avatar validation | Wrong type / >5 MB → inline error, input cleared, no upload. |
| Duplicate submission | Guards: `completing` (roadmap), `busy` (journey), `deviceBusy`, `starting`, `exporting`, `dialogBusy` prevent double-submits. |
| Mock features issue no requests | Interviews, Progress (and KG evolution/opportunities, badge AI/progress) make NO backend calls — do not assert on network there. |
| YouTube key absent | Learning-session watching stage falls back to AI-provided video id/query (no external call). |
| localStorage unavailable | Tour silently disabled (wrapped in try/catch); session still uses in-memory signals for the session. |

---

## Appendix A — Full frontend → backend endpoint map

| UI action | Method & path (via gateway `http://localhost:8080`) |
|---|---|
| Register | `POST /auth/register` (multipart) |
| Login | `POST /auth/login` |
| Verify 2FA (login) | `POST /auth/2fa/verify` |
| Avatar image stream | `GET /auth/users/{userId}/image` |
| Onboarding start/goal/assessment | `POST /ai/onboarding/start` \| `/goal` \| `/assessment` |
| Roadmap read / complete phase | `GET /ai/roadmaps/me` \| `POST /ai/roadmaps/me/phases/{i}/complete` |
| Daily plan | `GET /ai/daily-plans/me` |
| Daily journey | `GET/POST /daily-journey/*` (see page) |
| Learning sessions | `GET/POST /learning-sessions/*` |
| Knowledge graph viz | `GET /ai/knowledge-graph/me/visualization` |
| Achievements | `GET /badges`, `GET /badges/me` |
| Streaks | `GET /progress/streaks` |
| Insights | `GET /ai/insights/me` |
| Settings | `GET/PUT /users/me/settings` |
| Account | `GET /account/me`, `PATCH /account/profile`, `POST /account/email`, `POST /account/password`, `POST /account/image` |
| Security | `GET /account/login-activity`, `GET /account/2fa`, `POST /account/2fa/setup\|enable\|send-code\|disable`, `GET /account/devices`, `POST /account/devices/{id}/revoke`, `POST /account/devices/revoke-others` |
| Export | `GET /account/export` |
| External | YouTube Data API v3 (`googleapis.com`, optional key) |

*Mock/stub UIs (Interviews, Progress, and KG evolution/recs, badge AI/progress) issue no backend calls.*
