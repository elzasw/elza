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
| `FileSystemRepoService` | Reworked by #9944 (2026-07-28 → 2026-08-06): all defects from `fs-repo-analysis.md` §2 fixed (path traversal guard, parent file group + repair changeset `20260728120000`, real MIME detection, per-(repository, fund) packages, skip-and-log unrecognized entries, '/' path normalization with data migration `20260803120000`). Since step 3a (2026-08-10) linking creates only the `ArrDao` anchor — no per-file entities. N4 normalization (2026-08-11, changeset `20260811135500`): `isFileSystemRepository` keys off `digitalRepositoryType == FILESYSTEM` and repository URLs hold plain paths — the `file://` prefix is gone. Filesystem repos are **not** migrated to `DaAip`/`DaDao` (revised Phase 3). |
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

This risk is retired by the revised step 3b (2026-08-11): the link entity is split into JOINED
subtypes and `dao_id` becomes `NOT NULL` again — inside `arr_legacy_dao_link` (see §3).

## 3. The Shared Bridge: arr_dao_link

`ArrDaoLink` was extended (changeset `20240717125500`) to serve both models: today one flat table
carries `node_id` / `create_change_id` / `delete_change_id` (shared), `dao_id` (nullable since
da-main, was NOT NULL) + `scenario` (old model), and `aip_id` + `da_dao_id` + `link_type` (new DA
model) — the target groups distinguished only by which columns are non-NULL.

**Step 3b (revised 2026-08-11) restructures this into an entity hierarchy** — the generic-type +
specialization pattern the codebase already uses for `ArrData`, `ArrItem` and `ArrRequest` — with
JOINED tables:

| Entity | Table | Own columns | Notes |
|--------|-------|-------------|-------|
| `ArrDaoLink` (abstract base) | `arr_dao_link` | `node_id` NOT NULL, `create_change_id` NOT NULL, `delete_change_id`, `link_type` | shared spine; `link_type` gets backend-neutral names (`CONTAINER`/`PART`/`COMPONENT`, old values kept as aliases) |
| `ArrLegacyDaoLink` | `arr_legacy_dao_link` | `dao_id` NOT NULL, `scenario` | transitional shape; class and table are deleted in Phase 5 |
| `ArrDaLink` | `arr_da_link` | `aip_id` NOT NULL, `da_dao_id` | `da_dao_id` NULL = the whole AIP (today's `LinkType.AIP`) |
| `ArrFsLink` | `arr_fs_link` | `digital_repository_id` NOT NULL, `path` | `path` NULL = the repository root |

The pattern the flat table already followed becomes explicit: every link is a **container
reference plus an optional member reference** — `aip_id` + `da_dao_id` for DA,
`digital_repository_id` + `path` for filesystem — where a NULL member means "the whole
container". `link_type` already encodes that distinction (`AIP` = whole container,
`PART_AIP`/`COMPONENT_AIP` = a member within it) and stays on the base, since granularity is a
cross-backend concept.

The subtype a row belongs to *is* its target shape, so the previously planned `CHECK` constraint
over nullable column groups is replaced by per-subtype NOT NULL constraints — stronger, and it
also retires the §2.4 risk. Residual gap: no DB constraint prevents child rows in two subtype
tables for one `dao_link_id` (Hibernate never creates that state); Phase 5 validation includes a
verification query. Jackson polymorphism for the node cache follows the `ArrData` precedent
(`@JsonTypeInfo(use = Id.CLASS, property = "@class")` on the base); because existing cached rows
predate the marker, step 3b drops `arr_cached_node` rows of nodes present in `arr_dao_link` and
the startup sync rebuilds them.

After migration (Phase 5), `ArrLegacyDaoLink` and its table are dropped together with the
`arr_dao` family; the base keeps its table name — renaming to `da_dao_link` is optional and low
priority.

## 4. Migration Plan (Gradual)

