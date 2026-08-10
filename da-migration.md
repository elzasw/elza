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
| `PUT /fund/{fundId}/fsrepo/.../dao-link` | fundFsCreateDAOLink | Result slimmed to `daoLinkId` (step 3a) |

New endpoints were added via `AipController` (10 endpoints) and `DaoController` (2 new endpoints)
for the DA model — these do not interfere with legacy functionality.

### 2.2 Service & Entity Layer

| Component | Status |
|-----------|--------|
| `ArrDao` entity | Identical to 3.3.x |
| `ArrDaoLink` entity | Extended with 3 new nullable fields (`aip`, `daDao`, `linkType`); `dao` field made nullable |
| `DaoRepository` | Identical — all query methods preserved |
| `DaoLinkRepository` | Extended — new query methods for DA, plus `findLinksByDigitalRepository` (#9944) feeding the fs browser's per-item link info through the `dao_id` join |
| `DaoServiceInternal` | Identical — all CRUD methods intact |
| `DaoService` | Extended — new DA methods added, all old methods unchanged |
| `FileSystemRepoService` | Reworked by #9944 (2026-07-28 → 2026-08-06): all defects from `fs-repo-analysis.md` §2 fixed (path traversal guard, parent file group + repair changeset `20260728120000`, real MIME detection, per-(repository, fund) packages, skip-and-log unrecognized entries, '/' path normalization with data migration `20260803120000`). Since step 3a (2026-08-10) linking creates only the `ArrDao` anchor — no per-file entities. Filesystem repos are **not** migrated to `DaAip`/`DaDao` (revised Phase 3). |
| `FileSystemRepoBrowser` | **New** (#9944) — live directory listing (no persistence), locale-aware sorting, type/link/substring filters, paging with `truncated` flag; link state and node references resolved via `DaoLinkRepository.findLinksByDigitalRepository`. Step 3a added `listDaoFiles` — the live, capped, recursive file listing behind the arrangement DAO panel. |
| `DaoSyncService` | Identical |

### 2.3 Tests

All 4 test methods in `DaoCoreServiceTest` are present in both branches. The only change is that
da-main requires setting `DigitalRepositoryType.FILESYSTEM` on the test digital repository.

#9944 added `FileSystemRepoBrowserTest` and `FileSystemRepoServiceTest`
(`elza-core/src/test/java/cz/tacr/elza/service/fsrepo/`) covering path resolution incl. traversal
inputs, MIME detection, browsing (sort/filter/paging) and repository listing — the fs-repo feature
is no longer untested, which lowers the risk of the Phase 3 conversion.

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
| `digital_repository_id` | `arr_digital_repository` | *planned* | nullable |
| `path` | — (repository-relative path) | *planned* | nullable |

A row with `dao_id != NULL, da_dao_id = NULL` is a legacy link.
A row with `da_dao_id != NULL` is a new DA link (`dao_id` may be NULL).
A row with `digital_repository_id != NULL` will be a filesystem link (`path` NULL means the
repository root) — see revised Phase 3.

The two *planned* columns are added by step 3b of revised Phase 3, not by the original
`20240717125500` changeset. They complete a pattern the table already follows: every link is a **container reference
plus an optional member reference** — `aip_id` + `da_dao_id` for DA, `digital_repository_id` + `path`
for filesystem — where a NULL member means "the whole container". `link_type` already encodes that
distinction (`AIP` = whole container, `PART_AIP`/`COMPONENT_AIP` = a member within it).

After migration, `arr_dao_link` will be evolved in place — drop `dao_id` and `scenario` columns,
keep the table as the universal DAO-to-node link table. Renaming to `da_dao_link` is optional
and low priority.

**Constraint gap:** the mutually-exclusive target groups are enforced only by convention today. A
`CHECK` constraint asserting exactly one group is populated per row should be added alongside the
planned columns (revised Phase 3), before a third group makes the invariant harder to verify.

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

### Phase 3: Retire arr_dao in Filesystem Repositories (revised)

> **Decision (2026-07-28):** filesystem repositories are **not** migrated onto `DaAip`/`DaDao`.
> This supersedes the original Phase 3, which said "update `FileSystemRepoService` to create
> `DaDao`/`DaDaoFile` instead of `ArrDao`/`ArrDaoFile`". Rationale and evidence:
> `fs-repo-analysis.md` §5.

A filesystem repository is not an AIP-shaped store. Two constraints of the DA model make the
original plan a poor fit:

- `DaDao.aip` is `nullable = false` (`DaDao.java:27`), so every browsed folder would require a
  fabricated `DaAip` — with no `aipVersion`, no PREMIS provenance, no ingestion event and no fixity
  manifest to populate it from.
- The DA model is write-once and change-versioned (`create_change_id NOT NULL` /
  `delete_change_id NULL` on all seven mutable entities; see `DaoProcessor.java:227-240`). A
  filesystem is mutable by nature — files appear, change and vanish outside Elza's control — so
  mirroring one into that store means emitting a `DaChange` per detected difference: a
  preservation-grade audit trail for something that is not a preservation store.

The Phase 5 goal (removing `arr_dao`) is still reached, by a different route: **filesystem browsing
stops persisting entities altogether.**

> **Progress note:** the #9944 series (2026-07-28 → 2026-08-06) modernized the fs browser (see
> §2.2) but did **not** include the §5.4 schema change — its features read links through the
> `dao_id` join (`DaoLinkRepository.findLinksByDigitalRepository`), which step 3b converts. Two
> facts from that series matter for the remaining work:
>
> - `arr_digital_repository.multiple_links` (changeset `20260806120000`, default `false`) declares
>   whether one item may be linked to more than one node. It is editable in the admin UI but **not
>   yet enforced server-side** — implement the enforcement as "is `(repository, path)` already
>   live-linked", which works identically before and after step 3b.
> - The '/' path normalization (changeset `20260803120000`) makes `ArrDao.code` a canonical
>   repository-relative path, so the mechanical `dao_id` → `(digital_repository_id, path)`
>   conversion below is simpler and safer than when this phase was drafted.

#### Directions reviewed (2026-08-07)

Prompted by the #9944 wave, all evolution directions for the filesystem side of `arr_dao` were
re-examined against the current code:

| Direction | Verdict |
|---|---|
| Migrate fs onto `DaAip`/`DaDao` (original Phase 3) | Still rejected. Both blocking constraints re-verified on current code (`DaDao.aip` NOT NULL, write-once discipline in `DaoProcessor.java:227-240`); #9944 moved further away from it (specialized live-read browser). |
| Keep `arr_dao` permanently as the fs-native store | More defensible than before #9944 (the model is repaired and tested), but keeps five tables plus `dao_id` indefinitely and keeps the structural staleness: persisted trees are snapshots synced only at link time while the browser reads disk live, so the arrangement DAO panel and the fs browser show diverging data. Rejected as end state. |
| Path-based links, converted in one step (the 2026-07-28 plan as written) | Still the right end state; feasibility improved (canonical '/' codes, per-fund packages), but the conversion cost grew — the fresh #9944 queries and UI read through `dao_id` and would be rewritten in the same step that changes the data. |
| **Path-based links via two independently shippable steps (3a, 3b below)** | **Adopted; step 3a implemented 2026-08-10.** Step 3a removes the persisted file trees (the pure liability); step 3b converts the link anchor. Each is separately testable, and neither blocks further fs-repo feature work. |

For repositories where `DigitalRepositoryType = FILESYSTEM`:

**Step 3a — stop persisting file trees. IMPLEMENTED 2026-08-10.**
`FileSystemRepoService.createDao` creates only the `ArrDao` anchor (path in `code`) — no per-file
rows are written at link time. The arrangement DAO panel reads filesystem files live:
`DaoService.getDaoFiles`/`countDaoFiles` branch per repository type in the service layer
(filesystem: `FileSystemRepoBrowser.listDaoFiles` — recursive, flat, path-ordered, capped at 1000,
returned as transient entities; other types: persisted rows), and `ClientFactoryVO` stays pure
transformation, assigning negative wire ids to files without a persistent id.
`FsCreateDaoLinkResult.skippedEntries` was removed from the contract — nothing is walked at link
time, so nothing can be skipped. `arr_dao_file_group` was dropped entirely — table, FK column,
entity, repository, VO and the dead `DaoSyncService` methods — via changesets `20260810120000`
(delete stale fs `arr_dao_file` rows, discriminated by `sys_external_system.url LIKE 'file://%'`,
not the heuristically backfilled `digital_repository_type`) and `20260810120001` (drop the group
structures). `arr_dao_file` remains, now written exclusively by the SOAP/WSDL flows, which store
genuine preservation metadata there; the SOAP contract's `Folder`/`FolderGroup` XSD types are
unchanged (folders are rejected at import, as before).

**Step 3b — links target the path, not a DAO row — reusing the existing link model.**
`arr_dao_link` rows for filesystem repositories carry `dao_id` pointing at an `ArrDao` whose only
purpose is to record a repository-relative path. No specialized link table is needed:
`ArrDaoLink.LinkType` (`ArrDaoLink.java:85-89`) already expresses *"container reference + optional
member reference"* — `AIP` means whole container (`da_dao_id` NULL), `PART_AIP`/`COMPONENT_AIP`
mean a member within it. Filesystem linking has the same shape, with
`(digital_repository_id, path)` in place of `(aip_id, da_dao_id)`. Note DA links never address an
individual file either — there is no `DaDaoFile` reference on `ArrDaoLink` — so both models stop at
the same granularity. See `fs-repo-analysis.md` §5.4. The conversion must update the #9944 code
built on the `dao_id` join in the same step: `DaoLinkRepository.findLinksByDigitalRepository`
(becomes a direct read of `path` from the link row — no join), the
`DaoRepository.findDettachedByFundAndCodes` lookup in `FileSystemRepoService.createDao`, `FsLink`
assembly in `FileSystemRepoBrowser`, and the `multiple_links` enforcement once it exists.

Two further points, unchanged from the 2026-07-28 revision:

- **Unify at the SPI, not at the entity model.** Introduce a `DigitalRepositoryBackend` interface
  (`list` / `open` / `link` / `refresh`) with a `FileSystemBackend` implementation, and a
  backend-neutral paged browse contract. This is the level at which DA and FILESYSTEM genuinely
  share behaviour. `FileSystemRepoBrowser` gives the browse logic a home but is fs-specific — it
  is a precursor of `FileSystemBackend`, not the SPI itself. There is still no such abstraction:
  `DigitalRepositoryType` dispatch occurs at exactly two sites (`DaConnector.java:106`,
  `DaScheduler.java:51`; re-verified 2026-08-07) and the three backends are disjoint parallel code
  paths sharing one entity table.
- **Ingest stays explicit and optional.** Where a folder genuinely *is* an ingest candidate — the
  user wants it archived, not merely browsed — provide a deliberate "ingest folder as AIP" action
  that mints a real `DaAip` with an intentional code and version. This is the only case where AIP
  semantics are true rather than synthesised, and it is far smaller than blanket migration: a
  `FilesystemAipProcessor` sibling to `DaoProcessor` reusing the already-generic `DaService`
  factories (`createDaDao` `:893`, `createDaDaoFileFolder` `:911`, `createDaDaoFile` `:920`), plus a
  non-PREMIS variant of `PackageInfoService` to mint the `DaAip`/`DaAipState`.

**Schema change required — decided 2026-07-28, retimed 2026-08-07** (lands with step 3b; the
original plan scheduled it for Phase 1 of `fs-repo-analysis.md` §7, but that phase shipped in #9944
without it — it must in any case precede Phase 5 here):
add `path` and `digital_repository_id` to `arr_dao_link`, widen `link_type` to backend-neutral names
(retaining `AIP`/`PART_AIP`/`COMPONENT_AIP` as aliases so DA rows need no data migration), and add a
`CHECK` constraint asserting exactly one target group is populated per row — an invariant the table
relies on by convention today. Existing filesystem `ArrDaoLink` rows are then converted mechanically:
read the path from the `ArrDao.code` they point at (canonical '/' form since changeset
`20260803120000`), write `path`, set `digital_repository_id` from the DAO's package, set
`link_type`, clear `dao_id`. This replaces the "same entity migration as Phase 2" that the original
Phase 3 assumed.

**Consequence for Phase 5:** filesystem links will never have `da_dao_id` populated, so the Phase 5
exit criterion cannot be "all links have `da_dao_id`". See the revised criterion in Phase 5.

### Phase 4: Migrate WSDL Repository Data (if still used)

Same pattern as Phase 2/3. If WSDL integration is no longer active, deprecate
`DigitalRepositoryType.WSDL` instead.

### Phase 5: Remove Old Structures

1. Verify all `arr_dao_link.dao_id` values are NULL. Note the two routes by which this becomes
   true (revised Phase 3): DA- and WSDL-type links get `da_dao_id` populated, whereas
   **filesystem-type links get `digital_repository_id` + `path` instead** and never have
   `da_dao_id`. Validate both populations separately — every surviving link must satisfy exactly one
   target group, which the Phase 3 `CHECK` constraint enforces from that point on. A residual
   filesystem link with `dao_id != NULL` means the Phase 3 conversion is incomplete, not that
   migration failed.
2. Drop columns from `arr_dao_link`: `dao_id`, `scenario`
3. Drop tables: `arr_dao_file`, `arr_dao`, `arr_dao_package`, `arr_dao_batch_info`
   (`arr_dao_file_group` was already dropped by step 3a, changeset `20260810120001`)
4. Remove Java entities: `ArrDao`, `ArrDaoFile`, `ArrDaoPackage`, `ArrDaoBatchInfo`
5. Remove repositories: `DaoRepository`, `DaoFileRepository`, `DaoPackageRepository`,
   `DaoBatchInfoRepository`
6. Remove old endpoints from `ArrangementController` (or redirect to DA equivalents)
7. Update `DaoServiceInternal` — remove all `arr_dao` creation methods
8. Clean up `ArrDaoLink` entity — remove `dao` and `daoId` fields
9. Update tests, API DTOs, and frontend components that reference old entities. Known readers of
   `ArrDao`/`ArrDaoFile` beyond the DAO services (audit before dropping): node cache
   (`DaoService.updateNodeCacheDaoLinks` → `ArrangementCacheService.updateDaoLinks`), native XML
   export (`dataexchange/output/sections/DaoLoader`, `LevelInfoLoader`), print model
   (`print/Dao.java`), SOAP WS (`ws/core/v1/daoservice/DaoCoreServiceWsImpl`), and the arrangement
   DAO panel VOs (`ClientFactoryVO.createDaoList`).

## 5. Key Files to Modify

### Entities to remove (after migration)
- `elza-core/.../domain/ArrDao.java`
- `elza-core/.../domain/ArrDaoFile.java`
- `elza-core/.../domain/ArrDaoPackage.java`
- `elza-core/.../domain/ArrDaoBatchInfo.java`

### Entity to update
- `elza-core/.../domain/ArrDaoLink.java` — remove `dao`/`daoId` fields

### Services to update
- `elza-core/.../service/dao/FileSystemRepoService.java` — step 3a (done 2026-08-10) removed the
  `syncFilesAndFolders` tree-building; step 3b removes `createDao` (the `ArrDao` anchor). Do
  **not** convert it to create DA entities. See revised Phase 3 and `fs-repo-analysis.md` §5.
  The independent defects documented in `fs-repo-analysis.md` §2 were fixed by #9944
  (2026-07-28 → 2026-08-06, incl. repair changeset `20260728120000`) — nothing to fix first anymore.
- `elza-core/.../service/dao/FileSystemRepoBrowser.java` — in step 3b, switch link info from the
  `dao_id` join (`findLinksByDigitalRepository`) to `(digital_repository_id, path)` on the link row
- `elza-core/.../service/dao/DaoServiceInternal.java` — remove arr_dao methods
- `elza-core/.../service/DaoService.java` — remove arr_dao code paths
- `elza-core/.../service/da/DaService.java` — extend for migration support

### Repositories to remove
- `elza-core/.../repository/DaoRepository.java`
- `elza-core/.../repository/DaoFileRepository.java`
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
| `arr_dao_link` target groups enforced only by convention | The table already carries mutually-exclusive target groups (`dao_id` vs `aip_id`+`da_dao_id`) with no constraint; adding the filesystem pair makes this worse. Add the `CHECK` constraint described in revised Phase 3 at the same time as the new columns |
| Generic link-reading code must handle three target shapes during transition | Legacy `dao_id`, DA `aip_id`+`da_dao_id`, and filesystem `digital_repository_id`+`path` coexist until Phase 5 drops the first. Route all reads through one resolver rather than inspecting columns at call sites |
| #9944 features are built on the `dao_id` join | `findLinksByDigitalRepository`, `findDettachedByFundAndCodes` and the `FsLink` UI read through `arr_dao`; they must be converted atomically with the step 3b data migration. The surface is small and covered by `FileSystemRepoBrowserTest` |
| `multiple_links` declared but not enforced | The flag exists on `arr_digital_repository` and in the admin UI, but link creation does not check it yet. Implement the check as "is `(repository, path)` already live-linked" so it survives step 3b unchanged; enforcing it against `ArrDao` rows would be rewritten |
