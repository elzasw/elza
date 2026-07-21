# Implementation guide: investigation tools — `searchEntities`, `getArchivalDescription`, `searchNodes.referencesEntityId`

**Status: contract + provider implemented (2026-07-14); Elza executors
implemented per this guide (2026-07-14): `SearchEntitiesTool`,
`GetArchivalDescriptionTool`, the `searchNodes` extension.**
Contract in [main.tsp](main.tsp); provider side
in elza-ai-provider.git (`search_entities` model tool, the
`get_archival_description` hybrid with tree navigation, the extended
`search_nodes`; design record:
`elza-ai-provider.git/doc/archival-investigation-tools-proposal.md`).
Companion to [node-search.md](node-search.md) (the `searchNodes` executor this
builds on) and the `getArchivalEntity` executor (`GetArchivalEntityTool`),
whose patterns — `AiTool` registration, `TransactionTemplate`, owner-permission
enforcement on the poller thread, capped results — all three pieces reuse.

Settled decisions baked into the contract (do not relitigate here): no
`getEntityUsage` tool (usage = the two searches); `searchEntities` searches
`NEW` + `APPROVED` entities; `getArchivalDescription` always returns the
level's full items (no `withItems` flag); children/sibling listings are
**windows** designed for levels with ~10 000 nodes.

## What the model does with these (context for testing)

- *"Do we have an entity for X?" / duplicate check* → `searchEntities`
  (fulltext + `typeCode` + `onlyMainPart`).
- *"Find X's children / who references X"* → `searchEntities` with `relatedTo`
  (relations are one-way; incoming refs are indexed).
- *"Where is entity X used in descriptions?"* → `searchNodes` with
  `referencesEntityId`.
- *"What does this series contain / what precedes this unit?"* →
  `getArchivalDescription` with `withChildren` / `withSiblings` (windowed).
- A search hit (title-only `NodeHit`) → `getArchivalDescription` by `nodeId`
  reads its items; the provider caches them, so the model's follow-up items
  ask costs no second round-trip.

---

## 1. `SearchEntitiesTool` (new `AiTool`)

Wire: `SearchEntitiesParams` → `SearchEntitiesResult` (hits are
`ArchivalEntityInfo` — identity + classification + `preferredName`, no parts).

### Backing search

The existing AP search, both backends behind one filter
(`ApAdvanceSearchFilter`):

- **fulltext present** → Lucene over the cached-AP index:
  `ApCachedAccessPointRepositoryImpl.findApCachedAccessPointisByQuery(search,
  filter, apTypeIdTree, scopeIds, state, revState, from, count, sdp)` — paging
  + `total().hitCount()` built in.
- **no fulltext** (typeCode/relatedTo only) → Criteria path:
  `AccessPointService.findApAccessPointBySearchFilter(...)`
  (`ApStateSpecification`; the relation filter has its Criteria equivalent —
  `RECORD_REF` comparator `CT_EQ`).

The routing precedent is `AccessPointController.accessPointSearch`
(`POST /accesspoint/search`): non-empty `search` → Lucene, else Criteria.

### Parameter mapping

| Wire | Filter | Notes |
|------|--------|-------|
| `fulltext` | `filter.search` | tokenizer handles `"…"` phrases already |
| `area` (`preferNames`/`allNames`/`allParts`) | `filter.area` = `PREFER_NAMES`/`ALL_NAMES`/`ALL_PARTS` | default `ALL_NAMES` when absent; `ENTITY_CODE` deliberately not exposed |
| `onlyMainPart` | `filter.onlyMainPart` | `ALL_PARTS` forces it false in the repo — fine |
| `typeCode` | **`apTypeIdTree` parameter, NOT `filter.aeTypeIds`** | resolve `ApType.code` → id via `StaticDataProvider`, expand with `ApTypeRepository.findSubtreeIds(...)`. **Trap**: the Lucene path ignores `filter.aeTypeIds` — the tree must go in as the repo-call parameter. Unknown code → `ToolResult.error` ("unknown entity type code …"), not an empty hit list. |
| `relatedTo.accessPointId` | `filter.relFilters = [ApSearchByRelation{code=id}]` | `relTypeId == null` already means "ANY reference" (matches the shared `rel_ap_id` index field) — exactly the incoming-relations semantics |
| `relatedTo.relationTypeCode`/`relationSpecCode` | `relFilters[0].relTypeId`/`relSpecId` | resolve codes → ids via `StaticDataProvider` (`getItemTypeByCode` / spec lookup); unknown code → `ToolResult.error` |
| `limit` | `count` | cap server-side (recommend `MAX_TOTAL_HITS = 50`, mirroring `SearchNodesTool`); `partial = totalCount > returned` |

Validation: at least one of `fulltext` / `typeCode` / `relatedTo` — otherwise
`ToolResult.error` (the provider's tool description already tells the model
this, but the executor must not serve an unconstrained dump).

### Permissions & state — the critical part

Run on the poller thread with the conversation owner's identity (the
`SearchNodesTool` pattern):

- **Scopes**: resolve the owner's readable scope ids the way
  `AccessPointService.getScopeIdsForSearch(...)` does (`AP_SCOPE_RD_ALL` → all,
  else `userService.getUserScopeIds()`), and pass them as the `scopeIds`
  parameter — the restriction lands **inside the query** (`SCOPE_ID` field /
  `ap_state.scope_id`), never post-hoc. Empty readable-scope set → empty
  result, not an error.
- **State**: search `ApState.StateApproval.NEW` + `APPROVED` only (settled
  decision Q2). The repo call takes a single `state` parameter — either extend
  it to accept a set, or (simpler) leave `state = null` and add the two-state
  restriction to the filter/spec; pick whichever touches less. Deleted/
  replaced entities must not surface.

