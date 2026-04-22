# DA Migration: Deprecation of arr_dao Structures

## 1. Background

The `da-main` branch introduced `da_xxx` entities for managing digital archival objects (AIPs, DAOs)
from external digital archives. The original `arr_dao` entity family was not removed — both structures
coexist, connected through the shared `arr_dao_link` table.

This document describes the current compatibility status and the gradual migration path toward full
deprecation of the old `arr_dao` structures.

## 2. Current Compatibility Status

### 2.1 All Old Endpoints Are Functional

All 10 DAO endpoints from 3.3.x are present and working in da-main:

| Endpoint | Method | Status |
|----------|--------|--------|
| `GET /daopackages/{fundVersionId}` | findDaoPackages | Unchanged |
| `GET /daos/{fundVersionId}` | findDaos | Unchanged |
| `GET /daos/{fundVersionId}/{daoPackageId}` | findDaosByPackage | Unchanged |
| `PUT /daos/{fundVersionId}/{daoId}/{nodeId}/create` | createDaoLink | Unchanged |
| `POST /daos/{fundVersionId}/nodes/{nodeId}/sync` | syncDaoLink | Unchanged |
| `POST /daos/{fundVersionId}/all/sync` | syncDaosByFund | Unchanged |
| `DELETE /daolinks/{fundVersionId}/{daoLinkId}` | deleteDaoLink | Unchanged |
| `POST /requests/{fundVersionId}/dao/add` | daoRequestAdd | Unchanged |
| `POST /daos/{id}/change-scenario` | daoChangeLinkScenario | Unchanged |
| `PUT /fund/{fundId}/fsrepo/.../dao-link` | fundFsCreateDAOLink | Unchanged |

New endpoints were added via `AipController` (10 endpoints) and `DaoController` (2 new endpoints)
for the DA model — these do not interfere with legacy functionality.

### 2.2 Service & Entity Layer

| Component | Status |
|-----------|--------|
| `ArrDao` entity | Identical to 3.3.x |
| `ArrDaoLink` entity | Extended with 3 new nullable fields (`aip`, `daDao`, `linkType`); `dao` field made nullable |
| `DaoRepository` | Identical — all query methods preserved |
| `DaoLinkRepository` | Extended — 4 new query methods, all old methods preserved |
| `DaoServiceInternal` | Identical — all CRUD methods intact |
| `DaoService` | Extended — new DA methods added, all old methods unchanged |
| `FileSystemRepoService` | Identical |
| `DaoSyncService` | Identical |

### 2.3 Tests

All 4 test methods in `DaoCoreServiceTest` are present in both branches. The only change is that
da-main requires setting `DigitalRepositoryType.FILESYSTEM` on the test digital repository.

### 2.4 Known Risk: ArrDaoLink.dao Is Now Nullable

In 3.3.x, `dao_id` in `arr_dao_link` was `NOT NULL`. In da-main, it is nullable to support new DA
links where `da_dao_id` is set instead. Old code paths still set `dao_id`, so this only affects rows
created through the new `AipController` endpoints. Code that iterates over **all** `ArrDaoLink` rows
(not filtered by repository type) should handle `dao == null` on newer rows.

## 3. The Shared Bridge: arr_dao_link

`ArrDaoLink` was extended (changeset `20240717125500`) to serve both models:

| Column | Target | Old/New | Nullable |
|--------|--------|---------|----------|
| `node_id` | `arr_node` | shared | NOT NULL |
| `dao_id` | `arr_dao` | old | nullable (was NOT NULL) |
| `create_change_id` | `arr_change` | shared | NOT NULL |
| `delete_change_id` | `arr_change` | shared | nullable |
| `scenario` | — | old | nullable |
| `aip_id` | `da_aip` | **new** | nullable |
| `da_dao_id` | `da_dao` | **new** | nullable |
| `link_type` | enum | **new** | nullable |

A row with `dao_id != NULL, da_dao_id = NULL` is a legacy link.
A row with `da_dao_id != NULL` is a new DA link (`dao_id` may be NULL).

After migration, `arr_dao_link` will be evolved in place — drop `dao_id` and `scenario` columns,
keep the table as the universal DAO-to-node link table. Renaming to `da_dao_link` is optional
and low priority.

## 4. Migration Plan (Gradual)

Migration proceeds per repository type. Both models operate in parallel during the transition.
Old endpoints remain functional until Phase 5.

### Phase 1: Prepare (no data changes)

1. **Audit existing data** — Count `arr_dao` records per repository type (DA, FILESYSTEM, WSDL).
   Identify edge cases (orphaned records, NULL foreign keys, invalid JSON in `attributes`).
