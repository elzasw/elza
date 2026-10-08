# Rules-package customization layer and first internationalized version — plan

Status as of 2026-10-08. Design record for extending rules packages without forks (addon packages,
customizations edited in the admin UI), for entity description frameworks beside CAM, and for the
first internationalized version of ELZA (an English rule set based on ISAD(G)). Finished work is
described for implementers in the implementation guide (`elza/docs/implementation-guide`, English
only) and in the release notes; this plan keeps the decisions later steps build on and the open work.
Line numbers refer to the `3.4.x` tree at the time of writing.

## 0. Next

Direction agreed 2026-10-08: **A1 (texts built in core) as a decision record, then 2b
`rules-en-isadg`.** The customization-layer pilots (Phase 1 rest, Phase 3) stay independent and can
be interleaved; 2c.6 (EAC-CPF export) and R2 (reference scope) wait for a concrete need and for the
answers to R2's questions.

1. **A1** — inventory done (section 7): three rules proposed (persisted findings as codes rendered
   by the client; request-time texts from a core catalog in the request language; texts a package
   writes in the package language). Waiting for the user's decision, then the implementation step.
2. **2b** — `rules-en-isadg` (section 7); prerequisite 2c.5 met.
3. Release gate of 3.4 (section 6): re-import of unchanged CZ_BASE 89 and ZP2015 on a PostgreSQL copy
   after the four declaration migrations, `view_order` and spec order unchanged.
4. Follow-up of 2c.5 (section 7): **L2** (unit-date text per language) - steps 1-3 done 2026-10-08
   (shared lexicon and corpus, tolerant parsers on both sides, English rendering of request-time
   texts); step 4 follows the language rule agreed 2026-10-08 - what a user sees follows the user,
   what a package writes follows the package, everything else carries no language - and needs no
   language on funds.

Nothing blocks implementation. Decisions still open are listed in section 5 (R2 Q1–Q4) and in the A1
record once it exists.

## 1. Goals and principles

**Goals.**
- Institutions customize ZP2015 (and later the generic rule set) without forking it. DPP, CT and UK
  maintain patch overlays on the stock package ZIPs; their changes are small and ~90 % additive.
- An English-speaking archive describes funds and entities in English rule sets, with English names
  everywhere.
- One installation holds several entity description frameworks: CAM (Czech methodology) and an
  international one based on ISAAR(CPF), plus scope-bound extensions of CAM (portal.nacr.cz).

**Principles.**
- **The layer is a package.** Every customization is a `rul_package` (file addon or a customization
  edited in the UI). Only the importer writes rule tables.
- **DRL first.** Customization behaviour is the customization's own rules (priority ≥ 200, or
  extension rules when scoped); declarative primitives only where rules prove repetitive.
- **Methodologies are not translated.** CZ_BASE (CAM) and ZP2015 stay Czech; the international
  version gets its own packages with English source texts. Translations serve those packages and
  customizations.
- **Definition and use are separate.** A definition (class, part type, item type, specification) may
  be declared by several packages under one code and exists once; each declaration carries its own
  texts in its package's language. Rule sets state which definitions they use (members) and in which
  order.
- **Precedence is one rule everywhere:** the package deeper in dependency order wins, ties by package
  code (translations, declarations, members). An installation fixes a conflict of unrelated packages
  with a small local package depending on both.
- **Revalidation is the author's decision** through `compatibility-rul-package`.
- **No compile check at import.** A broken rule shows up where it runs; ELZA stays operational and a
  fixed package can be imported. Only the rule editor of Phase 4 compiles before saving.
- **Migrations are SQL changesets for PostgreSQL**; H2 is used only by tests and holds no data.

## 2. Done (2026-09 – 2026-10)