Migration proceeds per repository type. Both models operate in parallel during the transition.
Old endpoints remain functional until Phase 5.

### Phase 1: Prepare (no data changes)

1. **Audit existing data** — Count `arr_dao` records per repository type, classified by
   `digital_repository_type`. The column is authoritative since changeset `20260811135500`
   (N4, 2026-08-11): repositories with a `file://` URL were re-typed `FILESYSTEM` one final time
   from the URL evidence and the prefix was stripped, so URLs now hold plain paths and
   URL-prefix classification is neither possible nor needed. (Historical note: before that
   changeset the column was heuristically backfilled and demonstrably wrong — on frnk all three
   filesystem repositories were typed `WSDL`.) Identify edge cases (orphaned records, NULL
   foreign keys, invalid JSON in `attributes`).

> **Revised 2026-08-11:** the original items 2–5 (metadata mapping for `DaDaoItem` conversion,
> programmatic `DaAip`/`DaDao` factories, the LOGICAL+REPRESENTATION hierarchy convention, one
> `DaAip` per `ArrDao`) existed solely to prepare the Phase 2 conversion. With Phase 2 reduced to
> a verification (below), they are not built up front. They remain the agreed recipe if Phase 4
> decides to migrate WSDL data — or if the Phase 2 verification ever finds DA-type legacy rows —
> and are preserved in the Phase 4 contingency note.

### Phase 2: Verify DA-type Repositories Carry No Legacy Data (revised)

> **Decision (2026-08-11):** no production installation has a `DigitalRepositoryType = DA`
> repository with legacy `arr_dao` objects — the DA integration arrived with da-main, and DA
> links are created with `aip_id`/`da_dao_id` from the start. The original Phase 2 (converting
> legacy `ArrDao` rows of DA repositories into `DaAip`/`DaDao` hierarchies, decomposing
> `attributes` into `DaDaoItem`, migrating files) therefore has an empty input set and is
> replaced by an assertion.

1. **Verify per installation** (part of the step 3b migration wave, before the restructure):

   ```sql
   SELECT count(*)
     FROM arr_dao d
     JOIN arr_dao_package p ON p.dao_package_id = d.dao_package_id
     JOIN arr_digital_repository r ON r.external_system_id = p.digital_repository_id
    WHERE r.digital_repository_type = 'DA';
   ```

   Expected 0, and likewise for `arr_dao_link` rows reaching a DA-type repository through
   `dao_id`. The `digital_repository_type` column is a valid key for this query since changeset
   `20260811135500` normalized it (N4, 2026-08-11); the verification must run **after** that
   changeset. A non-zero count still means "inspect the repository row first" — a repository
   mis-typed `DA` by hand is more likely than genuine DA legacy data.
2. **If the count is zero** (the expected case), nothing is migrated: after step 3b every
   DA-type link is an `ArrDaLink` born that way, and the Phase 5 criterion for DA repositories
   is satisfied vacuously.
3. **If rows are found**, they are either mis-typed repositories (fix the type) or genuine legacy
   data in a DA repository — in that case fall back to the conversion recipe preserved in the
   Phase 4 contingency note.

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
(delete stale fs `arr_dao_file` rows, discriminated by `sys_external_system.url LIKE 'file://%'`
because at that time `digital_repository_type` was still heuristically backfilled; changeset
`20260811135500` has since normalized the type column and stripped the `file://` prefix, making
the type attribute the discriminator from then on — N4) and `20260810120001` (drop the group
structures). `arr_dao_file` remains, now written exclusively by the SOAP/WSDL flows, which store
genuine preservation metadata there; the SOAP contract's `Folder`/`FolderGroup` XSD types are
unchanged (folders are rejected at import, as before).

