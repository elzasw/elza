# Rules-package customization layer and first internationalized version — plan

Status as of 2026-10-07. This is the design record for extending rules packages without forks
(addon packages, customizations edited in the admin UI) and for the first internationalized
version of ELZA (an English generic rule set based on ISAD(G)). Line numbers refer to the `3.4.x`
tree at the time of writing.

## 1. Goal and principles

**Goals.**
- Institutions customize ZP2015 (and later the generic rule set) without forking it. Today three
  institutions (DPP, CT, UK) maintain patch overlays on the stock package ZIPs. Their changes are
  small and ~90 % additive: own item types, specifications on upstream item types, availability and
  validation rules, small UI-settings edits. The overlays exist only because a dependent package
  could not contribute to ZP2015.
- An English-speaking archive can describe a fund in an English rule set, with English names
  everywhere it looks, as early as possible.

**Principles.**
- **The layer is a package.** Every customization is a `rul_package`: a file addon (a ZIP
  depending on ZP2015 / CZ_BASE) or a named customization edited in the admin UI. Database rows stay
  a projection of imported packages; only the importer writes rule tables. A UI-made customization
  and a git-managed addon are the same artefact.
- **DRL first.** Behaviour a customization needs is expressed in its own Drools rules, running after
  the base rules (priority ≥ 200), or in extension rules when scoped. A customization made in the UI
  carries rules generated from fixed templates. Declarative primitives are added later, where rules
  prove repetitive; the generated templates are their natural candidates.
- **Data only where rules cannot help.** The first phase adds database changes only for what DRL
  cannot express: the position of a new specification among the existing ones.
- **Any number of customizations per installation** (per domain, per fund, per part of a fund),
  each one a `rul_package`, optionally scoped through an arrangement extension of its own.
- **Translations are package content too.** English names for entities of another package (CZ_BASE)
  are contributed by a package, the same way specifications are.

## 2. Existing functionality this plan builds on

Already in place; listed only as functionality the plan relies on.

- **Addon contribution to a foreign rule set.** A dependent package that ships
  `rul_rule_set/<FOREIGN_CODE>/` is imported as a `RuleState.ADDON` context. Its arrangement rules,
  extensions, actions and filters are stored against the addon package and run after the base rules
  when their priority is higher. Specifications attach to foreign item types through
  `<item-type-assign>`. Proven by `elza-core/src/test/java/cz/tacr/elza/rules/addon/AddonPackageTest.java`
  with the test package `elza-core/src/test/resources/rules-addon-test/`.
- **Rule order.** ZP2015 and CZ_BASE declare every arrangement rule at priority 100; addon and
  customization rules use 200 or more. `DescItemTypesRules.execute` runs arrangement rules by
  priority, then the extension rules active on the node and its ancestors.
- **Package deletion.** `PackageService.deletePackage` cleans rule files in every rule set the
  package contributed to and reloads static data after commit. It does not yet publish the
  websocket `PACKAGE` event.
- **In-memory import entry.** `PackageContext.init(Map<String, ByteArrayInputStream>)` exists; only
  the public `importPackageInternal(File, boolean)` signature forces a file.
- **Per-fund scoping.** Arrangement extensions (`rul_arrangement_extension`) are switched on per node
  (`arr_node_extension`, inherited by the subtree) in the node settings dialog; their extension rules
  run only there. CT already uses this for `IDEC_Doporuceny`.
- **Bilingual UI chrome.** The React UI has Czech and English catalogs. Names that come from
  packages (item types, specifications, ap types, part types, rule sets) are shown verbatim and are
  Czech in CZ_BASE and ZP2015.
- **Placement of specifications** (done in Phase 1). `view-after` on `<item-type-assign>` places a
  specification after another specification of the same item type; stored in
  `rul_item_type_spec_assign.view_after_spec_code` (changeset `20261006140000`, which also adds the
  unique key `ux_rul_item_type_spec_assign`), applied by `ItemTypeUpdater.postSpecsOrder` through
  `packageimport/AnchoredOrder` on every import, exported back by `ItemSpec.fromEntity`.
- **Addon item-type filter** (done in Phase 1). Rule type `ITEM_TYPE_FILTER`: rules of other
  packages run after the rule set's own filter in `RuleService.getItemTypeCodesByRuleSet`
  (`AvailableItemsRules.execute`), so addon item types reach the grid, search, add-item dialog and
  AI dictionary.
- **Documentation.** Package format and customization are described for implementers in the
  English-only implementation guide `elza/docs/implementation-guide` (chapters "Rules Packages",
  "Package Files", "Customizing a Rule Set"); the Czech documentation points to it instead of
  describing packages itself. Each implemented step updates the guide; this plan keeps only open
  work.

## 3. Changes against the previous version of this plan

- **DRL first instead of declarative primitives.** The availability flag, `specs-extensible` with a
  runtime policy, and the `rul_rule_set_item_type` table are gone from the early phases. Spec
  visibility and scoping are rules of the customization; addon item types reach the grid, search
  and AI dictionary through an addon filter rule.
