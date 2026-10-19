**Partly a proposal.** How the EAD3 export follows the Czech EAD3 profile
`CZ_EAD3_PROFILE_20260501`, the decisions taken where the profile leaves room, and the gaps that
remain (bug 10164 and its dependencies).

# EAD3 export (Czech profile)

## Where EAD is produced

| Producer | Code | Used for |
|---|---|---|
| Finding aid / output export | FreeMarker template `rules-cz-zp2015/src/rul_rule_set/ZP2015/templates/INVENTAR_EAD/index.ftl`, model `cz.tacr.elza.print.*` (`OutputModel`, `Node`, `Record`, `Dao`) | Templates `ZP2015_INVENTAR_EAD`, `ZP2015_MANIP_SEZNAM_EAD`, `ZP2015_STARSI_POMUCKA_1958_EAD` (`rul_template.xml`), validated against `ead3.xsd` |
| Change package for the digital archive | `DaEadContextGenerator`, called from `DaService.createEad`, renders the same template in the contextual mode | `metadata/descriptive/EAD-CONTEXT.xml` of every exported AIP |

The profile is the Sphinx document in `ead3doc.git` (`doc/source`); chapter names below refer to it.

Restricted data are handled by the optional output filter `ZP_ACCESS_RESTRICT`
(`output_filters/ACCESS_RESTRICT.yaml`): it hides levels, items and DAOs and swaps restricted
name/content for their public variants. The template itself does not use the profile's
`audience` model.

`Zp2015EadExportTest` generates the EAD output and the contextual description for the fixture
fund `zp2015/ead-export-fund.xml` and checks the mappings below; both are validated against
`ead3.xsd`.

## Contextual description of an AIP

The change package that ELZA sends to the digital archive carries the contextual archival
description of the AIP (`EAD-CONTEXT.xml`). It is produced by the finding-aid template, so both
forms of the description stay consistent:

- The template runs with `eadContext = true` and an output model built for an unsaved output
  (`OutputGeneratorFactory.createOutputModel`); the output's fund comes from the linked units, its
  type from the template.
- Content: the fund root (`archdesc level="fonds"`), the path to every unit linked to the AIP and
  the whole subtree of each linked unit (the output model adds ancestors and subtrees of the
  output nodes). Ancestors are context, not partially described levels, so no `otherfindaid` is
  written.
- A linked unit has `<dao identifier="…" coverage="whole">`: the METS ID of the linked part of the
  AIP, or the AIP code for a link to the whole AIP. There is no `href`; the description is inside
  the package it refers to.
- The EAD namespace is the default namespace and `cam` keeps its prefix, as the profile requires
  for packages of the digital archive. The result is validated against `ead3.xsd` before it is
  written; a failure stops the export of that AIP with a message.
- The units of an AIP must belong to one fund, the description has one `archdesc`.
- The template is looked up by code (`ZP2015_INVENTAR_EAD`); a fund of other rules cannot be
  exported until it has a template of its own.

ZAF does not validate the content of `EAD-CONTEXT.xml` in a change package (profile
`SIP_CHANGE`); its prepared rules for a contextual description (`EARK_CONTEXTUAL_DESC`) are not
active. The output follows those rules where ELZA has the data.

## Decisions in the finding-aid export

- **Level identifiers.** Every level has `id="uuid-<node uuid>"`, the profile's meaningless
  identifier. The URI form (`base`) is not written; `id` is the identifier for new finding aids.
- **Links to the digital archive.** A unit linked to an AIP has
  `<dao href="aip:<AIP code>" identifier="…" coverage="whole">` with the identifier as in the
  contextual description. `daotype` is `derived` when the unit has a storage unit
  (`ZP2015_STORAGE_ID`, a digitised analogue record), otherwise `borndigital`. Links hidden by the
  output filter (`hiddenDao`) are not written.
- **Empty `<did>`.** A level whose `<did>` would stay empty gets
  `<unitid localtype="ID" label="identifikátor jednotky popisu">uuid-…</unitid>`; an empty `<did>`
  is invalid and would fail the XSD validation of the whole output.
- **Rules.** `RULES` is `CZ_ZP1958` for the output type `STARSI_POMUCKA_1958`, for the finding-aid
  type "prozatímní inventární seznam" (the profile ties it to the 1958 rules) and for a root
  arranged as "starší pomůcka"; otherwise `CZ_ZP2013`. `FINDING_AID_TYPE` is chosen by the
  specification code; an unknown code writes no `FINDING_AID_TYPE` rather than an invalid value.
- **Other identifiers.** A `ZP2015_OTHER_ID` specification without its own profile value is written
  as `localtype="JINE"` with the specification name in `label` (the profile's rule for other
  identifiers), so nothing is dropped.
