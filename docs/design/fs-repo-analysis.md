# Filesystem repository — architecture and decisions

The filesystem repository is on its target architecture: browsing, panel files and downloads are
live disk reads; links are `ArrFsLink` rows targeting `(repository, path)`; `multiple_links` is
enforced; filesystem repositories persist **zero** entities.

**What this document is.** The part the code cannot state for itself: the invariants new code must
respect (§1), the behaviours that look like bugs until you know why they were chosen (§1.1, §1.2),
standing notes (§2), the reasoning behind the central architectural decision (§3), the follow-ons
the feature leaves (§4) and the decisions themselves (§5). §6 says how it is verified.

**Scope:** `FileSystemRepoService`, `FileSystemRepoBrowser` and the filesystem-repository browser.
**Related:** `da-migration.md` — the link model shared with the DA and the remaining removal of
the `arr_dao` family.

Finding ids (C*n*, N*n*, A*n*, D*n*) and section numbers are cited from code comments and
changesets and are never reused or renumbered.

---

## 1. What the feature is

| Layer | Location | Role |
|---|---|---|
| Contract | `elza-development/typespec/main.tsp` (`FsRepo`, `FsItem`, `FsItems`, `FsLink`, `DigitalRepositoryTestResult`) | Paged, sorted, filtered browse contract; `FsItemSortType` covers name/size/last-change × asc/desc; `FsItems.lastKey` is an opaque keyset cursor (§1.2); `FsItem.links` carries node references incl. a `readable` flag for funds the resolver could not read; `FsItem.hasChildren` drives the tree's expand marker; `FsRepo.available` marks a repository whose root is not reachable |
| Browsing | `FileSystemRepoBrowser` (`browseItems`, `listRepos`, `listDaoFiles`, `testRepository`) | All listing logic; live disk reads, nothing persisted. `browseItems` is a single-pass `walkFileTree` at depth 1, attributes taken from the visitor callback, then a keyset slice (§1.2) |
| Primitives | `FileSystemRepoService` | Containment-checked path resolution, MIME detection, input streams, templated-URL helpers (`isTemplatedUrl`, `getFixedUrlPrefix`); no persistence at all |
| Linking | `FundController.fundFsCreateDAOLink` → `DaoService.createFsDaoLink`; move via `fundFsMoveDAOLink` / `NodeController.nodeFsRelink` → `DaoService.moveFsDaoLink` | Mints the `ArrFsLink` (`digital_repository_id` + `path`, no `ArrDao`), enforces `multiple_links`, returns `daoLinkId`; move re-points a live link at another node atomically |
| Administration | `ExternalSystemController.externalSystemTestDigitalRepository`; `ArrDigitalRepositoryVO.createEntity` | ADMIN-only configuration test (root reachable + sample of the root); settings that do not apply to a filesystem repository are cleared on save (§1.1) |
| Panel files | `DaoService.listFsLinkFiles` + `ClientFactoryVO.createFsDaoList` | fs links render as synthesized `ArrDaoVO`s (wire id `-daoLinkId`); files listed live via `listDaoFiles` (recursive, flat, path-ordered, capped at `DAO_FILE_LIMIT` = 1000); package DAOs of other repository types read persisted `arr_dao_file` rows |
| Download | `FundController.fundFsRepoItemData` | Single authorized endpoint (`FUND_RD`-checked); streams a `FileSystemResource`, so Content-Length and HTTP Range work; MIME-typed, inline for renderable types |
| UI | `elza-react/src/components/arr/daos/file-system-browser/{FileSystemBrowser,Tree}.tsx`; DAO panel `ArrDaos.jsx`/`ArrDao.jsx` | Tree + list with toolbar filters and link popovers; panel viewer consumes `ArrDaoVO.fileList` |

Two invariants new code must respect:

- **`digital_repository_type` is the only discriminator.** `isFileSystemRepository()` keys off
  `DigitalRepositoryType.FILESYSTEM`; the `url` column holds a plain path, so classification by a
  `file://` prefix is neither possible nor needed.
- **Link state is global.** `ArrFsLinkRepository.findLinksByDigitalRepository` is one query per
  repository with **no fund predicate** (§5 item 3), reading `(digital_repository_id, path)` from
  `arr_fs_link` directly.

### 1.1 Repository configuration and availability

- **A fixed root that is missing is reported, not hidden.** `listRepos` returns the repository with
  `available = false` and logs a WARN. The tree renders it greyed with a warning icon and it cannot
  be expanded; selecting it explains why in the file pane instead of firing a request that would
  fail. An unresolvable configuration (blank URL) is reported the same way.