- **Spec ordering stays early.** Placing a new specification relative to the existing ones is
  essential for archivists and customization authors. It cannot be expressed in DRL, so it is the
  one database change of Phase 1.
- **The English rule set moves from the last phase to Phase 2.** It never depended on the
  customization layer. It needs core neutrality fixes and English names for CZ_BASE entities.
- **Localization is a generic translation layer, not a language of the entity.** Entities keep one
  source text; translations are optional key-value rows (entity, code, field, language) contributed by
  any package. A language column on `rul_item_type` was rejected: whether an archive uses an item
  type is decided by the rule set, and duplicating shared elements per language would split data,
  rules and exports. Localization of package texts becomes its own Phase 2a, the proposed next step.
- **Phases merged.** Settings composition joins the pilots, because the pilots are its only early
  consumer. Customization backend, specification UI and the rule editor become one phase, because a
  UI-made specification needs a generated rule anyway.
- **A compile check at import is required.** Today a DRL is compiled lazily on first use
  (`Rules.reloadRules`), so a broken addon rule would surface as errors in archivists' forms. With
  DRL first this check is mandatory.

## 4. Phases

| Phase | Content | DB | UI | Exit criterion |
|---|---|---|---|---|
| **1. Layer core** | Done: spec placement, addon item-type filter rules. Open: DRL compile check at import; revalidation on rule change; export fixes; `PACKAGE` event on delete | done (changeset A) | refTable invalidation only | `AddonPackageTest` covers each item; re-importing unchanged ZP2015 and CZ_BASE is a no-op on a production copy |
| **2a.1 Localization infrastructure** (done, 7.0.1) | Changeset T; translation files in packages; resolver `PackageTexts`; `GET /api/v1/languages`; translated `GET /api/v1/rules/itemTypes` | changeset T | none | done |
| **2a.1b Review fixes** (done, 7.0.2) | source hash in translation files, same-language overrides, case-insensitive tags, file validation, message pattern check, SIMPLE-DEV version, public language endpoint | none | the client message shows the reason of a refused translation | done |
| **2a.2 Language of requests and read sites** (next, 7.0.3) | the client sends its UI language and offers the `ui_enabled` languages; server `LocaleResolver` with the `elza.locale` default; all read sites through the resolver | none | small (language header, language picker, refetch on switch) | with the English UI, names of translated entities appear in English everywhere; without a header the installation's language is used |
| **2a.3–2a.5 Messages, translator support, content** (7.0.4) | message keys for validation messages; missing/outdated/orphaned translations endpoint and page; `CZ_BASE_EN` | own changeset (conformity message keys) | message rendering, admin page | with the English UI, entity types, part types and item types of CZ_BASE and the validation messages appear in English; Czech unchanged |
| **2b. First internationalized version** | `rules-en-isadg` package; core neutrality fixes; description language for generated content | none | none | an English user creates an ISAD(G) fund, describes and validates it, and sees no Czech text |
| **3. Pilots** | Settings composition; DPP and CT converted to file addons; their overlays retired | none | none | both pilots run on the dev server against stock ZP2015 |
| **4. Customizations in the UI** | `kind`, archive, `CustomizationPackageService`, OpenAPI `customization`, admin page: customizations, specifications with position and generated rule, rule editor with compile check | changeset B | yes | an admin adds "osobní číslo" after an existing identifier type, and it appears in the node form at that position without restart |
| **5. Declarative primitives and overrides** | Templates that repeat become data (`specs-extensible` guard); item-type ordering anchors and layout pass; structure extensions across packages; removable new-level scenarios; code aliases; spec retirement; UK pilot | changeset C | partial | UK overlay retired |
| **6. Convergence** | Rule-set inheritance; renames of semantically generic ZP2015 codes towards the ISAD(G) catalogue via aliases | later | — | — |

Phases 1 and 2 are independent and can run in parallel. Phase 2a uses nothing from the open part of
Phase 1; its first step 2a.1 (7.0.1) and the fixes from its review (7.0.2) are done; 2a.2 (7.0.3)
comes next; 2b builds on 2a.

## 5. Database changes

All changes go into `elza-core/src/main/resources/db/changelog/db.elza-3-part-03.xml`, with
changeset ids in the `yyyyMMddHHmmss` convention. The changelog also supports MSSQL, so no
PostgreSQL-only constructs (partial indexes) are used. Every new column has a default that
reproduces today's behaviour.

### 5.1 Changeset A — spec ordering (Phase 1, done)

Implemented as changeset `20261006140000`: `rul_item_type_spec_assign.view_after_spec_code
nvarchar(50) NULL` and the unique key `ux_rul_item_type_spec_assign (item_type_id, item_spec_id)`.
The changeset deletes duplicate assignments (keeping the oldest row) before adding the key. Behaviour
is described in the implementation guide, chapter "Rules Packages", section "Order of
specifications".

### 5.2 Changeset T — translations of package-provided texts (Phase 2a)

