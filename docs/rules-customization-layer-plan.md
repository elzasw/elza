# Rules-package customization layer and first internationalized version — plan

Status as of 2026-10-06. This is the design record for extending rules packages without forks
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
| **2. First internationalized version** | `rules-en-isadg` package; core neutrality fixes; package-provided name translations; English translation package for CZ_BASE; client sends its language | changeset T | small (language header, validation message) | an English user creates an ISAD(G) fund, describes and validates it, and sees no Czech text |
| **3. Pilots** | Settings composition; DPP and CT converted to file addons; their overlays retired | none | none | both pilots run on the dev server against stock ZP2015 |
| **4. Customizations in the UI** | `kind`, archive, `CustomizationPackageService`, OpenAPI `customization`, admin page: customizations, specifications with position and generated rule, rule editor with compile check | changeset B | yes | an admin adds "osobní číslo" after an existing identifier type, and it appears in the node form at that position without restart |
| **5. Declarative primitives and overrides** | Templates that repeat become data (`specs-extensible` guard); item-type ordering anchors and layout pass; structure extensions across packages; removable new-level scenarios; code aliases; spec retirement; UK pilot | changeset C | partial | UK overlay retired |
| **6. Convergence** | Rule-set inheritance; renames of semantically generic ZP2015 codes towards the ISAD(G) catalogue via aliases | later | — | — |

Phases 1 and 2 are independent and can run in parallel. If only one can go first and the English
version is the priority, Phase 2 can start immediately; it uses nothing from Phase 1.

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

### 5.2 Changeset T — translations of package-provided names (Phase 2)

| Change | Definition | Meaning |
|---|---|---|
| new `rul_translation` | `translation_id int PK`, `package_id int NOT NULL FK rul_package` (contributor), `entity_type nvarchar(50) NOT NULL`, `entity_code nvarchar(50) NOT NULL`, `lang nvarchar(10) NOT NULL`, `name nvarchar(250) NOT NULL`, `shortcut nvarchar(50) NULL`, `description ${type.text} NULL`, `ux_rul_translation (entity_type, entity_code, lang, package_id)` | A localized name of an entity defined by any package. |

- `entity_type` values: `ITEM_TYPE`, `ITEM_SPEC`, `AP_TYPE`, `PART_TYPE`, `RULE_SET`,
  `STRUCTURED_TYPE`, `POLICY_TYPE`, `OUTPUT_TYPE`, `TEMPLATE`, `ARRANGEMENT_EXTENSION`, `ISSUE_TYPE`,
  `ISSUE_STATE`. More can be added without a schema change.
- **Code-based, not id-based**, so translations survive re-imports of the translated package and a
  translation package can be installed or upgraded independently.
- **Resolution**: for the requested language, the translation from the package latest in dependency
  order wins; with none, the original name is used.
- **Lifecycle**: rows belong to the contributing package (`deletePackage` removes them, export
  writes them back).

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

## 7. Phase 2 — first internationalized version

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
  the translation package (7.3). This avoids duplicating the language list; the code is never shown.
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
- Missing-item validation message: `DataValidationResults` writes a Czech sentence
  ("Prvek X musí být vyplněn."). Store no text for the generic missing-item case and let the client
  render it from the item type name in its language. Messages written by DRL stay in the rule
  author's language (English in ISADG).
- `ArrangementService` (`ZP2015_ITEM_LINK`) and `ReportServiceQuery` (four ZP2015 output codes) are
  null-safe for other rule sets and stay as they are.

### 7.3 Translations of package-provided names
- Changeset T (5.2); entity `RulTranslation`, repository, static data map
  `(entityType, code, lang) → translation`.
- Package format: `translations.xml` at the package root,
  `<translations lang="en"><translation entity="AP_TYPE" code="PERSON"><name>Person</name></translation>…</translations>`;
  imported by the package that ships it, deleted with it, exported back.
- Read side: one resolver (`PackageTextResolver`) used by the VO factories and mappers that return
  names of item types, specifications, ap types, part types, rule sets, policy types, structured
  types, issue types and states (`ClientFactoryVO`, `RulesMapper`, `ApFactory` and the rule
  endpoints). The language comes from the request (`Accept-Language`, Spring
  `LocaleContextHolder`); the server-wide `elza.locale` stays the default.
- Client: every API call sends `Accept-Language` with the UI language from user settings; switching
  language marks the rule refTables dirty.
- Translation package `CZ_BASE_EN` (`elza/package-cz-base-en`), depending on CZ_BASE: English names
  for CZ_BASE ap types, part types, the entity item types and specifications used in the entity
  forms, the 166 language specifications, issue types and states. It applies the layer principle:
  a separate package contributes to entities it does not own.

### 7.4 Tests and exit
- `IsadgPackageTest` (pattern of `Zp2015EjCountTest`: import CZ_BASE and ISADG once, unload after):
  filter contents, required items at fonds level, new-level scenarios, validation messages.
- Translation tests: English name returned for `Accept-Language: en`, Czech otherwise; translation
  package survives a re-import of CZ_BASE; deleting it restores the originals.
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
- Conventions: Liquibase changesets only in `db.elza-3-part-03.xml`; REST OpenAPI-first
  (`rest/elza-openapi.yml`, tag → generated `<Tag>Api`, `@RestController @RequestMapping("/api/v1")`,
  `@AuthMethod(permission = Permission.ADMIN)`).

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
- Packages: `rules-simple-dev/` (skeleton), `rules-cz-zp2015/`, `package-cz-base/`, new
  `rules-en-isadg/` and `package-cz-base-en/`; `elza/pom.xml`,
  `distrib/distribution/src/assembly/distribution.xml`.
- Client: `elza-react/src/websocketActions.jsx`, `components/arr/FundTreeMain.jsx`, API client setup
  (language header), `components/arr/item-form/desc-items/ErrorDisplay.tsx`.
- Tests: `elza-core/src/test/java/cz/tacr/elza/rules/addon/AddonPackageTest.java`,
  `elza-core/src/test/resources/rules-addon-test/`, `rules/zp2015/Zp2015EjCountTest.java` (pattern).
