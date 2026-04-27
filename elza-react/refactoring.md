# ELZA React UI — Refactoring Roadmap

Living document. Tick items off as they land; move completed sections to the bottom with the release tag that shipped them.

Baseline analysis date: 2026-04-23 (branch `Branch_dec979e2`).

Stack reference at baseline: React 18, TypeScript 5, Redux 4, react-redux 8, redux-form 8.3.10 (+ final-form 4.20), Vite 4, react-router-dom 5.1.2, react-bootstrap 2.9, @fluentui/react-components 9.51, SCSS. No tests.

---

## Top architectural debts (prioritized)

### 1. Class components + legacy Redux base class — [ ] open
- **Evidence:** 159 files `extend AbstractReactComponent`. Largest: [pages/arr/ArrPage.jsx](src/pages/arr/ArrPage.jsx) 1246 LOC, [pages/registry/RegistryPage.jsx](src/pages/registry/RegistryPage.jsx) 926 LOC, [pages/arr/ArrOutputPage.jsx](src/pages/arr/ArrOutputPage.jsx) 768, [pages/fund/FundPage.jsx](src/pages/fund/FundPage.jsx) 709.
- Deprecated lifecycle hooks (`UNSAFE_componentWillReceiveProps`) still in use (e.g. [AppRouter.jsx](src/pages/AppRouter.jsx)).
- **Impact:** blocks React 18 concurrent features (Suspense, transitions); hooks adoption is patchy.
- **Direction:** freeze new class components; convert top-down, smallest pages first.

### 2. Untyped Redux layer — [ ] open
- **Evidence:** ~78 action modules in [src/actions/](src/actions/) are `.jsx`, not `.tsx` (all `actions/arr/*.jsx`, `actions/admin/*.jsx`, `actions/refTables/*.jsx`, `actions/global/*.jsx`).
- [tsconfig.json:20-26](tsconfig.json) has `"strict": false`, `"noImplicitAny": false`, `"noUnusedLocals": false` with TODOs to flip.
- Custom store orchestration in [stores/AppStore.ts](src/stores/AppStore.ts) (596 LOC) with a hand-rolled `inlineFormSupport` duplicating redux-form semantics.
- **Impact:** no typed action payloads, no discriminated unions, refactors are blind.
- **Direction:** rename `actions/*.jsx` → `.ts`, introduce RTK `createSlice` pattern in one domain (e.g. `refTables`), flip `strict: true` per-file.

### 3. Two forms libraries and two UI kits in parallel — [ ] open
- **Forms:** `redux-form@8.3.10` (unmaintained since 2020) coexists with `final-form` + `react-final-form`. No migration plan.
- **UI kits:** `react-bootstrap@2.9` and `@fluentui/react-components@9.51` both used extensively.
- **Impact:** doubled bundle size, inconsistent a11y/focus behavior, visual drift, unmaintained redux-form is a security/compat liability.
- **Direction:** pick the keepers (react-final-form + Fluent v9 recommended), freeze new usage of the others, publish sunset release.

### 4. Dual API layers with unclear ownership — [ ] open
- **Evidence:** [src/api/generated/](src/api/generated/) is the OpenAPI-generated client; [src/api/old/](src/api/old/) still has files referenced by live code; [src/actions/WebApi.ts](src/actions/WebApi.ts) is **2378 lines** of hand-written REST+WebSocket wrapper starting with `@ts-ignore`.
- `elza-api` is declared as `file:target/elza-api` — generated client is a build artifact, not properly npm-managed.
- **Impact:** three ways to call the backend; drift between generated client and `WebApi.ts` is invisible until runtime.
- **Direction:** make generated client the single source of truth, shrink `WebApi.ts` to a thin adapter, remove `api/old/`.

### 5. Monolithic "god pages" — [ ] open
- **Evidence:** ArrPage 1246, RegistryPage 926, ArrOutputPage 768, FundPage 709, FundActionPage 594 LOC. Each orchestrates tree + forms + ribbon + modals + drag-drop + search + history in one `connect()`-ed class.
- No reselect-style memoized selectors; re-renders cascade across unrelated slices.
- **Impact:** touching one feature risks five others; onboarding cost is weeks.
- **Direction:** extract feature containers (`TreeContainer`, `RibbonContainer`, `NodeEditorContainer`) with scoped selectors, page becomes a thin shell.

