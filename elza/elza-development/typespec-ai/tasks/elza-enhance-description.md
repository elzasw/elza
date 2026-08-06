# Task type `elza.enhanceDescription` — AI úprava popisu na pokyn uživatele

Status: **task definition** (2026-08-06), spec 0.14.0. Companion pages:
[tasks/elza-revision.md](elza-revision.md) (the advisory check this task
complements) and [archival-description.md](../archival-description.md) (how
levels and the item dictionary travel).

The task turns a **user instruction** over one description level into
**machine-applicable item operations** — e.g. *"extract the dimensions from
the text into the dedicated elements"* → add a width item + rewrite the source
text without the extracted fragment. Where `elza.revision` *finds* problems
unprompted and answers with advisory text, this task *acts on an explicit
request* and answers with an operational delta (`elza.nodeUpdateProposals`).
The two compose: a revision finding can be handed to this task ("prepare the
fix for this finding") as context.

**Propose-only by design.** The task never changes description data. The
pipeline is propose → validate → review → apply: the provider returns typed
proposals, Elza validates every change deterministically, the user confirms
each change, and only then does Elza apply it through its normal versioned
change machinery (`ArrChange` — standard undo and history). The archivist can
also **clarify** ("refine the request") instead of accepting, which is a
`parentTaskId` follow-up.

## 1. Task input (typed objects, spec 0.14.0)

Parameters (names as the provider's catalog declares them):

- **`subject`** — the level(s) to modify: one `elza.archivalDescription` per
  level, with:
  - `allowedItemTypes` filled (the POSSIBLE/REQUIRED item-type codes Elza's
    rules compute for the node — every `ProposedItemValue.type` must stay
    within it),
  - `itemObjectId` filled on every item (the anchor for update/delete
    operations),
  - `issues` filled (formal findings, so proposals can also target them).

`userInstructions` carries the user's request. It is what drives the task —
the Elza UI always collects and sends it; without instructions the provider
returns an empty proposal with a markdown block asking what to do.

Context (supplementary `AiObject`s):

- the **fund** — `elza.fundInfo` (identity + `ruleSetCode`, keying the item
  dictionary the provider resolves via `getItemTypes`);
- the **ancestor path** — full `elza.archivalDescription` per ancestor
  (inherited values must be visible: extraction must not duplicate a value
  the level inherits);
- optionally **`elza.revisionFindings`** — when the request is "fix finding X",
  the finding(s) provide the precise anchor.

## 2. Binding prompt constraints (provider-owned prompt, normative)

- **Catalog discipline**: `ProposedItemValue.type` only from the subject's
  `allowedItemTypes`; `spec` only from the dictionary's spec list for that
  type. When the instruction needs an element that is not allowed on the
  level, do not propose it — explain in the markdown block instead.
- **No invented entities**: a `RECORD_REF` proposal may reference only an
  entity received in the input or found via `searchEntities` /
  `getArchivalEntity`. New entities are at most named in the markdown as
  candidates (the `access_point_candidate` stance of `elza.revision`).
- **Anchoring**: update/delete operations reference the exact item by
  `itemObjectId` and quote its `currentValue` verbatim whenever the item has a
  text form — the anti-hallucination anchor and the client's staleness guard.
- **Full-state updates**: `newItem` is the item's complete new state, never a
  fragment; an update keeps the item's type (re-typing = delete + add in one
  change).
- **Consistent extraction**: moving a fact out of a free text is one
  `ProposedChange` containing the `addItem` **and** the `updateItem` of the
  source text without the extracted fragment. When removing the fragment
  would weaken or change the source's meaning, keep the source text unchanged
  and say so in `reason` (the duplication is then the archivist's informed
  choice).
- **Repeatability heuristics** (advisory — Elza's rules are authoritative and
  validate every change): free-text elements (`TEXT`, `FORMATTED_TEXT`, and
  `STRING` prose) are not repeatable; spec-carrying elements are usually
  repeatable, typically one item per spec; the level type (`ENUM`) is never
  repeatable. When unsure, prefer updating/merging into the existing item over
  adding a second one.
- **Data-type scope**: propose only text/number/date kinds, `ENUM` and
  `RECORD_REF`; never `STRUCTURED`, `FILE_REF`, `COORDINATES`, `JSON_TABLE`.
- **Language**: `reason` texts are user-facing and self-contained, in the
  language of the described material / the user's instruction.
- **Scope**: proposals only for subject levels. An empty `proposals` array is
  a valid result; the markdown block then explains why nothing was proposed.

## 3. Output

`resultTypes` = `[elza.markdown, elza.nodeUpdateProposals]`:

- **`elza.markdown`** — the narrative: what is proposed, what could not be
  done and why (element not allowed, entity not found, meaning would change).
  Rendered as the answer text in the AI panel.
- **`elza.nodeUpdateProposals`** — `proposals[]` of `NodeUpdateProposal`
  (`nodeId`, `changes[]`), each change = `reason` + `confidence` +
  `operations[]` (`ItemOperationAdd` / `ItemOperationUpdate` /
  `ItemOperationDelete`, discriminated by `kind`).

## 4. Elza-side validation & application

Validation runs before display and again at apply time; unlike
`elza.revision` (silent drop + counter), a change that fails validation is
**shown blocked with the reason** — the user explicitly asked for the action,
so silence would be confusing, and the blocked reason feeds the clarify loop.

1. `nodeId` must be a subject level — otherwise the proposal is dropped.
2. Every `ProposedItemValue.type` must be in the level's `allowedItemTypes`;
   `spec` must be valid for the type (dictionary) — otherwise blocked.
3. `itemObjectId` must reference an open item of the level — otherwise
   blocked (stale anchor).
4. An update must keep the anchored item's type — otherwise blocked.
5. Repeatability: evaluated against the rules
   (`RuleService.getDescriptionItemTypes` — the computed
   `RulItemTypeExt.repeatable`) and the level's current items; an `addItem`
   that would create a second value of a non-repeatable type is blocked.
6. Staleness: `currentValue` must match the item's current text form (and the
   client's own submit-time snapshot) — otherwise blocked as stale.
7. `accessPointId` must resolve to an entity readable by the user — otherwise
   blocked.
8. Data-type scope (see §2) is enforced: out-of-scope operations are blocked.
9. An operation of unknown `kind` (version skew) invalidates its whole change
   — never "apply the operations understood".

**Consent unit = the change.** Accept/reject applies to a `ProposedChange` as
a whole; its operations are applied together, in one versioned `ArrChange`,
through the standard description-item services (create/update/delete as new
versions — undo and history for free). Never auto-applied, no exceptions.

**Persistence & audit (v1)**: the proposal block lives on the `ai_request`
result (the transient result/audit artifact), together with `taskId` and
`promptVersion`. Per-change UI states: *proposed*, *applied*, *rejected*,
*blocked (reason)*, *superseded* (replaced by a clarify follow-up). Accept /
reject decisions are the evaluation signal: acceptance rate per
`promptVersion`, cost per applied change from `usage`.

## 5. Clarify (follow-up) loop

"Upřesnit" submits a new task with `parentTaskId` + the clarification in
`userInstructions`. Rules:

- The follow-up's result **supersedes** the previous proposal — the UI marks
  the old block superseded; individual decisions do not carry over.
- When the node changed since the parent task (some changes applied, manual
  edits), the client re-sends the level's **current** `elza.archivalDescription`
  as context — the provider holds the old conversation, not the new reality.
- Refinement works only within the provider's retention window
  (`Limits.taskRetentionDays`); an expired parent degrades gracefully to a
  fresh task with the level re-sent.
- A blocked change's reason ("would create a second value of a non-repeatable
  element") is quotable directly as clarification text.

## 6. Tools

The baseline run must work tool-less: the subject carries `allowedItemTypes`,
and the provider resolves codes via its cached `getItemTypes` dictionary.
Declared tools, under a strict budget:

- `getItemTypes` — dictionary resolution (effectively always needed once per
  rule set, then cached provider-side);
- `searchEntities` / `getArchivalEntity` — only for `RECORD_REF` proposals:
  find the existing entity to reference, never to create one.

Deliberately **no** specialized allowed-types/repeatability tool: the primary
scenario has the catalog in the payload, prompt heuristics cover the rest, and
Elza's validation is authoritative anyway. If evidence shows the model needs
per-node facts, the additive path is a structured sibling of
`allowedItemTypes` (e.g. `{type, repeatable, required}` — the same
`getDescriptionItemTypes` call already computes it) and/or a
`withAllowedItemTypes` flag on `getArchivalDescription`; both deferred.

## 7. Elza-side work items

| Piece | Where |
|-------|-------|
| fill `itemObjectId` (`ArrItem.descItemObjectId`; `ApItem.objectId` for entity parts) | `AiContextResolver` |
| fill `allowedItemTypes` for this task's subject (same as revision subjects) | `AiContextResolver` |
| proposal validation (§4) + repeatability evaluation | new service next to the AI request handling |
| change application (one `ArrChange` per accepted change) | `DescriptionItemService` orchestration |
| proposal cards UI (accept / reject / clarify, blocked states, diff render old→new) | AI panel |
| result-block mapper (`elza.nodeUpdateProposals` → display block) | analogous to `RevisionFindingsBlockMapper` |

## 8. Sizing

Same class as the revision unit check: one full level (150–400 tokens) +
ancestors + instruction ⇒ ~1–4k input tokens; output proportional to the
requested changes (a handful of operations, well under 1k tokens). Interactive,
panel-native.

## 9. Open questions

1. **Task-type name** — `elza.enhanceDescription` is the working code; rename
   before the first provider implementation if a better fit emerges (the
   `TaskType` set is open, but the code is documented in the contract).
2. **Per-operation expert override** — whether the UI offers applying a subset
   of a change's operations (default consent unit stays the change).
3. **Position of added items** — v1 appends (rule-driven ordering applies at
   render anyway); an explicit position hint only if evidence demands it.
4. **Multi-level subjects** — the shapes support several subjects/proposals;
   v1 UI targets the single active level. Branch-wide instructions ("unify
   the spelling of X across the series") come with the revision v2 windowing
   experience.
5. **Acceptance feedback to the provider** — accept/reject rates stay
   client-side in v1; a feedback channel is a separable, additive protocol
   feature.
