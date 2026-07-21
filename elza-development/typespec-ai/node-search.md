# Proposal: `searchNodes` — cross-fund search as a model tool

**Status: implemented end to end (prototype).** Contract in
[main.tsp](main.tsp) (spec 0.7.0); provider side in elza-ai-provider.git
(`search_nodes` model tool, `ChatPause` suspend/resume,
`ai_task.model_conversation`, protocol 1.5); Elza side in elza-core
(`SearchNodesTool` + `AiToolContext`, session-free `NodeSearchService` core
with the query-level fund restriction — also closing the `NodeController`
per-fund `FUND_RD` TODO — and `FundInfo.fundId` filled by `AiContextResolver`).
How the AI model gets the
ability to search description units across the archive's funds — the Elza
`POST /node/search` capability exposed as a standard tool the model can
parametrize during a chat turn. Companion to
[archival-description.md](archival-description.md), which describes the
existing context objects and the `getItemTypes` tool this proposal builds on.

## Motivation

Today `elza.chat` reasons only over the levels the client explicitly sent as
context. Questions like *"do we have any records about brewery X?"* or *"which
funds contain photographs from the 1930s?"* cannot be answered — the model has
no way to reach data the client did not send. Elza already has a cross-fund
search (`POST /node/search`, Hibernate Search over `ArrCachedNode`); this
proposal makes it callable **by the model**, with the model choosing the query.

The pieces largely exist:

- the protocol already defines client-executed tools (`awaiting_tools`,
  `ToolCall`/`ToolResult`, `StandardToolName`, `POST …/tool-results`,
  `TOOL_TIMEOUT`), with `getItemTypes` as the working precedent;
- the provider already runs a model tool loop (`search_knowledge`,
  `get_section`, `get_archival_description` in `AnthropicChatAdapter`);
- Elza already executes tools from the poller (`AiRequestPoller` →
  `AiToolRegistry` → `AiTool` beans).

What is genuinely new: the existing client tool is driven **by the handler
before the model turn** (deterministic `ContextPreparation`), while a search is
**model-initiated mid-turn** — the provider must suspend the model conversation
across the `awaiting_tools` round-trip and resume it when the results arrive.
That suspend/resume machinery is the bulk of the work, and it is reusable for
every future model-initiated fetch from Elza (e.g. a later `getNodes` tool).

## The flow

```
model turn (provider)                     Elza (client)
─────────────────────                     ─────────────
model emits tool_use: search_nodes(…)
provider: no local executor for it
  → snapshot the conversation (task row)
  → Turn.tools([ToolCall searchNodes])
  → task AWAITING_TOOLS ────────────────► poller sees awaiting_tools
                                          SearchNodesTool executes the search
                                          (user-scoped, capped, flattened)
task QUEUED ◄──────────────────────────── POST /tasks/{id}/tool-results
worker re-runs step():
  snapshot present + tool results
  → restore messages, append tool_result
  → continue the model loop (cache read)
model answers (or searches again, capped)
```

One round-trip costs one poll cycle (the client long-polls, so ~seconds). The
conversation prefix is a provider-side prompt-cache read on resume, so the
resumed call re-reads the whole history at ~0.1× price.

## Ownership (unchanged split)

- **Contract** (this repo): the tool name in `StandardToolName` and the typed
  argument/result models below.
- **Client (Elza)**: advertises the tool in `SubmitTask.tools`, executes it
  under the requesting user's permissions, caps and flattens the result.
- **Provider**: the model-facing tool definition (name, prompt/description,
  JSON Schema derived from the contract models) and the suspend/resume of the
  model conversation.

## Contract additions (`main.tsp`)

### Tool name

```tsp
enum StandardToolName {
  getItemTypes,
  searchNodes,
}
```

Doc for the catalog entry: *`searchNodes` — search description units across
the archive's funds. Arguments `SearchNodesParams`, result `SearchNodesResult`.
The client executes the search with the requesting user's read permissions and
returns a capped, flattened hit list grouped by fund.*

### Arguments

Deliberately **not** a mirror of the Elza REST `SearchParams` — the
polymorphic `AbstractFilter` tree (discriminators, recursive `logical`
nesting) is hostile to the model and to the OpenAPI generators. A flat shape
covers the useful v1 queries; conditions are ANDed. Extensions stay additive.