**Model.** Every entity keeps its text in its own table (`rul_item_type.name` etc.), written in the
source language of the package that defines it; that text is the default and the fallback. A
translation is an optional row per translatable piece of text and language. One item type exists
once, whatever the number of languages: an ISAD(G) element has an English source name and a Czech
translation; a country-specific element (`ZP2015_NAD`) has only its Czech source name. Which item
types an archive uses is decided by the rule set, never by language.

| Change | Definition | Meaning |
|---|---|---|
| `sys_language.tag` | `nvarchar(10) NOT NULL`, unique `ux_sys_language_tag` | BCP 47 tag of the language; the key used by translation files, `Accept-Language`, the client setting and display names. Filled for the rows inserted by `db.elza-init.xml`: cze `cs`, eng `en`, fre `fr`, ger `de`, heb `he`, ita `it`, lat `la`, pol `pl`, rus `ru`, slo `sk`, spa `es`. Installations have no own rows; should one exist, a precondition stops the update with the list of codes without a tag, and the administrator fills them before restarting. |
| `sys_language.ui_enabled` | `boolean NOT NULL DEFAULT false`; `true` for `cs`, `en` | The UI can be used in this language. |
| `sys_language.scope_enabled` | `boolean NOT NULL DEFAULT true` | The language can be chosen as the language of an entity scope (`ap_scope.language_id`); `true` for all rows keeps today's choice. |
| new `rul_translation` | `translation_id int PK`; `package_id int NOT NULL FK rul_package` (contributing package); `entity_type nvarchar(50) NOT NULL`; `entity_code nvarchar(100) NOT NULL`; `field nvarchar(30) NOT NULL`; `language_id int NOT NULL FK sys_language`; `text_value ${type.text} NOT NULL`; `source_hash nvarchar(20) NULL`; `ux_rul_translation (entity_type, entity_code, field, language_id, package_id)`; index `ix_rul_translation_package (package_id)` | One translated piece of text: field `field` of the entity `entity_type`/`entity_code`, in the given language. |
| `rul_package.language_id` | `int NOT NULL FK sys_language`, default the row of `cs` | Source language of the texts the package defines (`<language>cs</language>` in `package.xml`). Lets the client mark untranslated texts and lets the import skip a "translation" into the source language. |

**`sys_language` is the language registry.** It already holds the languages of entity scopes; it now
also says which languages the UI offers, and every translation refers to it. Liquibase owns its rows:
adding a UI language needs a new frontend catalog and so a code release, which carries the changeset
setting `ui_enabled`. A new usage of languages becomes a new flag column - each usage needs code
reading it anyway, so a separate usage table would add nothing. The codes in `code` are ISO 639-2/B
(`cze`, `ger`, `slo`, `fre`), not the terminology codes (`ces`, `deu`, `slk`, `fra`) some libraries
expect; every lookup goes through `tag`, so neither convention leaks.

**Names of languages are not stored.** Display names come from the Unicode CLDR data, by tag and the
language of the reader: on the server `Locale.forLanguageTag(tag).getDisplayLanguage(locale)` (JDK),
in the browser `Intl.DisplayNames`. This gives `němčina` in the Czech UI, `German` in the English UI
and `Deutsch` as the name of the language in itself, which is what a language picker shows. The
existing `sys_language.name` (Czech names) stays only as the fallback for a language CLDR does not
know; no `LANGUAGE` translations and no native-name column are needed.

- **Generic key-value, not fixed columns.** A row is one piece of text, so any field of any entity
  can be translated (output types, templates, extensions, type groups, messages) without a schema
  change, a missing translation is a missing row, and a translation of only the shortcut is
  possible.
- **`field` is required**: `name`, `shortcut`, `description`, or another documented name for entity
  kinds with more texts. A required field keeps one row = one text; an optional "context" would let
  two rows differ only by an empty or filled value.
- **Structured key, not one string.** Separate columns let the import check that the translated
  entity exists, let deletion and orphan checks use plain queries, and give the list of
  untranslated texts per language with a join against the entity tables. The package file and the
  client may show the key as one string (`ITEM_TYPE.ZP2015_NAME.name`); the database does not.
- **Globally unique keys.** Codes of item types, specifications, rule sets, entity types, part
  types, structured types, extensions, output types, templates, policy types and issue types/states
  are globally unique and used as they are. Entities whose codes are unique only within a rule set
  are qualified with the rule set code: type groups `ZP2015/01_BASE`. Messages are qualified with the
  package code (5.2 `MESSAGE` below).
- **`entity_type` values** (documented in the implementation guide; adding one needs no schema
  change): `ITEM_TYPE`, `ITEM_SPEC`, `RULE_SET`, `AP_TYPE`, `PART_TYPE`, `STRUCTURED_TYPE`,
  `POLICY_TYPE`, `OUTPUT_TYPE`, `TEMPLATE`, `ARRANGEMENT_EXTENSION`, `TYPE_GROUP`, `ISSUE_TYPE`,
  `ISSUE_STATE`, `MESSAGE`. Languages are not among them: their names come from CLDR (above).