- **Availability is probed with a bound (N7).** The probe of a root is cached for 30 s and given
  2 s; a timeout or an I/O failure reports the repository as unavailable, so a hanging network
  share cannot block the repository list.
- **A missing *templated* root is skipped**, and that asymmetry is deliberate: when the URL
  carries `{fundId}`/`{fundCode}` parameters the root is fund-dependent, so "no directory" means
  "this repository holds nothing for this fund" — a normal state, not a misconfiguration.
- **The list is ordered by name** through `ElzaLocale.getCollator()`, i.e. by the locale configured
  on the server (`elza.locale`), the same collator the file listing uses.
- **Administrators can test a repository** (`GET /extsystem/digitalrepository/{id}/test`): it
  separates missing path / not a directory / not readable / unlistable / blank URL, and on success
  returns the first 10 root entries. For a templated URL it tests the fixed part of the path and
  says so. Results are technical English strings; the localized headline is composed by the client.
- **Settings that do not apply are hidden and cleared.** For `FILESYSTEM`, `viewDaoUrl`,
  `viewFileUrl`, `viewThumbnailUrl`, `username` and `password` are cleared and `sendNotification`
  is forced to `false` on save (`ArrDigitalRepositoryVO.createEntity`); changeset `20260812120000`
  cleans existing rows. Rationale and the per-field evidence: §5 item 5.

### 1.2 Paging semantics *(C2)*

`lastKey` is a **keyset cursor**: a base64url-encoded JSON blob carrying the last emitted entry's
sort key (`lastName`, `lastSize`, `lastChange`, `lastWasFolder`) plus the sort parameters it was
minted under. An offset would let a file created between two "load more" calls shift every
following entry, and the client would silently skip or duplicate rows. Three properties follow,
and new work must not regress them:

- **Stable under concurrent modification.** The next page resumes strictly *after* the cursor
  position via `Collections.binarySearch` over the sorted listing. If the cursor row is still
  there, listing resumes after it; if it was deleted meanwhile, the negative insertion point
  already denotes the first entry greater than the cursor — no duplicates, no gaps either way.
- **Self-invalidating.** A cursor minted under a different `sortingType` or `foldersFirst` is
  dropped and the listing restarts, so pages of different orders can never be mixed. A malformed
  or stale key does the same with a WARN instead of an error — the client resets on sort/filter
  change anyway, so that branch only catches hand-crafted keys and leftovers across a redeploy.
- **Still one full scan per page, by design.** The directory is re-listed and re-sorted on every
  request; a filesystem offers no index to seek into, and holding a sorted snapshot server-side
  would mean session state with no invalidation signal. `SCAN_CAP` (10 000) bounds one scan and
  `truncated` reports when it bites. This is the accepted cost — see the §6 scale benchmark.

---

## 2. Standing backend notes

One decision recorded so it is not re-raised, one documented behaviour new client code must
respect, and one optimization deliberately deferred.

### C8 — No authorization on repository visibility *(accepted, not a defect)*

`fundFsRepos` returns every `FILESYSTEM`-typed external system to anyone who can read the fund.
This is accepted and will not be changed — see §5 item 4. It does not relax the containment check
on path resolution or the single authorized download endpoint.

The response also includes repositories whose root is missing (§1.1), still carrying the resolved
server path in `FsRepo.path`. The audience is the same (any fund reader sees `path` for a working
repository too), so this does not reopen C8 — but a misconfigured path is visible to
non-administrators as well.

### N2 — Live-listed files have synthetic negative ids

`ClientFactoryVO` assigns `-N` wire ids (unique within one response) to files without a persistent
id. The panel's selection round-trips these ids through component state; they are **not stable
across requests** — client code must not persist them.

### N5 — One disk walk per fs DAO per `findDaos(detail)` request

Acceptable for typical per-node link counts; revisit (batching or caching) only if nodes with many
filesystem links become common.

---

## 3. Filesystem repository vs. the DA/AIP architecture

The central architectural question is whether to keep the filesystem repository as a specialized
implementation or migrate it onto the DA/AIP model. It stays specialized (§3.3). This section is
the decision record, so the reasoning can be reviewed rather than taken on trust.

### 3.1 What the DA model actually requires

