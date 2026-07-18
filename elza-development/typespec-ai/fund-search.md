# Proposal: `searchFunds` — fund lookup/listing tool (+ batch node fetch and bulk soft-validation notes)

Status: **IMPLEMENTED, all sides** (2026-07-18) — `main.tsp` 0.10.0
(`searchFunds` in `StandardToolName`, `SearchFundsParams`/`SearchFundsResult`,
`FundInfo.rootNodeId`), regenerated OpenAPI; Elza side: `SearchFundsTool`
executor, `rootNodeId` on context `elza.fundInfo`
(`AiContextResolver.buildFundInfo`), fund links in the activity feed
(`AiActivityMapper`); provider side (`elza-ai-provider.git`): copied OpenAPI,
`search_funds` ToolDef + routing in `ChatToolset`, both adapters, prompt
guidance, `FundInfoContext` renders the context fund's root node id. §4 (batch
node fetch) and §5 (bulk soft validation) remain future work. Companion of
[node-search.md](node-search.md) and
[investigation-tools.md](investigation-tools.md); contract changes are additive
TypeSpec as usual (deployment order: TypeSpec here first → `npm run build` →
provider copies the emitted OpenAPI → Elza executor ships with the client
release).

## 0. Why

The model can today search *description units* (`searchNodes`), search and fetch
*entities* (`searchEntities`, `getArchivalEntity`) and navigate the arrangement
tree of a level it already knows (`getArchivalDescription`). What it cannot do
is resolve a **fund**: "which fund holds X?", "do we have the fund *Obecná
škola Horní Lhota*?", "what funds does institution Y hold?", "open fund number
127". Funds surface only *indirectly*, as the grouping of `searchNodes` hits —
there is no way to find a fund by its own identity (name, fund number, internal
code, mark) and no way to *enter* it (nothing returns the fund's root level, so
even a known fund cannot be browsed with `getArchivalDescription`'s windows).

`searchFunds` closes both gaps: query funds by any identifier, get back their
`FundInfo` **plus the root node id** — the entry point for
`getArchivalDescription(nodeId, withChildren)` browsing.

## 1. What Elza can serve (verified)

`FundRepositoryImpl.findFundsWithPermissions(search, institutionId, firstResult,
maxResults, userId)` (`FundRepositoryImpl.java:162`) already does almost exactly
this:

- **One query string matches every identifier**: `LOWER(f.name) LIKE :search OR
  LOWER(f.internalCode) LIKE :search OR CAST(f.fundNumber AS text) LIKE :search
  OR LOWER(f.mark) LIKE :search` (`buildFundFindQuery`, `:200-207`) — so the
  wire tool needs a single `fulltext` parameter, not separate exact-match
  fields; "127" finds fund number 127 as well as any fund with 127 in the name.
- **Permissions inside the query**: the `WithPermissions` variant joins
  `usr_permission_view` for the user (same pattern the AI tools already follow:
  `ADMIN`/`FUND_RD_ALL` → unrestricted `findFunds`, else the permission join —
  see `UserService.findFundsWithPermissions`, `UserService.java:283-293`).
- **Paged with a true total**: `firstResult`/`maxResults` + a count query,
  ordered by name — funds are a bounded, ordered list (hundreds, not millions),
  so *windowed listing is legitimate here*, unlike node/entity search.
- **Root node**: each fund's open version (`ArrFundVersion` with
  `lockChange == null`) carries `getRootNode()` (`ArrFundVersion.java:131`);
  the executor resolves it per returned page (≤ page size lookups, batchable
  via the version repository). The version also yields `ruleSetCode`.
- `institutionId` narrowing exists; resolving an `institutionCode`
  (`ParInstitution.internalCode`) to the id is one lookup.

Executor cost is one JPA query + one open-version lookup per hit — no index
work, no new query facility.

## 2. Contract sketch (additive, bump to 0.10.0)

```tsp
enum StandardToolName {
  // …existing…
  searchFunds,
}

/**
 * Arguments of the `searchFunds` tool. All optional: an unconstrained call
 * lists the funds the requesting user may read, ordered by name — funds are a
 * bounded ordered list, so (unlike `searchNodes`) listing and paging are the
 * intended access pattern.
 */
model SearchFundsParams {
  /**
   * Query matched against every fund identifier at once: name, internal
   * (evidence) code, fund number and mark/signature (case-insensitive
   * substring). "127" finds fund number 127 as well as names containing 127.
   */
  fulltext?: string;

  /** Restrict to funds of one holding institution (`ParInstitution.internalCode`). */
  institutionCode?: string;

  /** 0-based offset of the window (default 0). */
  from?: int32;

  /** Requested maximum funds in the window; the client may cap it lower. */
  limit?: int32;
}

/** Result of the `searchFunds` tool: a window of the matching funds, ordered by name. */
model SearchFundsResult {
  /** The window's funds; each carries identity, institution and `rootNodeId`. */
  funds: FundInfo[];

  /** 0-based index of `funds[0]` within the whole matching list. */
  from: int32;

  /** Total matching funds (before windowing). */
  totalCount: int64;
}
```