- **`MESSAGE`**: texts of validation and other messages written by rules. A rule reports a message
  key instead of a sentence (`ZP2015/UJ_012`); the package defines the source text of the key in its
  own translation file for its source language, and other languages in further files. Arguments are
  `java.text.MessageFormat` placeholders (`{0}`, `{1}`), formatted with the locale of the target
  language; a literal apostrophe is written `''`. Replaces the separate "message keys" item of the
  earlier plan.
- **Languages** are rows of `sys_language`; files and requests name them by tag (`cs`, `en`). A
  request for `en-GB` falls back to `en`. An unknown tag in a translation file refuses the import:
  the language must be added to `sys_language` first.
- **Stale translations**: `source_hash` is a short hash of the source text the translation was made
  from (the same idea as `srcHash` in the UI catalog `en.json`). The import computes the current
  hash; a mismatch marks the translation outdated. An outdated translation is still used, and listed
  as outdated for the translator.
- **Precedence**: when several packages translate the same text, the package latest in dependency
  order wins (dependency depth, ties by package code), so a customization package can override a
  translation of the base package.
- **Lifecycle**: rows belong to the contributing package; `deletePackage` removes them, the package
  export writes them back to the translation files. Rows translating an entity that no longer exists
  stay (the entity may come back with the next version of its package) and are reported as orphans.

### 5.3 Changeset B — customization packages (Phase 4)

| Change | Definition | Meaning |
|---|---|---|
| `rul_package.kind` | `nvarchar(20) NOT NULL DEFAULT 'IMPORTED'`, values `IMPORTED`, `LOCAL` | `LOCAL` = edited in the admin UI. A file import of a code whose row is `LOCAL` is refused. Never written into `package.xml`. |
| new `rul_package_archive` | `package_archive_id int PK`, `package_id int NOT NULL FK rul_package ON DELETE CASCADE`, `version int NOT NULL`, `content blob NOT NULL` (bytea / varbinary(max)), `create_date timestamp NOT NULL`, `user_id int NULL FK usr_user`, `ux_rul_package_archive (package_id, version)` | The exact ZIP of every version of a customization: the source the UI edits, the export download and the undo history. |

- The ZIP is stored rather than regenerated from rows, because the UI change is applied to the
  package model and re-imported. Customizations are user data, so they belong in the database
  backup, not in the work directory. A customization is a few kilobytes per version.

### 5.4 Changeset C — declarative primitives and overrides (Phase 5, sketch)

- `rul_item_type.specs_extensible` — the owning package declares a vocabulary open; a guard against
  adding specifications to closed vocabularies such as `ZP2015_LEVEL_TYPE`, and the basis for
  replacing the generated "spec follows type" rule by data.
- `rul_item_type.view_after_code` — item-type ordering anchors, with a layout pass that replaces the
  per-package block allocation of the globally unique `view_order`.
- `rul_item_type_spec_assign.retired` — withdraw a used specification from the offer while existing
  values keep resolving.
- `rul_item_type_alias`, `rul_item_spec_alias` — permanent old codes after a rename in place.

## 6. Phase 1 — layer core (open part)

Spec placement and addon item-type filter rules are done (section 2). The items below need further
discussion before implementation.

### 6.1 Compile check at import
- `processArrangementRules`, `processExtensionRules` and the output-type rules compile each new or
  changed DRL in memory with the `KieFileSystem`/`KieBuilder` code of `Rules.reloadRules`
  (`drools/Rules.java:78-110`), extracted into a shared `DrlCompiler`. Errors refuse the import with
  the file name and Drools messages (`PackageCode.INVALID_RULE`).
- The same compiler serves the rule editor in Phase 4.

### 6.2 Revalidation, publish, export
- For an `ADDON` context, when an arrangement or extension rule was added, removed, or its
  `rul_component.hash` changed, call `addCodeRuleToRevalidateFunds(ruleSetCode)`. Unchanged
  re-imports enqueue nothing. Same when the package adds or removes a spec on a foreign item type.
  The decision lives in a small `RevalidationPolicy` with a unit test.
- `deletePackage` publishes `ActionEvent(EventType.PACKAGE)` after commit and enqueues revalidation
  of the rule sets it cleaned.
- Export fixes: `exportItemSpecs` reads `itemSpecRepository.findByRulPackage` plus assignments via
  `findByItemSpecIn`, writes one element per spec and includes unassigned specs;
  `findByRulPackageFetchItemType` (no DISTINCT, duplicates multi-assigned specs so the export cannot
  be re-imported) is removed; `exportSettingsForRuleset` reports a missing rule set instead of an NPE.
- Client: `websocketActions.jsx` `packageEvent()` also invalidates `refTables.groups`,
  `structureTypes` and `apTypes`.

### 6.3 Tests and gates
- `AddonPackageTest` already covers spec placement (also after a ZP2015 re-import) and the addon
  filter rule. Still to add:
  - spec `ADT_AGENDA_TEST` on `ZP2015_AGENDA_TYPE` with an addon rule raising it where the type is
    possible: offered on a folder;
  - a scoped variant: a second addon with its own extension lowers its `ZP2015_OTHER_ID` spec
    everywhere in an arrangement rule and raises it in its extension rule; offered only below a node
    with the extension set;
  - an addon with a syntax error in a DRL is refused at import with the Drools message;
  - revalidation enqueued on a changed addon rule and not on an unchanged re-import;
  - export → delete → re-import round trip.