| Entity | Key constraints |
|---|---|
| `DaAip` | `code` NOT NULL, `digitalRepository` NOT NULL, unique `(code, digital_repository_id)`. **No change columns — a thin identity row.** |
| `DaAipState` | `daAip` NOT NULL, `createChange` NOT NULL, `deleteChange` nullable, `aipVersion` NOT NULL. Holds all versioned metadata. |
| `DaDao` | **`aip` NOT NULL**, `createChange` NOT NULL, `type` NOT NULL, `code` NOT NULL. |
| `DaDaoRelation` | `dao` + `parentDao` both NOT NULL, change-versioned. No ordering column. |
| `DaDaoFileFolder` | `label` NOT NULL (one path *segment*), `representationDao` NOT NULL, `parentFileFolder` nullable self-FK — **a genuine recursive tree**. |
| `DaDaoFile` | `dao` NOT NULL, `createChange` NOT NULL, checksum/mime/size/image-dimensions/duration. |
| `DaChange` / `DaChangeType` | Only two values: `AIP_CREATE`, `AIP_UPDATE`. |

Two structural facts dominate the decision:

1. **`DaDao.aip` is `nullable = false`.** There is no way to have a DA-model DAO without inventing
   an AIP for it. A filesystem folder is not an AIP — it has no `aipVersion`, no PREMIS
   provenance, no ingestion event, no fixity manifest.
2. **The DA model is write-once + change-versioned.** The mutable entities carry
   `create_change_id NOT NULL` / `delete_change_id NULL`, and `DaoProcessor` follows the
   discipline: content changes close old rows and insert new ones. **A filesystem repository is
   mutable by nature** — files appear, change and vanish outside Elza's control. Mirroring a live
   mutable directory into a write-once versioned store means generating a `DaChange` on every
   detected difference, which is a preservation-grade audit trail for something that is not a
   preservation store.

The AIP model *can* express a hierarchy, but its hierarchy carries semantics (representation vs.
logical structure, versioned fixity, ingestion events) that a filesystem cannot supply.
Synthesising those fields produces a record that looks like preservation metadata but is not.

### 3.2 What migration would actually cost

`DaService`'s entity factories are generic: `createDaDao`, `createDaDaoRelation`,
`createDaDaoFileFolder` and `createDaDaoFile` take plain Java types with no METS in their
signatures. `DaoProcessor.findOrCreateFileFolder` splits an `href` on `/` and builds a folder chain
segment by segment — that logic maps directly onto a filesystem path.

But everything *around* those factories is METS/PREMIS-bound:

- **`DaAip` is created only from DA input.** `PackageInfoService.processPackageInfo` creates it
  from a `PACKAGE-INFO.xml` (`AIP_ID`, `FONDS_ID`, `INSTITUTION_ID`, `AIP_VERSION`, `AIP_SIZE`);
  the only other `new DaAip()` is the placeholder `DaService.recordAipProblem` makes for an AIP
  whose download or processing failed before ELZA knew it.
- **`DaDao` can only be created by `DaoProcessor`**, whose constructor takes `MetsType` and
  `PremisComplexType`. `DaService.doCreateDaoStructure` hard-requires `METS.xml` and `PREMIS.xml`
  inside a ZIP registered in `DaLocalCache`.
- **`getComponent` reads every file out of the stored AIP ZIP** (the requested entry, spooled to a
  temporary file when large). For a filesystem repository, where the bytes are *already on disk in
  their final place*, this is strictly worse than the direct read.
- **The sync machinery is hard-filtered to DA.** `DaScheduler` skips repositories that are not
  `DigitalRepositoryType.DA`, and `DaConnector` throws for anything else.
  `DaRemoteRepositorySync.nextQuery` is an opaque *remote* cursor; a filesystem needs mtime
  scanning instead.
- **The browse contracts do not converge.** `ExplorerTreeNode` is an eager, complete, recursive
  tree with back-references and no paging; `FsItems` is a flat, paged, one-level-at-a-time
  listing. A filesystem has exactly one hierarchy, so `parentFolderLogical` has no source.

**There is no abstraction over repository backends.** `DigitalRepositoryType` is a bare 3-value
enum, and behavioural dispatch on it occurs in five classes (`DaConnector`, `DaScheduler`,
`ExternalSystemController`, `FileSystemRepoService.isFileSystemRepository`, and
`ArrDigitalRepositoryVO` for the administration save path). DA, FILESYSTEM and WSDL are three
disjoint parallel code paths sharing one entity table — not polymorphic siblings. The filesystem
branch is consulted from browsing, linking, the DAO panel, URL construction and the
settings-clearing save path, which is the concrete shape the SPI of §3.3 point 2 would absorb.

### 3.3 Decision: a specialized backend behind a shared browsing contract

The filesystem repository requires specialization and is not treated as just another
implementation of a generic DAO/AIP repository.

