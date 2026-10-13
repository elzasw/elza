# DA Migration: Deprecation of arr_dao Structures

## 1. Background

ELZA has two models for digital archival objects. The `da_xxx` entities (`DaAip`, `DaDao`, …)
manage AIPs and DAOs received from an external digital archive (DA). The older `arr_dao` entity
family (`ArrDao`, `ArrDaoFile`, `ArrDaoPackage`, `ArrDaoBatchInfo`) is still written by the SOAP/WSDL
integration and by package-DAO linking. Both models meet in the shared link table `arr_dao_link`.

Filesystem repositories use neither: they persist no entities at all and link straight to
`(repository, path)` (see `fs-repo-analysis.md`). The goal is to remove the `arr_dao` family; what
stands in the way is the WSDL decision (Phase 4) and the removal itself (Phase 5).

## 2. Current State

### 2.1 The link model: one spine, three target shapes

`ArrDaoLink` is an abstract JOINED base — the codebase's usual generic-type + specialization
pattern (`ArrData`, `ArrItem`, `ArrRequest`):

| Entity | Table | Own columns | Notes |
|--------|-------|-------------|-------|
| `ArrDaoLink` (abstract base) | `arr_dao_link` | `node_id` NOT NULL, `create_change_id` NOT NULL, `delete_change_id`, `link_type` | shared spine |
| `ArrLegacyDaoLink` | `arr_legacy_dao_link` | `dao_id` NOT NULL, `scenario` | created only by the SOAP/WSDL flow and package-DAO linking; removed in Phase 5 |
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
repository, or a case for the conversion recipe of Phase 4), not a migration bug.
`digital_repository_type` is the authoritative discriminator of repository kinds.

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

## 3. Remaining phases

### Phase 4: WSDL repository data — deprecate or migrate

WSDL is the only repository type whose legacy `arr_dao` data may ever need conversion onto the DA
model — the SOAP import (`DaoCoreServiceWsImpl`) actively writes `arr_dao`/`arr_dao_file` rows
with genuine preservation metadata. Genuine WSDL repositories are those with
`digital_repository_type = 'WSDL'`. The decision is needed before Phase 5:

- **If WSDL integration is no longer active**, deprecate `DigitalRepositoryType.WSDL` and leave
  its historical links as `ArrLegacyDaoLink` rows until they are archived/removed by product
  decision — Phase 5 cannot run while they exist.
- **If it is still used**, build the conversion machinery (it does not exist).

The DID of the DAO web services (the notification of a link, the DAO and digitization requests,
`getDid`) carries the identifier of the unit only.

**Audit first** — count `arr_dao` records per repository type (classified by
`digital_repository_type`), identify edge cases: orphaned records, NULL foreign keys, invalid
JSON in `attributes`.

**Conversion recipe** (for WSDL migration, or for DA-type legacy rows should the Phase 2 guard
ever find any):

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

### Phase 5: Remove old structures

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
   `arr_dao` creation methods from `DaoServiceInternal`; retire `DaoSyncService` scenarios
   (legacy-only).
6. Update tests, API DTOs, and frontend components referencing old entities. Known readers of
   `ArrDao`/`ArrDaoFile` beyond the DAO services (audit before dropping): node cache
   (`DaoService.updateNodeCacheDaoLinks` → `ArrangementCacheService.updateDaoLinks`), native XML
   export (`dataexchange/output/sections/DaoLoader`, `LevelInfoLoader`), print model
   (`print/Dao.java`), SOAP WS (`ws/core/v1/daoservice/DaoCoreServiceWsImpl`), the arrangement
   DAO panel VOs (`ClientFactoryVO.createDaoList`), and the "DAO level" marker in
   `LevelTreeCacheService`.

## 4. Risks

| Risk | Mitigation |
|------|------------|
| WSDL integration disruption | Phase 4 assesses usage before deciding deprecation vs migration |
| Malformed `attributes` JSON | Only relevant if the Phase 4 conversion runs; pre-scan and report unmappable attributes before migrating |
| Data loss during a Phase 4 migration | Run in transaction; keep the replaced `arr_legacy_dao_link` rows in a backup table until validated |
| Phase 2 premise wrong on some installation (DA-type repository with legacy `arr_dao` rows) | The guard changeset HALTs the migration before the restructure; inspect the data (mis-typed repository vs. the conversion recipe) |
| Cached link JSON binds to class names (`@JsonTypeInfo(Id.CLASS)`) | Renaming the link entity classes invalidates `arr_cached_node` rows — pair any rename with a cache-invalidation changeset (as in `20260811180000`–`20260811180005`) |

## Open work

- **Phase 4** — audit `arr_dao` data and decide whether WSDL integration is deprecated or migrated.
- **Phase 5** — remove `ArrLegacyDaoLink` and the `arr_dao` family.
- Optional: backend-neutral `LinkType` names (`CONTAINER`/`PART`/`COMPONENT` instead of
  `AIP`/`PART_AIP`/`COMPONENT_AIP`); the enum is part of the OpenAPI contract and the React client,
  so the rename is API churn with no functional gain until something consumes fs `link_type`.
- Optional, low priority: rename `arr_dao_link` to `da_dao_link`.