Plus **one additive field on `FundInfo`** instead of a new hit model:

```tsp
model FundInfo {
  // …existing fundId / name / internalCode / ruleSetCode / fundNumber / mark /
  // unitDate / institution…

  /**
   * Root level of the fund's open version (`ArrNode.nodeId`); echo-only — the
   * entry point for exploring the fund's arrangement with
   * `getArchivalDescription` (`withChildren` from here walks the tree).
   */
  rootNodeId?: int32;
}
```

Why reuse `FundInfo` rather than a `FundSearchHit`: it already carries every
field a hit needs, the client already marshals it, and — the synergy — the
`elza.fundInfo` **context** object gets `rootNodeId` for free: a conversation
opened over a fund can then browse that fund's tree immediately
(`get_archival_description(rootNodeId, withChildren)`) with **zero searches**.
That alone fixes today's dead end where the model knows the fund it is in but
has no node id to start from.

Window semantics (`from`/`totalCount`, no `partial`): consistent with
`NodeWindow`, because ordered bounded lists page by design; `searchNodes`-style
"refine, don't page" is for unbounded fulltext hit sets, which this is not.

## 3. The model's perspective vs. the wire (provider side)

The wire tool is a plain permission-scoped metadata search. The model-facing
tool (`search_funds` in `ChatToolset`) is *mostly* the same — always
client-executed when offered, like `search_nodes`/`search_entities` — but the
provider layer differs in what it *teaches* and *renders*:

- **Steering**: the prompt distinguishes three asks — *find records* →
  `search_nodes`; *find/resolve a fund* → `search_funds`; *read/browse* →
  `get_archival_description`. And: the fund of the working context already
  carries its identity and `rootNodeId` — don't search for the fund you are in.
- **Listing is allowed**: unlike the other searches, an unconstrained
  `search_funds` (or institution-scoped) listing is legitimate; the tool
  description says so, with the window mechanics (`from`, `totalCount`) the
  model already knows from `getArchivalDescription` windows.
- **Rendering**: compact one-line-per-fund (name, number/mark, institution,
  dating, `fundId`, `rootNodeId`) — the result is a *directory*, not content.
  Returned `fundId`s feed `SearchNodesParams.fundIds`; `rootNodeId` feeds
  navigation. Fund hits join the turn's known-funds so follow-up scoping is
  free.
- **No hybrid**: `get_archival_entity`/`get_archival_description` are hybrids
  because their data may already be in context; a fund *search* has nothing to
  serve internally (context holds at most the current fund, whose identity is
  already in the prompt). Plain escalation, shared per-turn client budget.

## 4. Batch node fetch — `getArchivalDescription` over an array (separate task)

Should `getArchivalDescription` accept `nodeIds: int32[]`? Analysis:

- **The protocol already batches per round**: `Task.toolCalls` is a list and
  the client answers all calls in one `POST …/tool-results` — so *N* fetches
  the model emits in parallel already cost **one round-trip**. The real costs
  of the miss-by-one-call pattern are: models emit parallel tool calls
  unreliably; and the per-turn client budget counts *calls*, so reading five
  search hits burns the whole budget (5) that one batched call would count as
  one.
- **The array is not additive on the existing tool**: `ArchivalDescriptionDetail.node`
  is required (singular), and the navigation windows (children/siblings
  centered on an anchor) are single-anchor semantics — grafting `nodeIds` onto
  them muddies both models.
- **Recommendation**: keep it a **separate, additive task** (as suspected) —
  a new wire tool `getArchivalDescriptions { nodeIds: int32[] }` (cap ~20) →
  `{ nodes: { node, fundId, ruleSetCode }[], errors? }`, per-node misses
  reported without failing the batch, **no navigation**. Model-facing it stays
  *one* tool: `get_archival_description`'s `nodeId` grows to accept a list; the
  provider routes 1 → the existing tool, >1 → the batch tool when offered
  (fallback: fan out within one round). Elza executor reuses the single-fetch
  path per id — trivial.