2. **Define metadata mapping** — Map `ArrDao.attributes` JSON keys to `RulItemType`/`RulItemSpec`
   pairs for `DaDaoItem` conversion.
3. **Extend DA services** — Ensure `DaService` can create `DaAip`/`DaDao` from programmatic input
   (not only from METS/EAD import).
4. **Hierarchy convention** — Each migrated flat `ArrDao` becomes: one `DaDao` (LOGICAL) + one
   `DaDao` (REPRESENTATION) linked via `DaDaoRelation`. File groups become `DaDaoFileFolder`.
5. **AIP granularity** — One `DaAip` per `ArrDao` (each digital object becomes its own AIP).

### Phase 2: Migrate DA-type Repository Data

For repositories where `DigitalRepositoryType = DA`:
1. For each `ArrDaoLink` with `dao_id != NULL` and matching DA repository:
   - Create `DaAip` + `DaDao` hierarchy (LOGICAL + REPRESENTATION)
   - Migrate `ArrDaoFile` -> `DaDaoFile`, `ArrDaoFileGroup` -> `DaDaoFileFolder`
   - Decompose `ArrDao.attributes` -> `DaDaoItem` records
   - Update `arr_dao_link`: set `da_dao_id`, `aip_id`, `link_type`; keep `dao_id` for rollback
   - Create single `DaChange` (type=`AIP_CREATE`) per migrated AIP
2. Validate: all DA-type links have `da_dao_id` populated.

### Phase 3: Migrate Filesystem Repository Data

For repositories where `DigitalRepositoryType = FILESYSTEM`:
1. Same entity migration as Phase 2
2. Update `FileSystemRepoService` to create `DaDao`/`DaDaoFile` instead of `ArrDao`/`ArrDaoFile`

### Phase 4: Migrate WSDL Repository Data (if still used)

Same pattern as Phase 2/3. If WSDL integration is no longer active, deprecate
`DigitalRepositoryType.WSDL` instead.

### Phase 5: Remove Old Structures

1. Verify all `arr_dao_link.dao_id` values are NULL (all links migrated)
2. Drop columns from `arr_dao_link`: `dao_id`, `scenario`
3. Drop tables: `arr_dao_file`, `arr_dao_file_group`, `arr_dao`, `arr_dao_package`, `arr_dao_batch_info`
4. Remove Java entities: `ArrDao`, `ArrDaoFile`, `ArrDaoFileGroup`, `ArrDaoPackage`, `ArrDaoBatchInfo`
5. Remove repositories: `DaoRepository`, `DaoFileRepository`, `DaoFileGroupRepository`,
   `DaoPackageRepository`, `DaoBatchInfoRepository`
6. Remove old endpoints from `ArrangementController` (or redirect to DA equivalents)
7. Update `DaoServiceInternal` — remove all `arr_dao` creation methods
8. Clean up `ArrDaoLink` entity — remove `dao` and `daoId` fields
9. Update tests, API DTOs, and frontend components that reference old entities

## 5. Key Files to Modify

### Entities to remove (after migration)
- `elza-core/.../domain/ArrDao.java`
- `elza-core/.../domain/ArrDaoFile.java`
- `elza-core/.../domain/ArrDaoFileGroup.java`
- `elza-core/.../domain/ArrDaoPackage.java`
- `elza-core/.../domain/ArrDaoBatchInfo.java`

### Entity to update
- `elza-core/.../domain/ArrDaoLink.java` — remove `dao`/`daoId` fields

### Services to update
- `elza-core/.../service/dao/FileSystemRepoService.java` — create DA entities
- `elza-core/.../service/dao/DaoServiceInternal.java` — remove arr_dao methods
- `elza-core/.../service/DaoService.java` — remove arr_dao code paths
- `elza-core/.../service/da/DaService.java` — extend for migration support

### Repositories to remove
- `elza-core/.../repository/DaoRepository.java`
- `elza-core/.../repository/DaoFileRepository.java`
- `elza-core/.../repository/DaoFileGroupRepository.java`
- `elza-core/.../repository/DaoPackageRepository.java`
- `elza-core/.../repository/DaoBatchInfoRepository.java`

### Migration scripts
- `elza-core/src/main/resources/db/changelog/db.elza-da.xml` — add migration changesets

## 6. Risks

| Risk | Mitigation |
|------|------------|
| Data loss during migration | Run in transaction; keep `dao_id` for rollback until validated |
| Malformed `attributes` JSON | Pre-scan and report unmappable attributes before migration |
| `ArrDaoLink.dao` null in old code paths | Audit code iterating all links; add null checks before Phase 5 |
| Frontend breakage | Identify UI components referencing `ArrDao` DTOs; update in parallel |
| WSDL integration disruption | Assess usage before deciding on deprecation vs migration |