1. **Keep the filesystem repository specialized.** Its natural hierarchy is the directory tree, it
   is inherently mutable, and its content requires no fixity or preservation-event modelling.
   Browsing, linking and the DAO panel are all live reads; no per-file persistence exists.

2. **Unify at the browsing contract and the SPI, not at the entity model.** A
   `DigitalRepositoryBackend` interface — `list(path, sort, filter, cursor)`, `open(path)`,
   `link(path, node)`, `refresh(path)` — with a `FileSystemBackend` and later a `DaBackend`.
   *Not built (§4).* `FileSystemRepoBrowser` gives the browse logic a home but is fs-specific — a
   precursor of `FileSystemBackend`, not the SPI itself.

3. **Generalize the paged contract rather than adopting `ExplorerTreeNode`.** `FsItems` paging is
   the correct shape; extending `FsItem`/`FsItems` into a backend-neutral `RepoItem`/`RepoItems`
   is the convergence point. *Not built (§4).* The contract (sort, filters, paging, `truncated`)
   is backend-neutral, and the opaque `lastKey` cursor of §1.2 keeps that property — its contents
   are the backend's business, never the client's.

4. **Where a filesystem folder genuinely *is* an ingest candidate**, provide an explicit,
   user-initiated "ingest this folder as an AIP" action that mints a real `DaAip` with a
   deliberate code and version: a `FilesystemAipProcessor` sibling to `DaoProcessor`, reusing the
   generic `DaService` factories plus a non-PREMIS variant of `PackageInfoService`.
   *Optional, not built (§4).*

5. **Link-status queries.** One batch query per repository, **globally scoped** (no fund predicate,
   §5 item 3). `FsItem.links` exposes `nodeId`/`fundId`/`fundName`/`nodeLabel`/`nodePath`
   cross-fund, rendered as clickable references in the popover, with `FsLink.readable = false`
   marking links whose fund/nodes the resolver could not read.

### 3.4 Why filesystem links need no specialized link model

*(Rationale for the link model documented in `da-migration.md` §2.1.)*

Filesystem linking is not a special case. `ArrDaoLink.LinkType` encodes *what level of the
repository hierarchy a link points at* — `AIP` (whole package, member reference null) versus
`PART_AIP`/`COMPONENT_AIP` (something inside it) — i.e. **container reference + optional member
reference**. A filesystem target is the same shape: whether the user selects the repository root, a
folder or a single file, it is always *(repository, relative path)*. DA links never address an
individual file either — the finest DA granularity is a `DaDao` of type `FILE`, a node in the
hierarchy, not a byte stream — so both models stop linking at the same level.

The result is one link model: an abstract `ArrDaoLink` base (`@Inheritance(JOINED)`) carrying the
shared spine, with `ArrLegacyDaoLink`, `ArrDaLink` and `ArrFsLink` as JOINED subtypes. JOINED is
chosen over SINGLE_TABLE deliberately — consistency with the `ArrData` / `ArrItem` / `ArrRequest`
precedents, and each subtype table declares real NOT NULL constraints on its target columns, so a
link's shape is *which subtype row exists* rather than a `CHECK` over nullable column groups.

Two consequences bind new work:

- **Cached link JSON binds to fully-qualified class names** (`@JsonTypeInfo(Id.CLASS)`, the
  `ArrData` precedent) — renaming a link entity class invalidates `arr_cached_node` rows; pair any
  rename with a cache-invalidation changeset (the startup sync rebuilds dropped rows).
- **Nothing at the DB level prevents child rows in *two* subtype tables** for one `dao_link_id`.
  Hibernate never creates that state and JOINED type resolution would break on it;
  `da-migration.md` Phase 5 validation keeps a verification query for it.

---

## 4. Open work

Follow-ons the feature leaves, in the order they become worth doing; nothing here blocks anything
else.

1. **N5 — batch the per-DAO disk walks** (§2). Deferred by choice; revisit only if nodes carrying
   many filesystem links appear in practice.
2. **The `DigitalRepositoryBackend` SPI** (§3.3 points 2 and 3) — the backend interface plus the
   backend-neutral `RepoItem`/`RepoItems` the `FsItem`/`FsItems` contract is shaped to become. It
   pays off only when a second backend is actually written against it: doing it earlier means
   designing an abstraction from a single implementation. `FileSystemRepoBrowser` is already the
   shape a `FileSystemBackend` would take, so the cost of waiting is low.
3. **Ingest-as-AIP** (§3.3 point 4) — the only sanctioned way a filesystem folder acquires real
   `DaAip` semantics. Optional; only if the need is asked for.
4. **`da-migration.md` Phases 4–5** — the WSDL decision and the removal of the `arr_dao` family;
   independent of everything above.