- **Text roles (`ZP2015_ENTITY_ROLE_TEXT`)** are weak entities: `<relation>` with
  `<relationentry>` only. The output filter creates them for living persons. As there is no
  entity, the class comes from the role: `resourcerelation` for roles naming places, types,
  awards, positions, classification and captured entities, and for the generic "related entity"
  roles; `cpfrelation` for all other roles and for the generic roles 66–69 (person, family,
  corporation, event). Inheritance is written as for linked roles.

## Remaining gaps

Ordered by impact.

### A. Wrong meaning or leaked data

| # | Finding | Profile chapter |
|---|---|---|
| A2 | Without the output filter, restricted values are exported as public: `ZP2015_CONTENT` goes to `<scopecontent>` without `audience`, `ZP2015_CONTENT_PUBLIC` is ignored, `ZP2015_APPLIED_RESTRICTION` (`ABSTRACT`, `ARCHENTITY`, `DAO`) and unpublished levels are not marked. Whether a file is "public" depends only on whether the user picked the filter; the file does not say which it is. | Omezení přístupu a podmínky zveřejnění |
| A3 | `ZP2015_NAME_PUBLIC` is written with `audience="external"`; the profile forbids `external` on description elements — the public variant has no `audience`, the restricted one `audience="internal"`. | Možnosti zveřejnění jednotky popisu |

### B. Content that is not exported

| # | Finding | Profile chapter |
|---|---|---|
| B2 | Output-level intro items (`ZP2015_UNIT_HIST`, `UNIT_ARR`, `UNIT_CONTENT`, `UNIT_SOURCE`, `FUTURE_UNITS`, `UNIT_CURRENT_STATUS`, `ARRANGE_RULES`, `ARRANGEMENT_INFO`, `RELATED_UNITS`, `SOURCES`, `LANGUAGE`, `DATE_RANGE`). Only intro items stored on the root node reach the export. | Uložení úvodu a prvků popisu tiráže, Převažující jazyk |
| B3 | `<odd localtype="FINDING_AID_INTRO">` for 1958 finding aids. | Úvod starší archivní pomůcky |
| B7 | Carrier unit types `FPA`, `ANZ`, `MHZ`, `FVA`, `FDR`, `GD`, `MAT`, `MG`, `AKA`, `KDI` and `KTT_BOX`, `KTT_ITEM` fall back to `file`/`item`; the profile has no value for them. | Druh archiválie |
| B8 | Roles 91 (autor předlohy) and 92 (autor hudebního aranžmá) fall back to `ENTITY`; the profile has no value for them. Date type `DISCUSSION` (jednání) is written without `localtype` for the same reason. | Definice rolí, Jiné datace |
| B9 | Links to other units (`ZP2015_RELATED_UNITS_LINK`, `ZP2015_ITEM_LINK`); the profile writes them as `<relatedmaterial><archref><ref target="uuid-…">`. | Související vlastní JP |
| B10 | Record references `ZP2015_GEO_REF`, `ZP2015_AP_REF`, `ZP2015_LEGALACT_REF` produce no `<relation>`. | Role entit |
| B11 | `ZP2015_DAO_LINK` (URI of a digitised copy) is not exported. | Odkaz na digitální objekt |
| B12 | Range in bytes (`spaceoccupied`, `unittype=byte`); bm come from parsing the string `ZP2015_UNITS_AMOUNT` although `ZP2015_COLL_EXTENT_LENGTH` is a decimal. | Rozsah zpřístupněných archiválií |

### C. Format differences

| # | Finding | Profile chapter |
|---|---|---|
| C7 | Entities are written in CAM schema v1 (`http://cam.tacr.cz/2019`). V2 (`http://cam.tacr.cz/2025`) is accepted now and will be the only one later; ELZA already has a v2 builder (`cz.tacr.elza.cam.v2.export.CamExportBuilder`). Check that the export carries `CAM_REAL_ID` and `DISPLAY_NAME` (`cam:si`). | Entita dle standardu CAM |
| C8 | Structured and text forms of Technika are written together (`TECHNIQUE` next to `TECHNIQUE_DETAIL`/`MEDIUM`/…); the profile forbids combining them. | Technika, adjustace, nosič a látka záznamu |
| C9 | Restricted DAOs: `audience="internal"` on `<dao>` is not written (only possible without the filter, see A2). | Možnosti zveřejnění reprodukce |
| C10 | `eventdatetime` must include the time zone; `${output.changeDateTime}` renders `OffsetDateTime.toString()`. Not covered by a test yet. | control/maintenancehistory |
| C11 | `<unittype>` in "Počet EJ" comes from the `NAME` column of `ZP2015_UNIT_COUNT_TABLE`; the profile requires the abbreviation from ZP 2.9 (`kar`, `map`, …). Verify what the bulk action stores. | Počet evidenčních jednotek |