| Step | Result | Commit |
|---|---|---|
| Phase 1 (part) | spec placement `view-after` (changeset `20261006140000`), addon item-type filter rules (`ITEM_TYPE_FILTER`), `AddonPackageTest` | earlier |
| 2a.1, 2a.1b | `rul_translation`, `sys_language.tag/ui_enabled/scope_enabled`, `rul_package.language_id`; `translations/<lang>.xml` with `src-hash`; resolver `PackageTexts`; `GET /api/v1/languages` | `74506e2356` + follow-up |
| 2a.2 | UI language in the cookie `elza-lang`, `PackageTexts.requestLanguage()`, read sites of names, language picker (settings, login) | `916851c229` |
| 2c.1 | entity rules `rul_rule_set/<RS>/rul_entity_rule.xml`, table `rul_entity_rule` with FKs to class and part type, `RuleSet.getEntityRules` | `d7ad30f6bd` |
| 2c.2 | class members `rul_rule_set/<RS>/rul_ap_type.xml` (`assignable`), server enforcement (`AP_TYPE_NOT_IN_RULE_SET`), scope-aware Fluent class picker, `scopeRuleSetMap` | `7041ea5d8a` |
| 2c.3 | classes declared by several packages (`rul_ap_type_declaration`), parents must agree (`AP_TYPE_CONFLICT`), names by language | `4681181964` |
| 2c.4a | name/index Groovy scripts as entity rules of kind `INDEX`, PT_* structured types removed | `7571baeaf9` |
| 2c.4b | part types declared by several packages (`rul_part_type_declaration`); order of classes (`position`) and the part list of a rule set (`rul_rule_set/<RS>/rul_part_type.xml`, `rul_rule_set_part_type`) | `6669125db8` |
| 2c.4c prep | unused item type flag `is_value_unique` removed (changeset `20261008120000`; the XML element is ignored) | `d586f7e1c8` |
| 2c.4c | item types declared by several packages (`rul_item_type_declaration`, changeset `20261008130000`); stored-data values must agree (`ITEM_TYPE_CONFLICT`), removal of a used item type refused (`ITEM_TYPE_IN_USE`); export writes the mask and structured type it omitted | `19166f8303` |
| 2c.5a | core without CZ_BASE (CAM checks only with CAM item types, JWT user scope/class settings, institution short name, report), `AUTO_ITEMS` as an entity rule (changeset `20261008140000`), sequence fix of `ap_scope`/`par_institution_type` (`20261008150000`), task types deleted with their package; `StandaloneFrameworkTest` | `1ced6233ce` |
| 2c.4d | specifications declared by several packages (`rul_item_spec_declaration`, `rul_item_spec_assign_declaration`, changeset `20261008160000`); assignments are the union of the packages' declarations; `ITEM_SPEC_CONFLICT`, `ITEM_SPEC_IN_USE`; export fixes (each specification once, unassigned ones too) | `3dd400a540` |
| 2c.5b-prep | class tree of a rule set = its members (union for the installation), `item-aptypes` of foreign declarations equal to the owner's allowed, startup import includes packages without dependencies (bug) in code order | `26d6d50d0b` |
| L1 | a rule set sees only the specifications assigned by packages related to its own (`PackageRelations`, `RuleSet.getItemSpecs`, Drools models and `ItemTypeExtBuilder` built per rule set); `IsaarCpfPackageTest` languages, `PackageRelationsTest`; guide chapters 01 and 02 | `3290e86392` |
| L2 (1-3) | unit dates: lexicon `unitdate/lexicon.json` and corpus `unitdate/cases.json` shared by server and client; `UnitDateConverter` and `components/shared/unitdate/parse.ts` built from the lexicon, tolerant parsing, rendering per language, English forms; request-time texts in the UI language; format help from `examples.ts`; `UnitDateCorpusTest`, `unitdate.test.ts` | `1582e2b6ee` |
| 2c.5b-d | package `package-isaar-cpf` (ISAAR_CPF, version 1, English): shared classes, part types, 31 shared item types + 8 own, 121 specifications, rules and scripts, UI settings; distribution and test wiring; `IsaarCpfPackageTest` (alone, entities, export round trip, with CZ_BASE in both orders); guide chapter 04 | `cb38e0e182`, `222ce99cec` |
| L1-lang | ISAAR_CPF `NM_LANG`: the ISO 639-2 list with English names (486 languages; `LNG_` + bibliographic code as in CAM, `qaa-qtz` left out; 142 shared with CZ_BASE); generator committed (`package-isaar-cpf/generator/gen_isaar.py` with the LoC list `iso639-2.txt`); `IsaarCpfPackageTest` (486 for ISAAR, 166 for CAM, `LNG_aar` not in CAM, `LNG_0as` not in ISAAR); guide chapters 02 and 04, release notes. ISAAR_CPF stays version 1 (not released) | (uncommitted) |
| strict-xml | package files read strictly: the first unmarshalling event stops the import, the error names the file, the line and the element (`PackageUtils.convertXmlStreamToObject`; before, JAXB skipped unknown elements silently). Fixed in the packages: CZ_BASE 280 `<category>` without `<categories>` (no CZ_BASE specification ever had a category; the 166 languages are grouped now), `<hierarchical>` in `ap_type.xml` (CZ_BASE, ISAAR_CPF, two test packages, `EntityRulesTest`), `<view-order>` on specifications (ZP2015, simple-dev), unescaped `<Odkaz>` in a simple-dev description; versions unchanged (CZ_BASE 89, simple-dev 43 open; ZP2015 only lost ignored elements). `PackageXmlFilesTest` reads every file of the delivered and test packages | (uncommitted) |

**Decisions later steps build on.**
- *Translations:* key `(entity_type, entity_code, field, language)`, rows owned by the contributing
  package; an outdated translation (source hash differs) is still used. Declared names are added to
  the translations in static data (no rows stored).
- *Declarations (2c.3, 2c.4b, 2c.4c, 2c.4d):* the defining row (`ap_type`, `rul_part_type`,
  `rul_item_type`, `rul_item_spec`) holds the summary - owner and structural values from the winning
  declaration, the texts in the installation language. Item types and specifications differ: the
  package that created them stays the owner while it declares them (the owner decides the place in
  `view_order` / among the specifications), the other values come from the owner's declaration, and
  a declaration of another package takes no place and may state RECORD_REF classes (`item-aptypes`)
  only equal to the owner's; data type, use of specifications, structured type and table columns
  must agree. Specification assignments are the union of the packages' assignment declarations.
  Nothing reads the owner of a shared definition any more; the repositories no longer offer
  package-scoped finders. A definition is removed with its last declaration, refused while entities
  use it (`PART_TYPE_IN_USE`, `ITEM_TYPE_IN_USE`, `ITEM_SPEC_IN_USE`) or other packages refer to it
  (`FOREIGN_DEPENDENCY`: entity rules, members, part lists, child parts). Conflicting structure
  (class parent) refuses the import; values that do not affect stored data (part `repeatable`,
  `child_part`) come from the winning declaration.