---

## 5. Decisions

1. **Filesystem repositories stay specialized** and are not migrated onto `DaAip`/`DaDao`
   (§3.3).

2. **Links reuse the `arr_dao_link` model, specialized by JOINED inheritance** (§3.4) — the entity
   hierarchy `ArrLegacyDaoLink` / `ArrDaLink` / `ArrFsLink` under an abstract `ArrDaoLink` base.
   `arr_fs_link` is a JOINED subtype of the one link model, not a parallel link model for
   filesystem repositories.

3. **"Unlinked" is scoped globally, not per-fund.** `findLinksByDigitalRepository` filters on the
   repository and `delete_change_id IS NULL` only — no fund predicate. A user browsing fund A sees
   paths linked from fund B as linked, and `UNLINKED` hides them; the question answered is "has
   this file already been catalogued anywhere". The linking nodes are exposed cross-fund
   (clickable, with `FsLink.readable` marking unresolvable funds).

4. **Repository visibility stays as it is — C8 is not actioned.** Every configured filesystem
   repository remains visible to any user who can read a fund.

5. **A filesystem repository carries no external-system settings.** The fields that address a
   *remote* repository are not merely hidden for `FILESYSTEM` — they are cleared, because a value
   that is invisible but still consulted is worse than one that is visible. Per-field evidence:
   `sendNotification` — both notification sites skip filesystem repositories explicitly
   (`DaoService`); `viewDaoUrl` — `getDaoUrl` returns `null` for them before reading it;
   `username`/`password` — read only by `DaConnector`, i.e. the DA/WSDL connection. `viewFileUrl`
   and `viewThumbnailUrl` would still override the built-in `item-data` URL in `getDaoFileUrl`,
   but clearing them on save means the built-in URL is always used: an override cannot be
   administered through a hidden field, and the built-in endpoint is the one correct answer for a
   repository ELZA serves itself. What stays configurable: `url`, `elzaCode`
   (templated-root parameters) and `multipleLinks` (enforced by `createFsDaoLink`).

---

## 6. Verification

Where the automated coverage lives, and what each class is responsible for — extend the matching
one rather than starting somewhere new:

| Test class | Covers |
|---|---|
| `FileSystemRepoServiceTest` | Path containment incl. traversal inputs, MIME detection, inline-renderable classification |
| `FileSystemRepoBrowserTest` | Browse sort/filter/paging incl. Czech collation, last-change ordering, keyset paging across an insert or delete between pages (§1.2); link resolution incl. multi-fund rows; `listDaoFiles` recursive/cap/missing-path; repository availability incl. the templated-root asymmetry; `testRepository` per failure mode; `normalizeRelatPath` and the templated-URL helpers |
| `DaoServiceFsLinkTest` | `createFsDaoLink` and `moveFsDaoLink` — `multiple_links` enforcement, idempotent re-link on the same node, containment check on the link target, the link-move flow |
| `ArrDigitalRepositoryVOTest` | Clearing of the settings of §5 item 5, pass-through for other repository types, `file://` strip |
| `DaoCoreServiceTest` | SOAP DAO flow — the regression guard that the filesystem code leaves it untouched |

**Manual acceptance — the checks worth repeating before a release:**

- **Frontend.** Hit "Obnovit" (or switch tabs onto the filesystem tab) and confirm the repository
  list and the tree collapse, then reload — the tree's children cache is wiped and the expansion
  markers stay in sync with it. Point a repository at a missing path, verify it appears greyed
  with an explanation (§1.1), fix the path from administration, hit "Obnovit" and confirm the tree
  updates *without* reopening the dialog. Disconnect the server or block the API and confirm both
  panels render error placeholders instead of empty space. Switch the sort selector across all six
  orders and confirm the listing restarts from the first page each time (§1.2).
- **Scale.** Benchmark a 50 000-entry directory on a network share — the single-pass listing, the
  paged `hasChildren` probe and the keyset slice are unmeasured at that size; the worst case is a
  folder containing many files and no subfolder, with a page size that surfaces every folder.
  Since every page re-scans the directory (§1.2), the cost of "load more" is the number to watch.
- **Repository configuration.** A templated root (`{fundId}`) that resolves for one fund and not
  another stays hidden for the second; the administration test reports each failure mode
  distinctly; saving a filesystem repository clears the settings of §5 item 5.

**Running the application:** standard Maven/Spring Boot launch for `elza-core` plus the
`elza-react` dev server; the browser is reached from a fund's DAO panel and needs at least one
external system of type `FILESYSTEM` whose `url` holds a plain path to a readable directory.
