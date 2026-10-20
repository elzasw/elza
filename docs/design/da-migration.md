# DA Migration: Deprecation of arr_dao Structures

## 1. Background

ELZA has two models for digital archival objects. The `da_xxx` entities (`DaAip`, `DaDao`, …)
manage AIPs and DAOs received from an external digital archive (DA). The older `arr_dao` entity
family (`ArrDao`, `ArrDaoFile`, `ArrDaoPackage`, `ArrDaoBatchInfo`) is still written by the SOAP/WSDL
integration and by package-DAO linking. Both models meet in the shared link table `arr_dao_link`.

Filesystem repositories use neither: they persist no entities at all and link straight to
`(repository, path)` (see `fs-repo-analysis.md`). The goal is to remove the `arr_dao` family; what
stands in the way is the WSDL data (Phase 4, section 3) and the removal itself (Phase 5,
section 4).

## 2. Current State

### 2.1 The link model: one spine, three target shapes

`ArrDaoLink` is an abstract JOINED base — the codebase's usual generic-type + specialization
pattern (`ArrData`, `ArrItem`, `ArrRequest`):

| Entity | Table | Own columns | Notes |
|--------|-------|-------------|-------|
| `ArrDaoLink` (abstract base) | `arr_dao_link` | `node_id` NOT NULL, `create_change_id` NOT NULL, `delete_change_id`, `link_type` | shared spine |
| `ArrLegacyDaoLink` | `arr_legacy_dao_link` | `dao_id` NOT NULL, `scenario` | created only by the SOAP/WSDL flow and package-DAO linking; `scenario` names the block of `arr_dao.attributes` the node's items were provided from (the kind of level and the counterpart's locked metadata, see §3.5.1); removed in Phase 5 |
| `ArrDaLink` | `arr_da_link` | `aip_id` NOT NULL, `da_dao_id` | `da_dao_id` NULL = the whole AIP (`LinkType.AIP`) |
| `ArrFsLink` | `arr_fs_link` | `digital_repository_id` NOT NULL, `path` | `path` NULL = the repository root; canonical '/' form |

Every link is a **container reference plus an optional member reference** — `aip_id` + `da_dao_id`
for DA, `digital_repository_id` + `path` for filesystem — where a NULL member means "the whole
container". `link_type` (base) encodes that granularity. Exclusivity of the target shapes is
structural (per-subtype NOT NULL constraints); no DB constraint prevents child rows in two subtype
tables for one `dao_link_id` — Hibernate never creates that state, and Phase 5 validation includes
a verification query for it. The rationale for this model is in `fs-repo-analysis.md` §3.4.

Filesystem linking creates no `ArrDao`: `DaoService.createFsDaoLink` mints the `ArrFsLink`
directly and enforces `multiple_links` as "is `(repository, path)` already live-linked". The
arrangement DAO panel renders fs links as synthesized VOs (wire id `-daoLinkId`, files listed
live), the fs browser resolves link state from `arr_fs_link` with no join, the native XML export
writes fs links as `DaoInfo(repositoryCode, path)`, and the print model constructs `Dao` from
either subtype. Node deletion severs links of all three subtypes.

The node cache carries legacy and fs links (DA links are not cached); polymorphic JSON uses
`@JsonTypeInfo(Id.CLASS)` per the `ArrData` precedent, so renaming the link classes invalidates
cached rows.

### 2.2 Invariant: DA repositories carry no legacy data (Phase 2)

A repository of type `DA` never holds `arr_dao` rows; its links are always `ArrDaLink`. The
restructuring changeset `20260811180000` enforces this as a HALT precondition, together with "no
link carries both or neither target group". A halt means "inspect the data" (a mis-typed
repository, or a case for Phase 4), not a migration bug.
`digital_repository_type` is the authoritative discriminator of repository kinds.

The switch of a WSDL repository (§3.2.1) relaxes this invariant to: *a DA repository has no live
legacy link once its migration is finished*.

### 2.3 Endpoints

All 10 DAO endpoints from 3.3.x remain functional. `createDaoLink`
(`PUT /daos/{fundVersionId}/{daoId}/{nodeId}/create`) serves package DAOs only and rejects
filesystem repositories; `fundFsCreateDAOLink` is the filesystem path and returns `daoLinkId`;
`daoChangeLinkScenario` and the sync endpoints are legacy-only by nature. `AipController` and
`DaoController` serve the DA model.