**Step 3b — links target the path, not a DAO row — as a subtype of the existing link model.**
`arr_dao_link` rows for filesystem repositories carry `dao_id` pointing at an `ArrDao` whose only
purpose is to record a repository-relative path. No *parallel* link model is needed:
`ArrDaoLink.LinkType` (`ArrDaoLink.java:85-89`) already expresses *"container reference + optional
member reference"* — `AIP` means whole container (`da_dao_id` NULL), `PART_AIP`/`COMPONENT_AIP`
mean a member within it. Filesystem linking has the same shape, with
`(digital_repository_id, path)` in place of `(aip_id, da_dao_id)`; in the entity hierarchy of §3
it is the `ArrFsLink` subtype. Note DA links never address an
individual file either — there is no `DaDaoFile` reference on `ArrDaoLink` — so both models stop at
the same granularity. See `fs-repo-analysis.md` §5.4. The conversion must update the #9944 code
built on the `dao_id` join in the same step: `DaoLinkRepository.findLinksByDigitalRepository`
(becomes a direct read of `arr_fs_link` — no join), the
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

**Schema change required — decided 2026-07-28, retimed 2026-08-07, revised to the JOINED split
2026-08-11** (lands with step 3b; the original plan scheduled it for Phase 1 of
`fs-repo-analysis.md` §7, but that phase shipped in #9944 without it — it must in any case precede
Phase 5 here). Restructure `arr_dao_link` into the hierarchy of §3:

1. Create `arr_legacy_dao_link`, `arr_da_link` and `arr_fs_link`; populate the first two from the
   existing nullable columns (`dao_id`/`scenario` and `aip_id`/`da_dao_id` respectively). Widen
   `link_type` to backend-neutral names, retaining `AIP`/`PART_AIP`/`COMPONENT_AIP` as aliases so
   DA rows need no data migration.