### 6. Outdated router and core peer deps — [ ] open
- **Evidence:** `react-router-dom@5.1.2` and `react-router@5.1.2` (2019-era, EOL); `redux@4`; `react-redux@8`; `vite@4`.
- **Impact:** every release accumulates more HOC patterns (`withRouter`, `useHistory`) that must later be unwound.
- **Direction:** schedule a Router v6 migration as a dedicated release item; then upgrade redux/react-redux/vite together.

### 7. No automated tests — [ ] open
- **Evidence:** no `test` script in [package.json](package.json); no `*.test.tsx`/`*.spec.tsx` under `src/`; no Jest/Vitest config.
- **Impact:** every refactor above is unsafe. Regressions ship to archives.
- **Direction:** add Vitest + React Testing Library baseline **before** touching 1–6. See `## Plan: Baseline tests` below.

---

## Suggested ordering for the next release

Pragmatic slice (not everything at once):

1. **Baseline tests** (item 7) — prerequisite for everything else.
2. **`actions/*.jsx` → `.ts` + flip `strict` per-file** (item 2, low-risk high-value).
3. **Write down sunset plans for redux-form and react-bootstrap** (item 3 — plans only, not yet migrate).

Items 1, 4, 5, 6 are multi-release initiatives; flag owners, don't commit as single-release deliverables.

**Highest-urgency debt:** redux-form is unmaintained and still holds a large share of forms — the only item that is actively rotting, not just old.

---

## Plan: Baseline tests (item 7)

Goal: establish a test harness and a minimal safety net **before** any refactor in items 1–6 starts. Not aiming for high coverage — aiming for a trusted runner, a shared provider/render helper, and smoke coverage on the highest-blast-radius code (store, WebApi, largest page render).

### Tooling

| Concern | Choice | Why |
|---|---|---|
| Runner | **Vitest 1.x** | Native fit with existing [vite.config.ts](vite.config.ts) — same aliases, same plugin, no Jest/Babel duplication. Drop-in for future `vitest --ui` and coverage via `@vitest/coverage-v8`. |
| Component testing | **@testing-library/react 14** + **@testing-library/jest-dom** + **@testing-library/user-event 14** | React 18 compatible; user-event 14 is async-correct. |
| DOM env | **jsdom** (via `vitest` built-in) | Enough for unit + component tests; reserve browser mode for later if needed. |
| HTTP mocking | **MSW 2.x** | Mock at the network layer so [WebApi.ts](src/actions/WebApi.ts) stays unmocked. Avoids coupling tests to internals of the 2378-line wrapper. |
| Redux | No extra lib — use the existing [stores/AppStore.ts](src/stores/AppStore.ts) in a factory (`createTestStore()`). | Testing the real store catches reducer wiring bugs. |

Explicitly **not** adopting Jest. Keeping the stack single-toolchain.

### Setup work (one PR)

1. **Dev dependencies** — add to [package.json](package.json):
   ```
   vitest @vitest/coverage-v8
   @testing-library/react @testing-library/jest-dom @testing-library/user-event
   jsdom msw
   ```
2. **Scripts** — in [package.json](package.json):
   ```
   "test": "vitest run",
   "test:watch": "vitest",
   "test:coverage": "vitest run --coverage"
   ```
3. **Vitest config** — extend existing [vite.config.ts](vite.config.ts) with a `test` block (environment `jsdom`, `setupFiles`, globals on, CSS off to skip SCSS parsing in tests). Keep the existing `resolve.alias` so imports like `actions/...` keep working.
4. **Setup file** `src/test/setup.ts` — imports `@testing-library/jest-dom`, registers MSW server (`server.listen()` / `resetHandlers` / `close`), silences known console noise (react-intl missing-translation warnings for non-cs locales).
5. **Render helper** `src/test/test-utils.tsx` — exports `renderWithProviders(ui, { preloadedState?, route? })` that wraps in:
   - `<Provider store={createTestStore(preloadedState)}>`
   - `<IntlProvider locale="cs" messages={{}}>` (or a stub loader consistent with [LangProvider.tsx](src/components/shared/lang/LangProvider.tsx))
   - `<MemoryRouter initialEntries={[route ?? '/']}>` (router v5, use `Router` with `createMemoryHistory`)