```tsp
/** Arguments of the `searchNodes` tool. */
model SearchNodesParams {
  /**
   * Full-text query matched against the whole description unit (all item
   * values). At least one of `fulltext` / `itemConditions` must be present.
   */
  fulltext?: string;

  /**
   * Conditions on individual description items, ANDed. Item types are named
   * by their stable codes (the same vocabulary as
   * `elza.archivalDescription` / `getItemTypes`).
   */
  itemConditions?: ItemCondition[];

  /**
   * Restrict the search to these funds. Omitted = every fund the requesting
   * user may read. Ids come from `elza.fundInfo.fundId` in context or from a
   * previous `searchNodes` result.
   */
  fundIds?: int32[];

  /**
   * Maximum number of hits to return in total (the client may cap it lower —
   * see `SearchNodesResult.partial`). Omitted = client default.
   */
  limit?: int32;
}

/** One condition on a description item, e.g. `ZP2015_UNIT_DATE` intersects `1930-1939`. */
model ItemCondition {
  /** Item-type code (`RulItemType.code`), e.g. `ZP2015_TITLE`. */
  type: string;

  /** Specification code for enumerated types, if the condition targets one spec. */
  spec?: string;

  /** How to compare the item's value. */
  operation: ItemConditionOperation;

  /** The comparison value; absent for `NOT_NULL`. Dating uses ISO-ish text (as in the UI). */
  value?: string;
}

/**
 * Comparison operators — a subset of what the Elza search supports; may grow
 * additively. `INTERSECT` applies to dating items (`UNITDATE`).
 */
enum ItemConditionOperation {
  EQ, CONTAINS, GT, GTE, LT, LTE, INTERSECT, NOT_NULL,
}
```

### Result

Flattens Elza's two-step API (`POST /node/search` → funds with counts,
`GET /node/search/{fundId}` → nodes): the client runs both steps and returns
per-fund groups with the first hits of each, in tree order. The grouping gives
the model the distribution (which funds, how many) even when hits are capped,
so it can refine (scope by `fundIds`, sharpen the query) instead of paging.

```tsp
/** Result of the `searchNodes` tool. */
model SearchNodesResult {
  /** Funds with hits, each with its first hits in tree order. */
  funds: FundHits[];

  /** Total hits across all funds (before capping). */
  totalCount: int64;

  /**
   * True when the result is incomplete — the hit cap, the per-fund cap, or the
   * search time limit was reached. The model should refine rather than page.
   */
  partial: boolean;
}

/** Hits within one fund. */
model FundHits {
  /** Fund id (matches `FundInfo.fundId`). */
  fundId: int32;

  /** Fund name. */
  name: string;

  /** Holding institution name, if known. */
  institution?: string;

  /** Total hits in this fund (may exceed `nodes.length`). */
  count: int32;

  /** The first hits, in tree order. */
  nodes: NodeHit[];
}

/** One matching description unit. */
model NodeHit {
  /** Node id — usable with `get_archival_description`-style follow-ups and UI links. */
  nodeId: int32;

  /** Node uuid. */
  uuid?: string;

  /** Tree title of the unit. */
  title?: string;

  /**
   * Reference designation from the fund root, as in
   * `ArchivalDescription.referenceMark`; its length also tells the hit's depth
   * in the arrangement tree.
   */
  referenceMark?: string[];
}
```

### `FundInfo.fundId` (small additive change)

`FundInfo` currently has no identifier, so the model could not scope a search
to "the fund in scope". Add:

```tsp
model FundInfo {
  /** Fund id (`ArrFund.fundId`) — lets tools reference the fund, e.g. `searchNodes.fundIds`. */
  fundId?: int32;
  // … existing fields unchanged
}
```

`AiContextResolver` fills it; optional, so old payloads stay valid.

## Provider (`elza-ai-provider.git`)

### Model-facing tool

One more `Tool` definition in the chat adapter, alongside `search_knowledge`
et al. — model-facing name `search_nodes` (snake_case like the internal
tools), schema derived from `SearchNodesParams`. The description does the
teaching; a draft:

> Search description units (levels) across the archive's funds. Use it when
> the question concerns records that are not in the working context — e.g.
> "do we have anything about X". `fulltext` matches all item values;
> `itemConditions` filter on specific item types (use codes from the item-type
> dictionary, e.g. dating via `INTERSECT`). Results are capped: when
> `partial` is true, refine the query or scope it with `fundIds` instead of
> asking again with the same terms. Each search pauses the conversation for a
> round-trip to the archive — prefer one well-chosen query over many.

