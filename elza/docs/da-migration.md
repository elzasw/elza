# DA Migration: Deprecation of arr_dao Structures

## 1. Background

The `da-main` branch introduced `da_xxx` entities for managing digital archival objects (AIPs,
DAOs) from external digital archives. The original `arr_dao` entity family was not removed — both
models coexist and meet in the shared link table `arr_dao_link`.

As of 2026-08-12 the link model is an entity hierarchy (see §2), filesystem links no longer use
`arr_dao` at all, and the remaining migration work is the WSDL decision (Phase 4) and the final
removal of the `arr_dao` family (Phase 5).

## 2. Current State

### 2.1 The link model: one spine, three target shapes

`ArrDaoLink` is an abstract JOINED base — the codebase's usual generic-type + specialization
pattern (`ArrData`, `ArrItem`, `ArrRequest`):

| Entity | Table | Own columns | Notes |
|--------|-------|-------------|-------|
| `ArrDaoLink` (abstract base) | `arr_dao_link` | `node_id` NOT NULL, `create_change_id` NOT NULL, `delete_change_id`, `link_type` | shared spine |
| `ArrLegacyDaoLink` | `arr_legacy_dao_link` | `dao_id` NOT NULL, `scenario` | created only by the SOAP/WSDL flow and package-DAO linking; deleted in Phase 5 |
| `ArrDaLink` | `arr_da_link` | `aip_id` NOT NULL, `da_dao_id` | `da_dao_id` NULL = the whole AIP (`LinkType.AIP`) |
| `ArrFsLink` | `arr_fs_link` | `digital_repository_id` NOT NULL, `path` | `path` NULL = the repository root; canonical '/' form |

Every link is a **container reference plus an optional member reference** — `aip_id` + `da_dao_id`
for DA, `digital_repository_id` + `path` for filesystem — where a NULL member means "the whole
container". `link_type` (base) encodes that granularity. Exclusivity of the target shapes is
structural (per-subtype NOT NULL constraints); no DB constraint prevents child rows in two subtype
tables for one `dao_link_id` — Hibernate never creates that state, and Phase 5 validation includes
a verification query for it.

Filesystem linking creates no `ArrDao`: `DaoService.createFsDaoLink` mints the `ArrFsLink`
directly and enforces `multiple_links` as "is `(repository, path)` already live-linked". The
arrangement DAO panel renders fs links as synthesized VOs (wire id `-daoLinkId`, files listed
live), the fs browser resolves link state from `arr_fs_link` with no join, the native XML export
writes fs links as `DaoInfo(repositoryCode, path)`, and the print model constructs `Dao` from
either subtype. Node deletion severs links of **all three** subtypes (before the split, DA links
silently survived node deletion).

The node cache carries legacy and fs links (DA links were never cached); polymorphic JSON uses
`@JsonTypeInfo(Id.CLASS)` per the `ArrData` precedent, so renaming the link classes invalidates
cached rows.

### 2.2 Old endpoints

All 10 DAO endpoints from 3.3.x remain functional. `createDaoLink`
(`PUT /daos/{fundVersionId}/{daoId}/{nodeId}/create`) serves package DAOs only and rejects
filesystem repositories; `fundFsCreateDAOLink` is the filesystem path and returns `daoLinkId`;
`daoChangeLinkScenario` and the sync endpoints are legacy-only by nature. `AipController` and
`DaoController` serve the DA model.

### 2.3 Completed migration steps (history)