6. **MSW handlers** `src/test/mocks/handlers.ts` — start with 5–10 of the most-called endpoints from [WebApi.ts](src/actions/WebApi.ts); grow on demand.
7. **ESLint** — add `eslint-plugin-testing-library` + `eslint-plugin-vitest` to [.eslintrc.cjs](.eslintrc.cjs), scoped to `**/*.test.{ts,tsx}`.
8. **Test file convention** — colocate as `Foo.test.ts(x)` next to source. No separate `__tests__` folder.

### First tests (priority order)

Target: one week of wall-clock work, ~15–25 tests. Each bullet is one small PR.

1. **Reducer smoke tests** — pure functions, zero-risk starting point.
   - Pick 3 reducers from [src/reducers/](src/reducers/): pick ones with small surface (e.g. `global/status`, `global/modalDialog`, one `refTables/*`).
   - Dispatch a couple of actions, assert state transitions.
   - Outcome: proves the test runner, providers, and aliasing all work.

2. **`createTestStore` + action round-trip** — integration across actions + reducers.
   - Dispatch 2–3 action creators from `actions/global/*.jsx`, assert store shape.
   - Flushes out any untyped-action-payload issues early (feeds item 2 in the roadmap).

3. **MSW + one WebApi call** — verify the network-mocking path works.
   - Mock `GET /api/fund` (or similar), call the corresponding `WebApi.xxx(...)`, assert response shape.
   - Intentionally **do not** test all of `WebApi.ts` — that comes later.

4. **One small pure component** — render smoke test through `renderWithProviders`.
   - Candidate: [ValidationResultIcon.tsx](src/components/ValidationResultIcon.tsx) or similar small `.tsx`.
   - Asserts: renders without throwing, correct class/icon for given props.

5. **One page render smoke** — the real value target.
   - Candidate: a smaller page first, e.g. [pages/admin/AdminLogsPage.jsx](src/pages/admin/AdminLogsPage.jsx), not ArrPage yet.
   - With MSW handlers for the page's initial fetches, preloaded store state, and `renderWithProviders`, assert that it mounts without errors and the main heading appears.
   - This is the canary for future refactors: if a class→function conversion breaks page mounting, this catches it.

### CI integration

- Add `npm test` to the existing build pipeline (Maven `exec:exec` profile or GitHub Actions — confirm with DevOps which one runs on PRs).
- Fail the build on test failures.
- **Not** enforcing a coverage threshold yet — set up `--coverage` reporting but leave the gate off for the baseline release; introduce a minimum (e.g. 20%) only after item 2 refactors land.

### Definition of done for "baseline tests" release

- [ ] Vitest + RTL + MSW installed and configured
- [ ] `npm test` runs green locally and in CI
- [ ] `src/test/test-utils.tsx` with `renderWithProviders` exists and is used by at least one test
- [ ] MSW server lifecycle wired in [src/test/setup.ts](src/test/setup.ts)
- [ ] ≥15 tests across reducers/actions/component/page-smoke categories
- [ ] `README.md` or this file has a "How to write a test" 10-line snippet
- [ ] CI blocks PRs on test failures

### WebSocket / STOMP handling

Several pages depend heavily on STOMP (see [websocketActions.jsx](src/websocketActions.jsx)) — `window.ws = new websocket(...)` is constructed at module import time and wraps `@stomp/stompjs` `Client`. Tests must **never** open a real socket.

Approach: **mock `@stomp/stompjs` globally** in [src/test/setup.ts](src/test/setup.ts) via `vi.mock` and route `new Client(...)` to [FakeStompClient](src/test/mocks/stomp.ts). The fake records published frames, exposes subscriptions, and lets tests simulate inbound frames with `deliverFrame(destination, body)` or STOMP/WS errors with `triggerStompError` / `triggerWebSocketClose`.

Tests that care about WebSocket behavior grab the fake with `getLatestStompClient()` and drive it like a puppet. Tests that don't care do nothing — the mock is installed globally and the singleton in `websocketActions.jsx` silently gets a fake client.

