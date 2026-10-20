# Import from XLSX by named templates

**Proposal, not implemented yet.** Nothing below exists in the code. The
import specification's models, prepared for the AI provider contract, are
in the appendix of this document; they enter `elza-development/typespec-ai`
only once the import and the template store exist in Elza.

An archivist imports a spreadsheet (XLSX) into the archival description
through the batch import, using a **named template** that says how the
sheet's columns become description items and how its rows form the tree.
A template is reusable across workbooks of the same source. The AI
assistant can author a template from a workbook (the assisted path
below), but it is optional: the built-in templates and hand-edited templates work
without any AI provider.

## Why

Today a spreadsheet reaches Elza through ElzaConvert, a separate web
tool: the archivist writes element codes into row 2 of the workbook,
converts it to the EDX XML and imports that through the batch import. The
mapping lives inside the data file, the expressive part is a
developer-maintained YAML configuration bound to converter classes, the
conversion report is prose, and nothing of it is reusable inside Elza.
Elza must not depend on an external hosted tool for an import, so the
conversion moves into Elza — and instead of porting the row-2 convention,
the mapping gets a format of its own that Elza can store, check, reuse
and, with the assistant, generate.

## The import specification

The mapping of one sheet is an **import specification** (*importní
předpis*): a separate, declarative, closed-vocabulary document. Its model is
`ImportSpec` (appendix); Elza stores and exchanges it in a YAML file form
with the same field names, and the same model later becomes part of the
AI provider contract unchanged. In short:

- `headerRows`, `sheetIndex`, `ruleSetCode`; `skipRows` (rows that are not
  units), `continueRow` (a row continuing the previous unit);
- `hierarchy`: `level` (a depth column), `titleColumns` (the title sits in
  one of several columns, depth = which one) or `flat`;
- `columns[]`: exactly one rule per data column — `item` (type, optional
  spec, `split` for repeated values, `valueMap`, `fallbackType` for an
  unparsable dating, `partOf` + `expandRange` for a structured object such
  as a storage unit), `entity` (a `RECORD_REF` type + spec + entity class),
  or `skip` with a reason;
- `addItems[]`: items written to every unit matching a depth or a cell
  condition (level types, folder types, arrangement type) — each a
  `ProposedItemValue`;
- `decisions[]`: questions the author left to the archivist.

The format knows no rule set: it names item types, specifications and
entity classes by their codes, and the engine reads what a code means
(data type, specifications) from the rule set. Level types, dating and
storage units are ordinary items. Nothing in the format is code or an
expression; a need that does not fit is a decision for the archivist or a
later addition to the vocabulary, which is versioned (`spec`).

## Flows

**Template first.** The archivist downloads a built-in template workbook
(headers and example rows), fills it, attaches it in the import dialog and
imports it with the template's spec.

**Own data first.** The archivist attaches an existing workbook and
either picks a template that fits or asks the assistant for a spec; after
the import succeeded, *Uložit jako šablonu* stores the spec under a name.
The first import is the template authoring.

**Repeat run.** Attach the next workbook, choose the template. Elza
profiles the sheet, runs the fit check (below), simulates the spec on
sample rows and shows the preview and the engine's messages; *Použít*
converts the whole sheet to EDX in memory, creates the batch item and runs
the import's test run; the real run is the user's click. No AI is
involved.

**Drift.** When the fit check reports changed or missing columns or a
code the rule set no longer has, the archivist either edits the exported
YAML and uploads it again, or — with an AI provider configured — asks the
assistant to adapt the template to the new workbook. The adapted spec
becomes the template's next version or a new template.

## The assisted path

With an AI provider configured, the archivist attaches the workbook to the
chat and asks for a template. Elza sends the profile (`ImportSource`:
sheets, header rows, per-column header, counts and examples, a capped sample
of rows as displayed strings — the file itself stays in Elza) as the input
of the task `elza.importMapping`, together with a template to adapt (`spec`)
and the last report (`report`) on follow-ups. The provider answers with an
`ImportSpec` and a markdown explanation. Binding rules of that task:

