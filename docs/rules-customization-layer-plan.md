# Rules-package customization layer and first internationalized version — plan

Status as of 2026-10-12. Design record for extending rules packages without forks (addon packages,
customizations edited in the admin UI), for entity description frameworks beside CAM, and for the
first internationalized version of ELZA (an English rule set based on ISAD(G)). Finished work is
described for implementers in the implementation guide (`docs/implementation-guide`, English
only) and in the release notes; this plan keeps the decisions later steps build on and the open work.
Line numbers refer to the `3.4.x` tree at the time of writing.

## 0. Next

Status 2026-10-12. 2b (`rules-en-isadg`): step 1, the walking skeleton, is done (section 2, row
"2b step 1"; section 7, "Step 1 done"): the package, its generator, the test next to CZ_BASE and
ZP2015, the user guide with nine chapters written. The skeleton found two places that block an
ISADG fund in a Czech installation; the persistent sort (D9 part 2) is resolved the same day, the
pinned search filters as one global setting remain; institution types are closed. The
customization-layer pilots (Phase 1 rest, Phase 3) stay independent and can be interleaved; 2c.6
(EAC-CPF export) and R2 (reference scope) wait for a concrete need and for the answers to R2's
questions.

1. **The UI pass of the skeleton** (user): an ISADG fund described in the user interface of a dev
   installation; what breaks goes into the core places of section 7.
2. **2b step 2** (section 7, about 1 day): the pinned search filters per rule set, the container
   generator, the citation script.
3. **2b steps 3-4** (section 7, about 6 days): L2 step 4 with the EAD3 template; PDF, the
   remaining core places, implementation guide chapter 05, user guide chapter 9 and part II,
   release notes.
4. **A1 rule 3** - texts a package writes follow the package (section 7, proposal); done together
   with 2b's print templates and index scripts.
5. Release gate of 3.4 (section 6): re-import of unchanged CZ_BASE 89 and ZP2015 on a PostgreSQL copy
   after the four declaration migrations, `view_order` and spec order unchanged.