2. Convert existing filesystem links mechanically: read the path from the `ArrDao.code` they point
   at (canonical '/' form since changeset `20260803120000`), insert the `arr_fs_link` child
   (`path`, `digital_repository_id` from the DAO's package), set `link_type`, delete the
   `arr_legacy_dao_link` child.
3. Drop the moved columns (`dao_id`, `scenario`, `aip_id`, `da_dao_id`) from `arr_dao_link`.
4. Delete `arr_cached_node` rows for nodes present in `arr_dao_link` — cached link JSON predates
   the `@JsonTypeInfo` marker — and let the startup sync rebuild them.

Java side: `ArrDaoLink` becomes abstract with `@JsonTypeInfo(use = Id.CLASS)` (the `ArrData`
precedent); creation sites instantiate `ArrLegacyDaoLink` (SOAP import), `ArrDaLink` (DA linking)
or `ArrFsLink` (fs linking); `DaoLinkRepository` queries touching subtype fields move to
subtype-scoped repositories. `RevertingChangesService` needs no change — its `arr_dao_link`
mutations are HQL, which Hibernate expands over the subtype tables (verified 2026-08-11), and its
native reads touch only base-spine columns. This replaces the "same entity migration as Phase 2"
that the original Phase 3 assumed, and the plain-column form of this step decided 2026-07-28.

**Consequence for Phase 5:** filesystem links end as `ArrFsLink` rows and never have `da_dao_id`
populated, so the Phase 5 exit criterion cannot be "all links have `da_dao_id`". See the revised
criterion in Phase 5.

### Phase 4: WSDL Repository Data — Deprecate or Migrate

With Phase 2 reduced to a verification (2026-08-11), WSDL is the **only** repository type whose
legacy `arr_dao` data may ever need conversion onto the DA model — the SOAP import
(`DaoCoreServiceWsImpl`) still actively writes `arr_dao`/`arr_dao_file` rows with genuine
preservation metadata. Identify genuine WSDL repositories by `digital_repository_type = 'WSDL'` —
reliable since changeset `20260811135500` (N4) re-typed every `file://` repository as
`FILESYSTEM` (frnk's mislabeled rows included); filesystem data is handled by Phase 3, not here.
Decide before Phase 5:

- **If WSDL integration is no longer active**, deprecate `DigitalRepositoryType.WSDL` and leave
  its historical links as `ArrLegacyDaoLink` rows until they are archived/removed by product
  decision — Phase 5 cannot run while they exist.
- **If it is still used**, the conversion machinery originally planned for Phase 2 must be built
  here (it was never built, since Phase 2's input set is empty).

**Contingency note — the preserved conversion recipe** (originally Phase 1 items 2–5 + Phase 2;
applies to WSDL migration, or to DA-type legacy rows should the Phase 2 verification ever find
any):

1. Define the metadata mapping: `ArrDao.attributes` JSON keys → `RulItemType`/`RulItemSpec` pairs
   for `DaDaoItem` conversion; pre-scan for malformed/unmappable `attributes` first.
2. Extend `DaService` so `DaAip`/`DaDao` can be created from programmatic input (not only from
   METS/EAD import).
3. Hierarchy convention: each migrated flat `ArrDao` becomes one `DaDao` (LOGICAL) + one `DaDao`
   (REPRESENTATION) linked via `DaDaoRelation`; `ArrDaoFile` → `DaDaoFile` (file groups are gone
   since step 3a).
4. AIP granularity: one `DaAip` per `ArrDao` (each digital object becomes its own AIP), one
   `DaChange` (type=`AIP_CREATE`) per migrated AIP.
5. Convert each link: insert the `arr_da_link` child, delete the `arr_legacy_dao_link` child,
   and keep the deleted child rows in a backup table until validated (after step 3b a row cannot
   carry both target groups).

### Phase 5: Remove Old Structures

1. Verify `arr_legacy_dao_link` is empty. Note the three routes by which this becomes true:
   DA-type links were born as `ArrDaLink` rows (Phase 2 verified there was nothing to convert),
   WSDL-type links were converted or retired per the Phase 4 decision, and **filesystem-type
   links end as `ArrFsLink` rows** (revised Phase 3) and never have `da_dao_id`. Validate the
   populations separately, and verify no `dao_link_id` has children in two subtype tables
   (exclusivity is structural but not DB-enforced across tables). A residual filesystem row in
   `arr_legacy_dao_link` means the Phase 3 conversion is incomplete, not that migration failed.
2. Drop table `arr_legacy_dao_link` and entity `ArrLegacyDaoLink` (the `dao_id`/`scenario`
   columns left the base table already in step 3b)
3. Drop tables: `arr_dao_file`, `arr_dao`, `arr_dao_package`, `arr_dao_batch_info`
   (`arr_dao_file_group` was already dropped by step 3a, changeset `20260810120001`)
4. Remove Java entities: `ArrDao`, `ArrDaoFile`, `ArrDaoPackage`, `ArrDaoBatchInfo`
5. Remove repositories: `DaoRepository`, `DaoFileRepository`, `DaoPackageRepository`,
   `DaoBatchInfoRepository`
6. Remove old endpoints from `ArrangementController` (or redirect to DA equivalents)
7. Update `DaoServiceInternal` — remove all `arr_dao` creation methods
8. `ArrDaoLink` base needs no cleanup — the legacy fields live entirely in the dropped subclass
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

### Entities to restructure (step 3b) and remove (Phase 5)
- `elza-core/.../domain/ArrDaoLink.java` — becomes the abstract JOINED base (shared spine +
  `@JsonTypeInfo`); new subclasses `ArrLegacyDaoLink.java`, `ArrDaLink.java`, `ArrFsLink.java`.
  Phase 5 then deletes `ArrLegacyDaoLink.java`

### Services to update
- `elza-core/.../service/dao/FileSystemRepoService.java` — step 3a (done 2026-08-10) removed the
  `syncFilesAndFolders` tree-building; step 3b removes `createDao` (the `ArrDao` anchor). Do
  **not** convert it to create DA entities. See revised Phase 3 and `fs-repo-analysis.md` §5.
  The independent defects documented in `fs-repo-analysis.md` §2 were fixed by #9944
  (2026-07-28 → 2026-08-06, incl. repair changeset `20260728120000`) — nothing to fix first anymore.
- `elza-core/.../service/dao/FileSystemRepoBrowser.java` — in step 3b, switch link info from the
  `dao_id` join (`findLinksByDigitalRepository`) to a direct `arr_fs_link` read
  (`digital_repository_id`, `path`)
- `elza-core/.../service/dao/DaoServiceInternal.java` — remove arr_dao methods
- `elza-core/.../service/DaoService.java` — remove arr_dao code paths
- `elza-core/.../service/da/DaService.java` — extend for migration support only if the Phase 4
  contingency (WSDL migration) is chosen

### Repositories to remove
- `elza-core/.../repository/DaoRepository.java`
- `elza-core/.../repository/DaoFileRepository.java`
- `elza-core/.../repository/DaoPackageRepository.java`
- `elza-core/.../repository/DaoBatchInfoRepository.java`

### Migration scripts
- migration changesets go to the **last** file included by
  `elza-core/src/main/resources/db/changelog/db.changelog-master.yaml`
  (`db.elza-3-part-03.xml` at the time of writing); all earlier files are
  frozen — see the reorganization of 2026-08-12 (former `db.elza-da.xml` is
  the frozen `db.elza-3-part-02.xml` with a pinned logical path)

## 6. Risks

| Risk | Mitigation |
|------|------------|
| Data loss during migration | Run in transaction; keep the replaced legacy rows (deleted `arr_legacy_dao_link` children in a backup table) until validated. Applies to step 3b's fs conversion and to the Phase 4 contingency |
| Malformed `attributes` JSON | Only relevant if the Phase 4 contingency (conversion to `DaDaoItem`) runs; pre-scan and report unmappable attributes before migrating |
| Phase 2 premise wrong on some installation (DA-type repository with legacy `arr_dao` rows) | The Phase 2 verification query runs per installation before the step 3b restructure; a non-zero count stops and escalates (mis-typed repository vs. the Phase 4 contingency recipe) instead of migrating blind |
| `ArrDaoLink.dao` null in old code paths | Audit code iterating all links; add null checks before Phase 5. *Retired by step 3b:* `dao_id` is NOT NULL inside `arr_legacy_dao_link`, and code sees the shape as a type |
| Frontend breakage | Identify UI components referencing `ArrDao` DTOs; update in parallel |
| WSDL integration disruption | Assess usage before deciding on deprecation vs migration |
| `arr_dao_link` target groups enforced only by convention | *Resolved by the step 3b JOINED split:* each subtype table declares NOT NULL on its target columns. Residual: no DB constraint prevents child rows in two subtype tables for one link — Phase 5 validation includes a verification query |
| Generic link-reading code must handle three target shapes during transition | Legacy, DA and filesystem links coexist until Phase 5 drops the first. The step 3b entity hierarchy *is* the single resolver — subtype dispatch replaces column inspection at call sites |
| Old cached node JSON carries no polymorphic type marker on links | Step 3b deletes `arr_cached_node` rows of nodes present in `arr_dao_link`; the startup sync rebuilds them (cost bounded by the number of linked nodes). Cached JSON then binds to class names via `@class` (the `ArrData` precedent) — renaming link classes later invalidates cache rows the same way |
| #9944 features are built on the `dao_id` join | `findLinksByDigitalRepository`, `findDettachedByFundAndCodes` and the `FsLink` UI read through `arr_dao`; they must be converted atomically with the step 3b data migration. The surface is small and covered by `FileSystemRepoBrowserTest` |
| `multiple_links` declared but not enforced | The flag exists on `arr_digital_repository` and in the admin UI, but link creation does not check it yet. Implement the check as "is `(repository, path)` already live-linked" so it survives step 3b unchanged; enforcing it against `ArrDao` rows would be rewritten |