- propose only — the spec is applied by the user, the import is started by
  the user;
- every data column gets exactly one rule, except the hierarchy's; a column
  not imported is `skip` with a reason;
- codes come from the rule set's dictionary (`getItemTypes`); nothing
  rule-set-specific is in the format;
- one hierarchy strategy from the evidence in the profile (level column,
  title columns, else flat with a decision entry); dating columns map to
  the `UNITDATE` type with a `fallbackType`; entities are `entity` rules
  with the `RECORD_REF` type, the role and the class; an ambiguity goes to
  `decisions`, never into a guess;
- the provider checks the spec against the dictionary before answering
  and, when Elza offers `simulateImportSpec`, previews it on sample rows
  and corrects itself first; a spec that fails the checks is never
  returned as a result.

Elza's part of the path: chat attachments, the import-item context, the
`simulateImportSpec` executor (the simulation of the components above), the
spec and report blocks in the panel, *Použít* and the report → follow-up
handoff. The provider's part: the task handler with the forced spec tool,
the checks and both model adapters.

## Components

- **Engine** (`cz.tacr.elza.imp.spec`, Spring-free): a table abstraction
  over the sheet, the rules → units (depth, parent, items, access points,
  structured objects), messages with row and column; a POI adapter reads
  the XLSX; units → EDX through the existing `cz.tacr.elza.schema.v2`
  classes, so everything after conversion is the EDX import as it is
  (fund pairing, access-point checks, rule validation, the test run).
  The engine never writes to the database.
- **Batch item kind XLSX**: the workbook in the DMS, the applied spec (as
  YAML), the template version it came from, the derived EDX and the
  engine's report on the item; the batch keeps the EDX settings (scope,
  fund strategy).
- **Profile builder**: per sheet the header rows, per column the header,
  fill and distinct counts and examples, plus a capped sample of rows as
  displayed strings (`ImportSource`, appendix). Used by the fit
  check, the preview and, when the assistant is asked, as the only thing
  sent to the provider.
- **Template store**: templates with versions, the fingerprint (the
  source's headers and column letters at save time), YAML export and
  import; built-in templates as resources, read-only.
- **Fit check**: every column the spec references exists; its header
  equals the fingerprint's (case- and diacritics-insensitive); new
  columns are listed; every code exists in the rule set's dictionary.
  Outcomes: fits, differs (the user decides), fails (adapt first).
- **Code-row converter**: a workbook with ElzaConvert's row-2 column codes
  (`CE:<type>;<spec>`, `UROVEN`, `UROVEN_TYP`, `DATACE_*`, `UKL_*`) is
  recognised and its codes become a spec, so existing universal-import
  workbooks import unchanged.
- **Simulation**: runs the engine on a window of rows without writing and
  returns the units it would produce and the messages — the preview in
  the dialog, and the executor of the `simulateImportSpec` standard tool
  when the assistant checks its own draft.
- **Dating**: the engine parses the archivist notations (years, ranges,
  centuries, approximations, roman months) for `UNITDATE` items; an
  unparsable value goes to the `fallbackType` item with a warning.
- **UI**: the import dialog gains the template picker, the fit-check
  result, the spec as a table (column · header · rule · note), the
  decisions, the preview, the engine and import reports, *Uložit jako
  šablonu*, YAML download and upload.

## Data model

- `imp_template`: identity, name, description, rule set code, built-in
  flag, created by / at.
- `imp_template_version`: the template, version number, the spec as YAML,
  the fingerprint, created by / at. The latest version is the one offered;
  older versions stay because batch items reference them.
- `imp_item` (XLSX kind): the template version, the applied spec as YAML
  (it may differ from the template after an adaptation), the engine
  report.

## Invariants

- The workbook is never modified; the spec is the only mapping and the
  only thing a template stores.
- Every data column has exactly one rule or is named by the hierarchy; an
  unmapped column is an error, not a silent gap.
- Codes are validated against the rule set when a spec is applied, never
  assumed; a stale template fails the fit check, not the import.
- The same workbook and spec always produce the same EDX (identifiers
  aside).
- The engine knows no rule set and runs without a database; the EDX
  import keeps all the authority over what gets written.

## Dependencies

- The import's full test run and structured message list (the EDX test
  run validates only the XSD today) — the engine's conversion report uses
  the same message shape.