### 2.4 Changesets

New changesets go to the **last** file included by
`elza-core/src/main/resources/db/changelog/db.changelog-master.yaml`; all earlier files are frozen.
The former `db.elza-da.xml` is the frozen `db.elza-3-part-02.xml` with a pinned logical path. The
link hierarchy is created by changesets `20260811180000`–`20260811180005` (guard, subtype tables,
column moves, conversion of fs links, cache invalidation); `20260812110000` removes the `arr_dao`
rows that filesystem repositories held.

## 3. Phase 4: WSDL repository data

WSDL is the only repository type whose legacy `arr_dao` data may ever need conversion onto the DA
model — the SOAP import (`DaoCoreServiceWsImpl`) actively writes `arr_dao`/`arr_dao_file` rows
with genuine preservation metadata. Genuine WSDL repositories are those with
`digital_repository_type = 'WSDL'`. Each of them ends in one of three ways before Phase 5:

- **The counterpart moves to the DA API** — the repository switches its type and every link is
  converted when its AIP arrives (§3.1–§3.8). This is the designed path; it is a proposal, not
  implemented.
- **The WSDL integration is no longer active** — deprecate `DigitalRepositoryType.WSDL` and leave
  its historical links as `ArrLegacyDaoLink` rows until they are archived/removed by product
  decision. Phase 5 cannot run while they exist.
- **The data must move but no counterpart will serve it as AIPs** — ELZA builds DA entities from
  the legacy rows itself (§3.9); the machinery does not exist.

The DID of the DAO web services (the notification of a link, the DAO and digitization requests,
`getDid`) carries the identifier of the unit only.

**Audit first** — count `arr_dao` records and live links per repository, check that the
identifier the counterpart will use as the AIP code is present on every legacy object, and
identify edge cases: orphaned records, NULL foreign keys, invalid JSON in `attributes`.

### 3.1 Starting point

The archive system (the counterpart, for example AIS) pushes digital objects to ELZA through the
SOAP DAO import (`DaoCoreServiceWsImpl`). Each archival unit of the counterpart becomes:

- one `arr_dao_package` + one `arr_dao` with the same `code` = the counterpart's identifier of
  the unit (a number such as `394069`),
- one `ArrLegacyDaoLink` of a `LEVEL` object to a node, mostly with a scenario (§3.5.1),
- description items on that node provided from `arr_dao.attributes` by the scenario, the
  counterpart's ones marked `readOnly`; among them the identifier itself (`ZP2015_AIP_ID`, equal
  to `arr_dao.code`).

The same counterpart then starts serving the DA API. Its `/updates` returns every unit it holds
as an AIP whose `aipId` is the same identifier, and the `AIP_ID` in its PACKAGE-INFO is the same
value again. ELZA identifies an AIP by that value (`da_aip.code`), so the identifier the legacy
object carries and the AIP code are equal strings. That equality is the whole pairing key.

The two integrations cannot run side by side for the same units: once a unit is linked as an AIP,
the WSDL import has nothing to work on (§3.6).

### 3.2 Decisions

#### 3.2.1 The existing repository switches its type

The administrator changes the type of the existing repository from `WSDL` to `DA` (and fills in the
DA url and credentials). The switch is what starts the migration; nothing else is configured.

- The repository keeps its code, its id and every reference to it (`arr_dao_package`, settings,
  permissions). Pairing is scoped to that one repository: the legacy objects and the AIPs are
  rows of the same `digital_repository_id`.
- A repository of type `DA` that still holds live legacy links is **in migration**. This is a
  derived state, not a stored flag: it ends when the last legacy link is converted (§3.7).
- The switch is one-way. `ExternalSystemService.update` allows the type change only
  `WSDL → DA`, and refuses `DA → WSDL` and any other type change of a repository that holds data.

**Rejected alternative: a new DA repository pointing to its WSDL predecessor** (a
`legacy_repository_id` column). It keeps the "DA repository holds no `arr_dao`" invariant
literally, but needs a new column, an admin field and a rule for which predecessor wins; the WSDL
repository would stay live and accept SOAP imports into data that has already moved. The switch
models what really happens - the same counterpart changes its protocol.

The invariant of §2.2 is relaxed accordingly. The repository's `arr_dao` rows stay until Phase 5,
because closed `ArrLegacyDaoLink` rows reference them and the node history
(`RevertingChangesService`) reads them.

