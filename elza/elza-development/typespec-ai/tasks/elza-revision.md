# Task type `elza.revision` — AI obsahová kontrola archivního popisu (v2)

Status: **task definition** (2026-07-20). Supersedes the v1 draft (windowed
`outputSchema` design — it predated the typed-object protocol; the old text is
in git history). The methodics grounding — what the check looks for, why, and
the ZP2015 citations — is `doc/kontrola-popisu.md` in `elza-ai-provider.git`;
this page is the **normative contract**: the typed inputs/outputs (spec
0.13.0), the checks catalog codes, the Elza-side validation and `WfIssue`
mapping, and the phasing.

The task contract (typed object shapes) is owned by Elza and versions with
Elza releases; the provider owns the prompt (reported back as
`promptVersion`) and the multi-phase execution inside one task.

**Advisory by design**: the task produces *suggestions for professional
assessment*, never data changes and never authoritative errors. The AI must
not change description data, must not declare an archival statement factually
wrong on linguistic evidence alone, and every finding must be anchored to a
verbatim excerpt. Elza's deterministic validation (rules → `ArrNodeConformity`)
stays authoritative; this task complements it with what formal validation
cannot see.

## 1. Checks catalog

Category codes are **Elza-owned and additive**; the provider prompt describes
each, unknown codes produce no findings. Grounding: `doc/kontrola-popisu.md`
§"Co má kontrola hledat" (with ZP2015 citations).

| Code | Looks for | Needs |
|------|-----------|-------|
| `hidden_data` | structured facts hidden in free text (dating, language, physical state, dimensions, access restrictions, originator info) despite a dedicated element existing; formal title / thematic description in the wrong field | items + allowed-types catalog |
| `note_misuse` | content/formal/technical data hidden in the internal note; ordinary facts in the public note despite a defined element (ZP2015: public note only for significant facts expressible nowhere else) | items |
| `wrong_level` | parent-level information valid only for some children; branch-common information needlessly repeated per unit; detailed enumeration at series level | items + hierarchy (outline) |
| `duplicate` | repetition of dating/originator/other facts inside content; verbatim repetition of a value inherited from a higher level; the same information in several free texts (ZP2015 on obsah/regest: do not repeat other elements or higher levels; Elza presents content merged with inherited values, so the check must know the hierarchy) | items + ancestors |
| `formulation` | vague expressions ("různé", "ostatní", "běžná agenda"), display-dependent references ("viz výše"), unexplained local abbreviations, internal instructions in public elements, evaluative/speculative/anachronistic wording, unreadable lists or overlong sentences, grammar errors changing meaning — **language heuristics, not automatic ZP2015 violations** | items (title incl.) |
| `contradiction` | text stating a different dating/count/language/originator than the structured element; title vs. content describing different things; child contradicting a parent's general statement; public vs. internal variants conflicting beyond necessary anonymization | items + ancestors |
| `inconsistency` | similar units named differently within a series/branch; alternating abbreviations, name order, place names, formulation templates; similar facts recorded in different elements | outline (titles) + item samples |
| `access_point_candidate` | persons, corporations, places, events that could be access-point candidates — flagged as *candidates for assessment only*, never auto-created | items |

The earlier "title checks" (nominative-case head phrase — *hlavní část popisu
v 1. pádu* — and title repetition) live inside `formulation` and
`inconsistency`/`duplicate`; they are prompt content, not separate codes.

## 2. Task input (typed objects, spec 0.13.0)