- Apache POI in `elza-core` (new dependency).
- The models of the appendix (`ImportSpec`, `ImportSource`, `ImportReport`,
  `ImportUnit`) as Elza's own storage and REST types; the AI provider
  contract adopts them later without change.
- Chat attachments in the AI panel — only for the assisted authoring.

## Open work

- The engine, the POI adapter and units → EDX, with unit tests on
  synthetic workbooks.
- The XLSX batch item kind, the profile builder and the simulation.
- The template store with versions, fingerprint, YAML export / import;
  the three built-in templates and their workbooks (a flat list of items;
  a list with level and level-type columns — ElzaConvert's universal
  layout, with the code-row converter; an inventory with series / folder /
  item as title columns); the fit check.
- The REST API (TypeSpec in `elza-development/typespec`) for templates and
  the XLSX item kind; the import dialog.
- The dating parser: port ElzaConvert's notation parsers or extend
  `ArrDataUnitdate.valueOf`.
- The AI provider contract: add the appendix (three object types, the task
  type `elza.importMapping`, the standard tool `simulateImportSpec`) to
  `elza-development/typespec-ai` once the import and the template store
  exist; then the provider's handler and Elza's part of the assisted path
  (chat attachments, the import-item context, the `simulateImportSpec`
  executor, the spec and report blocks in the panel).
- Template scopes beyond installation-wide (private, shared by fund);
  whether the fingerprint should keep sample rows for the assisted
  adaptation; one spec per sheet vs. several sheets per workbook;
  hierarchy by formatting (bold, indentation); pairing created access
  points with existing ones by name; a provenance note with the source
  row in the imported description.

## Appendix: the import specification as TypeSpec

The prepared, additive contract change for `elza-development/typespec-ai/main.tsp`:
three object types, the task type `elza.importMapping`, the standard tool
`simulateImportSpec`, 19 schemas. The vocabulary is minimal by decision — a
construct is added when a real workbook needs it (hierarchy by reference marks
or parent ids, linking existing access points by an id column, per-column
conditions, inferred column classes, message codes and locations are the
known candidates) — and names nothing of any rule set. Kept here until the
Elza side exists; the models are also the shape of Elza's own template
storage and REST types.