| Step | Shipped | Substance |
|------|---------|-----------|
| fs-repo repair wave (#9944) | 2026-07-28 → 2026-08-06 | the A1–A9 defects of `fs-repo-analysis.md` (write-ups in its git history), per-(repository, fund) packages, '/' path normalization |
| Step 3a — no persisted file trees | 2026-08-10 | linking writes no per-file rows; DAO panel reads disk live; `arr_dao_file_group` dropped (changesets `20260810120000/1`) |
| N4 — repository type authoritative | 2026-08-11 | `digitalRepositoryType` is the discriminator; `file://` prefix stripped (changeset `20260811135500`) |
| Phase 2 — DA repositories carry no legacy data | 2026-08-11 | implemented as a HALT precondition of the 3b wave: legacy `arr_dao` rows under a DA-type repository, or a link with both/neither target groups, stop the migration for inspection |
| Step 3b — link hierarchy + fs conversion | 2026-08-11 | changesets `20260811180000-180005`: subtype tables, column moves, fs links converted from `ArrDao.code`, cache invalidation; rationale in `fs-repo-analysis.md` §3.4 |
| Changelog reorganization | 2026-08-12 | sequential part files, one open file (see `db.changelog-master.yaml`); new changesets go to the **last** included file |
| fs orphan cleanup | 2026-08-12 | changeset `20260812110000`: the fs `ArrDao` anchors, their per-fund packages and their (meaningless) request references deleted — filesystem repositories now persist **zero** entities and no longer appear in the package/unassigned tabs |

The rejected alternative — migrating filesystem repositories onto `DaAip`/`DaDao` — and its
reasoning remain recorded in `fs-repo-analysis.md` §3 (decision record).

## 3. Remaining Plan

### Phase 4: WSDL Repository Data — Deprecate or Migrate

WSDL is the only repository type whose legacy `arr_dao` data may ever need conversion onto the DA
model — the SOAP import (`DaoCoreServiceWsImpl`) still actively writes `arr_dao`/`arr_dao_file`
rows with genuine preservation metadata. Identify genuine WSDL repositories by
`digital_repository_type = 'WSDL'` (authoritative since N4). Decide before Phase 5:

- **If WSDL integration is no longer active**, deprecate `DigitalRepositoryType.WSDL` and leave
  its historical links as `ArrLegacyDaoLink` rows until they are archived/removed by product
  decision — Phase 5 cannot run while they exist.
- **If it is still used**, build the conversion machinery here (it was never built — the original
  Phase 2 turned out to have an empty input set).

**Audit first** — count `arr_dao` records per repository type (classified by
`digital_repository_type`), identify edge cases: orphaned records, NULL foreign keys, invalid
JSON in `attributes`.

**Contingency note — the preserved conversion recipe** (applies to WSDL migration, or to DA-type
legacy rows should the Phase 2 guard ever find any):

1. Define the metadata mapping: `ArrDao.attributes` JSON keys → `RulItemType`/`RulItemSpec` pairs
   for `DaDaoItem` conversion; pre-scan for malformed/unmappable `attributes` first.
2. Extend `DaService` so `DaAip`/`DaDao` can be created from programmatic input (not only from
   METS/EAD import).
3. Hierarchy convention: each migrated flat `ArrDao` becomes one `DaDao` (LOGICAL) + one `DaDao`
   (REPRESENTATION) linked via `DaDaoRelation`; `ArrDaoFile` → `DaDaoFile`.
4. AIP granularity: one `DaAip` per `ArrDao` (each digital object becomes its own AIP), one
   `DaChange` (type=`AIP_CREATE`) per migrated AIP.
5. Convert each link: insert the `arr_da_link` child, delete the `arr_legacy_dao_link` child,
   and keep the deleted child rows in a backup table until validated (a row cannot carry both
   target groups).

### Phase 5: Remove Old Structures

1. Verify `arr_legacy_dao_link` is empty. The three routes: DA-type links were born as
   `ArrDaLink` rows (Phase 2 guard proved there was nothing to convert), WSDL-type links were
   converted or retired per Phase 4, and filesystem links are `ArrFsLink` rows (step 3b). Also
   verify no `dao_link_id` has children in two subtype tables.
2. Drop table `arr_legacy_dao_link` and entity `ArrLegacyDaoLink`.
3. Drop tables: `arr_dao_file`, `arr_dao`, `arr_dao_package`, `arr_dao_batch_info`. Filesystem
   rows are already gone (cleanup changeset `20260812110000`), so at this point the tables hold
   only WSDL/legacy data resolved by Phase 4.
4. Remove Java entities: `ArrDao`, `ArrDaoFile`, `ArrDaoPackage`, `ArrDaoBatchInfo`; repositories
   `DaoRepository`, `DaoFileRepository`, `DaoPackageRepository`, `DaoBatchInfoRepository`;
   `ArrLegacyDaoLinkRepository`.
5. Remove old endpoints from `ArrangementController` (or redirect to DA equivalents); remove
   `arr_dao` creation methods from `DaoServiceInternal`; retire `DaoSyncService` scenarios
   (legacy-only).
6. Update tests, API DTOs, and frontend components referencing old entities. Known readers of
   `ArrDao`/`ArrDaoFile` beyond the DAO services (audit before dropping): node cache
   (`DaoService.updateNodeCacheDaoLinks` → `ArrangementCacheService.updateDaoLinks`), native XML
   export (`dataexchange/output/sections/DaoLoader`, `LevelInfoLoader`), print model
   (`print/Dao.java`), SOAP WS (`ws/core/v1/daoservice/DaoCoreServiceWsImpl`), the arrangement
   DAO panel VOs (`ClientFactoryVO.createDaoList`), and the "DAO level" marker in
   `LevelTreeCacheService`.

### Deferred cleanups (optional, any time)

- **`LinkType` backend-neutral naming** (`CONTAINER`/`PART`/`COMPONENT` instead of
  `AIP`/`PART_AIP`/`COMPONENT_AIP`): deferred in step 3b because the enum leaks into the OpenAPI
  contract and the React client — the rename is API churn with no functional gain until something
  consumes fs `link_type`.
- **Renaming `arr_dao_link` to `da_dao_link`**: optional, low priority.

## 4. Migration Changesets

New changesets go to the **last** file included by
`elza-core/src/main/resources/db/changelog/db.changelog-master.yaml` (`db.elza-3-part-03.xml` at
the time of writing); all earlier files are frozen. Former `db.elza-da.xml` is the frozen
`db.elza-3-part-02.xml` with a pinned logical path.

## 5. Risks

| Risk | Mitigation |
|------|------------|
| WSDL integration disruption | Phase 4 assesses usage before deciding deprecation vs migration |
| Malformed `attributes` JSON | Only relevant if the Phase 4 contingency (conversion to `DaDaoItem`) runs; pre-scan and report unmappable attributes before migrating |
| Data loss during a Phase 4 migration | Run in transaction; keep the replaced `arr_legacy_dao_link` rows in a backup table until validated |
| Phase 2 premise wrong on some installation (DA-type repository with legacy `arr_dao` rows) | The step 3b guard changeset HALTs the migration before the restructure; a halt means "inspect the data" (mis-typed repository vs. the Phase 4 contingency recipe) |
| Cached link JSON binds to class names (`@JsonTypeInfo(Id.CLASS)`) | Renaming the link entity classes invalidates `arr_cached_node` rows — pair any rename with a cache-invalidation changeset (the step 3b wave shows the pattern) |
