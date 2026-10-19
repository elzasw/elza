# Rules-package customization layer and the internationalized version

Design record for extending rules packages without forks (addon packages, customizations edited in
the admin UI), for entity description frameworks beside CAM, and for the internationalized version
of ELZA (an English rule set based on ISAD(G), `rules-en-isadg`, with entities described by
ISAAR(CPF), `package-isaar-cpf`). Implemented mechanisms are described for implementers in the
implementation guide (`docs/implementation-guide`) and for administrators in the release notes
(`docs/admin-guide/source/release-notes`); this document keeps the goals, the decisions with their
rationale, the design of mechanisms still to be built and the open work. Research behind the entity
framework: [entity-framework-research.md](entity-framework-research.md) and
[isaar-cpf-cam-comparison.md](isaar-cpf-cam-comparison.md).

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
  code (translations, declarations, members, layered settings). An installation fixes a conflict of
  unrelated packages with a small local package depending on both.
- **Revalidation is the author's decision** through `compatibility-rul-package`.
- **No compile check at import.** A broken rule shows up where it runs; ELZA stays operational and a
  fixed package can be imported. Only the rule editor of Phase 4 compiles before saving.
- **Migrations are SQL changesets for PostgreSQL**; H2 is used only by tests and holds no data.

## 2. Decisions

- *Addon placement and filters:* an addon places its specifications by `view-after` and contributes
  item types to a foreign rule set by `ITEM_TYPE_FILTER` rules; the item-type filter of a rule set is
  its own DRL plus the filter rules of other packages.
- *Translations:* key `(entity_type, entity_code, field, language)`, rows owned by the contributing
  package (`translations/<lang>.xml` with `src-hash`); an outdated translation (source hash differs)
  is still used. Declared names are added to the translations in static data (no rows stored). The
  UI language travels in the cookie `elza-lang` and is read by `PackageTexts.requestLanguage()`.
- *Declarations:* the defining row (`ap_type`, `rul_part_type`, `rul_item_type`, `rul_item_spec`)
  holds the summary - owner and structural values from the winning declaration, the texts in the
  installation language. Item types and specifications differ: the package that created them stays
  the owner while it declares them (the owner decides the place in `view_order` / among the
  specifications), the other values come from the owner's declaration, and a declaration of another
  package takes no place and may state RECORD_REF classes (`item-aptypes`) only equal to the owner's;
  data type, use of specifications, structured type and table columns must agree. Specification
  assignments are the union of the packages' assignment declarations. Nothing reads the owner of a
  shared definition; the repositories offer no package-scoped finders. A definition is removed with
  its last declaration, refused while entities use it (`PART_TYPE_IN_USE`, `ITEM_TYPE_IN_USE`,
  `ITEM_SPEC_IN_USE`) or other packages refer to it (`FOREIGN_DEPENDENCY`: entity rules, members,
  part lists, child parts). Conflicting structure (class parent) refuses the import; values that do
  not affect stored data (part `repeatable`, `child_part`) come from the winning declaration.
- *Members and order:* a rule set without members offers everything. The class tree of a rule set
  shows its members only: a member's parent chain is walked through members, the nearest member
  ancestor is the parent, otherwise the member is a root; the global tree (search filters) is the
  union over the entity rule sets. A rule set that wants a visible grouping lists it as a
  non-assignable member. Display order: members of the owner of the rule set in file order, then
  contributing packages by dependency depth, package code, file order; a code listed twice keeps its
  first position. The part list is applied by the client (detail and copy dialog); an unlisted part
  type with existing parts is still shown. The `parts-order` UI setting is only the fallback for rule
  sets without a list. A class outside the members of the scope's rule set is refused
  (`AP_TYPE_NOT_IN_RULE_SET`).
- *Open part types:* the Drools model's `PartType` is a class with interned instances
  (`PartType.of(code)`), not an enum, so packages with own part types write rules against
  `Part.typeCode` while CAM's rules and the CAM sync stay unchanged.