- Priority: it serves the **investigation flow** (search → read several hits),
  not the bulk-validation task below (which pushes nodes as parameters and
  never fetches them) — so it can wait until the pilot shows hit-reading is
  budget-starved.

## 5. Bulk "soft" validation task — cost data for further consideration

The future task (advisory validation of language, terminology, logic — the
things rule-based validation cannot catch) is `elza.revision`
([tasks/elza-revision.md](tasks/elza-revision.md)); its design already embodies
the central economic decision: **never node-by-node**. Data points and levers
to hold onto:

- **Windowing** (already designed): one task per window of ~25 nodes in tree
  order + ancestors as context ⇒ ~4–10k input tokens + a few hundred output
  per task; a 1000-node run ≈ 40 tasks. Window size is admin-config, so the
  cost/quality trade is tunable without contract change.
- **Push, don't pull**: at this volume the tool loop is the wrong shape — node
  windows travel as task *parameters*; tools (§6a of the revision doc) exist
  only for on-demand context (window-edge siblings etc.), budget-capped, and a
  baseline run can ignore them entirely.
- **Profile = the cost knob the contract already has**: `SubmitTask.profile`
  (and `GET /info` profiles) lets a language-quality pass run on an economy
  model while `logic`/`dating_consistency` runs on a stronger one. Splitting
  checks across profiles = splitting the `checks` array across two runs — no
  contract change.
- **Vendor batch APIs (~50 % discount)**: the async task loop (submit →
  long-poll, no interactivity) is exactly the shape vendor Batch APIs want.
  A provider can route an economy profile through them; the only visible
  effect is latency, which the bulk action already tolerates. If profiles
  prove too coarse, an advisory `SubmitTask` field (e.g. `priority:
  interactive | batch`) is an additive later option — collect pilot data
  first.
- **Provider-side prompt caching**: the per-task prefix (system prompt, check
  catalog, item-type dictionary) is identical across a run's ~40 windows;
  submitted with small parallelism the cache hits, so the marginal window
  costs mostly its own nodes.
- **Elza-side reducers** (the biggest multiplier, all pre-contract):
  - *incremental runs* — send only nodes changed since the last AI revision
    (Elza's `arr_change` tracking knows); re-running a fund weekly then costs
    proportional to edits, not size;
  - *sampling* — a fund-level "how is the language" verdict needs a sample,
    not exhaustion; exhaustive runs are for pre-publication gates;
  - *cheap screens first* — deterministic pre-filters (length, casing
    anomalies, OCR-artifact regexes) can shortlist suspicious nodes for the
    expensive checks;
  - `excludeItemTypes` already strips never-relevant items (storage ids…).
- **What the pilot must measure** (revision doc §7): tokens/window, tool-turn
  frequency, acceptance rate per check × promptVersion, cost per 1000 nodes
  and per *accepted* finding — these numbers decide window size, profile
  split, and whether batch routing is worth surfacing in the contract.

## 6. Elza executor sketch

`SearchFundsTool implements AiTool` (name `SEARCH_FUNDS`), mirroring
`SearchNodesTool`'s permission pattern:

1. `ADMIN`/`FUND_RD_ALL` → `findFunds(search, institutionId, from, limit)`;
   else `findFundsWithPermissions(search, institutionId, from, limit, userId)`
   (permission join inside the query, nothing post-hoc).
2. `institutionCode` → `ParInstitution` lookup; unknown code → empty result
   (not an error; the model adapts).
3. Per hit: open `ArrFundVersion` → `rootNodeId`, `ruleSetCode`; map to
   `FundInfo` (the same mapping the context resolver uses, + `rootNodeId`).
4. Caps: default/max window size ~50; `from`/`totalCount` from
   `FilteredResult`.

Registered as a bean → `AiToolRegistry` advertises it automatically; no other
Elza wiring.

## 7. Open questions

1. **`institutionCode` in v1?** The repository variant exists and "list an
   institution's funds" is a real ask — but v1 could ship `fulltext`-only and
   add it additively. Leaning: include it (cheap, and the institution name is
   *not* covered by the fulltext LIKE).
2. **Institution-name matching** — the fulltext matches fund identifiers only.
   "Funds of the district archive in Z" needs either `institutionCode` (model
   gets the code from context/entity search) or a widened executor-side match
   over `institution.name`. Defer until asked-for.
3. **`FundInfo.rootNodeId` in context objects** — filling it when Elza builds
   `elza.fundInfo` context is a one-liner in the context resolver and makes
   fund browsing work without any search; ship together with the tool.
4. **Closed funds / historical versions** — v1 serves open versions only
   (matches everything else the tools expose).