Parameters (names as the provider's catalog declares them):

- **`subject`** — the reviewed unit(s): one `elza.archivalDescription` per
  reviewed level, **with `allowedItemTypes`** filled (the POSSIBLE/REQUIRED
  item-type codes Elza's rules compute for the node — the catalog element
  suggestions must stay within) and with `issues` filled (Elza's existing
  formal findings, so the AI does not repeat them). All items travel with
  stable codes + display values, as everywhere.
- **`config`** — one `elza.revisionConfig`: `checks` (category codes, omitted
  = all), `scope` hint (`unit` | `branch`), `language` (of descriptions and
  findings, e.g. `cs`).

Context (supplementary `AiObject`s):

- the **fund** — `elza.fundInfo` (identity + `ruleSetCode`, which keys the
  item dictionary the provider resolves via `getItemTypes`);
- the **ancestor path** — full `elza.archivalDescription` per ancestor
  (inherited values are visible to the model this way; Elza's inheritance
  merging means content-level checks must see them);
- the **surroundings** — one `elza.archivalOutline`: children of the reviewed
  unit and its nearest siblings as lightweight rows (`nodeId`,
  `referenceMark`, `depth`, `title`, `unitDate`). Deterministically built,
  ~10–20 tokens per row.

Notes:

- **Rule/catalog versioning**: `ruleSetCode` + the provider-resolved
  dictionary carry the current element catalog; `allowedItemTypes` pins what
  is actually usable on the level. The AI must not decide by field name or by
  an older code (the Název/Obsah migration caveat in `kontrola-popisu.md`).
- **Publication status** is carried by the item *types* themselves (public
  vs. internal note are distinct elements); Elza has no item-level publish
  flag, so no extra field is needed.
- **Tools**: the task may declare the standard tools (e.g.
  `getArchivalDescription` to pull a child's full items when a finding needs
  confirmation) under a strict budget — but the baseline run must work
  tool-less.

## 3. Provider-side execution (prompt architecture, provider-owned)

The multi-phase flow of `kontrola-popisu.md` — claim extraction → contextual
verification → critical revision → findings — is **provider-internal** (one or
more model passes inside one task; visible to the user only as task-event
phases). The protocol sees a plain task: typed parameters in,
`elza.revisionFindings` out. Binding constraints baked into the prompt (per
the extract): no data changes; verbatim excerpt per finding; suggested
elements only from the supplied catalog; contradiction ≠ factual error
(require professional verification); distinguish rule violation vs.
clarity recommendation vs. stylistic alternative; respect multi-level
description and inheritance; access points only as candidates; keep meaning —
recommend manual assessment when a split could weaken it; low-confidence
findings are never errors.

## 4. Output — `elza.revisionFindings` (contract-owned, typed)

`RevisionFindings.findings[]` of `RevisionFinding` (spec 0.13.0): `nodeId`,
`category`, `severity` (`high|medium|low`), `confidence` (0–100), `kind`
(`rule|contradiction|style`), `sourceItemType`/`sourceItemSpec`, **`excerpt`**
(verbatim, required), `explanation` (self-contained, in `config.language`),
`targetItemType` (only from `allowedItemTypes`), `action`
(`move|split|verify|reformulate|keep`), `proposedText` (advisory only),
`relatedNodeIds`. An empty `findings` array is a valid, expected result.

## 5. Elza-side validation & landing

The client validates every finding before anything is created (silent AI
errors must not become user-visible garbage); dropped findings are counted in
the run summary:

1. `nodeId` (and each `relatedNodeIds` entry) must be a reviewed (or supplied
   context) level — otherwise dropped.
2. `category` must be one of the requested codes — otherwise dropped.
3. `targetItemType`, when present, must be in the level's `allowedItemTypes`
   — otherwise the suggestion is stripped (the finding may survive without it).
4. `excerpt` must be non-empty; findings without evidence are dropped.
5. De-duplication: identical `(nodeId, category, excerpt)` kept once.

**Landing — v1 (decided 2026-07-20): markdown display only.** Findings render
in the panel as a readable markdown block (`RevisionFindingsBlockMapper` maps
the typed `elza.revisionFindings` result onto the existing MARKDOWN display
block — no new UI). The structured **„AI doporučení"** triage cards
(*Přijmout* → creates a `WfIssue`, *Odmítnout*) and the automatic
`RevisionFinding → WfIssue` conversion are a later, additive step reading the
same typed block. The planned `WfIssue` mapping for that step (existing issue
types, no package change):

| severity | `WfIssueType` |
|---|---|
| `high` | `IMPORTANT` (Zásadní) |
| `medium` | `RECOMMENDED` (Doporučující) |
| `low` | `MINOR` (Drobná) |

`WfIssue.node` = the finding's level, state `OPEN`, description template:

```
[AI] <explanation>
Podklad: <excerpt>
Návrh: <action / targetItemType / proposedText>     // when present
(kontrola: <category>, jistota: <confidence>, úloha: <taskId>)
```

The findings block itself stays on `ai_request` (the transient result/audit
artifact); `WfIssue` is the persistent triage artifact — the accept action
creates one from the other. Accept/reject decisions are the evaluation signal
(acceptance rate per category × `promptVersion`). "Neupozorňovat na tento
vzorec" (pattern suppression) is a later, additive concept — see open
questions.

## 6. Run vehicle & profiles (scopes)

**No bulk action** (decided 2026-07-20; see AI-INTEGRATION-PROPOSAL §5/N2):
the review runs as an ordinary AI task from the panel — the AI stack already
provides the async engine, progress, cancel and the `ai_request` ledger. The
three scopes from `kontrola-popisu.md` phase in as:

- **v1 — Rychlá kontrola jednotky** (the pilot): the user stands on a level
  and runs the check on it. `subject` = that one level; context = ancestors +
  children/sibling outline. Checks: `hidden_data`, `note_misuse`,
  `formulation`, `contradiction`, `access_point_candidate`. Small (~1–4k
  input tokens), interactive, panel-native — the cheapest end-to-end exercise
  of the whole chain (typed input → findings → validation → triage →
  evaluation).
- **v2 — Kontrola větve**: `subject` = a window of levels of one subtree
  (siblings stay together), plus the branch outline; adds `wrong_level`,
  `duplicate`, `inconsistency`. One conversation, one `ai_request` per
  window; `requestId` idempotency means a crashed run re-polls paid windows.
- **v3 — Kontrola archivního souboru**: batch pattern analysis (e.g. the same
  internal remark in hundreds of content fields). Deterministic
  pre-clustering first (repeated-value clusters computed by Elza, the model
  reads representatives + occurrence counts) — cheap and precise; design when
  v2 data exists.

## 7. Sizing & evaluation

- v1 unit check: one full level (150–400 tokens) + ancestors + outline
  (~10–20 tokens/row) + dictionaries ⇒ ~1–4k input tokens per run; economy
  profile plausible, measure.
- v2 windows: ~25 full levels ⇒ ~4–10k tokens per task; a 1000-level branch
  ≈ 40 tasks. Outline rows make the *whole-branch* context affordable where
  v1's design had none.
- Evaluation: acceptance rate = accepted / (accepted + rejected) per
  `category` × `promptVersion`; disable-by-default any category under ~50 %
  after the pilot. Cost per run and per accepted finding from `usage`.

## 8. Open questions

1. **Findings triage UI** — v1 renders findings as cards in the panel
   (accept/reject per finding); whether an "Upravit" (edit-then-accept) step
   is needed before creating the `WfIssue`, decide with the UI work.
2. **Pattern suppression** ("Neupozorňovat na tento vzorec") — needs stored
   suppressed patterns fed into subsequent runs (as config or context);
   additive, after the pilot shows which patterns recur.
3. **Issue-list granularity** (per run vs. per fund) and re-run
   de-duplication against open `[AI]` issues — inherited from v1 draft,
   decide after the pilot.
4. **`confidence` calibration** — keep only if it correlates with acceptance.