#### 3.2.2 Pairing by the identifier comes before every other placement

A legacy link is a link a person (or the counterpart) has already confirmed, so it outranks the
UUID match and `DA_MATCH` of [da-aip-matching.md](da-aip-matching.md):

1. AIP code = `arr_dao.code` of a live legacy link in the same repository → migrate (§3.3).
2. Otherwise the existing automatic linking: UUID match, then `DA_MATCH`.

Pairing needs only the identity and the fund of the AIP, both known after PACKAGE-INFO. It
therefore runs right after `PackageInfoService.processPackageInfo`, before any metadata download.
`DaAipAutoLinkService.linkReceivedAip` already skips an AIP with a live DA link, so it leaves a
migrated AIP alone without a new rule.

#### 3.2.3 An AIP is linked within one fund

An AIP must never end up linked in two funds. If the fund resolved from PACKAGE-INFO
(`DaAipReferenceResolver`) differs from the fund of the node holding the legacy link, nothing is
linked and the user is told (§3.4). The legacy fund is never used to *resolve* the AIP's
fund: fund resolution stays the one defined in `DaAipReferenceResolver`.

#### 3.2.4 Scenarios are not received; the package and the rules replace them

The DA API carries no scenario. What a scenario did is split in two and both halves live in
ELZA (§3.5):

- the **description** of the unit comes in the package (EAD) and is mapped by the `DA_IMPORT`
  script when ELZA creates the level;
- the **kinds of level a package may stand for** are computed by the rules from the package
  itself, and restrict where the whole package may be linked (for example never to a series).

### 3.3 Migrating one AIP

`DaLegacyMigrationService.migrate(DaAipState state)`, called per AIP after PACKAGE-INFO, in its own
transaction (a failure is recorded on the AIP like other processing failures and does not stop
the batch).

1. **Applies only** when the repository is of type `DA`, the AIP is valid and has no live DA link,
   and a valid `arr_dao` with `code = da_aip.code` in this repository has a live
   `ArrLegacyDaoLink`. Otherwise it does nothing.
2. **Fund check.**
   - Fund unresolved (`UNKNOWN_FUND` / `UNKNOWN_INSTITUTION`): do nothing; the existing problem is
     shown. The migration runs again when the references are remapped (`REMAP_REFERENCES`,
     `DaService.remapReferences`) or a new version arrives.
   - Fund differs from the node's fund: record `LEGACY_LINK_FUND_MISMATCH` (§3.4), do nothing
     else.
3. **Convert**, in one `ArrChange` of type `CREATE_DAO_LINK` on the node:
   - create the `ArrDaLink` of the whole AIP (`LinkType.AIP`, `da_dao_id` NULL) through
     `DaService.linkToNode` (lock, multiple-links policy, link state refresh). The level-kind
     restriction of §3.5.3 is not evaluated here: it needs the metadata package, which is not
     downloaded yet, and the legacy link was made under the counterpart's scenario, which only
     ever offered allowed kinds;
   - close the `ArrLegacyDaoLink` with the same change as its `delete_change`. The level is
     **not** deleted - the user path `DaoService.deleteDaoLink` deletes the whole level of a
     `LEVEL` object and must not be used. The link's scenario goes with it; the kind of level the
     scenario made the node is what the node is;
   - no `UNLINK` notification to the repository (it no longer speaks WSDL);
   - unlock the scenario items: the items of the node that the link's scenario provided from
     `arr_dao.attributes` (matched as `DaoSyncService.isItemFromScenario` does) get
     `readOnly = false`, values unchanged. Other read-only items stay locked - `SealUnitId` also
     sets `readOnly`. The identifier item (`ZP2015_AIP_ID`) is kept as an ordinary item: the link
     now carries the identifier, but the item keeps it searchable in the description;
   - refresh the node cache (`DaoService.updateNodeCacheDaoLinks`) and publish
     `DAO_LINK_DELETE` + `DAO_LINK_CREATE`.
4. **Metadata.** With `on_received = DOWNLOAD_METADATA` the metadata package is requested as for
   any received AIP; the DAO structure is built from it. The auto-link step then finds the live
   whole-AIP link and stops. The legacy files (`arr_dao_file`) are not converted: the files come
   from the DA package.

Why a whole-AIP link: one legacy object stands for one unit of the counterpart and was linked as
a whole; the AIP of that unit is the same thing.