- Unit test `RevalidationPolicyTest`.
- Gate on a copy of production (open for changeset A as well): apply the changesets, re-import
  unchanged CZ_BASE and ZP2015; spec order and `view_order` unchanged.

## 7. Phase 2 — localization and the first internationalized version

### 7.0 Localization of package texts (Phase 2a)

**Why next.** It depends on nothing open in Phase 1, it is the prerequisite of an English version
that does not show Czech CZ_BASE texts, and it fixes the stored Czech validation messages, which
affect every installation. It is a contained change: one table, one import step, one resolver, and
the read sites that already return names.

**Split.** 2a is realized in steps. Step 2a.1 builds the parts that are hard to change once
released - the schema (changeset T) and the package file format, a contract with package authors -
together with the resolver every later step calls. It ships with a test package and one real read
site, so the design is proven end to end before anything is built on it. The later steps only call
the resolver and are planned in detail once it exists.

#### 7.0.1 Step 2a.1 — localization infrastructure (done, commit `74506e2356`)

Implemented on `3.4.x`, not released. The package format is described in the implementation guide
(chapter "Package Files", section `translations/<lang>.xml`). Decisions that later steps build on:

- Changesets `20261007100000` and `20261007100100`; the translated text is in
  `rul_translation.text_value` (`value` is reserved in H2).
- `domain/TranslationEntityType` lists the 14 kinds with their entities and allowed fields.
- Import: `packageimport/PackageTranslationService`, the last step of the import, reading source
  texts from the database in the import transaction.
- Resolver `core/data/PackageTexts` with an explicit `SysLanguage`; `resolveRequestLanguage(header)`
  parses `Accept-Language` with `Locale.LanguageRange`.
- Precedence by dependency depth, ties by package code. A dependent package always has a larger depth
  than its dependencies, so "later in dependency order wins" also holds for diamonds.
- Read sites so far: `GET /api/v1/rules/itemTypes`; new `GET /api/v1/languages`.

#### 7.0.2 Step 2a.1b — fixes from the review of 2a.1 (done)

Implemented on `3.4.x` in the commit following `74506e2356`; the guide section
`translations/<lang>.xml` describes the result. Decisions that later steps build on:

- **Source hash.** Optional `src-hash` attribute on `<t>`. The import takes it from the file; without
  it a translation whose text is unchanged keeps the previous row's hash, a new or changed one gets
  the hash of the current source text. The export writes it. A translation therefore stays outdated
  after a re-import of the translating package; 2a.4 builds on this.