### Result mapping

`CachedAccessPoint` → `ArchivalEntityInfo`: `accessPointId`, `uuid`,
class/type names+codes (root vs. own `ApType` via `StaticDataProvider` — the
same mapping `AiContextResolver` uses for context entities), `preferredName`
from the preferred part's `DISPLAY_NAME` index, external binding when present.
Order: Lucene score order as returned (best match first — the contract says
so). `totalCount` from the query, `partial` on any truncation.

---

## 2. `GetArchivalDescriptionTool` (new `AiTool`)

Wire: `GetArchivalDescriptionParams` → `ArchivalDescriptionDetail`.

### Resolving the anchor

- Exactly one of `nodeId` / `uuid` (else `ToolResult.error`). `uuid` →
  `ArrangementInternalService.findNodeByUuid(...)`.
- Fund + open version from the node
  (`arrangementInternalService.getOpenVersionByFundId(...)` — the
  `SearchNodesTool` pattern). Node not found / deleted → `ToolResult.error`
  ("level … does not exist or is not accessible").
- **Permission**: the owner must hold `FUND_RD`/`FUND_RD_ALL`/`ADMIN` for the
  node's fund — same check `SearchNodesTool` applies via `UserPermission`; a
  denied fund answers `ToolResult.error`, indistinguishable from not-found
  (don't leak existence).

### The node itself — always with items

Build the same `ArchivalDescription` payload `AiContextResolver` builds for
context levels (title, referenceMark, depth, hasChildren, items with nested
entity/structuredObject refs — reuse that mapper; `focus` stays absent).
There is **no items flag**: the wire always carries the full items. Set
`fundId` and `ruleSetCode` (the fund version's rule set code) on the detail —
the provider drives the `getItemTypes` dictionary round off it, exactly like
`ArchivalEntity.ruleSetCode`.

### Windows — designed for 10 000-node levels

Work off the per-version tree cache
(`LevelTreeCacheService.getVersionTreeCache(version)` →
`Map<Integer, TreeNode>`; `TreeNode.getChildren()` is **ordered**):

1. **Window first, decorate after.** Take the ordered id list (children:
   `node.getChildren()`; siblings: `node.getParent().getChildren()`, root → a
   single-element list), compute the window as an in-memory `subList`, and
   only then decorate the ≤ window-size ids with
   `levelTreeCacheService.getNodesByIds(windowIds, version)` → title +
   `referenceMark` + `hasChildren`. Cost is proportional to the window, never
   the level. (`getNodeSiblings(fromIndex, maxCount, …)` is the existing
   precedent incl. the anchor index — `SiblingsNew.nodeIndex`; the children
   side has no public windowed accessor yet — this is the known small
   refactoring.)
2. **Sibling window default = centered on the anchor**: when `siblingsFrom` is
   absent, `from = clamp(anchorIndex - limit/2, 0, max(0, total - limit))`;
   when present, use it as the 0-based offset. Always report
   `NodeWindow.nodeIndex` = the anchor's index in the whole sibling list.
3. **Children window**: plain `childrenFrom` offset (default 0); no
   `nodeIndex`.
4. `NodeWindow.from` = the actual offset of `nodes[0]`, `totalCount` = the
   whole list's size. Cap the window server-side (recommend
   `MAX_WINDOW = 50`) regardless of the requested `limit`.
5. **Parents** (`withParents`): `getNodeParents(...)` gives root → immediate
   parent with titles; never windowed (depth is small).
6. `NodeHit` mapping: `nodeId`, `title`, `referenceMark` — note the tree-cache
   VOs carry **no uuid**; omit it (optional on `NodeHit`).

### Transaction & registration

One `TransactionTemplate.execute` for the whole build (anchor + windows read
the same version consistently); register in `AiToolRegistry` under
`StandardToolName.getArchivalDescription` like the existing tools.

---

## 3. `SearchNodesTool` extension — `referencesEntityId`

No internal contract or index change (settled decision: the existing search
vocabulary already covers the type-agnostic reference match indirectly, so no
`NodeFieldName` value is added for the shared `rel_ap_id` field):
`ArrCachedNodeBridge` indexes every `RECORD_REF` item's entity id under the
item type's own field too, and a reference item condition with a numeric value
matches by id (`getPredicateByRecordRef`).

**`SearchNodesTool`**: when `params.referencesEntityId` is present, add a
`LogicalFilter` `OR` of `FieldValueFilter{ DescItemField(type), EQ, <id> }`
over every `RECORD_REF` item type of the static data. Combines with
fulltext/conditions/fundIds as another ANDed filter; validation becomes "at
least one of fulltext / itemConditions / referencesEntityId".

No result-shape change. (The per-item-type variant needs nothing: a
`RECORD_REF` item condition with a numeric value already matches by id —
the provider's tool description now teaches the model that.)

---

## Cross-cutting notes

- **Error style**: a failed/unserviceable call answers `ToolResult.error` with
  one human-readable sentence — the provider passes it to the model, which
  adapts (this is the `getArchivalEntity` behavior; never let an executor
  exception kill the poller loop).
- **Caps + `partial`**: always report true totals (`totalCount`) next to the
  capped payload; the model is steered to refine (searches) or aim the next
  window (navigation) rather than re-ask.
- **The provider re-renders fetched objects** — the model never sees the raw
  JSON of `ArchivalDescriptionDetail` (levels render like context levels,
  windows as position-aware lists), so field additions here are cheap; keep
  payloads faithful to the models rather than pre-formatted.
- **Testing without the provider**: the elza-ai-provider dev console
  (`/dev-console/`) submits tasks with any `tools` list and shows the pending
  `ToolCall`s + lets you paste results — the same loop these executors serve;
  its task inspector shows the stored transcript including the tool rounds.