Why the items stay: they are the description of the unit as the user sees it, often edited
around; removing them would change the description. Only the lock goes, because no scenario will
ever refresh them again (§3.5.2).

### 3.4 Fund mismatch

New constant `AipProblemType.LEGACY_LINK_FUND_MISMATCH` (derived, not a processing failure: it is
recomputed whenever the references are resolved). Description: the AIP belongs to fund *X*, its
unit is linked through the former WSDL integration in fund *Y*. Shown in the AIP list and
filterable like the other problem types; added to the enum in `elza-openapi.yml` as well.

While the mismatch lasts:

- the legacy link stays live - the user still sees the object where it was;
- `DaAipAutoLinkService` does not place the AIP (a UUID match or `DA_MATCH` in fund *X* would
  link one unit in two funds);
- it is resolved by fixing the fund on either side: a new AIP version with the right fund, a fund
  mapping change followed by `REMAP_REFERENCES`, or the user moving/removing the legacy link.
  The next pairing attempt then migrates or places the AIP.

### 3.5 Scenarios

#### 3.5.1 What a scenario of the WSDL import is

`arr_dao.attributes` holds an `Items` XML. A `_ELZA_SCENARIO` string item opens a named block;
the items that follow it, up to the next marker, are what the object's node gets when that
scenario is chosen. `DaoSyncService.DaoDesctItemProvider` provides them when the link is made,
`changeScenario` removes the items of the old scenario and provides those of the new one, and the
user switches in the DAO panel (`ScenarioDropdown`, `fundSubNodeDaoChangeScenario`). An object
without markers has one implicit scenario: all its items.

A scenario therefore does two things at once:

1. It names the **kind of level** the object may stand for, with the fixed items that kind
   needs. Typically an object offers two scenarios - `jednotlivost` (`ZP2015_LEVEL_TYPE` = item,
   `ZP2015_UNIT_TYPE` = other) and `složka` (folder, `ZP2015_FOLDER_TYPE` = units) - and the user
   chooses; where the counterpart knows the kind itself (an item with a specific unit type -
   drawing, manuscript, ... - or a folder), the object offers no choice. A linked node is a leaf
   under a series or a folder, never a series.
2. It carries the **counterpart's metadata** and locks them: the identifier (`ZP2015_AIP_ID`),
   the storage unit (`ZP2015_STORAGE_ID`), the kind (`ZP2015_LEVEL_TYPE`) and the unit or folder
   type are `readOnly` on every linked node; the dates (`ZP2015_UNIT_DATE`), and on some objects
   a title (`ZP2015_CONTENT`, `ZP2015_NAME`), come editable. A re-import of the object refreshes
   the locked items. Everything else on those nodes - usually far more (entity roles, content,
   extent, serial numbers, languages, other identifiers, ...) - is written by the users in ELZA.

#### 3.5.2 The description comes in the package

The values the scenario carried come in the package's inherent description (EAD-INHERENT.xml) and
are mapped by the `DA_IMPORT` script of the rule set, which already does this for a package that
ELZA imports as new levels: title, dates, identifiers, and the kind of level derived from the
package (content type, `TYPE` of the METS div, EAD level).

Rules that follow from the decisions of [da-aip-matching.md](da-aip-matching.md) and
[da-aip-new-version.md](da-aip-new-version.md):

- Items are written **only when ELZA creates the level**. A level that already exists - matched
  by UUID, found by `matchBy`, or holding a migrated legacy link - is not enriched: the
  description in ELZA is the master.
- Nothing is locked. The counterpart owns no field of the description any more, so there are no
  `readOnly` items of the DA model, and the description of a new version of the AIP does not
  change the node (the open case D of `da-aip-new-version.md` stays open). This is the
  behavioural change the switch brings: after it, a change of the storage unit or of the dates on
  the counterpart's side is visible in the package, not in the description.
- The identifier is not an item: the link carries the AIP code, and the DAO panel shows it.

What the package must carry for this to hold - the sample packages of the converter carry only
part of it (title, dates, source identifier, sometimes `<container>`):