- **Own language.** In the file of the package's own language only rows of the package's own
  entities are skipped (owner from the entity's package; type groups by the rule set). Rows for
  other packages' entities and their messages are imported as same-language overrides.
- **Tags** are matched case-insensitively (`SysLanguageRepository.findByTagIgnoreCase`, static data
  lower-cases); tags are not rewritten to canonical form. One file per language.
- **Validation** in `readFile` (missing attributes, empty text, code length, `src-hash` length,
  message patterns compiled with `MessageFormat`); every refusal is `INVALID_TRANSLATION` with
  `reason`, shown by the client.
- Templates are read with `deleted = false`; SIMPLE-DEV version 43.
- `GET /api/v1/languages` is public and runs in a read-only transaction (static data are bound to a
  transaction; the endpoint failed over HTTP before, also for logged-in users, which the controller
  test calling the bean directly did not show).
- Upgrade notes: rows of `sys_language` without a tag stop the upgrade; the `cze` row must exist.

#### 7.0.3 Step 2a.2 — language of requests and read sites

- **The client sends its own `Accept-Language`.** Browsers send their own header on every request, so
  without an explicit header a Czech UI in an English browser would get English names. The client
  sets the header from its UI language on all API calls (axios defaults for the generated client and
  `WebApi`, including file downloads and exports).
- **Client language picker.** It offers the `uiEnabled` languages of `GET /api/v1/languages` for which
  the client ships a catalog, named by `Intl.DisplayNames` in the language itself, instead of the
  hard-coded `'cs' | 'en'`; the choice stays in the browser's user settings. Switching the language
  refetches the rule reference tables.
- **Server language per request.** A Spring `LocaleResolver` resolves the language once per request:
  the first `ui_enabled` language of the header (with the region fallback of `PackageTexts`), else
  `elza.locale`. A request without a usable header gets the installation's language, not the source
  texts. `PackageTexts` gains overloads without a language argument that read `LocaleContextHolder`.
  Work without an HTTP request (async jobs, websocket pushes) uses `elza.locale`; none of it returns
  package names today.
- **Read sites.** The VO factories and mappers that return package texts call the resolver:
  `ClientFactoryVO` (`/api/rule/descItemTypes` with specifications and their categories, rule sets,
  extensions, output types, templates, policy types, type groups), `ApFactory` (entity and part
  types, view settings), issue types and states, the data-grid CSV export headers (`ArrIOService`),
  and `GetItemTypesTool` (AI dictionary, with the conversation language). No server-side cache holds
  package texts today (titles are rebuilt per request), so no cache needs a language dimension.
- **Language names** shown anywhere (scope picker, entity forms) come from `Intl.DisplayNames`, on the
  server from `Locale.getDisplayLanguage`.
- **Tests.** One controller test per converted read site with `Accept-Language: en`; a request
  without the header returns texts in the `elza.locale` language.

#### 7.0.4 Later steps of 2a

- **2a.3 Messages.** `DataValidationResults.createMissing/createError` accept a message key; the
  conformity rows keep the key and its arguments in new columns of
  `arr_node_conformity_error/missing` (own changeset), and the server renders them in the request
  language when it returns the node's conformity. The generic "item X must be filled" message becomes
  a core key with the item type as argument. Literal sentences keep working for packages not yet
  converted. ZP2015 converts its `Validation.drl` messages to keys with Czech source texts.
- **2a.4 Translator support.** An admin endpoint listing, per package and language, the missing,
  outdated (by `src-hash`) and orphaned texts, and exporting them as a translation file to fill in;
  a small admin page on top. It comes before the content step, because it produces the files the
  translators fill.
- **2a.5 Content.** `CZ_BASE_EN` (`elza/package-cz-base-en`, depending on CZ_BASE): English texts for
  entity types, part types, entity item types and specifications, the 166 language specifications,
  issue types and states; optionally `ZP2015_EN`. Mostly translation work, starting from the files
  of 2a.4.

**Out of scope of 2a — description language.** Names stored as text keep the language in which they
were generated: structured object values (`arr_structured_object.value`), entity names and key values
(`ap_index`, `ap_key_value`), auto items, outputs (finding aids, EAD) and the fulltext index of ENUM
values. These are archival content and must follow the **description language**, never the user's UI language: the
language of the entity scope or fund (a `sys_language` row, resolved through its `tag`), with the
installation's `elza.locale` as the default. Phase 2b makes Groovy scripts and
print templates resolve names through the same resolver with the description language, so an
English-source rule set produces Czech content on a Czech installation. Prerequisite: scripts that use
names as identifiers switch to codes - `PT_IDENT.groovy` (compares spec names), the EAD template of
ZP2015 (switches on the lowercased finding-aid type name), the PDF content page (matches storage units
by spec shortcut or name).

### 7.1 English generic rule set `rules-en-isadg`
- Module `elza/rules-en-isadg` (package and rule set `ISADG`, prefix `ISADG_`), copied from the
  `rules-simple-dev` skeleton, `<dependency code="CZ_BASE"/>`. Wired into `elza/pom.xml`, the
  distribution assembly and `elza-core` test resources.
- One item type per ISAD(G) element, no STRUCTURED types:
  - identity: `ISADG_LEVEL` (ENUM: fonds, subfonds, series, subseries, file, item — EAD3 `@level`),
    `ISADG_REF_CODE` (UNITID), `ISADG_OTHER_ID`, `ISADG_TITLE`, `ISADG_UNIT_DATE`,
    `ISADG_UNIT_DATE_BULK`, `ISADG_UNIT_DATE_TEXT`, `ISADG_EXTENT`, `ISADG_EXTENT_QUANTITY`;
  - context: `ISADG_CREATOR` (RECORD_REF to CZ_BASE `PARTY_GROUP`, `PERSON`, `DYNASTY`),
    `ISADG_BIOG_HIST`, `ISADG_CUSTOD_HIST`, `ISADG_ACQ_INFO`;
  - content and structure: `ISADG_SCOPE_CONTENT`, `ISADG_APPRAISAL`, `ISADG_ACCRUALS`,
    `ISADG_ARRANGEMENT`;
  - access and use: `ISADG_ACCESS_RESTRICT`, `ISADG_USE_RESTRICT`, language (see below),
    `ISADG_PHYS_TECH`, `ISADG_OTHER_FIND_AID`;
  - allied materials: `ISADG_ORIGINALS_LOC`, `ISADG_ALT_FORM_AVAIL`, `ISADG_RELATED_MATERIAL`,
    `ISADG_BIBLIOGRAPHY`;
  - notes and description control: `ISADG_NOTE`, `ISADG_PROCESS_INFO`, `ISADG_RULES_CONVENTIONS`,
    `ISADG_DESCRIPTION_DATE`;
  - ELZA practicalities: `ISADG_DAO_LINK`, `ISADG_CONTAINER`, `ISADG_ACCESS_POINT`.
- Language: reuse CZ_BASE's `ZP2015_LANGUAGE` and its 166 `LNG_*` specifications, made English by
  the translation package `CZ_BASE_EN` (7.0). This avoids duplicating the language list; the code is
  never shown.
- Rules (English messages): item-type filter, available items with the ISAD(G) mandatory elements
  (reference code, title, creator, dates, extent, level) required at fonds level, new-level scenarios
  ("Fonds", "Sub-fonds", "Series", "Sub-series", "File", "Item"), validation, change impact; policy
  types; fund validation bulk action.
- UI settings: tree title `REF_CODE TITLE UNIT_DATE`, hierarchy icons by level, type groups by the
  seven ISAD(G) areas plus "Physical and digital management", grid view, fund issues.
- Outputs (PDF finding aid, EAD3) follow after the first version.

### 7.2 Core neutrality
- `elza-react/src/components/arr/FundTreeMain.jsx`: the "Compute EJ" menu item queues the ZP2015
  bulk action `ZP2015_INTRO_VYPOCET_EJ` for every fund; show it only when the fund's rule set offers
  that action.
- `elza-core/src/main/resources/script/groovy/createDid.groovy`: the DAO `did` is built only from
  ZP2015 codes; add the ISADG title, reference code and date.
- Validation messages: handled by `MESSAGE` translations in 7.0; ISADG writes its rules with message
  keys and English source texts from the start.
- Description language for generated content (7.0, "Out of scope of 2a"), including the switch of
  name-based identifiers in scripts and templates to codes.
- `ArrangementService` (`ZP2015_ITEM_LINK`) and `ReportServiceQuery` (four ZP2015 output codes) are
  null-safe for other rule sets and stay as they are.

### 7.3 Translations
Implemented in Phase 2a (7.0). ISADG declares `<language>en</language>` (the `sys_language` row `eng`) and ships
`translations/cs.xml` for the elements that make sense in Czech too.

### 7.4 Tests and exit
- `IsadgPackageTest` (pattern of `Zp2015EjCountTest`: import CZ_BASE and ISADG once, unload after):
  filter contents, required items at fonds level, new-level scenarios, validation messages.
- Exit: on the dev server with the English UI, create an ISADG fund, add levels, fill the mandatory
  elements, pick a creator and a language, run validation; no Czech text appears.

## 8. Later phases — design notes

**Phase 3 — pilots.** `SettingsService.resolveGlobal(type, entityType, entityId)` composes settings
rows by package dependency order (base → addons → customizations): last wins for FUND_VIEW,
STRUCTURE_TYPES, PARTS_ORDER, ITEM_TYPES, DAO_LEVEL_IMPORT, STRUCT_TYPE_*, FUND_ISSUES; TYPE_GROUPS
as a placement patch (add a type to a group, optional `after=`); GRID_VIEW appends. Nine read sites
route through it: `SettingsService` `:341` and `:393`, `ConfigRules.java:61`, `ConfigView.java:88`,
`IssueDataService.java:150` (also not invalidated today), `StructObjService.java:1321`,
`ClientFactoryVO.java:1251` and `:1343`, `ApFactory.java:894`, `DaoCoreServiceWsImpl.java:476`
(`Validate.isTrue(size()==1)`). `UISettings.isSameSettings` (`:132`) compares strings with `==`, so
the "other package owns this setting" guard in `processSettings` never fires; before fixing it, check
production for same-key rows owned by different packages. With composition, a same-key row is
accepted from a package that depends on the owner. Then DPP and CT become file addons and their
overlays are retired.

**Phase 4 — customizations in the UI.**
- Any number per installation, `kind = LOCAL`. The package code is also the code prefix: every code
  inside starts with `<CODE>_` and with no other installed package's code.
- Scope: global, or one arrangement extension of its own (code `<CODE>`) that archivists switch on in
  node settings.
- Dependencies derived on every save (owners of referenced item types, rule sets, ap types, at their
  installed version); version increments per save.
- `CustomizationPackageService.rebuild(code, change)`, on the same monitor as `importPackage`:
  pre-validate, load the latest archived ZIP, apply the change with the existing JAXB classes,
  regenerate `package.xml`, import in memory (new overload
  `importPackageInternal(Map<String, ByteArrayInputStream>, boolean)`), archive the new ZIP, enqueue
  revalidation when needed. A save takes 1–3 s and briefly stops the async workers, so the UI saves
  per dialog.
- Adding a specification writes the spec, its `view-after` position, and a generated rule from a
  template: "offered wherever its item type is possible" for global customizations; "lowered
  everywhere, raised where the extension is active" for scoped ones.
- The rule editor edits further DRL files of the customization, compiled with `DrlCompiler` before
  save; templates give starting rules for common intents.
- Detach and attach flip `kind` when a customization moves to git or back. Spec usage counts
  (`arr_item`, `ap_item`, `ap_rev_item`, `da_dao_item`, `arr_ref_template_map_spec`) protect deletion.
- OpenAPI tag `customization`, all `@AuthMethod(ADMIN)`; admin page `/admin/customization` with the
  customization list, create dialog, specifications tab (pick a spec-bearing item type, see all specs
  with owner, add with a "place after" choice) and rules tab.

**Phase 5 — declarative primitives and overrides.** Changeset C; generated templates that turn out
repetitive become data (`specs-extensible` and a runtime policy); item-type anchors with the layout
pass; structure extensions and `structure-type=` resolved across packages;
`NewLevelApproaches.remove(name)`; code aliases (`other-codes` for item types and specs, rename in
place keeping the id); retirement; UK pilot.

**Phase 6 — convergence.** Rule-set inheritance; renames of semantically generic ZP2015 codes towards
the ISAD(G) catalogue through aliases, batched per major release, never to bare codes such as `NAME`
(they collide with CZ_BASE codes like `HISTORY`, `LANG`).

## 9. Reference facts

- Spec availability: `RulItemTypeExt` sets every spec IMPOSSIBLE and only DRL raises it. ZP2015
  raises all specs generically for `ZP2015_OTHER_ID`, `ZP2015_UNIT_DAMAGE_TYPE` and the language
  types; `ZP2015_AGENDA_TYPE` ships with no specs and no spec rule. `ClientFactoryVO.createFormItemTypes`
  drops IMPOSSIBLE specs and `DescItemSpec.tsx` hides them in strict mode.
- Item-type filter: the rule set's own DRL plus `ITEM_TYPE_FILTER` rules of other packages
  (`RuleService.getItemTypeCodesByRuleSet`).
- Ordering: spec order within a type follows the topological package order, then `view-after`
  anchors (`postSpecsOrder`). Item-type `view_order` is globally unique and allocated per package
  as a block; the form orders by group, then `viewOrder`, so addon item types still land last in
  their group.
- Client refresh: `EventType.PACKAGE` marks `ruleSet`, `descItemTypes`, `outputTypes` and `templates`
  dirty (`stores/app/refTables/refTables.jsx:151`), not `groups`, `structureTypes` or `apTypes`.
- Startup: `autoImportPackages` imports distribution ZIPs in topological order and skips packages
  that exist only in the database; customizations are never re-applied at startup, so base imports
  must keep them consistent (`postSpecsOrder` runs over all item types).
- Language: there is no per-user server locale today; `ElzaLocale` is server-wide and used for
  collation and date formatting. `RulesController.rulesListItemTypes` already accepts
  `Accept-Language`.
- Languages: `sys_language` (`code` ISO 639-2/B, `name` in Czech) has the 11 rows of
  `db.elza-init.xml` and no others on the installations. The scope language list is the old REST
  `GET /api/registry/languages` in `ApController`.
- Conventions: Liquibase changesets only in `db.elza-3-part-03.xml`; REST OpenAPI-first: the source is
  `elza-development/typespec/main.tsp`, and the same delta is applied by hand to
  `rest/elza-openapi.yml` (post-processed, not byte-identical to the compiler output); tag →
  generated `<Tag>Api`, `@RestController @RequestMapping("/api/v1")`,
  `@AuthMethod(permission = Permission.ADMIN)` for admin operations.

## 10. Critical files

- Importer: `elza-core/src/main/java/cz/tacr/elza/packageimport/PackageService.java`
  (`importPackageInternal` :554/:687, `processSettings` :1246, `processArrangementRules` :1615,
  `processRuleSets` :2270, `deletePackage` :2538, export :2745-3383), `ItemTypeUpdater.java`
  (`processItemSpecs` :305, `assignItemTypesToSpec` :375, `postSpecsOrder` :591),
  `xml/ItemSpec.java`, `xml/ItemTypeAssign.java`.
- Runtime: `drools/Rules.java`, `drools/DescItemTypesRules.java`, `drools/AvailableItemsRules.java`,
  `domain/RulArrangementRule.java`, `service/RuleService.java`, `core/data/StaticDataProvider.java`,
  `controller/config/ClientFactoryVO.java`, `controller/mapper/RulesMapper.java`,
  `controller/factory/ApFactory.java`, `domain/vo/DataValidationResults.java`.
- Schema: `elza-core/src/main/resources/db/changelog/db.elza-3-part-03.xml`.
- Localization (2a.1): `domain/SysLanguage.java`, `domain/RulPackage.java`,
  `domain/RulTranslation.java`, `domain/TranslationEntityType.java`,
  `packageimport/xml/PackageInfo.java`, `xml/Translations.java`,
  `packageimport/PackageTranslationService.java`, `core/data/PackageTexts.java`,
  `core/data/PackageTranslations.java`, `controller/ApController.java` (scope languages),
  `controller/LanguagesController.java`, `controller/RulesController.java`.
- REST: `elza-development/typespec/main.tsp`, `elza-core/src/main/resources/rest/elza-openapi.yml`.
- Packages: `rules-simple-dev/` (skeleton), `rules-cz-zp2015/`, `package-cz-base/`, new
  `rules-en-isadg/` and `package-cz-base-en/`; `elza/pom.xml`,
  `distrib/distribution/src/assembly/distribution.xml`.
- Client: `elza-react/src/websocketActions.jsx`, `components/arr/FundTreeMain.jsx`, API client setup
  (language header), `components/arr/item-form/desc-items/ErrorDisplay.tsx`.
- Tests: `elza-core/src/test/java/cz/tacr/elza/rules/addon/AddonPackageTest.java`,
  `elza-core/src/test/resources/rules-addon-test/`, `rules/zp2015/Zp2015EjCountTest.java` (pattern);
  `rules-simple-dev/src/translations/en.xml`, new `translation-addon-test/` with
  `PackageTranslationTest`.