Decisions still open: A1 rule 3 (section 7) and R2 Q1-Q4 (section 5).
D4 is built as proposed; a change is cheap while the package is unreleased.

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
| L1-lang | ISAAR_CPF `NM_LANG`: the ISO 639-2 list with English names (486 languages; `LNG_` + bibliographic code as in CAM, `qaa-qtz` left out; 142 shared with CZ_BASE); generator committed (`package-isaar-cpf/generator/gen_isaar.py` with the LoC list `iso639-2.txt`); `IsaarCpfPackageTest` (486 for ISAAR, 166 for CAM, `LNG_aar` not in CAM, `LNG_0as` not in ISAAR); guide chapters 02 and 04, release notes. ISAAR_CPF stays version 1 (not released) | `ecd180c80d` |
| strict-xml | package files read strictly: the first unmarshalling event stops the import, the error names the file, the line and the element (`PackageUtils.convertXmlStreamToObject`; before, JAXB skipped unknown elements silently). Fixed in the packages: CZ_BASE 280 `<category>` without `<categories>` (no CZ_BASE specification ever had a category; the 166 languages are grouped now), `<hierarchical>` in `ap_type.xml` (CZ_BASE, ISAAR_CPF, two test packages, `EntityRulesTest`), `<view-order>` on specifications (ZP2015, simple-dev), unescaped `<Odkaz>` in a simple-dev description; versions unchanged (CZ_BASE 89, simple-dev 43 open; ZP2015 only lost ignored elements). `PackageXmlFilesTest` reads every file of the delivered and test packages | `ecd180c80d` |
| A1 rules 1-2 | validation messages of entities and funds translatable: a rule reports `results.addError("KEY", "text {0}", arg)` / `dvResults.createMissing(typeCode, "KEY", "text", policy, args)`, the key qualified by the rule's package; stored as one JSON line `{"key","text","args"}` in the existing `error_description` columns (typed arguments: numbers, references to item types / specifications / part types rendered as names in the reader's language, nested messages; `ValidationMessage`) and rendered when read (`PackageTexts.render`: `ApFactory`, `ApController`, `ClientFactoryVO.createNodeConformity`, the fund validation list, the AI context; the full-text index gets the source text); core messages are the enum `CoreMessage` with Czech and English shipped in `elza-core/src/main/resources/translations/{cs,en}.xml` (package file format, lowest layer of `PackageTranslations`, overridable by any package; `CoreMessagesTest`); request-time core texts through `PackageTexts.text(CoreMessage)`: CSV headers of the fund and issue exports, the 401 body of a refused API key, the reasons an AI proposal cannot be applied; ISAAR_CPF 2 with 5 keyed messages and `translations/cs.xml`; CZ_BASE and ZP2015 untouched; `IsaarCpfPackageTest`, `AddonPackageTest`, `PackageTextsTest`; guide chapter 02 "Messages" | `f3ff831704`, `a4f19b7541` |
| open part type | the Drools model's `PartType` is a class with interned instances instead of an enum (the seven CAM constants stay, `PartType.of(code)` for any declared code; rules unchanged on both sides, the CAM sync keeps its one-line diff); `Part.typeCode` / `ModelPart.typeCode` for rules of packages with own part types; the index model lists the installed part types; the client titles the dialogs of a foreign part type by its name and lost the dead TypeScript part enums; `EntityRulesTest` validates `PT_ENT_NOTE` by the rules of ENT_TEST (version 2) | with this row |
| A1 UI-owned, E3 | texts the client owns stay on the client: the logical AIP tree and the explorer label their virtual levels by `AipLevelType` (the server name is a fallback), the log viewer shows a message for `LogVO.error` (`NO_PATH`, `FILE_NOT_FOUND`, `READ_ERROR`) with the path; the dead Czech labels of the queue-state enums removed (`ExtAsyncQueueState`, `ExtSyncsQueueItem.ExtAsyncQueueState`, `DaSyncQueueItem.QueueItemState` - the client has its own) | with this row |
| search config | `index-search` composed per field (`SettingsService.getIndexSearch`, cached per static data; `loadIndexSearch` for the index bridge at startup): core defaults `accessPointId`, `index`, `pref_index` (`SettingIndexSearch.coreDefaults`), then the fields of every package, a field stated by several packages taken from the deepest in dependency order; before, the importer refused `index-search` from a second package (the type was not layered). CZ_BASE states only its own fields. Kept global, not per rule set: one search ranks entities of all rule sets on one scale. Guide: `02-package-files` "index-search"; `AddonPackageTest` | with this row |
| search fields, base vocabulary | the text search looks in every item field of `index-search` instead of fixed `nm_main`/`nm_minor` (`SettingIndexSearch.searchedItemFields`; area *names* by the new attribute `part-type`, CZ_BASE marks `nm_main`/`nm_minor` `PT_NAME`; fields missing from the index schema skipped until restart); weights unchanged for CZ_BASE. Decision (user, 2026-10-12): no part roles - the part types `PT_NAME`, `PT_BODY`, `PT_CRE`, `PT_EXT` and the items `NM_MAIN`, `NM_MINOR`, `CRE_DATE`, `EXT_DATE` are a base vocabulary with built-in logic, documented in guide 01 "Base parts and items"; a framework depends on CZ_BASE or declares them. `SettingIndexSearchTest` | with this row |
| D9 part 1 (core place 2 of 2b) | closed by removal (user, 2026-10-12): the DID sent to a WSDL digital repository or a digitization front desk (link notification, DAO and digitization requests, WS `getDid`) carries the identifier only - `WSHelper.createDid`; the script `createDid.groovy`, its copy into the working directory and the Groovy DID methods are removed, no rule type. The script had not compiled since 2020 (the converter class and the `Did` accessors renamed) and nothing missed it; the WSDL repository infrastructure stays, its future is Phase 4 of `da-migration.md`. Release notes 3.4.9 "Changes"; `DaoCoreServiceTest` | with this row |
| 2b step 1 | the walking skeleton of `rules-en-isadg` (package `ISADG` 1, rule set `ISADG`, depends on ISAAR_CPF 2): `generator/gen_isadg.py` writes the whole module from tables - 55 item types in the seven areas plus the container, the digital object, the access point and the finding aid; 539 specifications (6 levels, 9 units of measure, 15 kinds of material, 3 access statuses, 6 kinds of container, 7 roles of access points, 4 kinds of finding aid, the 486 ISO 639-2 languages of ISAAR_CPF shared by code and assigned to two items); the structured container with its DRL and Groovy; `ItemTypeFilter`, `AvailableItems` (level × area, the material-type gating of D4), `NewLevel` (scenarios named by the level, by the parent's level), `Validation` (root is a fonds, order of levels, dates within the parent, container at the end of a branch; keyed messages, Czech in `translations/cs.xml`), `ChangeImpact`, `Output_FindingAid`; 3 policy types; actions (validation, reference-code generator, persistent sort under its own name); UI settings (FUND_VIEW with icons per level, TYPE_GROUPS by area, GRID_VIEW, FUND_ISSUES, STRUCTURE_TYPES; DAO_LEVEL_IMPORT, favourite languages); institution types; wiring (root pom, core test resource, distribution, `elza.yaml`, admin guide "Loading the packages", `PackageXmlFilesTest`); `IsadgPackageTest` (vocabulary; a fonds built by the scenarios; availability by level; the validation rules rendered in en and cs; next to CZ_BASE and ZP2015); user guide `docs/user-guide` (Sphinx like the other guides, chapters 1-9 on ISADG written, 10-12 on ISAAR_CPF as scoped stubs; builds with `-W`). Found: two blocking core places and one closed, section 7 | with this row |
| D9 part 2 | bulk actions recognised by their kind, the YAML tag without `!`, not by the file name: one table of the kinds in `BulkActionConfigManager` registers the YAML tags and answers `BulkActionConfig.getType()` (only the nine top-level kinds - the parts of `!MultiAction` are not actions); REST API (TypeSpec): `BulkAction.type` (`BulkActionType`), `GET /action/fund-version/{id}`, `POST /action/fund-version/{id}/persistent-sort` (the server finds the sort of the rule set, 404 without one), the legacy `/api/action/queue/persistentSort` and `PersistentSortConfigVO` removed; one publisher of `BULK_ACTION_STATE_CHANGE` with `actionType`; the multi-fund dialog leaves out the sort by its kind (it kept ISADG's). Client: `components/arr/bulkActions.ts` (typed helpers), `PersistentSortForm.tsx` and `PersistentSortDialog.tsx` on react-final-form and `Api.funds`, `actions/arr/bulkActionStateChanged.tsx` (failure reported - the server sends `ERROR`, not `FAILED`; the tree of the fund of the run refreshed, not of the shown fund); `FundTreeMain` without `UNSAFE_componentWillReceiveProps`; the sort form restores direction and specification of a run. Tests: `BulkActionKindsTest`, `BulkActionServiceMultiFundTest` (sort by kind), `BulkActionControllerTest` (kinds, 404), `IsadgPackageTest`, `bulkActionStateChanged.test.ts`; guide 02 "rul_package_actions.xml"; release notes | with this row |

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
- *Messages (A1 rules 1-2, user 2026-10-09):* sources live in code (DRL `addError("KEY", "text")`,
  Java `CoreMessage`, react-intl `defaultMessage`); server-side translations always in the package
  XML format - in the package for a package's texts, in `elza-core/src/main/resources/translations/`
  for the core's - and client-side in `lang/translated/en.json`. A text is a core text only when
  neither the client (it knows the code or type) nor a package (rule-set logic) can own it; server
  artifacts (CSV headers, 401 bodies) and rule-evaluated reasons (validation, AI proposal blocked
  reasons) are core. Validation results are stored language-neutral and rendered on the way out;
  nothing is extracted from DRL or Java, a changed meaning changes the key, translations of messages
  defined in rules are not hash-checked. Rejected: a properties bundle (a third mechanism), a separate
  core-texts package (must be distributed, `elza.packages.enabled` can skip it), client-side
  rendering of package messages (the client catalog cannot hold customer texts; the server renders
  for the AI context, exception texts and outputs anyway), ICU4J for now (`ChoiceFormat` covers cs/en
  plurals; `ValidationMessage.format` is the one place to switch). Translations are loaded once into
  `PackageTranslations` with the static data - no per-message DB access. `RevisionFindingsBlockMapper`
  stays Czech by its own Javadoc (the findings' texts arrive from the AI in the run's language).
  *Language chain (user, 2026-10-09):* the UI language → the same without region → the language of
  the installation (`elza.locale`) → the text as its package wrote it, which counts as the text in
  the package's own language at that language's place in the chain (an English reader of ISAAR_CPF
  gets the rule's English before the Czech of a Czech installation); no language on funds or
  scopes. An international package ships every language ELZA ships (ISAAR_CPF: messages in cs and
  en; the Czech names of its own definitions are content work for when a Czech archive adopts it,
  `translations/cs.xml`, ~160 strings), a national package its own language only.

## 3. Phases and order

| Phase | Content | State |
|---|---|---|
| **2c. Entity description frameworks** | ISAAR_CPF package done (section 4); 2c.6 EAC-CPF export and R2 reference scope on demand | done for 2b |
| **A1. Texts built in core** | validation messages, request-time core texts, texts the client owns (section 7) | **done** (rules 1-2); rule 3 (texts a package writes) with 2b |
| **2b. First internationalized version** | `rules-en-isadg` referring to the ISAAR_CPF classes; core neutrality; texts written by packages in the package language | step 1 (walking skeleton) and D9 part 2 done 2026-10-12; next: the UI pass and the pinned search filters per rule set (section 7) |
| **2a.3 Remaining read sites** | tree titles, specification categories, further entity kinds, AI proposal rows | open, independent |
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
6. **Shipped with every installation**, imported where `elza.packages.enabled` lists it (or at the
   first start of an empty database without the key; decided 2026-10-09, see section 9); a scope
   uses it when its rule set is chosen.
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
Given (user, 2026-10-09): the CAM exchange is bound to the CAM rules - only an entity described by
them can be exported; the standard fixes the part types (the schema's `PartTypeXml`), so a part of
another type ends the export with an exception, which is correct and stays. For reference: an extra
item type is dropped by `ItemTypeMap.groovy`, an extra specification of a CAM item type drops the
whole part from the export.

**Remaining obstacles.**
- None in the code for extending entities by own rules (search fields from `index-search`, base vocabulary documented). ISAAR_CPF ships no `index-search`: alone it is searched by names (`index`) only, without the name-item weights of CZ_BASE.

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

### A1 — texts built in core

Rules 1 and 2 are done (section 2, rows "A1 rules 1-2" and "A1 UI-owned, E3"; decisions in section
2; implementation guide chapter 02 "Messages"). What stays:

- **Rule 3 (proposal): texts a package writes follow the package** (groups D1, D2 of the inventory
  of 2026-10-08). The text form of unit dates inside entity index names and outputs, the word for an
  undefined value in titles (`ArrangementService.UNDEFINED`; in messages it is already the term
  `CORE/UNDEFINED_VALUE`), default names and values written by imports (`DaoCoreServiceWsImpl:543`
  "Importováno - <date>", `ArrangementService:2239` "Šablona") are rendered in the language of the
  rules package whose script or template writes them (`rul_package.language_id`: CAM and ZP2015
  Czech, ISAAR_CPF and ISADG English) - never the UI language (background jobs write them without a
  user) and never a language of the fund (L2 step 4). What a user sees at request time follows the
  user; the DAO import value and the template name come from the rules or the client instead of
  core. Done together with 2b's print templates and index scripts.
- **Out of scope, Czech modules by decision:** the DA/AIP integration (`service/da/*`, ≈ 80 texts;
  the Czech national digital archive interface, like CAM), data type names (`db.elza-init.xml`, not
  shown to users), core resources (`createDid.groovy` asserts, the DA export samples).

### L2 — unit-date text per language

Steps 1-3 done 2026-10-08 (section 2, row "L2 (1-3)"): the stored form is language-neutral, the
text is rendered per language from one lexicon shared by server and client. Open: step 4, the
language rule for the remaining places.

*Decisions (user, 2026-10-08).*
1. **Parsing is language-independent, rendering is per language.** One tolerant parser on each side
   accepts every language's markers (BC, century, month names, date patterns) and the negative year
   whatever the UI language; rendering happens only on the server, in the language asked for.
2. **One grammar, not one per language.** The language-dependent tokens form a lexicon shared by
   both sides. Adding a language adds tokens, not a parser.
3. **Unified by a shared lexicon and a shared corpus, not by shared code.** Rejected: ANTLR with Java
   and TypeScript targets (a rewrite of the server parser and build plugins on both sides for ~15
   rules); server-only validation through an endpoint (grids and filters would wait on the network
   for every keystroke).

*Where things are.* Lexicon `elza-core/src/main/resources/unitdate/lexicon.json` - per language tag
the BC, century, month and date forms and the delimiters; the client imports it at build time by a
relative path (code versioned with both sides, not installation data). Corpus `unitdate/cases.json`
(93 cases) run by `UnitDateCorpusTest` and `unitdate.test.ts` - the drift guard. Server
`UnitDateConverter.convertToString(unitdate, languageTag)`; the overload without a language renders
Czech for package scripts and core jobs until step 4. Client `components/shared/unitdate/parse.ts`
mirrors the server algorithm (the Peggy grammar is gone: PEG cannot apply the lexicon's regular
expressions at token boundaries); the format help takes its examples from `examples.ts` per UI
language. Request-time VOs (`ApItemUnitdateVO`, `ArrItemUnitdateVO`) render in the language of the
request; `ApItemUnitdateVO.equalsValue` compares the stored form. English forms (confirmed
2026-10-08): `1968`, `500 BC`, `20th century`, `20th century BC`, `Aug 1968`, `21 Aug 1968`,
`21 Aug 1968 14:05`, `1968 – 1969`, `[1968]`, `1985/1990` - month names because the numeric English
forms collide with the delimiters (`1968-08`, `8/1968`); input also accepts the ISO full date
`1968-08-21`, every Czech form and the delimiters "-", " - ", " – ". Behaviour changes beyond
English: "0. st." is refused, the tokens of an interval are trimmed, markers are case-insensitive.

*Step 4 (rule agreed 2026-10-08, user): no language on funds.* The archivist knows the language of
the description; the application needs no setting of its own. Remaining places: the entity search
filter (`ApStateSpecification`, a text "contains" of the Czech form against the index), the fulltext
and `ap_index` texts, tree titles, `GroovyUnitdateFormatter`, `UnitDatePrintConvertor` (month names
are in the lexicon already), and the legacy client renderer in `party/DatationField.jsx` (search
form and bulk modifications). Three cases cover them:
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
  whatever language the user typed it in. The entity date filter compares normalized ranges.
  `DateRangeAction` and `ImportFromFund` store structured values and need no text.

Dropped by the rule: `arr_fund.language_id`, the per-fund "description language", and the A1 open
question on English funds. Step 4 is independent of 2b except the English print templates, which 2b
brings.

**2a.3 Remaining read sites.** Tree titles (specification names in node titles,
`DescriptionItemServiceInternal` `:164`); specification categories (kind `ITEM_SPEC_CATEGORY`); kinds
`EXPORT_FILTER`, `OUTPUT_FILTER`, `STRUCTURED_TYPE_EXTENSION`, `EXTERNAL_ID_TYPE`, `INSTITUTION_TYPE`,
`ACTION`; the item type names of AI proposal rows (`AiProposalService.displayName`).

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

### 2b — `rules-en-isadg`: design and the next step

Reviewed against ZP2015 351, SIMPLE-DEV 43 and ISAAR_CPF 2 on 2026-10-12; step 1, the walking
skeleton, built the same day (section 2, row "2b step 1"; "Step 1 done" below). Decisions D1-D8
are taken (D4 built as proposed) and D9 is closed (part 1 by removal, part 2 done the same day).
The mapping and the package shape below are what the package is; the core places and the next
steps are the open work.

**What ZP2015 teaches.** ZP chapter 4, the common elements (group `06_ZP2015_CH4`), is ISAD(G)
area 3 almost one to one. The bulk of ZP2015 - 138 item types, 409 specifications, the 7,191-line
availability matrix *unit type × element*, the EJ computation, the structured access restrictions,
the DA scripts, 12 templates - is Czech practice or ZP chapter 5 and is not carried over; an addon
brings such detail later (Phase 3 mechanisms). ISADG is therefore a *level × area* matrix: about
43 item types, ~15 availability rules, ~10 validation rules, 6 new-level scenarios. SIMPLE-DEV is a
skeleton of the files, not of the content (Czech, CZ_BASE-dependent). The package is generated from
tables (`rules-en-isadg/generator/gen_isadg.py`, as ISAAR_CPF is), so the filter rule, the type
groups, the grid, the pinned filters and `translations/cs.xml` come from one table and cannot
drift.

**Decisions (user, 2026-10-12).**
- *D1* ISADG depends on ISAAR_CPF (`item-aptypes` of a RECORD_REF must name declared classes,
  `ItemTypeUpdater:1439`); `elza.packages.enabled` lists ISADG, the import pulls ISAAR_CPF in.
- *D2* the six ISAD(G) levels. `NewLevel.drl` offers only the allowed children (fonds at the root;
  subfonds under fonds; series under fonds, subfonds, series; subseries under series, subseries;
  file under series, subseries, file; item under series, subseries, file, item); `Validation.drl`
  checks what scenarios cannot guard - a level changed later, an imported fund: the root is a
  fonds, a child's level ranks below its parent's. ZP2015 guards its hierarchy in the scenarios
  only; the reference-code generator and the EAD template assume it.
- *D3* a structured storage unit from the start: `ISADG_CONTAINER` after SIMPLE-DEV's `SRD_PACKET`
  (kind, prefix, number, postfix, location; ~200 lines of DRL and Groovy), the generator action
  (`GENEROVANI_UKL_JEDNOTEK.yaml` as the model), `ISADG_POL_CONTAINER`, the validation "every leaf
  branch has a container".
- *D5* access as text, a status (open, restricted, closed) and an opening date; structured
  restrictions later as an addon, they follow local legislation.
- *D6* one `ISADG_ACCESS_POINT` (RECORD_REF to the five ISAAR classes, the role as specification:
  subject, place, person, family, corporate body, author, other) for the index terms of EAD
  `controlaccess`; the creator stays its own item `ISADG_CREATOR`.
- *D7* the ISO 639-2 `LNG_*` specifications shared by code with ISAAR_CPF and assigned to
  `ISADG_LANGUAGE` and `ISADG_FA_LANGUAGE` (CZ_BASE assigns them to four item types the same
  way); script as text in version 1.
- *D8* one output type `ISADG_FINDING_AID` with EAD3 (FreeMarker, `validation-schema`), PDF
  (Jasper) and XML (engine `DE_XML`).
- *D9* closed: part 1 by removal (the DID of the DAO web services carries the identifier only),
  part 2 done (section 2): bulk actions recognised by their kind, the tree menu gated by the
  fund's actions.
- *D4, built as proposed in step 1 - extent and medium.* In the standard the element is one text
  ("128 boxes (12.5 linear metres)", "2 maps; 7 photographs"). ZP2015 types the material
  (`ZP2015_UNIT_TYPE`, 53 kinds) with a count, sums the counts up the tree (`!UnitCount`) and prints
  the totals in the finding aid; the size of one item has its own items (`ZP2015_AMOUNT`: a DECIMAL
  whose specification is the unit). Proposal keeping the standard's text and ZP2015's precision:
  `ISADG_EXTENT` (TEXT, the statement as it prints); `ISADG_EXTENT_QUANTITY` (DECIMAL with the unit
  as specification, repeatable: linear metres, boxes, folders, volumes, items, sheets, pages,
  minutes, bytes - later summed per unit by a `!UnitCount` configuration for the finding aid's
  statistics); `ISADG_MATERIAL_TYPE` (ENUM, repeatable; the "more specifically defined types": ~15
  base kinds - textual records, registers and official books, manuscripts, printed matter, maps and
  plans, technical drawings, graphic material, photographs, sound recordings, moving images,
  electronic records, seals, coins and medals, three-dimensional objects, other - extensible by an
  addon, which could carry the 53 kinds of ZP2015); detail items offered by the material type
  (`ISADG_DIMENSIONS`, `ISADG_SCALE` for maps and drawings, `ISADG_DURATION` for sound and moving
  images; more by addons, ZP chapter 5 being the Czech example). Sample: the fonds - extent "128
  boxes (12.5 linear metres)", quantity 12.5 [linear metres] and 128 [boxes]; a file - level file,
  title "Correspondence with the town council", dates 1921-1924, material type textual records,
  extent "1 folder (45 leaves)", quantity 45 [sheets], container box 12; a map in it - level item,
  material type maps and plans, scale "1:2880", dimensions "62 × 48 cm", quantity 1 [items]. Step 1
  builds this proposal; the generator makes a change cheap while the package is unreleased.

**Mapping.** Codes `ISADG_` + the ISAD(G) term; names and messages English.

| ISAD(G) | ZP2015 | ISADG (data type) |
|---|---|---|
| 3.1.1 Reference code | `ZP2015_UNIT_ID` (UNITID) + `!UnitIdGenerator` | `ISADG_REFERENCE_CODE` (UNITID), generator; country and repository codes from the institution in outputs |
| - other identifiers | `ZP2015_OTHER_ID` (24 specs), `ZP2015_SERIAL_NUMBER`, `ZP2015_INV_CISLO` | `ISADG_OTHER_ID` (STRING; specs former reference code, accession number, other) |
| 3.1.2 Title | `ZP2015_NAME`, `ZP2015_FORMAL_TITLE`, `*_PUBLIC` | `ISADG_TITLE` (STRING); formal and supplied titles not distinguished |
| 3.1.3 Dates | `ZP2015_UNIT_DATE`, `_BULK`, `_TEXT`, `_PRE`, `_POST`, `ZP2015_DATE_OTHER` (25 specs) | `ISADG_DATE` (UNITDATE, creation), `ISADG_DATE_BULK` (UNITDATE), `ISADG_DATE_TEXT` (STRING) |
| 3.1.4 Level | `ZP2015_LEVEL_TYPE` (6: soubor, dílčí list, série, složka, jednotlivost, část) + `ZP2015_FOLDER_TYPE` | `ISADG_LEVEL` (ENUM: `FONDS`, `SUBFONDS`, `SERIES`, `SUBSERIES`, `FILE`, `ITEM`) - D2 |
| 3.1.5 Extent and medium | `ZP2015_UNIT_TYPE` (53), `_UNIT_COUNT`, `_EXTRA_UNITS`, `_UNIT_COUNT_ITEM`, EJ computation, `ZP2015_COLL_EXTENT_LENGTH`, `ZP2015_SIZE_*`, `ZP2015_AMOUNT` | `ISADG_EXTENT` (TEXT), `ISADG_EXTENT_QUANTITY` (DECIMAL, unit as specification, repeatable), `ISADG_MATERIAL_TYPE` (ENUM, extensible), detail items `ISADG_DIMENSIONS`, `ISADG_SCALE`, `ISADG_DURATION` - D4 |
| 3.2.1 Creator | `ZP2015_ORIGINATOR` (CAM classes), `ZP2015_ORIGINATOR_SIMPLE` | `ISADG_CREATOR` (RECORD_REF: `PERSON_INDIVIDUAL`, `FAMILY`, `PARTY_GROUP`) - D1 |
| 3.2.2 Administrative / biographical history | - (CAM entity; `ZP2015_INTRO` aggregates it) | `ISADG_BIOGHIST` (TEXT); later aggregation from the ISAAR entity (`!AccessPointAggregation`) |
| 3.2.3 Archival history | `ZP2015_UNIT_HIST` | `ISADG_CUSTODHIST` (TEXT) |
| 3.2.4 Immediate source of acquisition | `ZP2015_UNIT_SOURCE` | `ISADG_ACQINFO` (TEXT) |
| 3.3.1 Scope and content | `ZP2015_CONTENT`, `ZP2015_UNIT_CONTENT` | `ISADG_SCOPE_CONTENT` (TEXT) |
| 3.3.2 Appraisal, destruction, scheduling | - | `ISADG_APPRAISAL` (TEXT) |
| 3.3.3 Accruals | `ZP2015_FUTURE_UNITS` | `ISADG_ACCRUALS` (TEXT) |
| 3.3.4 System of arrangement | `ZP2015_UNIT_ARR` | `ISADG_ARRANGEMENT` (TEXT) |
| 3.4.1 Conditions governing access | `ZP2015_UNIT_ACCESS` + `ZP2015_RESTRICTION_ACCESS_*`, publication events, two structured types, export and output filter `ZP_ACCESS_RESTRICT` | `ISADG_ACCESS_CONDITIONS` (TEXT), `ISADG_ACCESS_STATUS` (ENUM: open, restricted, closed), `ISADG_ACCESS_OPEN_DATE` (DATE) - D5 |
| 3.4.2 Conditions governing reproduction | `ZP2015_UNIT_COPY_RIGHTS`, `ZP2015_UNIT_INFO_RIGHTS` | `ISADG_REPRODUCTION_CONDITIONS` (TEXT) |
| 3.4.3 Language / scripts | `ZP2015_LANGUAGE`, `ZP2015_MAJOR_LANG` (CZ_BASE, `LNG_*`), `ZP2015_WRITING` | `ISADG_LANGUAGE` (ENUM, the 486 `LNG_*` of ISAAR_CPF shared by code), `ISADG_SCRIPT` (STRING; ISO 15924 later) - D7 |
| 3.4.4 Physical characteristics, technical requirements | `ZP2015_UNIT_CURRENT_STATUS`, `ZP2015_UNIT_DAMAGE_TYPE` | `ISADG_PHYSTECH` (TEXT) |
| 3.4.5 Finding aids | - (the output itself) | `ISADG_FINDING_AIDS` (TEXT) |
| 3.5.1 Existence and location of originals | `ZP2015_COPY_SOURCE` | `ISADG_ORIGINALS_LOC` (TEXT) |
| 3.5.2 Existence and location of copies | `ZP2015_EXISTING_COPY`, `ZP2015_DAO_ID` | `ISADG_COPIES_LOC` (TEXT) |
| 3.5.3 Related units of description | `ZP2015_RELATED_UNITS`, `_RELATED_UNITS_LINK`, `ZP2015_ITEM_LINK` | `ISADG_RELATED_MATERIAL` (TEXT), `ISADG_RELATED_LINK` (URI_REF) |
| 3.5.4 Publication note | `ZP2015_EDITION`, `ZP2015_SOURCES` | `ISADG_BIBLIOGRAPHY` (TEXT) |
| 3.6.1 Note | `ZP2015_NOTE`, `ZP2015_INTERNAL_NOTE` | `ISADG_NOTE` (TEXT), `ISADG_INTERNAL_NOTE` (TEXT) |
| 3.7.1 Archivist's note | `ZP2015_ARRANGER` (RECORD_REF), `_ARRANGEMENT_INFO`, `_ARRANGER_TEXT` | `ISADG_PROCESS_INFO` (TEXT), `ISADG_ARCHIVIST` (RECORD_REF `PERSON_INDIVIDUAL`) |
| 3.7.2 Rules or conventions | `ZP2015_ARRANGE_RULES` | `ISADG_RULES` (TEXT) |
| 3.7.3 Date(s) of descriptions | `ZP2015_DESCRIPTION_DATE` (TEXT) | `ISADG_DESCRIPTION_DATE` (TEXT) |
| EAD container, physloc | `ZP2015_STORAGE_ID` (STRUCTURED `ZP2015_PACKET`: type, prefix, number, postfix, metrage, 5 location items), `ZP2015_ITEM_ORDER` | `ISADG_CONTAINER` (STRUCTURED `ISADG_CONTAINER`: kind, prefix, number, postfix, location) - D3 |
| EAD dao | `ZP2015_DAO_LINK`, `ZP2015_DAO_ID`, `ZP2015_AIP_ID` | `ISADG_DAO_LINK` (URI_REF) |
| EAD controlaccess | `ZP2015_ENTITY_ROLE` (83 specs), `_AP_REF`, `_GEO_REF`, `_LEGALACT_REF` | `ISADG_ACCESS_POINT` (RECORD_REF to the five ISAAR classes; specs subject, place, person, family, corporate body, author, other); never the creator, which is `ISADG_CREATOR` - D6 |
| finding aid (output items) | `ZP2015_FINDING_AID_ID`, `_OUTPUT_TYPE`, `_FINDING_AID_DATE`, `_UNIT_COUNT_SUM`, `_UNIT_COUNT_TABLE`, `_UNITS_AMOUNT`, `_RELEASE_DATE_PLACE`, `_APPROVED_BY`, `_EDITOR`, `_DATE_RANGE`, `_ATTACHMENT`, `_ORIGINATOR_SIMPLE`, `ZP2015_ARCHDESC_LANG` | `ISADG_FA_TITLE` (STRING), `ISADG_FA_KIND` (ENUM: inventory, catalogue, guide, list), `ISADG_FA_AUTHOR` (TEXT), `ISADG_FA_DATE` (DATE), `ISADG_FA_LANGUAGE` (ENUM `LNG_*`), `ISADG_FA_NOTE` (TEXT), `ISADG_ATTACHMENT` (FILE_REF) - D8 |
| not carried over | `ZP2015_NAD`, `_ZNACKA_FONDU`, dílčí list (`LEVEL_SECTION`, `DilciListy.drl`), `_ARRANGEMENT_TYPE`, `_UNIT_SUBTYPE`, `_UNIT_EXT_TYPE`, `_AGENDA_TYPE`, `_RECORD_TYPE`, chapter-5 items (~45), `_PUBLISHED`, `_INVALID_RECORD`, `_CONNECTED_RECORD`, `_NOT_FOUND`, `_ALTERNATIVE_DESCRIPTION_INLINE`, `_ITEM_TITLE_REF`, `_AWARD_*`, `_ENTITY_ROLE_TEXT`, all 8 extensions, older-aid output, labels, migrations, test-data generator | Czech practice or ZP chapter 5; an addon on ISADG can add them later |

Open without a recommendation: the ISADG name of the Czech "formal title" (ZP 4.2.4), dropped in
the table; whether `ISADG_ARCHIVIST` stays (3.7.1 is a text in the standard).

**Package shape.** Module `rules-en-isadg` (package and rule set `ISADG`, `<language>en`,
dependency `ISAAR_CPF` min 2); no arrangement extensions, no `DA_IMPORT`/`DA_MATCH` (the Czech
national DA), no labels, no older-aid output in version 1. Rules: `ItemTypeFilter.drl`,
`AvailableItems.drl` (ZP2015's salience-100 preamble, everything possible at every level, required
by level, the material-type gating of D4), `Validation.drl` (the six essential elements of ISAD(G)
at the fonds - reference code, title, dates, level, extent, creator; level and title everywhere; a
date inside the parent's range; the hierarchy of D2; the container of D3), `NewLevel.drl`,
`ChangeImpact.drl` (ZP2015's with the codes replaced), `Output_FindingAid.drl`, a citation Groovy;
messages keyed, Czech in `translations/cs.xml`. Policy types `ISADG_POL_BASIC`,
`ISADG_POL_REFERENCE_CODE`, `ISADG_POL_CONTAINER`. Actions: `!FundValidation`, `!UnitIdGenerator`
(`levelTypeCode: ISADG_LEVEL`, `extraDelimiterAfter: ISADG_LEVEL_SERIES`), persistent sort, the
container generator. Templates: EAD3 from the skeleton of `INVENTAR_EAD/index.ftl` (control,
archdesc, the `dsc` recursion, the level mapping at lines 439-464 - ISADG levels are EAD level
values one to one), PDF from `SRD_INVENTORY_JASPER` (236 lines), XML free. UI settings of the rule
set: FUND_VIEW (tree `ISADG_REFERENCE_CODE ISADG_TITLE ISADG_DATE`, icons per level), TYPE_GROUPS by
the seven areas plus Finding aid, Access points, Container, GRID_VIEW, FUND_ISSUES (ZP2015's block),
STRUCTURE_TYPES, OUTPUT_DEFAULTS; of the package: DAO_LEVEL_IMPORT (its `<scenario>` is matched by
the scenario *name*), pinned search filters, favourite languages. Wiring: root `pom.xml`,
`elza-core/pom.xml` test resource, `distrib/distribution/pom.xml` and `distribution.xml`,
`elza.yaml`, admin guide "Loading the packages".

**Core and client places that assume ZP2015** (grep 2026-10-12 and the walking skeleton; nothing
keys on level or arrangement types):
- Done (D9 part 2, section 2): the persistent sort and the EJ menu item follow the actions of the
  fund's rule set; the action is recognised by its kind, every rule set names its own file.
- *Blocking for the search of ISADG funds:* the pinned search filters (`node-search-settings`,
  `SEARCH_NODE_FILTERS`) are one global setting that names the item types of one rule set; a second
  package is refused (`OTHER_PACKAGE`, found by the test), so ISADG ships none and an ISADG fund
  shows ZP2015's filters where ZP2015 is installed. Fix: the setting per rule set (entity type
  RULE, read by the fund's rule set) or layered. About half a day.
- Closed by the skeleton: institution types are declared per package (`par_institution_type.xml`)
  and ISAAR_CPF has none, so an installation without CZ_BASE could create no institution - ISADG
  declares archive, library, museum, other; the test creates its institution from an ISAAR_CPF
  corporate body (`institution-import.xml` of the tests carries a CAM entity).
- Tolerable: `ArrangementService:201` fixed links by `ZP2015_ITEM_LINK` (no type, no links; later by
  the URI schema `elza-node`); `ReportServiceQuery:355-379` finding-aid columns of the outputs
  report (left joins, empty for ISADG); `DaImportPlanner.ruleSetLabel` Czech text → `CoreMessage`;
  `DataGridImportDialog.tsx:102` sample CSV with ZP2015 codes → generic.
- Accepted limits of version 1: NEW_LEVEL scenario names are texts a rule writes and the identifier
  `DAO_LEVEL_IMPORT` matches - not translatable (A1 rule 3), a Czech user sees English scenario
  names; Czech names of the 486 languages come from CZ_BASE when installed, otherwise the generator
  copies them into `cs.xml`.

**Step 1 done (2026-10-12), the walking skeleton** - section 2, row "2b step 1". What it proved:
the package imports strictly, alone with ISAAR_CPF and next to CZ_BASE and ZP2015; the fonds is
created as a fonds by the only root scenario; each level offers the scenarios of the levels allowed
below it; the elements are offered by level (the essential elements required at the fonds, the
title below, the reference code and the dates recommended, the container only below the fonds,
the scale only for maps and drawings, the items of the finding aid and of the container never on
a unit); the 486 languages are offered and CAM's own codes are not; the validation reports the
root, the order of levels and the missing container with keyed messages rendered in English and in
Czech. D4 is built as proposed; a change is a change of the generator's tables while the package
is unreleased. Not done by the skeleton: the pass through the user interface of a dev
installation (fund creation, the tree, the forms, the grid, the structured objects panel, the
entity pickers) - the user does it; what breaks goes into the list above.

**Next steps.**
1. **The UI pass of the skeleton** (user, dev installation with ISAAR_CPF and ISADG enabled): an
   ISADG fund with a series, a file and an item; the elements of the seven areas; a container; a
   creator and an access point from an ISAAR_CPF scope; the validation panel; the output. Findings
   into the list above.
2. **The pinned search filters per rule set, half a day** (the remaining blocking core place); then
   the container generator action (`!ItemGenerator`, after `GENEROVANI_UKL_JEDNOTEK.yaml`) and the
   citation script.
3. **L2 step 4 and the EAD3 template, 3.5 days:** English unit dates in index names and outputs
   are a prerequisite of the template; the template is validated against the schema in the test.
4. **PDF, the remaining core places, the guides, 2.5 days:** the Jasper finding aid; implementation
   guide chapter 05 "International archival description (ISADG)"; user guide chapter 9 (outputs)
   and part II (ISAAR_CPF, chapters 10-12, now scoped stubs); release notes.
Exit: an English user creates an ISADG fund, fills the essential elements, links an ISAAR creator,
validates, generates the EAD and the PDF, and sees no Czech text; the same installation keeps a
ZP2015 fund working.

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
  by code; packages existing only in the database are not re-applied. The ZIPs considered are
  chosen by `AutoImportSelection` in `IndexConfigReaderImpl` (so the index schema matches): the
  installed packages, those listed in `elza.packages.enabled` and their dependencies; the other
  ZIPs are skipped, so a package deleted in the administration stays deleted. An empty database
  without the key imports every ZIP. The package administration lists the ZIPs not loaded
  (`AvailablePackageService`, `/api/v1/packages/available`); a mark is a `rul_package` row with
  `PENDING_VERSION` (0), which the selection counts as installed. `RestartService` exits with
  `elza.restart.exitCode` (default 3) for the service manager; no in-process restart (static state).
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
  `core/data/ValidationMessage.java`, `core/data/CoreMessage.java`, `core/data/CoreTranslations.java`
  (+ `resources/translations/{cs,en}.xml`), `service/RuleService.java`, `service/GroovyService.java`,
  `service/AccessPointService.java`, `controller/factory/ApFactory.java`,
  `domain/bridge/IndexConfigReaderImpl.java`.
- Schema: `elza-core/src/main/resources/db/changelog/db.elza-3-part-03.xml`.
- Packages: `package-cz-base/`, `package-isaar-cpf/` (generated by `generator/gen_isaar.py`),
  `rules-en-isadg/` (generated by `generator/gen_isadg.py`; Python 3 needed), `rules-cz-zp2015/`,
  `rules-simple-dev/`.
- Guides: `docs/admin-guide`, `docs/implementation-guide`, `docs/user-guide` (ISADG and ISAAR_CPF for
  archivists; Sphinx, `-W`).
- Client: `components/registry/ApDetailPageWrapper.tsx`, `ApTypePicker.tsx`,
  `modal/CreateAccessPointModal.tsx`, `components/shared/lang/language.ts`, `LanguagePicker.tsx`.
- Tests: `packageimport/EntityRulesTest.java` (`entity-rules-test`), `StandaloneFrameworkTest.java`
  (`entity-standalone-test`), `IsaarCpfPackageTest.java`,
  `IsadgPackageTest.java` (`rules-en-isadg`), `rules/addon/AddonPackageTest.java`
  (`rules-addon-test`), `PackageTranslationTest` (`translation-addon-test`),
  `PackageXmlFilesTest` (every package file read strictly), `core/data/PackageTextsTest`,
  `core/data/CoreMessagesTest` (every core message in every shipped language),
  `search/IndexConfigReaderTest.java`, `other/HelperTestService.java` (`deleteAllPackages`).