| Scenario item | In the package | `DA_IMPORT` |
|---|---|---|
| `ZP2015_NAME` / `ZP2015_CONTENT` | `<unittitle>` | mapped |
| `ZP2015_UNIT_DATE` | `<unitdatestructured>` | mapped |
| `ZP2015_OTHER_ID` (source id) | `<unitid localtype="ZDROJ_ID">` | mapped, also the `matchBy` key of loose files |
| `ZP2015_STORAGE_ID` | `<container>` | read only as the name of the collecting series; the item is a reference to a shared structured object, and finding one by its identifier is not implemented |
| `ZP2015_UNIT_TYPE`, `ZP2015_FOLDER_TYPE` | not present | to be agreed with the converter (the Czech EAD3 profile element for the unit type), then mapped |
| `ZP2015_LEVEL_TYPE` | derived (§3.5.3) | decided by the script |
| `ZP2015_AIP_ID` | `AIP_ID` of PACKAGE-INFO | the link, no item |

#### 3.5.3 The kinds of level a package may stand for

The scenario list (`jednotlivost|složka`) said which kinds of level the object may be. In the DA
model the same knowledge is computed by the rules from the package, so it needs no protocol:

- `DA_IMPORT` decides the kind of the package's top described unit (`result.level().item(LEVEL_TYPE,
  ...)`) and may declare named **alternatives** - the open item of
  [da-aip-matching.md](da-aip-matching.md): `.alternative("Složka").item(LEVEL_TYPE,
  "ZP2015_LEVEL_FOLDER")`. The primary kind plus its alternatives are the **allowed kinds** of the
  package. For ZP2015 loose files: item, alternative folder - the two scenarios of today.
- **Creating** a level for the package uses the primary kind; the connect pages offer the
  alternatives as a choice, the way the new-level dialog offers its scenarios.
- **Linking the whole package** (`LinkType.AIP`) to an existing node is allowed only when the
  node's kind is one of the allowed kinds. The kind of a node is read as the items the plan
  names (the item type and specification of the level-kind item, `ZP2015_LEVEL_TYPE` in ZP2015),
  so the check is generic: it compares the plan's kind items with the node's items and knows no
  item type of its own.
- A package below the top unit (a part link, `PART_AIP`, `COMPONENT_AIP`) is not restricted by
  this rule; the structure import decides those levels itself.

Enforcement, in `DaService.linkToNode` for `LinkType.AIP`:

- The allowed kinds come from the plan of the stored metadata package (`DaImportService` reopens
  it, `DaImportPlanner.plan`, the root of the plan). **Without the metadata package a manual
  whole-package link is refused** ("nejprve stáhněte metadata balíčku") - the restriction is a
  rule of the archival description and timing must not bypass it; with
  `on_received = DOWNLOAD_METADATA` the metadata are there before a user gets to link. A rule set
  without a `DA_IMPORT` script restricts nothing.
- A refused manual link is a `BusinessException` (`ArrangementCode.DAO_LINK_LEVEL_NOT_ALLOWED`)
  naming the node's kind and the allowed ones; the connect pages show it as they show
  `DAO_ALREADY_LINKED`.
- Automatic linking after a UUID match (`DaAipAutoLinkService`) records the derived problem
  `AipProblemType.LINK_LEVEL_NOT_ALLOWED` on the AIP instead of linking - the counterpart says
  the package *is* that unit, but the unit is of a kind the package cannot be, and a person has
  to look. Placement by `DA_MATCH` creates its own level of the primary kind and is never refused.
- The migration of §3.3 is the one exception: it links without the check.

A node's kind can be changed afterwards by editing its level-kind item; the link does not follow.
Validation would report it if the arrangement rules could see the DA links of a node - they see
none today (no DAO fact in the Drools model); that is open work.

#### 3.5.4 The DAO panel

A DA link has no scenario and no scenario switch. The `ScenarioDropdown` and the endpoint
`daoChangeLinkScenario` serve legacy links only, and on a switched repository the switch is
refused (§3.6). The choice they offered - item or folder - moves to the connect pages as the
alternatives of §3.5.3, and to ordinary editing of the level-kind item afterwards.

### 3.6 The WSDL side of a switched repository

- `DaoCoreServiceWsImpl.getRepository` refuses a repository that is not of type `WSDL` with a SOAP
  fault naming the switch; `removeDao` / `removePackage` (looked up by code globally) refuse
  objects of such a repository. Nothing is written into the legacy tables of a DA repository.
- Outgoing WSDL calls (`DaoService` link/unlink requests, `DaoSyncService.syncDaoLink`) are sent
  only for repositories of type `WSDL`; for a switched repository the url is the DA's, and a SOAP
  call would go to the wrong service.
- A legacy link not yet migrated stays visible in the DAO panel (`SubNodeDao`), but the scenario
  change (`fundSubNodeDaoChangeScenario`) is refused for it - the scenario data would never be
  refreshed.

### 3.7 Cut-over and progress

1. The counterpart stops the WSDL push. The DA API is ready and its packages carry the description
   of §3.5.2.
2. Administrator switches the repository `WSDL → DA`, sets url, credentials,
   `on_received = DOWNLOAD_METADATA`, `sync_delay`.
3. `DaScheduler` starts the repository (it reacts to `ArrDigitalRepositoryEvent`); there is no
   `da_remote_repository_sync` row, so the first `/updates` runs without `nextQuery` and returns
   every AIP. Each page is queued as `PACKAGE_INFO`; pairing runs per received package.
4. Progress = live legacy links left in the repository:

   ```sql
   SELECT count(*)
     FROM arr_dao_link l
     JOIN arr_legacy_dao_link ll ON ll.dao_link_id = l.dao_link_id
     JOIN arr_dao d ON d.dao_id = ll.dao_id
     JOIN arr_dao_package p ON p.dao_package_id = d.dao_package_id
    WHERE p.digital_repository_id = :repo AND l.delete_change_id IS NULL;
   ```

   The repository administration shows this count while it is above zero ("migrace z WSDL: N
   nepřevedených napojení").

Pairing looks up `arr_dao` by `(code)` within the repository; `arr_dao.code` has no index today
(only `arr_dao_package.code` is unique), so the change adds one.

For scale: a repository in production can hold tens of thousands of legacy links, all `LEVEL`
objects, one object per package, each node with `ZP2015_AIP_ID` equal to the object code. The
`DAO_LEVEL_IMPORT` setting collects objects the counterpart sends without a target in a series
"Importováno - <date>" under the root, the same role `DA_MATCH` plays for loose files.

### 3.8 Testing on a copy

A switched repository cannot go back (§3.2.1), and the counterpart's test DA can serve only
one ELZA. A test run therefore uses a restorable copy of the database: switch, let the first load
run, check the progress count and the problem list, restore the copy to repeat.

### 3.9 Converting legacy data without a counterpart

For legacy data no counterpart will ever serve as AIPs (a WSDL repository whose data must still
move, or DA-type legacy rows should the Phase 2 guard ever find any), ELZA would build the DA
entities itself:

1. Define the metadata mapping of `ArrDao.attributes` JSON keys and pre-scan for
   malformed/unmappable `attributes`. The DA model has no per-DAO description items (there is no
   `da_dao_item` entity), so the target of these metadata must be decided first.
2. Extend `DaService` so `DaAip`/`DaDao` can be created from programmatic input (not only from
   METS/PREMIS import).
3. Hierarchy convention: each migrated flat `ArrDao` becomes one `DaDao` (LOGICAL) + one `DaDao`
   (REPRESENTATION) linked via `DaDaoRelation`; `ArrDaoFile` → `DaDaoFile`.
4. AIP granularity: one `DaAip` per `ArrDao` (each digital object becomes its own AIP), one
   `DaChange` (type=`AIP_CREATE`) per migrated AIP.
5. Convert each link: insert the `arr_da_link` child, delete the `arr_legacy_dao_link` child,
   and keep the deleted child rows in a backup table until validated (a row cannot carry both
   target groups).

## 4. Phase 5: Remove old structures

1. Verify `arr_legacy_dao_link` is empty: DA-type links are `ArrDaLink` rows (Phase 2 invariant),
   WSDL-type links were converted or retired per Phase 4, and filesystem links are `ArrFsLink`
   rows. Also verify no `dao_link_id` has children in two subtype tables.
2. Drop table `arr_legacy_dao_link` and entity `ArrLegacyDaoLink`.
3. Drop tables `arr_dao_file`, `arr_dao`, `arr_dao_package`, `arr_dao_batch_info` (they hold only
   WSDL/legacy data resolved by Phase 4).
4. Remove Java entities `ArrDao`, `ArrDaoFile`, `ArrDaoPackage`, `ArrDaoBatchInfo`; repositories
   `DaoRepository`, `DaoFileRepository`, `DaoPackageRepository`, `DaoBatchInfoRepository`,
   `ArrLegacyDaoLinkRepository`.
5. Remove old endpoints from `ArrangementController` (or redirect to DA equivalents); remove
   `arr_dao` creation methods from `DaoServiceInternal`; retire the scenario mechanism
   (legacy-only): `DaoSyncService.DaoDesctItemProvider` / `changeScenario` / `matchScenario`,
   the endpoint `daoChangeLinkScenario`, and in the client `ScenarioDropdown` with
   `fundSubNodeDaoChangeScenario` and the `scenarios` field of `ArrDaoVO`.
6. Update tests, API DTOs, and frontend components referencing old entities. Known readers of
   `ArrDao`/`ArrDaoFile` beyond the DAO services (audit before dropping): node cache
   (`DaoService.updateNodeCacheDaoLinks` → `ArrangementCacheService.updateDaoLinks`), native XML
   export (`dataexchange/output/sections/DaoLoader`, `LevelInfoLoader`), print model
   (`print/Dao.java`), SOAP WS (`ws/core/v1/daoservice/DaoCoreServiceWsImpl`), the arrangement
   DAO panel VOs (`ClientFactoryVO.createDaoList`), and the "DAO level" marker in
   `LevelTreeCacheService`.

## 5. Risks

| Risk | Mitigation |
|------|------------|
| WSDL integration disruption | Phase 4 audits each WSDL repository before it is switched, deprecated or converted |
| A unit served through WSDL after it was migrated | The SOAP import refuses a switched repository (§3.6); the switch is one-way |
| Malformed `attributes` JSON | Only relevant to the conversion of §3.9; pre-scan and report unmappable attributes before migrating |
| Data loss during a Phase 4 migration | The switch closes legacy links in an `ArrChange` instead of deleting them, so node history keeps them; §3.9 runs in a transaction and keeps the replaced `arr_legacy_dao_link` rows in a backup table until validated |
| Phase 2 premise wrong on some installation (DA-type repository with legacy `arr_dao` rows) | The guard changeset HALTs the migration before the restructure; inspect the data (mis-typed repository vs. §3.9) |
| Cached link JSON binds to class names (`@JsonTypeInfo(Id.CLASS)`) | Renaming the link entity classes invalidates `arr_cached_node` rows — pair any rename with a cache-invalidation changeset (as in `20260811180000`–`20260811180005`) |

## Open work

- **Phase 4, switch of a WSDL repository** — all of §3.1–§3.8 is unimplemented: type switch rule
  in `ExternalSystemService.update`, `DaLegacyMigrationService` and its call after PACKAGE-INFO
  and after `REMAP_REFERENCES`, the auto-link exclusion for a mismatched AIP,
  `LEGACY_LINK_FUND_MISMATCH` and `LINK_LEVEL_NOT_ALLOWED` (Java + OpenAPI + UI label), the WSDL
  refusals of §3.6, the index on `arr_dao.code`, the progress count in the administration, tests
  (pairing, mismatch, unresolved fund, read-only items, refused SOAP import, refused link kind).
  Once implemented, update §2.2 and the Phase 2 guard.
- §3.5.3 depends on the alternatives of `DA_IMPORT` (open item of da-aip-matching.md) and on
  the level-kind check in `linkToNode`; `DaImportPlan` exposes the root's items but no notion of
  alternatives yet.
- The package must carry the unit type and the folder type (§3.5.2): agree the EAD element
  with the converter, then map it in `DA_IMPORT.groovy`.
- Importing the storage unit from `<container>` as a reference to the shared structured object
  needs a lookup of a structured object by its identifier (refused today, see
  da-aip-matching.md).
- Whether a new version of the AIP should update the description it created (case D of
  `da-aip-new-version.md`) - the only part of the scenario mechanism (refresh of locked items)
  that has no counterpart after the switch.
- DA links visible to the arrangement validation rules, so a linked node changed to a kind the
  package cannot be is reported.
- Legacy links whose AIP never arrives (the counterpart does not report the unit) stay legacy;
  what to do with them once the first load is complete is not decided (deprecate, or §3.9).
- **Phase 4, other WSDL repositories** — decide per repository: deprecate, or convert by §3.9
  (the machinery does not exist).
- **Phase 5** — remove `ArrLegacyDaoLink` and the `arr_dao` family.
- Optional: backend-neutral `LinkType` names (`CONTAINER`/`PART`/`COMPONENT` instead of
  `AIP`/`PART_AIP`/`COMPONENT_AIP`); the enum is part of the OpenAPI contract and the React client,
  so the rename is API churn with no functional gain until something consumes fs `link_type`.
- Optional, low priority: rename `arr_dao_link` to `da_dao_link`.