```typespec
// --- ObjectType: entries to add ------------------------------------------
  /** Profile of a spreadsheet attached on the client (structure, statistics, sample); payload `ImportSource`. */
  importSource: "elza.importSource",

  /** An import specification — the declarative mapping of one sheet onto archival description; payload `ImportSpec`. */
  importSpec: "elza.importSpec",

  /** Outcome of checking / simulating / converting / importing with a spec; payload `ImportReport`. */
  importReport: "elza.importReport",

// --- TaskType doc: entry to add --------------------------------------------
/**
 * - `elza.importMapping` — preparation of an import specification for a
 *   spreadsheet attached to the conversation ("AI příprava importu z
 *   tabulky", next contract version): from the profile of the attached file
 *   (`elza.importSource` — structure, statistics, a capped sample; the file
 *   itself stays with the client) the assistant proposes an
 *   `elza.importSpec` — the declarative mapping of the sheet onto archival
 *   description — and explains in `elza.markdown` what it saw in the data
 *   and why it mapped the columns as it did. The provider checks the spec
 *   against the rule set's dictionary and, when the client offers
 *   `simulateImportSpec`, previews it on sample rows before answering; the
 *   client reviews, applies (converts and test-runs) and sends the resulting
 *   `elza.importReport` back for a revised spec. Strictly propose-only: the
 *   spec is applied by the user, the import is started by the user. Task
 *   definition: the assisted path of this document.
 */

// --- StandardToolName: doc entry + enum member `simulateImportSpec` --------
/**
 * - `simulateImportSpec` — run an import specification on a window of rows
 *   of the attached spreadsheet (next contract version). Arguments
 *   `SimulateImportSpecParams`, result `SimulateImportSpecResult`: the client
 *   executes its real import engine on the rows, without writing anything,
 *   and returns the units it would produce (depth, level type, title, items)
 *   and the engine's messages as an `ImportReport`. Lets the provider check a
 *   draft spec on the actual data before answering — one engine, no drift
 *   between what the provider proposes and what the client imports. Offered
 *   by the client only when the conversation carries an `elza.importSource`.
 */

// ---------------------------------------------------------------------------
// Import specification (elza.importSource / elza.importSpec / elza.importReport)
// — a declarative mapping of one spreadsheet table onto archival
// description: the provider prepares it from a profile of the file attached
// on the client, checks it against the rule set's dictionary and (through
// `simulateImportSpec`) on sample rows; the client executes it into its own
// import. The file never leaves the client. The spec names item types,
// specifications and entity classes by their codes only — what a code means
// (its data type, its specifications) comes from the rule set's dictionary
// (`getItemTypes`); nothing rule-set-specific is hard-coded here. The
// vocabulary is deliberately minimal: a construct is added when a real
// workbook needs it, never ahead of it. Design record: elza-ai-provider.git
// doc/import-spec-proposal.md; task definition: the assisted path above.
// ---------------------------------------------------------------------------

/** One column of a sheet: identity and statistics over all its data rows. */
model ColumnInfo {
  /** Column letter (`A`, `B`, … `AA`) — the key every rule refers to. */
  column: string;

  /** Header text (the header rows' cells joined), when any. */
  header?: string;

  /** Non-blank data cells. */
  filledRows: int32;

  /** Distinct non-blank values. */
  distinctValues: int32;

  /** A few representative values (most frequent first), truncated by the client. */
  examples: string[];
}

/**
 * One row of a sheet: cells as the spreadsheet displays them (a blank cell is
 * `""`). Carried as a sample in `SheetInfo.rows` today; the same model serves
 * full rows when a client sends them.
 */
model TableRow {
  /** 1-based sheet row. */
  row: int32;

  /** One string per profiled column, in column order. */
  cells: string[];
}

/** One sheet of a workbook: structure, per-column statistics and rows. */
model SheetInfo {
  /** 1-based sheet index. */
  index: int32;

  name: string;

  /** Rows with any content. */
  rowCount: int32;

  /** Rows before the first data row, as the client guessed them (headers, titles). */
  headerRows: int32;

  columns: ColumnInfo[];

  /**
   * Data rows — today a capped sample (the first rows plus rows spread over
   * the sheet; all the provider sees of the data), all of them when the
   * client sends a small sheet in full (`rows` then covers every data row).
   */
  rows: TableRow[];
}

/**
 * Payload of `elza.importSource` — the profile of a spreadsheet attached to
 * the conversation on the client, which keeps the file: the provider
 * receives this profile and never the workbook. `sourceId` names the
 * attachment in `simulateImportSpec` calls.
 */
model ImportSource {
  /** Client-side id of the attachment; opaque to the provider. */
  sourceId: string;

  fileName: string;

  /** Rule set of the target fund (`RulRuleSet.code`), e.g. `ZP2015` — the dictionary a spec's codes are checked against. */
  ruleSetCode: string;

  /** The workbook's sheets; the first is the default subject of a spec. */
  sheets: SheetInfo[];
}

/** Import-source object — the profile of an attached spreadsheet. */
model ImportSourceObject extends AiObject {
  @invisible(Lifecycle)
  objectType: ObjectType.importSource;

  data: ImportSource;
}

/**
 * The one condition form of a specification — a test of one cell of the
 * current row. Exactly one of `isEmpty` / `equals` / `matches` is present.
 */
model CellCondition {
  /** Column letter. */
  column: string;

  /** True: the cell is blank; false: it is not. */
  isEmpty?: boolean;

  /** Case-insensitive equality with the trimmed cell text. */
  equals?: string;

  /** Regular expression (Java syntax) matched against the whole trimmed cell text. */
  matches?: string;
}

/** How a unit's depth and parent are derived from the rows. */
enum HierarchyStrategy {
  /** `levelColumn` holds the depth (1 = top); the parent is the nearest previous unit one level up. */
  level,

  /** The title sits in one of `titleColumns`; depth = position of the first non-blank of them. */
  titleColumns,

  /** Every unit is a child of the root. */
  flat,
}

/**
 * Derivation of the tree. The columns it names need no `ColumnRule`. Level
 * types and other depth-dependent items are not decided here — they are
 * ordinary items, from a column (`ColumnRule`) or added by rule per depth
 * (`AddItemRule.depth`); which types exist is the rule set's business.
 */
model HierarchyRule {
  strategy: HierarchyStrategy;

  /** `level`: the depth column. */
  levelColumn?: string;

  /** `titleColumns`: the title columns, top level first. */
  titleColumns?: string[];
}

/** What a column feeds. */
enum ColumnUse {
  /** One description item per value (`type`, optional `spec`). */
  item,

  /** An access point per value, created with `entityClass` and referenced by the `RECORD_REF` item `type` (+ `spec`). */
  entity,

  /** Not imported; `reason` says why. */
  skip,
}

/**
 * The rule of one column — every data column has exactly one, except the
 * columns `hierarchy` names. How a value is read follows the item type's data
 * type in the rule set's dictionary, not anything in this contract: numeric
 * types are parsed as numbers, `UNITDATE` types as an archivist dating
 * notation (years, ranges, centuries, approximations), enumerated types
 * resolve the cell text to a specification (by `valueMap`, else by name,
 * code or code suffix, case-insensitive), a `STRUCTURED` type is assembled
 * from the columns that name it in `partOf`. Empty cells produce nothing;
 * text is split only when `split` is given.
 */
model ColumnRule {
  /** Column letter. */
  column: string;

  use: ColumnUse;

  /** Item type code (`RulItemType.code`) the values are written to. */
  type?: string;

  /** Fixed specification code. */
  spec?: string;

  /** Separator of repeatable values within one cell (e.g. `;`). */
  split?: string;

  /** Cell text (case-insensitive) → specification code (enumerated types) or normalised value. */
  valueMap?: Record<string>;

  /** `UNITDATE` types: item type that receives the raw text when the dating cannot be parsed (then a WARN); absent = the value is dropped with an ERROR. */
  fallbackType?: string;

  /** The `STRUCTURED` item type this column is a part of (e.g. a storage unit); columns sharing it form one object per unit, `type` is the part's item type. */
  partOf?: string;

  /** With `partOf`: a numeric range in the cell (`12-14`) yields one object per number. */
  expandRange?: boolean;

  /** `entity`: entity class code (`ApType.code`) of the access points created from the values. */
  entityClass?: string;

  /** `skip`: why the column is not imported. */
  reason?: string;

  /** Note for the reviewer: what the author saw in the column and why this rule. */
  note?: string;
}

/**
 * An item written to every unit the selectors match — the same
 * `ProposedItemValue` the node update proposals use (type, specification,
 * value); the selectors are the only addition. Level types under the
 * title-columns strategy, folder types, the arrangement type.
 */
model AddItemRule {
  /** The item to add. */
  item: ProposedItemValue;

  /** Only units at exactly this depth (1 = top). */
  depth?: int32;

  /** Only rows matching the condition. */
  when?: CellCondition;
}

/**
 * Payload of `elza.importSpec` — an import specification (importní předpis):
 * how one sheet of the attached workbook becomes archival description. A
 * separate, declarative, closed-vocabulary document: the workbook is never
 * modified, every data column has exactly one rule, no rule contains code or
 * expressions — a need that does not fit is written into `decisions`.
 * Checked by the provider against the dictionary and by the client through
 * `simulateImportSpec`; executed by the client; reusable as a template for
 * the next workbook of the same source. The target fund and its institution
 * are the client's import settings, not part of the spec.
 */
model ImportSpec {
  /** Format version; this contract defines `1`. */
  spec: int32;

  /** 1-based sheet index; absent = the first sheet. */
  sheetIndex?: int32;

  /** Rows before the first data row. */
  headerRows: int32;

  /** Rule set the codes belong to (`ImportSource.ruleSetCode`). */
  ruleSetCode: string;

  /** Rows matching any of these are not units (totals, blank titles, separators). */
  skipRows?: CellCondition[];

  /** A row matching this continues the previous unit: its text values are appended to that unit's values of the same column. */
  continueRow?: CellCondition;

  hierarchy: HierarchyRule;

  /** Exactly one rule per data column (except the hierarchy's), in column order. */
  columns: ColumnRule[];

  /** Items added by rule. */
  addItems?: AddItemRule[];

  /** Questions left to the archivist, each naming the column(s) concerned. */
  decisions?: string[];
}

/** Import-specification object — the mapping, as a result or as context for a revision. */
model ImportSpecObject extends AiObject {
  @invisible(Lifecycle)
  objectType: ObjectType.importSpec;

  data: ImportSpec;
}

/** One message of a report. */
model ReportMessage {
  /** `ERROR`, `WARN` or `INFO`. */
  severity: string;

  /** Human-readable text in the client's language, naming the value concerned. */
  text: string;

  /** 1-based sheet row. */
  row?: int32;

  /** Column letter. */
  column?: string;
}

/**
 * Payload of `elza.importReport` — the outcome of one step done with a spec,
 * in one shape whatever the step. Sent as context so the assistant can
 * explain the problems and revise the spec.
 */
model ImportReport {
  /** Which step produced it: `check` (provider), `simulation`, `conversion` or `import` (client). */
  source: string;

  /** Messages of severity `ERROR`. */
  errors: int32;

  /** Messages of severity `WARN`. */
  warnings: int32;

  /** True when the message list was capped. */
  truncated: boolean;

  /** The messages, errors first. */
  messages: ReportMessage[];
}

/** Import-report object — the outcome of a step done with a spec. */
model ImportReportObject extends AiObject {
  @invisible(Lifecycle)
  objectType: ObjectType.importReport;

  data: ImportReport;
}

// --- Tool payloads -------------------------------------------------------
/**
 * Arguments of the `simulateImportSpec` tool: a draft spec and the window of
 * sheet rows to run it on (1-based sheet rows; the client may cap `limit`).
 */
model SimulateImportSpecParams {
  /** The attachment (`ImportSource.sourceId`). */
  sourceId: string;

  spec: ImportSpec;

  /** First sheet row of the window; absent = the first data row. */
  fromRow?: int32;

  /** Requested maximum rows; absent = the client's default. */
  limit?: int32;
}

/** One unit (level) produced from the rows — by a simulation or by the conversion itself. */
model ImportUnit {
  /** Sheet row the unit starts at. */
  row: int32;

  /** Depth, 1 = top. */
  depth: int32;

  /** The items as they would be imported, in rule order — the same `DescriptionItem` the client sends in `elza.archivalDescription` (resolved specification, entity by name). */
  items: DescriptionItem[];
}

/** Result of the `simulateImportSpec` tool. */
model SimulateImportSpecResult {
  /** The units, in sheet order. */
  units: ImportUnit[];

  /** Messages of the run (`source: simulation`), with row and column. */
  report: ImportReport;
}
```
