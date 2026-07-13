# AI archival description & item-type dictionary

How Elza sends description-unit ("level") and fund context to an AI provider,
and how the provider resolves the item codes it receives. The authoritative
definitions live in the contract ([main.tsp](main.tsp), emitted to the elza-core
OpenAPI) and the Elza services — this page is the cross-cutting map plus the
remaining work; it deliberately does not restate the model fields. Companion:
[tasks/elza-revision.md](tasks/elza-revision.md).

## The flow

When the user works on a level, the frontend sends an `AiContextNode`; Elza's
`AiContextResolver` turns it into:

- an `elza.archivalDescription` of the level — its description items as **bare
  stable codes** (`type` / `spec`) plus display text, with
  `nodeId`/`uuid`, `referenceMark`, `depth`, `parentId`, `hasChildren`, `focus`,
  the level's tree `title`, and its `issues` (problems already found by automatic
  checks). Each item states its `dataType` (same vocabulary as the dictionary's
  `ItemTypeInfo.dataType`), so a block is self-describing without a dictionary
  join. A reference item also carries its machine-readable target as a nested
  object: an access-point item (`RECORD_REF`) adds `entity` (an
  `ArchivalEntityInfo` — the referenced entity's id, preferred name and
  classification); a structured-object item (`STRUCTURED`) adds `structuredObject`
  (a `StructuredObjectInfo` — `structuredType` / `structuredObjectId` + optional
  `complement`), with `value` holding the entity's preferred name or the object's
  serialized name;
- for the `context` role, the level's **ancestors** up to the root and the
  fund's `elza.fundInfo` (which carries the fund's `ruleSetCode`).

Codes are the canonical, documentation-aligned identifiers, so they are not
expanded to names in the payload. Instead the provider calls the standard
**`getItemTypes`** tool (arguments `GetItemTypesParams`, result
`ItemTypeDictionary`) once per `ruleSetCode` to resolve codes → names / data
types / spec lists, and caches it. The same dictionary is exposed to the
frontend as `GET /rules/itemTypes?ruleSetCode=…` — both backed by one service.

## Ownership

- **Contract** owns each object/tool *interface*: the `elza.*` object types and
  the `StandardToolName` catalog with typed argument/result models.
- **Client (Elza)** owns marshalling domain data into those objects, which tools
  it supports, and executing them.
- **Provider** owns the model-facing prompt / tool description and derives the
  tool's argument schema from the contract.

## What shipped

| Piece | Where |
|-------|-------|
| `elza.archivalDescription` + `DescriptionItem`; `FundInfo.ruleSetCode` | `main.tsp` |
| level `title` + `issues` (`NodeIssue` / `NodeIssueKind`) | `main.tsp` |
| per-item `dataType`; nested reference objects — `entity` (`ArchivalEntityInfo`), `structuredObject` (`StructuredObjectInfo`) | `main.tsp` |
| `getItemTypes` standard tool; typed tool catalog `StandardToolName` | `main.tsp` |
| `GET /rules/itemTypes?ruleSetCode` filter | `typespec/main.tsp` + `elza-openapi.yml`; `RulesController` / `RuleService` |
| node & fund context → provider objects (roles, ancestors, `FUND_RD`) | `AiContextResolver` |
| tree title (`getNodesByIds`), issues (node conformity), reference-item ids | `AiContextResolver` |
| tool loop (`awaiting_tools` → execute → tool-results) | `AiRequestPoller`; `AiTool` / `AiToolRegistry` / `GetItemTypesTool` |

## Future work

- **Locked versions** — the resolver reads the fund's *open* version;
  `AiContextNode.fundVersionId` is reserved for locked-version reads.
- **Coordinates / JSON-table items** — emitted as code-only (the fulltext
  renderer yields no text); the print convertors could fill their `value`.
- **Lean ancestors** — ancestors currently carry full items; an
  identifying-items-only mode (title + dating) would trim the payload, but needs
  a rule-set-aware notion of "identifying" item types.
- **Multiple funds in one request** — a single active level ⇒ one fund; if
  levels from several funds are ever sent together, add a fund reference on the
  level to disambiguate against its `elza.fundInfo`.
- **Access-restriction filtering** — structured items such as "omezení
  přístupnosti" are now *represented* (`structuredObjectType`/`structuredObjectId`),
  but *filtering* restricted content out of what the AI sees, plus an
  `excludeItemTypes` filter (technical/storage identifiers), is still open and
  aligns with the revision task.
- **`elza.revision` alignment** — fold its input onto the shared
  `DescriptionItem` + `getItemTypes` dictionary instead of a bespoke blob.