- *Base vocabulary instead of part roles:* the part types `PT_NAME`, `PT_BODY`, `PT_CRE`, `PT_EXT`
  and the items `NM_MAIN`, `NM_MINOR`, `CRE_DATE`, `EXT_DATE` carry built-in logic (guide 01 "Base
  parts and items"); a framework depends on CZ_BASE or declares them. No role mapping is introduced:
  the vocabulary is small and shared by code anyway.
- *Scripts:* the index script of a part (`INDEX`) and the script computing items of the entity
  (`AUTO_ITEMS`) are entity rules; the most specific rule of the rule set of the entity's scope
  applies (`GroovyService.mostSpecificEntityRule`); a scope without a rule set uses the only ENTITY
  rule set. No `AUTO_ITEMS` rule means no computed items. The index script contract is in the
  implementation guide.
- *Core and CAM:* core paths reachable by any entity work without CAM's item types; the CAM checks
  written in Java (relations, identifiers, GEO) apply only when those item types exist.
- *Scopes:* the package import assigns the ENTITY rule set to scopes without one only when there is
  exactly one; a change of a scope's rule set is refused while it holds entities of classes the new
  rule set does not offer.
- *Startup import:* packages without dependencies are imported in code order (`CZ_BASE` before
  `ISAAR_CPF`), so a new Czech installation gets CAM on scopes without a rule set.
- *Package files:* read strictly - the first unmarshalling event (an element the binding
  `packageimport/xml` does not define, or one in the wrong place) refuses the import and names the
  file, line and element. Silently skipped elements hid real errors in delivered packages. There is
  no XSD; `PackageXmlFilesTest` reads every file of the delivered and test packages. Language codes
  of all packages are `LNG_` + ISO 639-2 bibliographic code, so frameworks share a language by code.
- *Specifications a rule set sees:* the assignments of packages related to the package of the rule
  set - itself, its dependencies and its dependents, both transitively
  (`PackageRelations.relatedPackages`); an assignment without a declaration is seen everywhere.
  Computed in static data (`RuleSet.getItemSpecs(ItemType)`) and applied wherever a rule model is
  built for a rule set: fund, output and structured-object rules (`ItemTypeExtBuilder`), entity rules
  and item-type filters (`RuleService.createModelItemTypes`), the item-type listing of a rule set
  (AI tool). `getAllDescriptionItemTypes` (no rule set) keeps every specification. Siblings are not
  related: a local package depending on CZ_BASE and ISAAR_CPF does not make CAM's specifications
  visible to ISAAR_CPF.
- *Layered settings:* `SettingsService.resolveGlobal` composes a setting type marked
  `UISettings.SettingsType.layered` by package dependency order (last wins; a setting without a
  package wins over all); import skips the `OTHER_PACKAGE` check for layered types. Layered so far:
  `OUTPUT_DEFAULTS`, `DAO_LEVEL_IMPORT`, `INDEX_SEARCH`.
- *Search configuration:* `index-search` is composed per field (`SettingsService.getIndexSearch`):
  core defaults `accessPointId`, `index`, `pref_index`, then the fields of every package, a field
  stated by several packages taken from the deepest in dependency order. The text search looks in
  every item field of `index-search` (area *names* by the attribute `part-type`). It stays global,
  not per rule set: one search ranks entities of all rule sets on one scale.
- *Bulk actions are recognised by their kind* (the YAML tag), not by the file name, so every rule set
  names its own files; the persistent sort and the menu items follow the actions of the fund's rule
  set (`BulkActionConfig.getType()`, `POST /action/fund-version/{id}/persistent-sort`).
- *DID of the DAO web services:* the DID sent to a WSDL digital repository or a digitization front
  desk carries the identifier only (`WSHelper.createDid`); no rules-package script and no rule type
  for it, so no rule set has to provide one. The future of the WSDL repository is in
  [da-migration.md](da-migration.md).
- *Tests:* packages stay installed across test classes; a test needing an installation without them
  calls `HelperTestService.deleteAllPackages()` and later classes re-import what they need. A second
  Spring context in the JVM is not possible (`DataType` keeps static state).

### Messages and languages

- *Sources live in code* (DRL `addError("KEY", "text")`, Java `CoreMessage`, react-intl
  `defaultMessage`); server-side translations always in the package XML format - in the package for
  a package's texts, in `elza-core/src/main/resources/translations/` for the core's - and
  client-side in `lang/translated/en.json`.
- *Who owns a text:* a text is a core text only when neither the client (it knows the code or type)
  nor a package (rule-set logic) can own it; server artifacts (CSV headers, 401 bodies) and
  rule-evaluated reasons (validation, AI proposal blocked reasons) are core. Texts for enums and
  states the client knows stay on the client.
- *Validation results* are stored language-neutral (one JSON line `{"key","text","args"}` with typed
  arguments) and rendered on the way out; nothing is extracted from DRL or Java, a changed meaning
  changes the key, translations of messages defined in rules are not hash-checked. Translations are
  loaded once into `PackageTranslations` with the static data - no per-message DB access.
  `RevisionFindingsBlockMapper` stays Czech by its own Javadoc (the findings' texts arrive from the AI
  in the run's language).
- *Rejected:* a properties bundle (a third mechanism), a separate core-texts package (must be
  distributed, `elza.packages.enabled` can skip it), client-side rendering of package messages (the
  client catalog cannot hold customer texts; the server renders for the AI context, exception texts
  and outputs anyway), ICU4J for now (`ChoiceFormat` covers cs/en plurals;
  `ValidationMessage.format` is the one place to switch).
- *Language chain:* the UI language → the same without region → the language of the installation
  (`elza.locale`) → the text as its package wrote it, which counts as the text in the package's own
  language at that language's place in the chain (an English reader of ISAAR_CPF gets the rule's
  English before the Czech of a Czech installation). The source texts of entities and declarations
  join `PackageTranslations` as rows in the package's language when static data loads; the names
  of type groups live in editable UI settings, so `RuleController.getGroups` passes the package of
  the rule set and the name takes that language's place at read time. No language on funds or
  scopes. An international package ships every language ELZA ships; a national package its own
  language only.
- *Texts a package writes follow the package* (proposal, not built). Names stored as text
  (structured object values, entity indexes and key values, auto items, outputs, the fulltext index
  of ENUM values, the text form of unit dates in index names and outputs) are written by the scripts
  and templates of a rules package and are rendered in the language of that package
  (`rul_package.language_id`: CAM and ZP2015 Czech, ISAAR_CPF and ISADG English) - never the UI
  language (background jobs write them without a user) and never a language of the fund. Core texts
  without a package stay on `elza.locale`. Concerned core places: the word for an undefined value in
  titles (`ArrangementService.UNDEFINED`; in messages it is the term `CORE/UNDEFINED_VALUE`), default
  names and values written by imports ("Importováno - <date>" in `DaoCoreServiceWsImpl`, "Šablona"
  in `ArrangementService`) - these come from the rules or the client instead of core. Prerequisite:
  scripts using names as identifiers switch to codes (`PT_IDENT.groovy`, the ZP2015 EAD template,
  the PDF content page).
- *Czech modules by decision:* the DA/AIP integration (`service/da/*`; the Czech national digital
  archive interface, like CAM), data type names (`db.elza-init.xml`, not shown to users), core
  resources (the DA export samples).

### Unit dates per language

1. **Parsing is language-independent, rendering is per language.** One tolerant parser on each side
   accepts every language's markers (BC, century, month names, date patterns) and the negative year
   whatever the UI language; rendering happens only on the server, in the language asked for.
2. **One grammar, not one per language.** The language-dependent tokens form a lexicon shared by
   both sides. Adding a language adds tokens, not a parser.
3. **Unified by a shared lexicon and a shared corpus, not by shared code.** Rejected: ANTLR with Java
   and TypeScript targets (a rewrite of the server parser and build plugins on both sides for ~15
   rules); server-only validation through an endpoint (grids and filters would wait on the network
   for every keystroke). The client parser mirrors the server algorithm; a PEG grammar cannot apply
   the lexicon's regular expressions at token boundaries.

*Where things are.* Lexicon `elza-core/src/main/resources/unitdate/lexicon.json` (imported by the
client at build time by a relative path: code versioned with both sides, not installation data);
corpus `unitdate/cases.json` run by `UnitDateCorpusTest` and `unitdate.test.ts` - the drift guard;
server `UnitDateConverter.convertToString(unitdate, languageTag)` (the overload without a language
renders Czech); client `components/shared/unitdate/parse.ts`, format help from `examples.ts`.
English forms: `1968`, `500 BC`, `20th century`, `20th century BC`, `Aug 1968`, `21 Aug 1968`,
`21 Aug 1968 14:05`, `1968 – 1969`, `[1968]`, `1985/1990` - month names because the numeric English
forms collide with the delimiters (`1968-08`, `8/1968`); input also accepts the ISO full date.

*Rule for the remaining places (no language on funds).* The archivist knows the language of the
description; the application needs no setting of its own (no `arr_fund.language_id`).
- **What a user sees follows the user.** Item values, forms and filters render in the request
  language. Tree titles are built on the server from the stored value and follow the request too -
  unless the level cache keeps the finished text; then it keeps the structured value and renders on
  the way out. The legacy client renderer in `party/DatationField.jsx` shows the server's text or
  renders through the lexicon of the UI language.
- **What a package writes follows the package.** Entity index names (`GroovyUnitdateFormatter`,
  `GroovyItem` in index scripts) and print outputs (`UnitDatePrintConvertor`, templates) render in
  the language of the package of the running script or template, so the date matches the words the
  script puts next to it; the default overload keeps Czech for callers without a package.
- **Everything else carries no language.** The fulltext index holds the text in every language of
  the lexicon, so a search finds the date whatever language the user typed it in. The entity date
  filter (`ApStateSpecification`) compares normalized ranges. `DateRangeAction` and `ImportFromFund`
  store structured values and need no text.

### Translation template (design)

`PackageTranslationService.template(packageCode, tag, of)`: every translatable text with
`src-hash`, the existing translation, the source text and state in a comment, orphans at the end;
`GET /api/v1/admin/packages/{code}/translation-template`. Built when the first translation is
maintained.

## 3. The international entity framework (ISAAR_CPF)

**Goal.** Persons, families and corporate bodies described according to ISAAR(CPF), with English
source texts, in the same installation as CAM entities and sharing with them what means the same.
ISADG refers to these entities as creators. Described for implementers in the implementation guide,
chapter "International entity description (ISAAR_CPF)".

**Decisions.**
1. **No dependency between ISAAR_CPF and CZ_BASE.** Definitions are shared through declarations of
   the same code; each package works alone.
2. **Roots only, kind of body as data** (research option (a)): assignable `PERSON_INDIVIDUAL`,
   `FAMILY`, `PARTY_GROUP`, `GEO`, `TERM`; own ENUM item types `ISAAR_CORP_TYPE`,
   `ISAAR_LEGAL_STATUS`, `ISAAR_FAMILY_TYPE`, `ISAAR_PLACE_TYPE`, `ISAAR_CONCEPT_SCHEME`. CAM's
   subclasses (kinds of bodies, fictional and non-human classes, family branches) stay CAM-only; the
   mapping of CAM classes to ISAAR values is in the guide chapter.
3. **Persons and families through option D:** the class tree of a rule set shows its members only;
   `PERSON` and `DYNASTY` are declared by ISAAR_CPF as parents (CAM's "person or being" is wider than
   ISAAR's person), not as members. CAM's logic is unchanged. Rejected: a visible grouping root (C)
   and ISAAR using the root class (E) - CAM is the Czech national system and its subclass concept is
   not shared with other methodologies.
4. **Non-agent classes:** `GEO` (Place) and `TERM` (Concept); no `EVENT` class (dated facts of an
   entity are `PT_EVENT` parts); functions and mandates as concepts and text; `ARTWORK` stays
   CAM-only.
5. **Vocabularies as specifications** on the own ENUM item types, reuse of CAM's codes where the
   meaning matches (name forms, languages, identifier types, kinds of beginning and end, event types,
   relation types with CAM's target classes). A later change of a value is a new specification code
   (package version bump, data migration for stored values).
6. **Shipped with every installation**, imported where `elza.packages.enabled` lists it (or at the
   first start of an empty database without the key); a scope uses it when its rule set is chosen.
7. **Research stays in the design documents**; what the guide needs from them (the CAM class
   mapping) is in the guide itself.

**Adjustments of the built package against the research proposal.** The package is generated by
`package-isaar-cpf/generator/gen_isaar.py` (regenerate rather than hand-edit; it writes the whole
module and reproduces the committed files exactly). CAM's `RT_RELATED` is not reused (its target
classes are not declared; own `ISAAR_RT_ASSOCIATED`); `CRE_CLASS`/`EXT_CLASS` are reused with
per-class subsets; languages of names are the 486 ISO 639-2 languages in CAM's `LNG_*` form
(`qaa-qtz` left out); `IDN_VALID_FROM/TO` reused; no `AUTO_ITEMS`.

**Known limits.** No EAC-CPF export (needs `uri` on specifications and relation categories derived
from the specification); `NM_LANG` offers ISO 639-2 only (a language outside it needs an addon
declaring it); concepts have no scheme hierarchy beyond `RT_SUPTERM`; Czech names of its own
definitions are content work for when a Czech archive adopts it (`translations/cs.xml`, ~160
strings); when ISAAR_CPF is imported before CZ_BASE it owns the shared codes and CAM's specification
order on those item types follows ISAAR_CPF; ISAAR_CPF ships no `index-search`, so alone it is
searched by names (`index`) only, without the name-item weights of CZ_BASE.

### Scope-bound CAM extension (R2)

A reference scope of portal.nacr.cz holds template entities with items beyond CAM (explanatory
items, a classification for browsing); they stay CAM entities. Given: the CAM exchange is bound to
the CAM rules - only an entity described by them can be exported; the standard fixes the part types
(the schema's `PartTypeXml`), so a part of another type ends the export with an exception, which is
correct. An extra item type is dropped by `ItemTypeMap.groovy`; an extra specification of a CAM item
type drops the whole part from the export. Open questions:
- Q1 Are reference entities exchanged with the central CAM system?
- Q2 Extra items inside CAM parts or extra parts?
- Q3 An extension activated per scope (`ap_scope_extension`, extension rules for entities of the
  scope) or a rule set inheriting CAM (Phase 6)?
- Q4 Relations across frameworks (CAM entity to ISAAR entity, fund description referring to both,
  shared search).

## 4. `rules-en-isadg`: the international archival description

**What ZP2015 teaches.** ZP chapter 4, the common elements (group `06_ZP2015_CH4`), is ISAD(G)
area 3 almost one to one. The bulk of ZP2015 - 138 item types, 409 specifications, the availability
matrix *unit type × element*, the EJ computation, the structured access restrictions, the DA
scripts, 12 templates - is Czech practice or ZP chapter 5 and is not carried over; an addon brings
such detail later (Phase 3 mechanisms). ISADG is therefore a *level × area* matrix. SIMPLE-DEV is a
skeleton of the files, not of the content (Czech, CZ_BASE-dependent). The package is generated from
tables (`rules-en-isadg/generator/gen_isadg.py`), so the filter rule, the type groups, the grid, the
pinned filters and `translations/cs.xml` come from one table and cannot drift.

**Decisions.**
- *D1* ISADG depends on ISAAR_CPF (`item-aptypes` of a RECORD_REF must name declared classes,
  checked in `ItemTypeUpdater`); `elza.packages.enabled` lists ISADG, the import pulls ISAAR_CPF in.
- *D2* the six ISAD(G) levels and the collection. `ISADG_LEVEL_COLLECTION` is a root like the fonds:
  the standard's glossary term for an artificial assemblage, offered by EAD, AtoM and ArchivesSpace
  alike. `NewLevel.drl` offers only the allowed children (fonds at the root, the only root scenario,
  because fund creation applies a scenario only when there is exactly one - a collection is made by
  changing the level of the root; subfonds under fonds, collection and subfonds, since ISAD(G) 3.1.4
  speaks of "first level sub-fonds, second level sub-fonds"; series under fonds, collection,
  subfonds, series; subseries under series, subseries; file under series, subseries, file; item
  under series, subseries, file, item); `Validation.drl` checks what scenarios cannot guard - a
  level changed later, an imported fund: the root is a fonds or a collection, a child's level ranks
  below its parent's or equals it from the subfonds down. ZP2015 guards its hierarchy in the
  scenarios only; the reference-code generator and the EAD template assume it.
- *D3* a structured storage unit from the start: `ISADG_CONTAINER` after SIMPLE-DEV's `SRD_PACKET`
  (kind, prefix, number, postfix, location; "Box 12-13" is the number 12 with the postfix "-13"), a
  generator action (`GENEROVANI_UKL_JEDNOTEK.yaml` as the model), `ISADG_POL_CONTAINER`. Possible at
  every level, the root included: a one-box collection carries its container at the top, as the top
  containers of ArchivesSpace do. ISAD(G) has no container element and the British, Dutch and
  Australian conventions keep the location outside the finding aid, so ZP2015's "every leaf branch
  has a container" is not carried over; `Validation.drl` reports a file or item without a container
  only when a sibling has one.
- *D4 extent and medium.* In the standard the element is one text ("128 boxes (12.5 linear
  metres)"). ZP2015 types the material (`ZP2015_UNIT_TYPE`, 53 kinds) with a count, sums the counts
  up the tree (`!UnitCount`) and prints the totals; the size of one item has its own items. ISADG
  keeps the standard's text and ZP2015's precision: `ISADG_EXTENT` (TEXT, the statement as it
  prints); `ISADG_EXTENT_QUANTITY` (DECIMAL with the unit as specification, repeatable - later summed
  per unit by a `!UnitCount` configuration for the finding aid's statistics); `ISADG_MATERIAL_TYPE`
  (ENUM, repeatable, ~15 base kinds, extensible by an addon, which could carry the 53 kinds of
  ZP2015); detail items offered by the material type (`ISADG_DIMENSIONS`, `ISADG_SCALE` for maps and
  drawings, `ISADG_DURATION` for sound and moving images). Example: a fonds - extent "128 boxes
  (12.5 linear metres)", quantity 12.5 [linear metres] and 128 [boxes]; a map - level item, material
  type maps and plans, scale "1:2880", dimensions "62 × 48 cm", quantity 1 [items]. The generator
  makes a change cheap while the package is unreleased.
- *D5* access as text, a status (open, restricted, closed) and an opening date; structured
  restrictions later as an addon, they follow local legislation.
- *D6* one `ISADG_ACCESS_POINT` (RECORD_REF to the five ISAAR classes, the role as specification)
  for the index terms of EAD `controlaccess`; the creator stays its own item `ISADG_CREATOR`.
- *D7* the ISO 639-2 `LNG_*` specifications shared by code with ISAAR_CPF and assigned to
  `ISADG_LANGUAGE` and `ISADG_FA_LANGUAGE`; script as text in version 1.
- *D8* one output type `ISADG_FINDING_AID` with EAD3 (FreeMarker, `validation-schema`), PDF
  (Jasper) and XML (engine `DE_XML`).
- *D9* bulk actions recognised by their kind and the DID carrying the identifier only (section 2):
  nothing in core keys on ZP2015's action file names or on a DID script.
- *D10 the reference code is assigned, not generated.* ISAD(G) 3.1.1 asks for "a specific local
  reference code, control number, or other unique identifier" and prescribes no form; the archives
  checked (TNA, Archives nationales, Bundesarchiv, Swiss BAR, Nationaal Archief, Polish state
  archives, Riksarkivet, PARES, NAA, NARA) and the tools AtoM and ArchivesSpace assign alphanumeric
  codes once and keep them, none derives them from the position in the tree
  ([research/isadg/reference-codes.md](research/isadg/reference-codes.md)). ZP2015's positional
  numbering (`!UnitIdGenerator`: `1/15/2//1//10/1-1`, digits only, `//` after the series) is the
  Czech rule 4.2.1; on an ISADG fonds it would refuse the code the standard requires on the root
  ("Root node has unit id"), number a subseries like a file and delete a manual code, since nothing
  seals values without `!SealUnitId`. ISADG therefore ships no numbering action. The data type stays
  UNITID (core's identifier string, EAD `<unitid>`); the code is REQUIRED on the fonds and
  RECOMMENDED below; `Validation.drl` reports a value carried by two units of the fonds through the
  core fact `DescItem.duplicateInFund` (answered lazily for UNITID items by one lookup, index
  `ix_arr_data_unitid_unit_value`); a replaced code goes to `ISADG_OTHER_ID_FORMER`, EAD
  `<unitid localtype="former">`. Positional numbering for ISADG is an addon candidate once the
  generator learns a root code, letter prefixes and nested subseries.

**Mapping.** Codes `ISADG_` + the ISAD(G) term; names and messages English.

| ISAD(G) | ZP2015 | ISADG (data type) |
|---|---|---|
| 3.1.1 Reference code | `ZP2015_UNIT_ID` (UNITID) + `!UnitIdGenerator` | `ISADG_REFERENCE_CODE` (UNITID) assigned by the archivist, unique in the fonds - D10; country and repository codes from the institution in outputs |
| - other identifiers | `ZP2015_OTHER_ID` (24 specs), `ZP2015_SERIAL_NUMBER`, `ZP2015_INV_CISLO` | `ISADG_OTHER_ID` (STRING; specs former reference code, accession number, other) |
| 3.1.2 Title | `ZP2015_NAME`, `ZP2015_FORMAL_TITLE`, `*_PUBLIC` | `ISADG_TITLE` (STRING); formal and supplied titles not distinguished |
| 3.1.3 Dates | `ZP2015_UNIT_DATE`, `_BULK`, `_TEXT`, `_PRE`, `_POST`, `ZP2015_DATE_OTHER` (25 specs) | `ISADG_DATE` (UNITDATE, creation), `ISADG_DATE_BULK` (UNITDATE), `ISADG_DATE_TEXT` (STRING) |
| 3.1.4 Level | `ZP2015_LEVEL_TYPE` (6: soubor, dílčí list, série, složka, jednotlivost, část) + `ZP2015_FOLDER_TYPE` | `ISADG_LEVEL` (ENUM: `FONDS`, `COLLECTION`, `SUBFONDS`, `SERIES`, `SUBSERIES`, `FILE`, `ITEM`) - D2 |
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
| finding aid (output items) | `ZP2015_FINDING_AID_ID`, `_OUTPUT_TYPE`, `_FINDING_AID_DATE`, `_UNIT_COUNT_SUM`, `_UNIT_COUNT_TABLE`, `_UNITS_AMOUNT`, `_RELEASE_DATE_PLACE`, `_APPROVED_BY`, `_EDITOR`, `_DATE_RANGE`, `_ATTACHMENT`, `_ORIGINATOR_SIMPLE`, `ZP2015_ARCHDESC_LANG` | `ISADG_FA_TITLE` (STRING), `ISADG_FA_KIND` (ENUM: inventory, catalogue, guide, list; possible, not required - the typology is ZP2015's `ZP2015_OUTPUT_TYPE`, no element of the standard or of EAD), `ISADG_FA_AUTHOR` (TEXT), `ISADG_FA_DATE` (DATE), `ISADG_FA_LANGUAGE` (ENUM `LNG_*`), `ISADG_FA_NOTE` (TEXT), `ISADG_ATTACHMENT` (FILE_REF) - D8 |
| not carried over | `ZP2015_NAD`, `_ZNACKA_FONDU`, dílčí list (`LEVEL_SECTION`, `DilciListy.drl`), `_ARRANGEMENT_TYPE`, `_UNIT_SUBTYPE`, `_UNIT_EXT_TYPE`, `_AGENDA_TYPE`, `_RECORD_TYPE`, chapter-5 items (~45), `_PUBLISHED`, `_INVALID_RECORD`, `_CONNECTED_RECORD`, `_NOT_FOUND`, `_ALTERNATIVE_DESCRIPTION_INLINE`, `_ITEM_TITLE_REF`, `_AWARD_*`, `_ENTITY_ROLE_TEXT`, all 8 extensions, older-aid output, labels, migrations, test-data generator | Czech practice or ZP chapter 5; an addon on ISADG can add them later |

**Package shape.** Module `rules-en-isadg` (package and rule set `ISADG`, `<language>en`,
dependency `ISAAR_CPF` min 2); no arrangement extensions, no `DA_IMPORT`/`DA_MATCH` (the Czech
national DA), no labels, no older-aid output in version 1. Rules: `ItemTypeFilter.drl`,
`AvailableItems.drl` (everything possible at every level, required by level, the material-type
gating of D4), `Validation.drl` (the six essential elements of ISAD(G) at the fonds - reference code,
title, dates, level, extent, creator; level and title everywhere; a date inside the parent's range;
the hierarchy of D2 with a fonds or a collection at the root; a reference code unique in the fonds,
D10; a container missing next to a unit that has one, D3), `NewLevel.drl` (scenarios named by the
level),
`ChangeImpact.drl`, `Output_FindingAid.drl`, a citation Groovy; messages keyed, Czech in
`translations/cs.xml`. Policy types `ISADG_POL_BASIC`, `ISADG_POL_REFERENCE_CODE`,
`ISADG_POL_CONTAINER`. Actions: `!FundValidation`, persistent sort, the container generator; no
numbering of reference codes (D10). Templates:
EAD3 from the skeleton of ZP2015's `INVENTAR_EAD/index.ftl` (control, archdesc, the `dsc` recursion;
ISADG levels are EAD level values one to one), PDF from `SRD_INVENTORY_JASPER`, XML. UI settings of
the rule set: FUND_VIEW (tree `ISADG_REFERENCE_CODE ISADG_TITLE ISADG_DATE`, icons per level),
TYPE_GROUPS by the seven areas plus Finding aid, Access points, Container, GRID_VIEW, FUND_ISSUES,
STRUCTURE_TYPES, OUTPUT_DEFAULTS; of the package: DAO_LEVEL_IMPORT (its `<scenario>` is matched by
the scenario *name*), pinned search filters, favourite languages. Institution types (archive,
library, museum, other) are declared by ISADG, since ISAAR_CPF has none and an installation without
CZ_BASE could otherwise create no institution.

**Core and client places that assume ZP2015.** Nothing keys on level or arrangement types.
- *Blocking for the search of ISADG funds:* the pinned search filters (`node-search-settings`,
  `SEARCH_NODE_FILTERS`) are one global setting that names the item types of one rule set; a second
  package is refused (`OTHER_PACKAGE`), so ISADG ships none and an ISADG fund shows ZP2015's filters
  where ZP2015 is installed. Fix: the setting per rule set (entity type RULE, read by the fund's rule
  set) or layered.
- *Tolerable:* fixed links by `ZP2015_ITEM_LINK` in `ArrangementService` (no type, no links; later
  by the URI schema `elza-node`); the finding-aid columns of the outputs report in
  `ReportServiceQuery` (left joins, empty for ISADG); `DaImportPlanner.ruleSetLabel` Czech text →
  `CoreMessage`; the sample CSV with ZP2015 codes in `DataGridImportDialog.tsx` → generic.
- *Accepted limits of version 1:* NEW_LEVEL scenario names are texts a rule writes and the
  identifier `DAO_LEVEL_IMPORT` matches - not translatable, a Czech user sees English scenario names;
  Czech names of the 486 languages come from CZ_BASE when installed, otherwise the generator copies
  them into `cs.xml`.

**Exit criterion.** An English user creates an ISADG fund, fills the essential elements, links an
ISAAR creator, validates, generates the EAD and the PDF, and sees no Czech text; the same
installation keeps a ZP2015 fund working.

## 5. Later phases — design notes

**Phase 1 — layer core, rest.**
- *Revalidation requested by an addon.* `compatibility-rul-package` lives on the owner's rule set and
  on extension and entity rules; an addon cannot ask for revalidation of a foreign rule set.
  Proposal: accept it on `<arrangement-rule>`; for rules contributed to a foreign rule set compare
  with the addon's installed version and revalidate the funds of that rule set.
- *Package deletion* publishes `ActionEvent(EventType.PACKAGE)` after commit and enqueues
  revalidation of the cleaned rule sets; the client's `packageEvent()` also invalidates `groups`,
  `structureTypes`, `apTypes`.
- *Export:* `exportSettingsForRuleset` reports a missing rule set instead of an NPE.

**Phase 3 — pilots.** Settings composed by package dependency order through `resolveGlobal`: last
wins for FUND_VIEW, STRUCTURE_TYPES, PARTS_ORDER, ITEM_TYPES, STRUCT_TYPE_*, FUND_ISSUES;
TYPE_GROUPS as a placement patch; GRID_VIEW appends. A type joins by setting the `layered` flag and
switching its read sites (`SettingsService`, `ConfigRules`, `ConfigView`, `IssueDataService`,
`StructObjService`, `ClientFactoryVO`, `ApFactory` view settings, `DaoCoreServiceWsImpl`) to
`resolveGlobal`. `UISettings.isSameSettings` compares strings with `==` - check production for
same-key rows of different packages before fixing. Then DPP and CT become file addons.

**Phase 4 — customizations in the UI.** `rul_package.kind` (`IMPORTED`, `LOCAL`),
`rul_package_archive` (every version's ZIP, in the database); package code = code prefix; scope
global or an own arrangement extension; dependencies derived on save;
`CustomizationPackageService.rebuild` imports in memory (`importPackageInternal(Map, boolean)`); a
specification is added with its position and a generated rule; the rule editor compiles before
save; detach/attach; spec usage counts protect deletion; OpenAPI tag `customization` (ADMIN), admin
page `/admin/customization`.

**Phase 5 — declarative primitives and overrides.** `rul_item_type.specs_extensible`, item-type
anchors with a layout pass replacing the `view_order` blocks, retirement of specs, code aliases,
structure extensions across packages, `NewLevelApproaches.remove`; UK pilot (its
`PERSON/PT_EVENT.groovy` patch becomes an `INDEX` rule).

**Phase 6 — convergence.** Rule-set inheritance; renames of generic ZP2015 codes towards ISAD(G)
through aliases.

## 6. Reference facts

- Spec availability: `RulItemTypeExt` sets every spec IMPOSSIBLE, only DRL raises it;
  `ClientFactoryVO.createFormItemTypes` drops IMPOSSIBLE specs.
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
  ts:strict-check` before every commit touching `.ts/.tsx`; the docs job builds the guides with
  Sphinx `-W`.
- A content change of a package without a version bump never reaches an installation, but an
  unreleased version takes further changes (check `git tag --contains`).

## 7. Critical files

- Import: `packageimport/PackageService.java`, `ItemTypeUpdater.java`, `APTypeUpdater.java`,
  `PackageDeclarations.java`, `PackageTranslationService.java`, `PackageUtils.java`, `xml/*`.
- Runtime: `core/data/StaticDataProvider.java`, `core/data/RuleSet.java`, `core/data/PackageTexts.java`,
  `core/data/ValidationMessage.java`, `core/data/CoreMessage.java`, `core/data/CoreTranslations.java`
  (+ `resources/translations/{cs,en}.xml`), `service/RuleService.java`, `service/GroovyService.java`,
  `service/AccessPointService.java`, `service/SettingsService.java`,
  `controller/factory/ApFactory.java`, `domain/bridge/IndexConfigReaderImpl.java`.
- Schema: `elza-core/src/main/resources/db/changelog/db.elza-3-part-03.xml`.
- Packages: `package-cz-base/`, `package-isaar-cpf/` (generated by `generator/gen_isaar.py`),
  `rules-en-isadg/` (generated by `generator/gen_isadg.py`; Python 3 needed), `rules-cz-zp2015/`,
  `rules-simple-dev/`.
- Guides: `docs/admin-guide`, `docs/implementation-guide`, `docs/user-guide` (ISADG and ISAAR_CPF for
  archivists).
- Client: `components/registry/ApDetailPageWrapper.tsx`, `ApTypePicker.tsx`,
  `modal/CreateAccessPointModal.tsx`, `components/shared/lang/language.ts`, `LanguagePicker.tsx`,
  `components/shared/unitdate/`, `components/arr/bulkActions.ts`.
- Tests: `packageimport/EntityRulesTest.java` (`entity-rules-test`), `StandaloneFrameworkTest.java`
  (`entity-standalone-test`), `IsaarCpfPackageTest.java`, `IsadgPackageTest.java`
  (`rules-en-isadg`), `rules/addon/AddonPackageTest.java` (`rules-addon-test`),
  `PackageTranslationTest` (`translation-addon-test`), `PackageXmlFilesTest` (every package file
  read strictly), `core/data/PackageTextsTest`, `core/data/CoreMessagesTest` (every core message in
  every shipped language), `UnitDateCorpusTest`, `search/IndexConfigReaderTest.java`,
  `other/HelperTestService.java` (`deleteAllPackages`).

## Open work

**Customization layer**
- Phase 1 rest: revalidation requested by an addon (open: whether deleting an addon revalidates);
  `PACKAGE` event and revalidation on package deletion; `exportSettingsForRuleset` without NPE for a
  missing rule set.
- Tests for `AddonPackageTest`: spec raised by an addon rule where its type is possible; a scoped
  variant with its own extension; revalidation by an addon rule; export → delete → re-import.
- Phase 3: the remaining setting types layered; DPP and CT as file addons.
- Phases 4-6 (section 5).
- Release gate of 3.4 (verify): on a PostgreSQL copy of production apply the 3.4 changesets,
  re-import CZ_BASE and ZP2015, check that spec order and `view_order` are unchanged and that
  ISAAR_CPF imports at startup next to them.

**Entity frameworks**
- EAC-CPF export of ISAAR_CPF (`uri` on specification declarations, relation categories from the
  specification); on demand.
- R2 scope-bound CAM extension: answers to Q1-Q4 first.
- `index-search` for ISAAR_CPF (name-item weights).
- Computed items and the Groovy model of a revision use the entity's current class, not a class
  changed in the revision (`GroovyService.convertAe`, `autoItemsScriptPath`) (verify).
- A shared base for package tests (`authorizeAsAdmin`, `tx`, `txGet` repeated in three tests).
- Optional: remove the unused category tree of specifications (`ClientFactoryVO.createTree`,
  `RulDescItemTypeVO.itemSpecsTree`; categories are stored and exported, never shown);
  `AP_MAPPING_TYPE` as an entity rule; the CAM relation and identifier checks of `RuleService` as
  CAM `VALIDATION` rules; a server-side check that a new part's type is in the rule set's part list;
  port the rest of `ApStateChangeForm` / `RevStateChangeForm` to Fluent UI; per-package RECORD_REF
  classes of shared item types and specifications.

**Localization and `rules-en-isadg`**
- UI pass of an ISADG fund in a dev installation (series, file, item, container, ISAAR creator and
  access point, validation, output); findings into section 4.
- Pinned search filters per rule set (the remaining blocking core place).
- ISADG container generator action (`!ItemGenerator`) and the citation script.
- Positional numbering of ISADG reference codes as an addon (D10): the generator needs a root code,
  letter prefixes and nested subseries first; a check that a child's code extends the parent's
  belongs to the same addon, since it is one convention among several.
- Fund creation with a choice among several root scenarios (core and client), so that a collection
  is created as one instead of by changing the level of the root (D2).
- Unit dates in the remaining places (section 2, "Rule for the remaining places") - prerequisite of
  the EAD3 template; then the EAD3 template validated against the schema in a test.
- PDF finding aid (Jasper); implementation guide chapter "International archival description
  (ISADG)"; user guide chapters 9 (outputs) and 10-12 (ISAAR_CPF), now stubs; release notes.
- Texts a package writes follow the package (section 2, proposal) - with the print templates and
  index scripts.
- Remaining read sites of translated names: specification names in tree titles
  (`DescriptionItemServiceInternal`), specification categories (`ITEM_SPEC_CATEGORY`), kinds
  `EXPORT_FILTER`, `OUTPUT_FILTER`, `STRUCTURED_TYPE_EXTENSION`, `EXTERNAL_ID_TYPE`,
  `INSTITUTION_TYPE`, `ACTION`; item type names of AI proposal rows (`AiProposalService`).
- Translation template (section 2), when the first translation is maintained.
- Open naming questions: the ISADG name of the Czech "formal title" (ZP 4.2.4); whether
  `ISADG_ARCHIVIST` stays (3.7.1 is a text in the standard).