Also stubbed in setup:
- `globalThis.serverContextPath = ''` — read at module top by [websocketActions.jsx](src/websocketActions.jsx) to build the STOMP URL.

### Known risks / open questions

- **redux-form & react-intl in tests:** both need providers; `renderWithProviders` in [src/test/test-utils.tsx](src/test/test-utils.tsx) wraps them. Validate with the first page-smoke test.
- **`.jsx` files without types:** tests on them will work but won't add type safety. That's fine for the baseline; typing work belongs to item 2.
- **SCSS imports:** Vitest `css: false` (set in [vitest.config.ts](vitest.config.ts)) so SCSS is skipped, not parsed — otherwise test startup is slow.
- **jQuery + `across-tabs` + `dom-scroll-into-view` and similar browser-only libs:** may need per-test mocks; handle lazily when the first test that imports them fails.
- **MSW 2 + Node:** requires Node 18+. Verify the CI image before wiring CI.

### Effort estimate

- Setup PR: **1–2 days** (tooling + helpers + first reducer test as proof).
- First-tests PRs: **3–5 days** spread across the team.
- Total: **~1 week** of focused work for one developer, or 2 weeks part-time.

### Status (2026-04-23)

Setup PR scaffolding drafted on branch `Branch_dec979e2`:

- [x] [package.json](package.json) — `test` / `test:watch` / `test:coverage` scripts + devDeps (vitest 1.6, @testing-library/*, jsdom, msw 2.6, @vitest/coverage-v8)
- [x] [vitest.config.ts](vitest.config.ts) — jsdom env, aliases mirror vite config, `css: false`
- [x] [src/test/setup.ts](src/test/setup.ts) — jest-dom matchers, `window.serverContextPath` stub, `@stomp/stompjs` mocked, MSW lifecycle
- [x] [src/test/test-utils.tsx](src/test/test-utils.tsx) — `createTestStore`, `renderWithProviders` (Provider + IntlProvider + MemoryRouter)
- [x] [src/test/mocks/stomp.ts](src/test/mocks/stomp.ts) — `FakeStompClient` with `deliverFrame` + error triggers
- [x] [src/test/mocks/handlers.ts](src/test/mocks/handlers.ts) + [server.ts](src/test/mocks/server.ts) — MSW placeholders
- [x] [src/stores/app/status.test.ts](src/stores/app/status.test.ts) — reducer smoke test (5 cases)
- [x] [src/test/mocks/stomp.test.ts](src/test/mocks/stomp.test.ts) — proves the STOMP mock contract; doubles as example
- [ ] `npm install` + `npm test` green locally (**waiting on reviewer**)
- [ ] CI wiring (`npm test` in PR pipeline)
- [ ] One page-mount smoke test (candidate: [AdminLogsPage.jsx](src/pages/admin/AdminLogsPage.jsx))
- [ ] "How to write a test" snippet in README

### How to write a test (quick reference)

**Reducer test** — no providers needed:
```ts
import { describe, it, expect } from 'vitest';
import myReducer from './myReducer';

it('handles FOO', () => {
    expect(myReducer(initialState, { type: 'FOO' })).toEqual({ ... });
});
```

**Component test** — use `renderWithProviders`:
```tsx
import { renderWithProviders, screen } from 'src/test/test-utils';
import MyPage from './MyPage';

it('renders the title', () => {
    renderWithProviders(<MyPage />, {
        preloadedState: { /* slice of the store */ },
        route: '/some/route',
    });
    expect(screen.getByRole('heading', { name: /title/i })).toBeInTheDocument();
});
```

**Simulating a WebSocket event**:
```ts
import { getLatestStompClient } from 'src/test/mocks/stomp';

// After something in the code under test calls `ws.connect()` and subscribes:
getLatestStompClient().deliverFrame('/topic/api/changes', {
    eventType: 'FUND_UPDATE',
    ids: [42],
});
// assert resulting store state / UI
```

**Mocking an HTTP response for one test** — override via MSW:
```ts
import { http, HttpResponse } from 'msw';
import { server } from 'src/test/mocks/server';

server.use(http.get('/api/fund/42', () => HttpResponse.json({ ... })));
```

---

## Completed

_(Move items here with release tag + date once shipped.)_