- *Members and order:* a rule set without members offers everything (today's behaviour). The class
  tree of a rule set shows its members only: a member's parent chain is walked through members, the
  nearest member ancestor is the parent, otherwise the member is a root; the global tree (search
  filters) is the union over the entity rule sets. Display order: members of the owner of the rule
  set in file order, then contributing packages by dependency depth, package code, file order; a
  code listed twice keeps its first position. The part list is applied by the client (detail and
  copy dialog); an unlisted part type with existing parts is still shown. The `parts-order` UI
  setting is only the fallback for rule sets without a list.
- *Scripts:* the index script of a part (`INDEX`) and the script computing items of the entity
  (`AUTO_ITEMS`) are entity rules; the most specific rule of the rule set of the entity's scope applies
  (`GroovyService.mostSpecificEntityRule`); a scope without a rule set uses the only ENTITY rule set.
  No `AUTO_ITEMS` rule means no computed items. The index script contract (`DISPLAY_NAME` required,
  `PT_PREFER_NAME`, `SORT_NAME`, `SHORT_NAME`) is in the implementation guide.
- *Core and CAM:* core paths reachable by any entity work without CAM's item types; the CAM checks
  written in Java (relations, identifiers, GEO) apply only when those item types exist.
- *Tests:* packages stay installed across test classes; a test needing an installation without them
  calls `HelperTestService.deleteAllPackages()` and later classes re-import what they need. A second
  Spring context in the JVM is not possible (`DataType` keeps static state).
- *Scopes:* the package import assigns the ENTITY rule set to scopes without one only when there is
  exactly one; a change of a scope's rule set is refused while it holds entities of classes the new
  rule set does not offer.
- *Startup import:* packages without dependencies are imported in code order (`CZ_BASE` before
  `ISAAR_CPF`), so a new Czech installation gets CAM on scopes without a rule set.
- *Package files:* read strictly - an element the binding (`packageimport/xml`) does not define, or
  one in the wrong place, refuses the import; there is no XSD, `PackageXmlFilesTest` checks the
  delivered and test packages. Language codes of all packages are `LNG_` + ISO 639-2 bibliographic
  code, so frameworks share a language by code.
- *Specifications a rule set sees (L1):* the assignments of packages related to the package of the
  rule set - itself, its dependencies and its dependents, both transitively
  (`PackageRelations.relatedPackages`); an assignment without a declaration is seen everywhere.
  Computed in static data (`RuleSet.getItemSpecs(ItemType)`) and applied wherever a rule model is
  built for a rule set: fund, output and structured-object rules (`ItemTypeExtBuilder`), entity
  rules and item-type filters (`RuleService.createModelItemTypes`), the item-type listing of a rule
  set (`rulesListItemTypes`, AI tool). `getAllDescriptionItemTypes` (no rule set) keeps every
  specification. Siblings are not related: a local package depending on CZ_BASE and ISAAR_CPF does
  not make CAM's specifications visible to ISAAR_CPF.

## 3. Phases and order

| Phase | Content | State |
|---|---|---|
| **2c. Entity description frameworks** | ISAAR_CPF package done (section 4); 2c.6 EAC-CPF export and R2 reference scope on demand | done for 2b |
| **A1. Analysis: texts built in core** | decide per group of texts the Java code builds (validation messages, CSV headers) whether they belong to core, the rules or the client | **inventory done, decision pending** (section 7) |
| **2b. First internationalized version** | `rules-en-isadg` referring to the ISAAR_CPF classes; core neutrality; texts written by packages in the package language | after A1 |
| **2a.3 Remaining read sites** | tree titles, specification categories, further entity kinds, AI context; CSV exports after A1 | open, independent |
| **2a.4 Translation template** | export of translatable texts with source, hash and state | on demand (first maintained translation) |
| **1. Layer core (rest)** | revalidation requested by an addon, `PACKAGE` event on delete, settings export guard (section 6) | open, independent |
| **3. Pilots** | settings composition; DPP and CT as file addons | after 1 |
| **4. Customizations in the UI** | `kind`, archive, customization service and admin page, rule editor with compile check | after 3 |
| **5. Declarative primitives and overrides** | `specs-extensible`, item-type anchors, structure extensions across packages, aliases, retirement; UK pilot | later |
| **6. Convergence** | rule-set inheritance, renames towards ISAD(G) | later |

## 4. The international entity framework (2c.5) — done

**Goal (R1).** Persons, families and corporate bodies described according to ISAAR(CPF), with English
source texts, in the same installation as CAM entities and sharing with them what means the same.
ISADG (2b) will refer to these entities as creators. The result is described in the implementation
guide, chapter "International entity description (ISAAR_CPF)".

### 4.1 Decisions (user, 2026-10-07 and 2026-10-08)

1. **No dependency between ISAAR_CPF and CZ_BASE.** Definitions are shared through declarations of
   the same code; each package works alone.
2. **Roots only, kind of body as data** (research R-a, option (a)): assignable `PERSON_INDIVIDUAL`,
   `FAMILY`, `PARTY_GROUP`, `GEO`, `TERM`; own ENUM item types `ISAAR_CORP_TYPE`,
   `ISAAR_LEGAL_STATUS`, `ISAAR_FAMILY_TYPE`, `ISAAR_PLACE_TYPE`, `ISAAR_CONCEPT_SCHEME`. CAM's
   subclasses (kinds of bodies, fictional and non-human classes, family branches) stay CAM-only; the
   mapping of CAM classes to ISAAR values is in the guide chapter.
3. **Persons and families through option D:** the class tree of a rule set shows its members only;
   `PERSON` and `DYNASTY` are declared by ISAAR_CPF as parents (CAM's "person or being" is wider than
   ISAAR's person), not as members. CAM's logic is unchanged. Rejected: a visible grouping root (C) and
   ISAAR using the root class (E) - CAM is the Czech national system and its subclass concept is not
   shared with other methodologies.
4. **Non-agent classes:** `GEO` (Place) and `TERM` (Concept) in the first version; no `EVENT` class
   (dated facts of an entity are `PT_EVENT` parts); functions and mandates as concepts and text;
   `ARTWORK` stays CAM-only.
5. **Vocabularies as specifications** on the own ENUM item types, reuse of CAM's codes where the
   meaning matches (name forms, languages, identifier types, kinds of beginning and end, event types,
   relation types with CAM's target classes). The proposed values are **accepted for version 1**
   (2026-10-08); a later change of a value is a new specification code (package version bump, data
   migration for stored values).
6. **Shipped with every installation**; a scope uses it when its rule set is chosen.
7. **Research documents stay out of git** (`docs/entity-framework-research.md`,
   `docs/isaar-cpf-cam-comparison.md`, `research_notes/`, `reports/`); what the guide needs from them
   (the CAM class mapping) is in the guide itself.

### 4.2 Results

- *2c.4d* (`3dd400a540`): specifications declared by several packages, assignments as the union,
  owner rule as for item types, `ITEM_SPEC_CONFLICT`, `ITEM_SPEC_IN_USE`, export of declarations;
  853 specifications and 1,596 assignments on dev declared by their packages.
- *Research R-a/R-b* (2026-10-08, `docs/entity-framework-research.md`, uncommitted): no archival
  standard or profile subclasses agents; kind of body and legal status are dated, vocabulary-backed
  data (ISAAR 5.2.4, EAC-CPF, RiC-O; AnF keeps 64 categories as SKOS). Practice ranks Place first among
  non-agent entities; place model = dated names, type (the nine EAC-CPF/GeoNames classes), dated
  part-of, coordinates, external identifiers. Decisions D1-D9 of the document are closed by 4.1.
- *2c.5b-prep* (`26d6d50d0b`): members-only class tree, equal `item-aptypes` in foreign declarations,
  startup import of independent packages.
- *2c.5b-d* (`cb38e0e182`): package ISAAR_CPF version 1 - generated by
  `package-isaar-cpf/generator/gen_isaar.py` (regenerate rather than hand-edit; it writes the whole
  module and reproduces the committed files exactly). Adjustments against the design: CAM's `RT_RELATED` not reused
  (its target classes are not declared; own `ISAAR_RT_ASSOCIATED`); `CRE_CLASS`/`EXT_CLASS` reused
  with per-class subsets; languages of names are ISO 639-2 in CAM's `LNG_*` form (486, L1-lang); `IDN_VALID_FROM/TO` reused; no
  `AUTO_ITEMS`. Tests pass alone and with CZ_BASE in both import orders.

**Known limits of the first version** (open work): no EAC-CPF export (2c.6; needs `uri` on
specifications and relation categories derived from the specification); `NM_LANG` offers ISO 639-2 only
(a language outside it needs an addon declaring it); concepts have no scheme hierarchy beyond
`RT_SUPTERM`; no Czech translation file (2a.4 template when needed); when ISAAR_CPF is imported before CZ_BASE it owns the
shared codes and CAM's specification order on those item types follows ISAAR_CPF.

## 5. Entity description frameworks — remaining work

**R2 Scope-bound CAM extension.** A reference scope of portal.nacr.cz holds template entities with
items beyond CAM (explanatory items, a classification for browsing); they stay CAM entities. Open
questions (user): Q1 are reference entities exchanged with the central CAM system? Q2 extra items
inside CAM parts or extra parts? Q3 an extension activated per scope (`ap_scope_extension`, extension
rules for entities of the scope) or a rule set inheriting CAM (Phase 6)? Q4 (R1) relations across
frameworks (CAM entity to ISAAR entity, fund description referring to both, shared search).

**Remaining obstacles.**
- The DRL model's `PartType` enum and the TypeScript part enum are closed; a new part code needs both
  opened (the test package's `PT_ENT_NOTE` works only because it is never validated).
- CAM exchange with extra content (R2): extra item types are dropped by `ItemTypeMap.groovy` (safe);
  an extra specification of a CAM item type drops the whole part from the export; an extra part type
  crashes the export (`PartTypeXml.fromValue`, `cam/v2/SearchFilterFactory` `:244`).
- Search boosts (`index-search` in the root `ui_setting.xml` of CZ_BASE) are global, not per rule set.

**Known gaps found while building 2c.5a.**
- Computed items and the Groovy model of a revision use the entity's current class, not a class
  changed in the revision (`GroovyService.convertAe`, `autoItemsScriptPath`; older behaviour).
- Test helpers (`authorizeAsAdmin`, `tx`, `txGet`) are repeated in `EntityRulesTest`,
  `StandaloneFrameworkTest` and `IsaarCpfPackageTest`; a shared base for package tests would remove
  them.

**Later, optional.** Remove the unused category tree of specifications
(`ClientFactoryVO.createDescItemTypeExt`/`createTree`, `RulDescItemTypeVO.itemSpecsTree` and its
client type): nothing calls it, the client never displayed categories, and it grouped only
consecutive specifications (by name the groups would split) - categories are stored and exported,
not shown. `AP_MAPPING_TYPE` (CAM export mapping) as an entity rule; the CAM relation and
identifier checks of `RuleService` as CAM `VALIDATION` rules; a server-side check that a
new part's type is in the rule set's part list; port the rest of `ApStateChangeForm` /
`RevStateChangeForm` to Fluent UI; per-package RECORD_REF classes of shared item types and
specifications.

## 6. Phase 1 — layer core, open part; release gate

- **Revalidation requested by an addon.** `compatibility-rul-package` lives on the owner's rule set
  and on extension and entity rules; an addon cannot ask for revalidation of a foreign rule set.
  Proposal: accept it on `<arrangement-rule>`; for rules contributed to a foreign rule set compare
  with the addon's installed version and revalidate the funds of that rule set. Open: whether deleting
  an addon revalidates.
- **Package deletion** publishes `ActionEvent(EventType.PACKAGE)` after commit and enqueues
  revalidation of the cleaned rule sets; the client's `packageEvent()` also invalidates `groups`,
  `structureTypes`, `apTypes`.
- **Export:** `exportSettingsForRuleset` reports a missing rule set instead of an NPE (the
  specification export fixes were done in 2c.4d).
- **Tests to add to `AddonPackageTest`:** spec raised by an addon rule where its type is possible; a
  scoped variant with its own extension; revalidation by an addon rule; export → delete → re-import.
- **Release gate of 3.4 (not done):** on a PostgreSQL copy of production apply the changesets
  (`20261006140000` … `20261008160000`), re-import CZ_BASE 89 and ZP2015, and check that
  spec order and `view_order` are unchanged (CZ_BASE 89 sets the categories of its 166 languages) and that ISAAR_CPF imports at startup next to them.

## 7. Localization and the internationalized version

### A1 — texts built in core (inventory 2026-10-08, decision pending)

*Method:* string literals with Czech diacritics in `elza-core/src/main/java` outside comments,
logging and exception texts (exceptions reach the client as codes mapped in `messages.ts`): 284
literals; plus core resources and init data. Texts without diacritics or in English are not
caught; the groups below are complete for the mechanisms, not necessarily for every string.

| Group | Where (class:line) | Count | Reaches the user as |
|---|---|---|---|
| A. Node conformity (fund validation) | `validation/impl/Validator` `:91,118,153,172,187,225,239`, `domain/vo/DataValidationResults` `:126,128` - "Prvek X musí být vyplněn.", "... se specifikací Y není možné evidovat ...", "Atribut X není opakovatelný.", "... odkazuje na zneplatněnou entitu (id)" ... | 7 templates | stored in `arr_node_conformity_error.description`, `arr_node_conformity_missing.description`; `NodeConformityErrorVO(descItemObjectId, description, policyTypeId)`, `NodeConformityMissingVO(descItemTypeId, descItemSpecId, description, policyTypeId)`; the client prints `description` (`NodePanel.jsx:718,742`, `ErrorDisplay.tsx:43`). Rule-owned messages (`createMissing(typeCode, message, policy)` from DRL, 31 calls in ZP2015 `Validation.drl`) share the column. |
| A2. Entity validation | `service/RuleService` `:1516-1684` - "V části X chybí povinný typ prvku ...", "... je zakázaná specifikace ...", "... je vztah ... vícekrát." ...; `AccessPointService:3619`, `PartService:70` (duplicate key value) | 9 templates | returned as `ApValidationErrorsVO` (strings); the duplicate-key text goes into the entity's error text |
| B. CSV headers | `ArrIOService` `:449-455` (data export: "Číslo záznamu", "Číslo JP", "Atribut", "Specifikace", "Hodnota", "ID entity"), `:537-539` (grid export + item shortcuts), `IssueService` `:470-480` (issues: "Druh", "Stav", "Uživatel", "Popis", "Komentáře" ...) | 19 strings | downloaded files (`DEExportService` headers are English identifiers, fine) |
| C. Data type names | `db.elza-init.xml` `:99-123` (`rul_data_type.name/description`: "Celé číslo", "Řetězec", "Datace" ...) | 12 rows | not read by the client (no `dataType.name` use in `elza-react`); only in server exception texts |
| D1. Content: unit-date text | `domain/converter/UnitDateConverter` `:37-52` (" př. n. l.", "%d. st. př. n. l."; input regexes accept the Czech form), `print/item/convertors/UnitDatePrintConvertor` `:36-42` (months, "%d. století") | 7 constants | `ArrDataUnitdate.getFulltextValue()` (index), `GroovyItem.value`/`GroovyAppender` (entity indexes and display names stored in `ap_index`), `ArrItemUnitdateVO`/`ApItemUnitdateVO.value` (REST display), tree titles (`DescriptionItemServiceInternal:185`), print outputs, `ImportFromFund` |
| D2. Content: values and names written by core | `DaoCoreServiceWsImpl:543` ("Importováno - <date>" as an item value of a node), `ArrangementService:2239` (default name "Šablona" of a reference template), `ArrangementService.UNDEFINED` = "výjimka" (tree titles `DescriptionItemServiceInternal:158`, validator text) | 3 | persisted content / titles |
| D3. DA / AIP module | `service/da/*` (`DaService` `:200-2190`, `AipProblem`, `DaAipReferenceResolver`, `DaImportBuilder`, `DaImportPlanner`, `DaLevelItems`, `DaMatchResult`, `EadUnitdateParser`, `DidElementConverters`, `AipPackageType`, `DaAipStepService:137`), `domain/DaDao.DaoType` labels | ≈ 80 | AIP state and step messages persisted on the AIP and shown in the AIP pages; script errors of `DA_IMPORT`/`DA_MATCH` |
| E1. AI module | `service/ai/AiProposalService` `:492-711` (why a proposal cannot be applied), `RevisionFindingsBlockMapper` `:41-155` (labels of the findings block: "Doporučení", "závažnost", severity words, action labels) | 27 | REST display, built in the request thread |
| E2. Request-time labels | `DaoService:1038,1043`, `AipService:110,134` (explorer tree: "Logická struktura", "Balíček", "Bez logické struktury"), `security/apikey/ApiKeyFailure` `:8-13` (401 bodies), `PasswordPolicyService:80`, `AdminOldController:199,238` (log viewer) | 14 | REST display |
| E3. Enum labels | `controller/vo/ExtAsyncQueueState`, `domain/ExtSyncsQueueItem` states, `DaSyncQueueItem` states ("Ke stažení", "Odesláno" ...) | 25 | the VOs serialize the enum constant, the client has its own labels - presumably dead; verify `value()` callers and delete |
| F. Core resources | `script/groovy/createDid.groovy` (assert texts only), `exportDaTemplates/*.xml` (DA export samples, Czech by design) | - | - |

*Three rules (proposal).*
1. **Persisted findings carry a code, the client renders them** (A, A2). `arr_node_conformity_error` and
   `arr_node_conformity_missing` get `message_code` (nullable) and `message_param` (the entity id of the
   deleted-entity case); `description` stays for rule-owned messages and old rows. Codes:
   `MISSING_REQUIRED`, `TYPE_IMPOSSIBLE`, `SPEC_IMPOSSIBLE`, `TYPE_NOT_ALLOWED`, `NOT_REPEATABLE`,
   `UNDEFINED_NOT_ALLOWED`, `DELETED_ENTITY_REF`. The VOs expose the code; the client renders it with
   the item type and specification names from its reference tables in the UI language (`messages.ts`),
   falls back to `description`. The same for `ApValidationErrorsVO`: structured entries
   `(code, partTypeCode, itemTypeCode, itemSpecCode)` beside the text. Rule-owned messages stay text;
   `TranslationEntityType.MESSAGE` (2a.1) is the path to translate them when a package wants to. No
   data migration: rows are rewritten by revalidation. Validation runs in workers without a request
   language, so server-side rendering is not an option for this group.
2. **Request-time texts come from a core catalog in the request language** (B, E1, E2): a resource
   bundle `core-messages_<tag>.properties` (cs, en) read through a `CoreMessages` helper next to
   `PackageTexts.requestLanguage()`; keys in code, no literals. About 60 keys (CSV headers 19, explorer
   labels 4, AI block labels and reasons 27, API-key failures 6, password policy 1). This is the
   "server catalog" option, limited to texts that are built inside a request and are not data.
3. **Texts a package writes follow the package** (D1, D2): the text form of unit dates inside entity
   index names and outputs, the word for an undefined value in titles, default names and values
   written by imports are rendered in the language of the rules package whose script or template
   writes them (`rul_package.language_id`: CAM and ZP2015 Czech, ISAAR_CPF and ISADG English) - never
   the UI language, because background jobs write them without a user, and never a language of the
   fund (rule agreed 2026-10-08, section 7 L2 step 4). What a user sees at request time follows the
   user (`PackageTexts.requestLanguage()`); the DAO import value and the template name come from the
   rules or the client instead of core.

*Out of scope, documented as Czech modules:* D3 (the DA/AIP integration is the Czech national digital
archive interface, like CAM), C (not shown), F. E3 is a cleanup.

*Open for the user:* (1) agree with the three rules; (2) A2 in the same implementation step as A
(recommended: the 2b exit criterion includes entity editing) or later; (3) DA/AIP and CAM declared
Czech-only, no catalog for them; (4) unit dates - resolved by L2 (section 7): no language on funds,
texts follow the user or the writing package. *Implementation step (after the decision):* 2
changesets, `Validator`/`DataValidationResults`/`RuleService` codes, 3 VOs, client rendering, the
`CoreMessages` catalog with the B/E1/E2 call sites, enum-label cleanup; tests for codes and both
languages of the catalog. Estimate 2-3 days.

### L2 — unit-date text per language (found 2026-10-08, design agreed 2026-10-08)

*Finding.* The stored form of a unit date is language-neutral (`valueFrom`/`valueTo` ISO, `format`);
only the text is Czech, in two places. Server: `UnitDateConverter` renders and parses one Czech form
(" př. n. l.", "%d. st.", dates `d.M.u`; a negative year is accepted as BC too),
`UnitDatePrintConvertor` prints Czech month names and "%d. století". Rendering reaches 14 files (REST
`ArrItemUnitdateVO`/`ApItemUnitdateVO.value`, the fulltext index `ArrDataUnitdate.getFulltextValue`,
`ap_index` through `GroovyItem`/`GroovyAppender`/`GroovyUnitdateFormatter`, tree titles,
`DateRangeAction`, `ImportFromFund`, print), parsing 10 (item input through `ArrDataUnitdate`, search
filters of nodes and entities, CAM search filters, `DrlUtils`, `ValidationController`). Client: the
grammar `shared/datace/datace.pegjs` validates in five places (entity part forms, fund item forms,
fund filter settings, search filters) and feeds the estimate conversion; it accepts the Czech century
and a `bc ` prefix the server does not parse (and not " př. n. l."); `convertToEstimate` in
`UnitdateField.tsx` splits the text on "-"; the format help `dataType.unitdate.format` is Czech only
(no English translation in `en.json`); the generated parser says Peggy 2.0.1, which `package.json`
does not declare (regenerated by hand); the server endpoint `ValidationController.validateUnitDate`
and its client wrapper `WebApi.validateUnitdate` have no callers. The client never renders a date: it
shows the server's `value`. `ApScope` already has a language (ISO 639-2); `ArrFund` has none.

*Decisions (user, 2026-10-08).*
1. **Parsing is language-independent, rendering is per language.** One tolerant parser on each side
   accepts every language's markers (BC, century, month names, date patterns) and the negative year
   whatever the UI language; rendering happens only on the server, in the language asked for.
2. **One grammar, not one per language.** The language-dependent tokens form a lexicon shared by
   both sides; the client keeps a single Peggy grammar. Adding a language adds tokens, not a parser.
3. **Unified by a shared lexicon and a shared corpus, not by shared code.** Rejected: ANTLR with Java
   and TypeScript targets (one source, but a rewrite of the server parser and build plugins on both
   sides for ~15 rules); server-only validation through the existing endpoint (grids and filters
   would wait on the network for every keystroke, the estimate conversion would need a call too).

*Design.*
- **Lexicon** `elza-core/src/main/resources/unitdate/lexicon.json`: per language tag (`cs`, `en`)
  the BC markers (prefix and suffix forms), century markers, month names (print, English input),
  the date, year-month and date-time patterns, the interval and estimate delimiters; plus the
  language-independent forms (negative year, ISO full date `u-MM-dd`). The server builds its
  forms from the union over languages at class init (`UnitDateLexicon`, `UnitDateConverter.Form`)
  and renders through the `render` templates of the language. The client imports the same file at
  build time (`components/shared/unitdate/lexicon.ts`, relative path into elza-core; the Vite dev
  server allows the directory): the lexicon is code, versioned with both sides, not installation
  data - so no endpoint, no loading state, no stale copy. *Changed against the first design:* the
  runtime endpoint was dropped for the build-time import.
- **Corpus** `elza-core/src/main/resources/unitdate/cases.json`: inputs with the expected normalized
  result (`valueFrom`, `valueTo`, `format`, estimates) or an expected error, covering every lexicon
  form, both languages and the known divergences (`bc ` prefix, negative year).
  `UnitDateConvertorTest` runs it; a vitest in elza-react reads the same file by relative path
  (`../elza-core/src/main/resources/unitdate/`, no jar staleness) and runs it through the client
  parser. The corpus is the drift guard.
- **Server rendering.** `UnitDateConverter.convertToString(unitdate, language)`; the overload
  without a language renders the default language of the lexicon (Czech) and is what package scripts
  and core jobs call until step 4. Request-time texts (REST VOs, the item lists of the client) use
  `PackageTexts.requestLanguage()`; the remaining places follow the rule of step 4 below.
- **Client.** `components/shared/unitdate/parse.ts` mirrors the server algorithm step by step
  (normalize, estimate interval, spaced delimiter, single part, legacy split; the same ordered forms
  built from the lexicon) and produces the stored form, so the corpus asserts identical output.
  *Changed against the first design:* the Peggy grammar is gone, not kept - PEG cannot apply the
  lexicon's regular expressions at token boundaries without consuming input, and a hand-written
  mirror of ~300 lines is one algorithm in two languages instead of two formalisms. `validateUnitDate`
  keeps its signature; `convertToEstimate` rewrites the text from the spans of the parsed parts and
  keeps the user's delimiters; the format help (three message sets: entity field, fund item form,
  node form tooltip) carries only labels, the examples come from `examples.ts` per UI language and
  `unitdate.test.ts` parses every one; the dead `WebApi.validateUnitdate` wrapper is removed.

*Q1 - English forms (confirmed by the user 2026-10-08).* Rendering: year `1968`, BC `500 BC`, century
`20th century` and `20th century BC`, year-month `Aug 1968`, date `21 Aug 1968`, date-time
`21 Aug 1968 14:05`, interval `1968 – 1969` (en dash with spaces, as the Czech print output),
estimate `[1968]`, estimated interval `1985/1990`. Month names because the numeric English forms
collide with today's delimiters: `1968-08` reads as the year interval 1968 to 8, `8/1968` as an
estimated interval, and `d/M/u` is ambiguous with `M/d/u` for users. Input additionally accepts the
ISO full date `1968-08-21` (three groups, unambiguous), every Czech form, and the delimiters "-",
" - ", " – ". Q2 (month names in print outputs) is covered by the lexicon.

*Steps.* (1) lexicon, corpus, server parser built from the lexicon, corpus test; (2) client parser
mirroring the server, estimate conversion, help texts, vitest with the corpus; (3) request-time
texts in the UI language - all three done 2026-10-08 in one day; (4) the language rule for the
remaining places, below - about 1.5 days.

*Done (2026-10-08, Q1 confirmed, steps 1-3).* Lexicon and corpus (`unitdate/lexicon.json`,
`unitdate/cases.json`, 93 cases); `UnitDateConverter` rebuilt on the lexicon with the same public
API, `convertToString(unitdate, languageTag)` and the `beginToString`/`endToString` overloads;
`UnitDateCorpusTest` (parse, render per language, rendered texts parse back) next to the untouched
`UnitDateConvertorTest`; request-time texts in the UI language: `ApItemUnitdateVO` and
`ArrItemUnitdateVO` take the language from `PackageTexts.requestLanguageTag()` (ApFactory,
ClientFactoryVO, whose converter map became an instance map), `ApItemUnitdateVO.equalsValue`
compares the stored form instead of the text (the text now depends on the request language);
client parser, examples, help and tests as above. Behaviour changes beyond English: "0. st." is
refused (the client already refused it, the server accepted a century 0); tokens of an interval are
trimmed ("1968  -  1969"); markers are case-insensitive. Untouched so far: the search filter of
entities compares the Czech text against the index (`ApStateSpecification`), the fulltext and
`ap_index` texts, titles, `GroovyUnitdateFormatter`, `UnitDatePrintConvertor` (month names are in
the lexicon already), and the legacy client renderer in `party/DatationField.jsx` (search form and
bulk modifications) which still writes Czech text itself.

*Step 4 (rule agreed 2026-10-08, user): no language on funds.* The archivist knows the language of
the description; the application needs no setting of its own. Three cases cover every remaining
place:
- **What a user sees follows the user.** Done for item values, forms and filters. Tree titles are
  built on the server from the stored value, so they follow the request too - unless the level cache
  keeps the finished text; then it keeps the structured value and renders on the way out (check
  first). The legacy client renderer in `party/DatationField.jsx` stops rendering Czech itself and
  shows the server's text or renders through the lexicon of the UI language.
- **What a package writes follows the package.** Entity index names (`GroovyUnitdateFormatter`,
  `GroovyItem` in index scripts) and print outputs (`UnitDatePrintConvertor`, templates) are written
  by the script or template of a rules package, often in background jobs without a user; they render
  in the language of that package (`rul_package.language_id`; CAM and ZP2015 Czech, ISAAR_CPF and
  ISADG English), so the date matches the words the script puts next to it. The Groovy formatter and
  the print convertor take the language from the package of the running script or template; the
  default overload keeps Czech for callers without a package.
- **Everything else carries no language.** The fulltext index (`ArrDataUnitdate.getFulltextValue`,
  the entity fulltext) holds the text in every language of the lexicon, so a search finds the date
  whatever language the user typed it in. The entity date filter (`ApStateSpecification`, today a
  text "contains" of the Czech form against the index) compares normalized ranges. `DateRangeAction`
  and `ImportFromFund` store structured values and need no text.

Dropped by the rule: `arr_fund.language_id`, the per-fund "description language", and the A1 open
question on English funds. Step 4 is independent of 2b except the English print templates, which 2b
brings.

**2a.3 Remaining read sites.** Tree titles (specification names in node titles,
`DescriptionItemServiceInternal` `:164`); specification categories (kind `ITEM_SPEC_CATEGORY`); kinds
`EXPORT_FILTER`, `OUTPUT_FILTER`, `STRUCTURED_TYPE_EXTENSION`, `EXTERNAL_ID_TYPE`, `INSTITUTION_TYPE`,
`ACTION`; AI context and proposal rows; CSV exports after A1.

**2a.4 Translation template (on demand).** `PackageTranslationService.template(packageCode, tag,
of)`: every translatable text with `src-hash`, the existing translation, the source text and state in
a comment, orphans at the end; `GET /api/v1/admin/packages/{code}/translation-template`.

**Language of texts written by packages** (rule agreed 2026-10-08; replaces the per-fund
"description language"). Names stored as text (structured object values, entity indexes and key
values, auto items, outputs, the fulltext index of ENUM values) are written by the scripts and
templates of a rules package and follow the language of that package (`rul_package.language_id`),
never the UI language and with no language on funds; core texts without a package stay on
`elza.locale`. 2b makes Groovy scripts and print templates resolve names in the language of their
package; prerequisite: scripts using names as identifiers switch to codes (`PT_IDENT.groovy`, the
ZP2015 EAD template, the PDF content page).

**2b `rules-en-isadg`.** Module `elza/rules-en-isadg` (package and rule set `ISADG`, prefix `ISADG_`)
from the `rules-simple-dev` skeleton, no dependency on CZ_BASE; one item type per ISAD(G) element, no
STRUCTURED types (identity, context with `ISADG_CREATOR` referring to the ISAAR_CPF classes, content
and structure, access and use with an own `ISADG_LANGUAGE`, allied materials, notes and control, DAO
link, container, access point); rules with English messages (filter, available items with the
mandatory elements at fonds level, new-level scenarios, validation, impact), policy types, fund
validation; UI settings by the seven ISAD(G) areas; Czech translation file. Core neutrality:
"Compute EJ" only for rule sets offering `ZP2015_INTRO_VYPOCET_EJ` (`FundTreeMain.jsx`),
`createDid.groovy` with ISADG codes, core messages per A1. Test `IsadgPackageTest`; exit: an English
user creates an ISADG fund, fills the mandatory elements, links a creator, validates, and sees no
Czech text.

## 8. Later phases — design notes

**Phase 3 — pilots.** `SettingsService.resolveGlobal(type, entityType, entityId)` composes settings
by package dependency order: last wins for FUND_VIEW, STRUCTURE_TYPES, PARTS_ORDER, ITEM_TYPES,
DAO_LEVEL_IMPORT, STRUCT_TYPE_*, FUND_ISSUES; TYPE_GROUPS as a placement patch; GRID_VIEW appends.
*Started 2026-10-08:* `resolveGlobal` exists (last wins; a setting without a package wins over all)
and `UISettings.SettingsType.layered` marks the types it applies to - so far only the new
`OUTPUT_DEFAULTS` (default output filter, guide chapter 02 "output-defaults"); import skips the
`OTHER_PACKAGE` check for layered types. The types above join by setting the flag and switching
their read site to `resolveGlobal`.
Nine read sites (`SettingsService` `:341`, `:393`, `ConfigRules` `:61`, `ConfigView` `:88`,
`IssueDataService` `:150`, `StructObjService` `:1321`, `ClientFactoryVO` `:1251`, `:1343`, `ApFactory`
view settings, `DaoCoreServiceWsImpl` `:476`). `UISettings.isSameSettings` (`:132`) compares strings
with `==` - check production for same-key rows of different packages before fixing. Then DPP and CT
become file addons.

**Phase 4 — customizations in the UI.** `rul_package.kind` (`IMPORTED`, `LOCAL`), `rul_package_archive`
(every version's ZIP, in the database); package code = code prefix; scope global or an own
arrangement extension; dependencies derived on save; `CustomizationPackageService.rebuild` imports in
memory (`importPackageInternal(Map, boolean)`); a specification is added with its position and a
generated rule; the rule editor compiles before save; detach/attach; spec usage counts protect
deletion; OpenAPI tag `customization` (ADMIN), admin page `/admin/customization`.

**Phase 5.** `rul_item_type.specs_extensible`, item-type anchors with a layout pass replacing the
`view_order` blocks, retirement of specs, code aliases, structure extensions across packages,
`NewLevelApproaches.remove`; UK pilot (its `PERSON/PT_EVENT.groovy` patch becomes an `INDEX` rule).

**Phase 6.** Rule-set inheritance; renames of generic ZP2015 codes towards ISAD(G) through aliases.

## 9. Reference facts

- Spec availability: `RulItemTypeExt` sets every spec IMPOSSIBLE, only DRL raises it;
  `ClientFactoryVO.createFormItemTypes` drops IMPOSSIBLE specs.
- Item-type filter: the rule set's DRL plus `ITEM_TYPE_FILTER` rules of other packages
  (`RuleService.getItemTypeCodesByRuleSet`).
- Startup: `autoImportPackages` imports distribution ZIPs in topological order, independent packages
  by code; packages existing only in the database are not re-applied.
- Same-version re-import in testing mode replaces the package directory; tests reading package files
  run before refusal tests.
- Conventions: changesets only in `db.elza-3-part-03.xml` (id `yyyyMMddHHmmss`, hibernate sequences in
  `db_hibernate_sequences` as `table|column`, allocation 20); REST OpenAPI-first
  (`elza-development/typespec/main.tsp`, delta applied to `rest/elza-openapi.yml`); client messages in
  `messages.ts` + `npm run locale:sync` + `lang/translated/en.json` (locale gate); `npm run
  ts:strict-check` before every commit touching `.ts/.tsx`; the docs job builds both guides with
  Sphinx `-W`.
- A content change of a package without a version bump never reaches an installation, but an
  unreleased version takes further changes (check `git tag --contains`): 3.4.7 released CZ_BASE 88,
  ZP2015 345 and simple-dev 41, so CZ_BASE 89, simple-dev 43 and ISAAR_CPF 1 (new) are open.

## 10. Critical files

- Import: `packageimport/PackageService.java`, `ItemTypeUpdater.java`, `APTypeUpdater.java`,
  `PackageDeclarations.java`, `PackageTranslationService.java`, `PackageUtils.java`, `xml/*`.
- Runtime: `core/data/StaticDataProvider.java`, `core/data/RuleSet.java`, `core/data/PackageTexts.java`,
  `service/RuleService.java`, `service/GroovyService.java`, `service/AccessPointService.java`,
  `controller/factory/ApFactory.java`, `domain/bridge/IndexConfigReaderImpl.java`.
- Schema: `elza-core/src/main/resources/db/changelog/db.elza-3-part-03.xml`.
- Packages: `package-cz-base/`, `package-isaar-cpf/` (generated by `generator/gen_isaar.py`),
  `rules-cz-zp2015/`,
  `rules-simple-dev/`.
- Client: `components/registry/ApDetailPageWrapper.tsx`, `ApTypePicker.tsx`,
  `modal/CreateAccessPointModal.tsx`, `components/shared/lang/language.ts`, `LanguagePicker.tsx`.
- Tests: `packageimport/EntityRulesTest.java` (`entity-rules-test`), `StandaloneFrameworkTest.java`
  (`entity-standalone-test`), `IsaarCpfPackageTest.java`, `rules/addon/AddonPackageTest.java`
  (`rules-addon-test`), `PackageTranslationTest` (`translation-addon-test`),
  `PackageXmlFilesTest` (every package file read strictly), `search/IndexConfigReaderTest.java`,
  `other/HelperTestService.java` (`deleteAllPackages`).