The system prompt gets a short section on when searching the archive is
appropriate (and that results reflect the asking user's permissions).

### Suspend/resume of the model conversation

The new machinery. Today `ChatCapability.answer()` runs the whole tool loop
synchronously and returns a `ChatResult`; client tools would break that.

**Capability API.** `answer()` returns a sealed alternative:

- `ChatResult` — as today; or
- `ChatPause` — the model requested client tools: the pending `ToolCall`s
  (see id mapping below), the serialized conversation snapshot, and the usage
  incurred so far.

`ChatTaskHandler.step()` translates `ChatPause` into `Turn.tools(...)` (which
`TaskWorker` already stores as `AWAITING_TOOLS`), and stores the snapshot.

**Snapshot.** A provider-owned JSON model (do not serialize SDK types
directly), persisted on `ai_task`:

```
model_conversation (text column, @JdbcTypeCode(SqlTypes.LONGVARCHAR) — never @Lob, see the PG gotcha)
{
  "version": 1,
  "model": "<model id the turn ran on>",   // resume must use the same model
  "round": <tool rounds consumed>,          // MAX_TOOL_ROUNDS continues, not restarts
  "clientToolCalls": <count so far>,        // cap on searches per turn
  "messages": [ { "role": "user"|"assistant",
                  "blocks": [ {"type":"text","text":…}
                            | {"type":"thinking","thinking":…,"signature":…}
                            | {"type":"redactedThinking","data":…}
                            | {"type":"toolUse","id":…,"name":…,"input":…}
                            | {"type":"toolResult","toolUseId":…,"content":…,"isError":…} ] } ]
}
```

Thinking blocks round-trip **verbatim** (signature included) — the API rejects
a resumed tool-use continuation without them. The snapshot is written in the
same store-transaction that sets `AWAITING_TOOLS` and cleared whenever the
task reaches a terminal state or produces output.

**Id mapping.** `ToolCall.callId` = the Anthropic `tool_use` id. It is opaque
and server-assigned, which is exactly what the contract promises, and it makes
resume trivial: each returned `ToolResult.callId` names the `tool_use` block
its `tool_result` must answer. A `ToolResult.error` becomes a `tool_result`
with `is_error: true` — the model sees the failure and can adapt.

**Mixed rounds.** With parallel tool use one model round may request both
internal tools (`search_knowledge`) and `search_nodes`. Execute the internal
ones immediately, append their `tool_result`s, and only then snapshot +
suspend for the remaining client calls. On resume, the client results complete
the round.

**Resume.** `step()` sees a stored snapshot + tool results: skip
`ContextPreparation` and prompt assembly, rebuild `List<MessageParam>` from
the snapshot, append the client `tool_result` blocks, and re-enter the loop at
the recorded round. The whole prefix is a prompt-cache read when the
round-trip lands within the cache TTL (it will — the client long-polls); a
cache miss merely re-prices the prefix, correctness is unaffected.

**Interaction with the engine** (`doc/task-engine.md` in the provider repo —
its state machine already loops `RUNNING → AWAITING_TOOLS → QUEUED`):

- *Timeout / cancel*: `TimeoutSweeper` and cancel already handle
  `AWAITING_TOOLS`; the snapshot is dead data on a terminal task (cleared).
- *Restart recovery*: unchanged — `AWAITING_TOOLS` is untouched at startup;
  a task re-queued from `RUNNING` re-runs its turn from the snapshot if one
  exists (the interrupted turn's API spend is lost, as today).
- *Usage*: `Turn.tools(...)` already carries usage; `TaskWorker.addUsage`
  accumulates across suspends. No change.
- *Progress*: report "Searching the archive…" before suspending so the UI
  shows more than "waiting".

**Caps.** Client tool rounds count against `MAX_TOOL_ROUNDS`, plus a separate
cap on `search_nodes` calls per turn (e.g. 3) and a rule that the final forced
round never suspends (tools are off there already).

## Elza (`elza-main.git`)

### Executor

`SearchNodesTool implements AiTool`, registered like `GetItemTypesTool`:
convert `arguments` → `SearchNodesParams`, run the search, map to
`SearchNodesResult`.

Two service-level changes are needed:

1. **Session-free search.** `NodeSearchService.nodeSearch()` stores per-fund
   node lists in a `@SessionScope` holder and `nodeGetSearchResult(fundId)`
   reads them back — a UI pattern; the poller thread has no HTTP session.
   Extract a core method that returns the `ArrFundToNodeList` collection (plus
   totals/partial flag) directly; the REST controller keeps the session
   behavior on top, the tool consumes the core. Node presentation reuses
   `LevelTreeCacheService.sortNodesByTreePosition` + `getNodesByIds` (same as
   `nodeGetSearchResult`) to fill `NodeHit` title/referenceMark/depth.

2. **Permission scoping.** `POST /node/search` currently allows only
   `ADMIN`/`FUND_RD_ALL`; per-fund `FUND_RD` is an open TODO in
   `NodeController.nodeSearch`. The tool must run **as the conversation's
   user** (the poller knows the request's userId): resolve the user's readable
   fund ids, intersect with `fundIds` from the arguments, and pass the
   restriction into the search predicate. Implementation note: check whether
   `fundId` is an indexed field on `ArrCachedNode` — if not, either add it to
   the index (reindex required) or filter post-fetch (weaker: caps apply
   before filtering). This work also closes the REST TODO — same code path.

   `AiTool.execute(Object arguments)` has no caller context today; extend to
   `execute(arguments, AiToolContext ctx)` carrying the conversation's
   userId/UserDetail (a small refactor of `AiTool`/`AiToolRegistry`/
   `AiRequestPoller`; `GetItemTypesTool` ignores the context).

3. **Caps.** Server-side defaults regardless of `limit`: e.g. ≤ 50 hits
   total, ≤ 10 per fund, ≤ 20 funds listed; set `partial` when any cap or the
   existing time limit (`elza.search.node.maxTimeMs`) bites. Keep the
   serialized result well under the provider's `maxToolResultBytes`.

### Advertising

`AiConversationService.submitExchange` adds `searchNodes` to
`SubmitTask.tools` (next to `getItemTypes`). A client build without the
executor simply does not advertise it — the provider then never requests it
(the contract's versioning story, unchanged).

## Testing

- **Provider**: extend the canned adapter to emit a scripted
  `search_nodes` `tool_use` mid-turn — drives the full suspend/resume path in
  `EchoTaskFlowTest`-style tests without Anthropic; unit tests for snapshot
  round-trip (thinking blocks verbatim, round counters).
- **Elza**: executor test against a seeded fund (permission scoping incl. a
  user with partial `FUND_RD`); poller integration test with a mock provider
  returning `awaiting_tools` + `searchNodes`.
- **End-to-end**: dev-console conversation exercising a real search.

## Effort estimate

| Piece | Size |
|-------|------|
| Contract: `searchNodes` + models, `FundInfo.fundId`, regen both sides | ~0.5–1 day |
| Provider: model-facing tool + prompt guidance | ~0.5 day |
| Provider: suspend/resume (capability API, snapshot + Liquibase, resume path, caps, tests) | ~3–4 days |
| Elza: session-free search core + permission scoping (incl. the FUND_RD TODO) | ~1–2 days |
| Elza: `SearchNodesTool`, `AiToolContext` refactor, advertising, tests | ~1 day |

Roughly **1–1.5 weeks** total.

## Alternatives considered

- **Provider calls Elza directly** — impossible: Elza is not reachable from
  the internet; all connections are Elza → provider (polling). Hence the
  client-executed tool round-trip.
- **Pre-turn deterministic search** (like `getItemTypes`): would avoid
  suspend/resume, but the whole point is that the *model* chooses the query
  from the conversation; no deterministic pre-pass can do that.
- **Mirroring REST `SearchParams` in the contract**: rejected — the
  polymorphic filter tree is a poor model interface and a known generator
  hazard; the flat v1 shape covers the useful queries and grows additively.

## Phase 2 seam (out of scope)

With suspend/resume in place, a `getNodes` tool (fetch full
`elza.archivalDescription` blocks for given nodeIds) is a natural follow-up:
search finds the hits, `getNodes` drills into them — completing the
"fetch data the client did not send" extension already anticipated by the
provider's `get_archival_description` tool.

## Open questions

1. **Result verbosity** — are title + referenceMark enough for the model to
   act on, or should `NodeHit` carry a short item excerpt (e.g. the matched
   value)? Excerpts cost result bytes; the phase-2 `getNodes` tool may be the
   better answer.
2. **Dating conditions** — `INTERSECT` on `UNITDATE` needs a defined value
   format in the contract doc (the UI's interval syntax vs. ISO). Decide when
   writing the model docs.
3. **Locked versions** — the search runs over open fund versions (as the REST
   endpoint does); fine for chat, but note it in the tool description so the
   model does not claim completeness over history.
4. **Per-user vs. installation signing keys** — with an installation-wide key
   the provider cannot verify the user; permission enforcement is entirely
   client-side (it already is for context, but a search widens the blast
   radius of a mis-scoped executor — review carefully).