### D. Maintainability

- Mapping tables (unit types, other ids, dates, roles) live only in the template; a new
  specification in `rul_item_spec.xml` silently gets a fallback.

## Proposed changes

### 1. Two explicit export modes (A2, A3, C9) — bug 10166

The filter-based approach stays, but the file must say what it is and the unfiltered export must
follow the profile's `audience` model:

- **Public export** (output filter `ZP_ACCESS_RESTRICT` applied): as now; hidden levels, items and
  DAOs are absent; no `audience` attributes; `NAME_PUBLIC`/`CONTENT_PUBLIC` already replaced by
  the filter and written without `audience`.
- **Complete export** (no filter): everything is written and restricted parts are marked —
  `audience="internal"` on the `<c>` of a level that cannot be published (`ZP2015_RESTRICTION_ARCHDESC*`,
  `ZP2015_PUBLISHED` = false), on `unittitle`/`scopecontent` restricted by `ABSTRACT` (with
  `NAME_PUBLIC`/`CONTENT_PUBLIC` as the unmarked public variant), on `<relation>` for
  `ARCHENTITY`, on `<dao>` for `DAO`/`DAO_INPERS_ONLY`; `audience="external"` only on a level that
  overrides a restriction inherited from above.

The restriction state per level is already computed for the filter (`OutputModel.levelRestrMap`,
`restrictedNodeIds`); expose it on `print.Node` (e.g. `node.restrictions`) instead of re-deriving it
in FreeMarker.

### 2. Change package for the digital archive — bugs 10133, 10157, 10134

The contextual description is done (see above). Open: the PREMIS event of creating it
(NMET2, CZDAP-IPM0002, bug 10134) and the NA's review of the content.

### 3. Remaining template fixes (C8) — bug 10165

Technika: both forms are written until the NA says which one wins; the text form cannot be
dropped silently, it may carry information the structured items do not have.

### 4. Model additions in `elza-core` (B9–B12, C7) — bug 10167

- URI_REF items resolved to the target node UUID when the target is in the same fund → `archref`.
- Record references → `<relation>`.
- `Record.exportData(CAM_SCHEMA_2025)` via the v2 builder; template parameter decides v1/v2.
- Decimal range (`ZP2015_COLL_EXTENT_LENGTH`) and byte range.

### 5. Intro of the finding aid (B2, B3) — bug 10168

The profile puts only publication data, unit counts and range into the intro; texts such as
Dějiny or Způsob uspořádání are expected on the root `<archdesc>`. Proposal: write output-level
intro items as the corresponding elements of the root `<archdesc>` (`custodhist`, `arrangement`,
`scopecontent`, `acqinfo`, `accruals`, `phystech`, `processinfo`, `relatedmaterial`,
`bibliography`), output-level `ZP2015_LANGUAGE` as `<langmaterial altrender="majority">`, and for
1958 finding aids the intro text as `<odd localtype="FINDING_AID_INTRO">`. If the root node
carries the same item, the output item wins.

### 6. Profile conformance test — bug 10169

`Zp2015EadExportTest` covers the mappings listed under "Decisions" and the fixed localtypes. Still
missing: a check that every specification of the mapped item types in `rul_item_spec.xml` has a
mapping, the time zone of `eventdatetime`, output-level items.

## Open work

Sections 1–6 above. Order by risk: 2 (wrong data sent to the DA), 1 (privacy), 3, 6, 4, 5.

Questions for the profile authors (NA):

- Values for ZP2015 data the profile does not know: date type `DISCUSSION` (jednání), roles 91
  (autor předlohy) and 92 (autor hudebního aranžmá), carrier unit types (`FPA`, `ANZ`, `MHZ`,
  `FVA`, `FDR`, `GD`, `MAT`, `MG`, `AKA`, `KDI`), card-index parts (`KTT_BOX`, `KTT_ITEM`),
  `ZP2015_UNIT_SUBTYPE` (matrika, hudebnina, …), dedicated values for other ids `PERSONAL_ID`,
  `PRINT_NUMBER`, `RESOLUTION_ID`, `INVALID_UNITID` (now `JINE`).
- Where output-level intro texts belong in a ZP2013 finding aid (proposal in 5).
- Whether a complete (unfiltered) export with `audience` marks is acceptable for exchange, or only the public form.
- Review of the contextual description in the change package: scope (path from the root and the
  linked subtrees), `dao identifier` = METS ID of the linked part or the AIP code.
- Profile example in "Pomůcky k části archivního souboru" still shows `MightExist`.
