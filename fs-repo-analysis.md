# FileSystemRepoService — architectural analysis and current state

**Status: the #9944 filesystem-repository work is CLOSED (2026-08-12); every open item below was
re-verified against the code on 2026-08-14.** The feature is fully on its target architecture:
browsing, panel files and downloads are live disk reads; links are `ArrFsLink` rows targeting
`(repository, path)`; `multiple_links` is enforced; filesystem repositories persist **zero**
entities (cleanup changeset `20260812110000`). What remains open (§7) are enhancements outside
#9944's scope — the `DigitalRepositoryBackend` SPI, the frontend D-items, and the C2/N5 scale
items — plus the DA-side Phases 4–5 tracked in `da-migration.md`.

**A follow-up wave landed after the closure** and is folded into the sections below: C5 resolved
by the single-pass `walkFileTree` rewrite, `FsItem.hasChildren` driving the tree's expand marker,
link move/relink (`fundFsMoveDAOLink`, `nodeFsRelink`), and the repository-configuration work of
§1.1 — unavailable repositories stay visible, an administration action tests a repository, the
list is name-ordered, and settings that do not apply to a filesystem repository are hidden and
cleared. The 2026-08-14 review opened three new findings (N6, N7, D7) and confirmed one coverage
gap (§9).
*(History: original analysis 2026-07-28, decisions settled the same day (§8); #9944 delivered §7
Phases 0–2 and part of Phase 4 by 2026-08-06; step 3a removed persisted file trees 2026-08-10;
step 3b landed the link model 2026-08-11.)*
**Scope:** `FileSystemRepoService` and the filesystem-repository browser feature
**Related:** `elza/da-migration.md` — its Phase 3 was revised to match §5.3 of this document and
is fully implemented (steps 3a + 3b); remaining DA-side work (Phases 4-5) is tracked there

---

## Context

The filesystem-repository browser (digital repositories of type `FILESYSTEM`) was originally built as a quick
experiment, became popular with users, and has now been promoted to a permanent, supported part of
Elza. The #9944 series delivered the promotion: sorting, filtering (type / link state / substring),
paging, per-item link popovers with clickable node references, and refresh of listings.

A second question drove this analysis: whether the filesystem repository should keep its own
specialized implementation, or be migrated onto the newer DA/AIP (EARK) architecture. §5 answers
that in detail; the conclusion (§5.3) is that it stays specialized, and `da-migration.md` Phase 3
was rewritten to match. That section remains the reviewable decision record.

**A note on phase numbering.** Both documents use "Phase N" for different things. Throughout this
document, *unqualified* phase numbers refer to the delivery sequencing in §7; migration phases are
always written as "`da-migration.md` Phase N".

---

## 1. What the feature is today

| Layer | Location | Role |
|---|---|---|
| Contract | `elza-development/typespec/main.tsp` (`FsRepo`, `FsItem`, `FsItems`, `FsLink`, `DigitalRepositoryTestResult`) | Paged, sorted, filtered browse contract; `FsItem.links` carries node references incl. a `readable` flag for funds the resolver could not read; `FsItem.hasChildren` drives the tree's expand marker; `FsRepo.available` marks a repository whose root is not reachable |
| Browsing | `FileSystemRepoBrowser` (`browseItems`, `listRepos`, `listDaoFiles`, `testRepository`) | All listing logic; live disk reads, nothing persisted |
| Primitives | `FileSystemRepoService` | Containment-checked path resolution, MIME detection, input streams, templated-URL helpers (`isTemplatedUrl`, `getFixedUrlPrefix`); no persistence at all since step 3b |
| Linking | `FundController.fundFsCreateDAOLink` → `DaoService.createFsDaoLink`; move via `fundFsMoveDAOLink` / `NodeController.nodeFsRelink` → `DaoService.moveFsDaoLink` | Mints the `ArrFsLink` (`digital_repository_id` + `path`, no `ArrDao`), enforces `multiple_links`, returns `daoLinkId`; move re-points a live link at another node atomically |
| Administration | `ExternalSystemController.externalSystemTestDigitalRepository`; `ArrDigitalRepositoryVO.createEntity` | ADMIN-only configuration test (root reachable + sample of the root); settings that do not apply to a filesystem repository are cleared on save (§1.1) |
| Panel files | `DaoService.listFsLinkFiles` + `ClientFactoryVO.createFsDaoList` | fs links render as synthesized `ArrDaoVO`s (wire id `-daoLinkId`); files listed live via `listDaoFiles` (recursive, flat, path-ordered, capped at `DAO_FILE_LIMIT` = 1000); package DAOs of other repository types read persisted `arr_dao_file` rows |
| Download | `FundController.fundFsRepoItemData` | Single authorized endpoint (`FUND_RD`-checked); streams a `FileSystemResource`, so Content-Length and HTTP Range work; MIME-typed, inline for renderable types |
| UI | `elza-react/src/components/arr/daos/file-system-browser/{FileSystemBrowser,Tree}.tsx`; DAO panel `ArrDaos.jsx`/`ArrDao.jsx` | Tree + list with toolbar filters and link popovers; panel viewer consumes `ArrDaoVO.fileList` |

Link state is resolved by `ArrFsLinkRepository.findLinksByDigitalRepository` — a single query per
repository with **no fund predicate** (§8 item 3), reading `(digital_repository_id, path)` from
`arr_fs_link` directly.

### 1.1 Repository configuration and availability

A repository whose configured root does not exist used to be dropped from `fundFsRepos` silently —
no entry in the tree, no log line, nothing to diagnose from. Current behaviour:

- **A fixed root that is missing is reported, not hidden.** `listRepos` returns the repository with
  `available = false` and logs a WARN. The tree renders it greyed with a warning icon and it cannot
  be expanded; selecting it explains why in the file pane instead of firing a request that would
  fail. An unresolvable configuration (blank URL) is reported the same way.
- **A missing *templated* root is still skipped**, and that asymmetry is deliberate: when the URL
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
  is forced to `false` on save (`ArrDigitalRepositoryVO.createEntity`); existing rows were cleaned
  by changeset `20260812120000`. Rationale and the per-field evidence: §8 item 5.

---

## 2. Correctness defects — all resolved in #9944

Kept as a compact record; the detailed write-ups are in this document's git history.

| Id | Defect | Resolution |
|---|---|---|
| A1 | `createDao` never assigned parent file groups (variable shadowing) — folder hierarchy lost | Code fix + repair changeset `20260728120000`; the whole concern is obsolete since step 3a stopped persisting trees and `arr_dao_file_group` was dropped |
| A2 | MIME detection recognized only JPEG | `Files.probeContentType` + name-based fallback table; inline-renderable allowlist |
| A3 | Path traversal in `resolvePath` | `resolveInsideRoot`: normalize + `startsWith(root)` assertion on every resolve |
| A4 | Two download endpoints with inconsistent authorization | Consolidated into `fundFsRepoItemData` with `@AuthMethod(FUND_RD…)`; the `DmsController` digirepo endpoint is gone |
| A5 | File size truncated to 32 bits | `FsItem.size` is `int64`; no cast |
| A6 | Unguarded iterator; silent listing truncation | Per-entry attribute read failures are skipped with a warning; `FsItems.truncated` flag reports the scan cap |
| A7 | Link sync aborted on non-regular files | Obsolete — step 3a removed the sync entirely (the interim skip-and-report `skippedEntries` shipped and was then removed from the contract with it) |
| A8 | `Collectors.toMap` crash on duplicate codes | Obsolete — the collecting code was removed in step 3a |
| A9 | Wrong `ArrDaoPackage` in multi-fund deployments | `findAllByDigitalRepositoryAndFund` + package code unique per (repository, fund) |

## 3. The cache — resolved

`FileSystemImage` and the Guava cache were deleted in #9944 (they cached nothing and broke
templated repository URLs). Every read is live; the preview path shares `fundFsRepoItemData`.

---

## 4. Structural weaknesses

Resolved in #9944 / step 3a: **C1** (browse logic extracted into `FileSystemRepoBrowser`),
**C3** (`pageSize` parameter replaced the `_DEBUG` hack), **C4** (locale-aware `Collator` sorting
with sort/direction/folders-first parameters), **C6/C7** (fs read endpoints are
`@Transactional(readOnly = true)` and the download returns a `FileSystemResource`, so the body
streams outside the transaction with Content-Length and HTTP Range support), **C9** (linking no
longer walks the tree at all — nothing heavy left in the request).

Still open:

### C2 — Pagination is offset-as-`lastKey`, recomputed from scratch

`FileSystemRepoBrowser.browseItems` re-lists and re-sorts the whole directory on every "load
more", then slices at the offset carried in `lastKey`. The `truncated` flag now reports the
`SCAN_CAP` (10 000), so the silent-truncation half of the original finding is fixed, but paging is
still **not stable**: a file added between pages shifts entries so the client silently skips or
duplicates rows. Since the sort key is a request parameter, an honest keyset cursor must encode
the sort key and direction.

### C5 — Per-entry attribute reads *(resolved 2026-08-12)*

`browseItems` walked the directory with `Files.list` + one `Files.readAttributes` per entry, which
on a network share made the per-entry round trip the dominant cost. It now uses
`Files.walkFileTree` with `maxDepth = 1`, taking `BasicFileAttributes` from the visitor callback —
one pass, no separate stat — matching what `listDaoFiles` always did. **But the win is partly given
back for directories: see N6.**

### C8 — No authorization on repository visibility *(accepted, not a defect)*

`fundFsRepos` returns every `FILESYSTEM`-typed external system to anyone who can read the fund.
**Decision (2026-07-28): accepted, will not be changed** — see §8 item 4. Recorded so it is not
re-raised as a finding later. It does not relax A3/A4, which are fixed.

Re-confirmed 2026-08-14 with one note: since §1.1, the response also includes repositories whose
root is missing, still carrying the resolved server path in `FsRepo.path`. The audience is
unchanged (any fund reader could always see `path` for a working repository), so this does not
reopen C8 — but it does mean a misconfigured path is now visible to non-administrators too.

### New findings (2026-08-10)

- **N1 — The DAO panel's live listing cap is silent.** `DaoService.getDaoFiles` caps at
  `DAO_FILE_LIMIT` (1000) and `ArrDaoVO` has no `truncated` indicator, so a linked folder with
  more files shows exactly 1000 path-ordered entries and `fileCount` tops out at 1000 with no hint
  that the list is incomplete. Same family as the fixed A6/C2 concerns.
  *Resolved 2026-08-11:* `ArrDaoVO` carries a truncation flag populated by the live listing and
  the DAO panel renders the indicator.
- **N2 — Live-listed files have synthetic negative ids.** `ClientFactoryVO` assigns
  `-N` wire ids (unique within one response) to files without a persistent id. The panel's
  selection round-trips these ids through component state; they are **not stable across
  requests** — client code must not persist them.
- **N3 — `multiple_links` is declared but not enforced.** The flag exists on
  `arr_digital_repository` and in the admin UI; link creation does not check it.
  *Resolved 2026-08-11 (step 3b):* `DaoService.createFsDaoLink` enforces it as
  "is `(repository, path)` already live-linked"; the legacy package-DAO path enforces it per DAO
  in `createOrFindDaoLink`.
- **N4 — Repository type enum and URL can disagree.** `DaoCoreServiceTest` creates a repository
  with `DigitalRepositoryType.FILESYSTEM` but a NULL `url`; `isFileSystemRepository()` (URL-prefix
  check) then treats it as non-filesystem, which is why the SOAP fixture works at all. The
  `digital_repository_type` column was heuristically backfilled and was **not** a reliable
  discriminator — before the fix, code and migrations had to key off `url LIKE 'file://%'`
  (the step 3a changesets do; they predate the fix).
  *Resolved 2026-08-11* (changeset `20260811135500`): rows with a `file://` URL were re-typed
  `FILESYSTEM` one final time from the URL evidence, the `file://` prefix was stripped so the URL
  column holds a plain path, and `isFileSystemRepository()` now keys off
  `digitalRepositoryType == FILESYSTEM`. The type attribute is the single source of truth from
  here on; URL-prefix classification is no longer possible (and no longer needed).
- **N5 — One disk walk per fs DAO per `findDaos(detail)` request.** Acceptable for typical
  per-node link counts; revisit (batching or caching) only if nodes with many filesystem links
  become common. *Still open, still deferred (re-verified 2026-08-14).*

### New findings (2026-08-14)

- **N6 — `hasChildren` reintroduces a per-directory probe, and its worst case is a full stat
  sweep.** `directoryHasSubfolders` opens a `DirectoryStream` filtered by `Files::isDirectory` for
  **every folder in the listing** and stops at the first subfolder found. For a folder whose
  subfolders sort early this is cheap; for a folder containing many files and *no* subfolder it
  stats every child before answering `false`. Listing a directory of N such folders is therefore
  O(total entries beneath them), which is exactly the network-share cost C5 removed — reinstated
  for the folder half. Options, cheapest first: probe only when the entry survives all filters and
  lands on the requested page; cache the answer per (repository, path, mtime); or drop the eager
  probe and let the client show the expand marker optimistically, correcting on the empty result.
- **N7 — Availability checks have no timeout.** `listRepos` runs `Files.isDirectory` per
  repository on every open of the browser, and `testRepository` walks the root. On an unreachable
  network mount these block for the OS mount timeout, so one dead mount can stall
  `GET /fund/{fundId}/fsrepos` for every user of every repository — the check added to *surface*
  breakage becomes the thing that hangs on it. A bounded check (short-lived task with a timeout,
  the result treated as `available = false` on expiry) would keep the listing responsive; the
  admin-initiated test may block longer, but should report a timeout rather than the request dying.

---

## 5. Filesystem repository vs. the DA/AIP architecture

The central architectural question was whether to keep the filesystem repository as a specialized
implementation or migrate it onto the DA/AIP model. It is **settled — it stays specialized**
(§5.3), and both steps of the resulting plan (3a and 3b) are implemented. This section stays as
the decision record, because the conclusion ran against what `da-migration.md` originally
scheduled and the reasoning should be reviewable rather than taken on trust.

### 5.1 What the DA model actually requires

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
2. **The DA model is write-once + change-versioned.** All seven mutable entities carry
   `create_change_id NOT NULL` / `delete_change_id NULL`, and `DaoProcessor.java:227-240` shows
   the discipline: content changes close old rows and insert new ones. **A filesystem repository
   is mutable by nature** — files appear, change and vanish outside Elza's control. Mirroring a
   live mutable directory into a write-once versioned store means generating a `DaChange` on every
   detected difference, which is a preservation-grade audit trail for something that is not a
   preservation store.

This is the substance of the concern: the AIP model *can* express a hierarchy, but its hierarchy
carries semantics (representation vs. logical structure, versioned fixity, ingestion events) that
a filesystem cannot supply. Synthesising those fields produces a record that looks like
preservation metadata but is not.

### 5.2 What migration would actually cost

The optimistic reading is that `DaService`'s entity factories are generic — and they genuinely
are. `createDaDao` (`DaService.java:893`), `createDaDaoRelation` (`:903`), `createDaDaoFileFolder`
(`:911`), `createDaDaoFile` (`:920`), `createDaDaoItem` (`:944`) all take plain Java types with no
METS in their signatures. `DaoProcessor.findOrCreateFileFolder` already splits an `href` on `/`
and builds a folder chain segment by segment — that logic maps directly onto a filesystem path.

But everything *around* those factories is METS/PREMIS-bound:

- **`DaAip` can only be created by parsing PREMIS.** `PackageInfoService.processPackageInfo` is
  the **only** `new DaAip()` in the main source tree. It requires a `PACKAGE-INFO.xml` from which
  it reads `AIP_ID`, `FONDS_ID`, `INSTITUTION_ID`, `AIP_VERSION`, `AIP_SIZE`.
- **`DaDao` can only be created by `DaoProcessor`**, whose constructor takes `MetsType` and
  `PremisComplexType`. `DaService.doCreateDaoStructure` hard-requires `METS.xml` and `PREMIS.xml`
  inside a ZIP registered in `DaLocalCache`.
- **`getComponent` unzips the entire AIP into a fresh temp directory on every single file
  request.** For a filesystem repository, where the bytes are *already on disk in their final
  place*, this is strictly worse than the current direct read.
- **The sync machinery is hard-filtered to DA.** `DaScheduler.java:51` filters
  `DigitalRepositoryType.DA`, and `DaConnector.get()` (`DaConnector.java:106`) throws for anything
  else. `DaRemoteRepositorySync.nextQuery` is an opaque *remote* cursor; a filesystem needs mtime
  scanning instead.
- **The browse contracts do not converge.** `ExplorerTreeNode` is an eager, complete, recursive
  tree with back-references and no paging; `FsItems` is a flat, paged, one-level-at-a-time
  listing. A filesystem has exactly one hierarchy, so `parentFolderLogical` has no source.

There is also a general observation worth recording: **there is still no abstraction over
repository backends.** `DigitalRepositoryType` is a bare 3-value enum, and behavioural dispatch on
it occurs at three sites (`DaConnector.java:106`, `DaScheduler.java:51`, and
`FileSystemRepoService.isFileSystemRepository` since N4 re-based the predicate on the type;
re-verified 2026-08-14). DA, FILESYSTEM and WSDL are three disjoint parallel code paths sharing one
entity table — not polymorphic siblings. The third site is the one that grew: the filesystem branch
is now consulted from browsing, linking, the DAO panel, URL construction and the administration
save path, which is the concrete shape the Phase 3 SPI would absorb.

### 5.3 Decision: a specialized backend behind a shared browsing contract

> **Agreed 2026-07-28.** The filesystem repository requires specialization and cannot be treated
> as just another implementation of a generic DAO/AIP repository. `da-migration.md` Phase 3 was
> revised to match; step 3a of that plan is implemented (2026-08-10).

1. **Keep the filesystem repository specialized.** Its natural hierarchy is the directory tree, it
   is inherently mutable, and its content requires no fixity or preservation-event modelling.
   *Status: implemented — browsing, linking and the DAO panel are all live reads; no per-file
   persistence remains.*

2. **Unify at the browsing contract and the SPI, not at the entity model.** Introduce a
   `DigitalRepositoryBackend` interface — `list(path, sort, filter, cursor)`, `open(path)`,
   `link(path, node)`, `refresh(path)` — with a `FileSystemBackend` and later a `DaBackend`.
   *Status: open. `FileSystemRepoBrowser` gives the browse logic a home but is fs-specific — a
   precursor of `FileSystemBackend`, not the SPI itself.*

3. **Generalize the paged contract rather than adopting `ExplorerTreeNode`.** `FsItems` paging is
   the correct shape; extending `FsItem`/`FsItems` into a backend-neutral `RepoItem`/`RepoItems`
   is the convergence point. *Status: open; the #9944 contract additions (sort, filters, paging,
   `truncated`) were designed to be backend-neutral.*

4. **Where a filesystem folder genuinely *is* an ingest candidate**, provide an explicit,
   user-initiated "ingest this folder as an AIP" action that mints a real `DaAip` with a
   deliberate code and version: a `FilesystemAipProcessor` sibling to `DaoProcessor`, reusing the
   generic `DaService` factories plus a non-PREMIS variant of `PackageInfoService`.
   *Status: open, optional (§7 Phase 6).*

5. **Link-status queries.** Implemented as `DaoLinkRepository.findLinksByDigitalRepository` — one
   batch query per repository, **globally scoped** (no fund predicate, §8 item 3). The §7 Phase 2
   preference to omit node references was decided the other way in implementation: `FsItem.links`
   exposes `nodeId`/`fundId`/`fundName`/`nodeLabel`/`nodePath` cross-fund, rendered as clickable
   references in the popover, with `FsLink.readable = false` marking links whose fund/nodes the
   resolver could not read. Since step 3b the query lives on `ArrFsLinkRepository` and reads
   `(digital_repository_id, path)` from `arr_fs_link` directly — same tuple shape, no join.

### 5.4 How filesystem links are represented — no specialization needed

*(This was the specification for `da-migration.md` step 3b — **implemented 2026-08-11**. Kept as
the rationale record; the implemented model is summarized in `da-migration.md` §2.)*

An earlier draft proposed either adding `path` columns to `arr_dao_link` or creating a separate,
self-standing link table for filesystem repositories. **Both were wrong in the same way** — they
treated filesystem linking as a special case instead of recognizing that `arr_dao_link` already
has the abstraction filesystem linking needs. (The 2026-08-11 revision below does introduce an
`arr_fs_link` table, but as a JOINED subtype of the one link model — not as the parallel link
model rejected here.)

**The existing DA link model is granularity-aware.** `ArrDaoLink.LinkType`
(`ArrDaoLink.java:85-89`) encodes *what level of the repository hierarchy the link points at*:

| `LinkType` | `aip` | `daDao` | Meaning |
|---|---|---|---|
| `AIP` | set | **null** | whole package |
| `PART_AIP` | set | set | a sub-node of the package |
| `COMPONENT_AIP` | set | set | a leaf component |

So "link to the whole thing" versus "link to something inside it" is **already** a first-class
concept in this table, expressed as *"container reference + optional member reference"* — and the
member reference is nullable precisely so the whole-container case can reuse the same row shape.

**Filesystem linking is the same shape.** At the time of this analysis `fundFsCreateDAOLink`
created an `ArrDao` whose `code` *was* the repository-relative path, then linked to it. Whether
the user selects the repository root, a folder, or a single file, the target is always
*(repository, relative path)* — a container plus a member, exactly like *(AIP, DaDao)*.

Note also that **DA links never address an individual file either**: there is no `DaDaoFile`
reference anywhere on `ArrDaoLink`. The finest DA granularity is a `DaDao` of type `FILE`, which
is a *node in the hierarchy*, not a byte stream. A filesystem path naming a single file is the
direct analogue. The two models agree on where linking stops.

**Consequence: one link model — an abstract base with three JOINED specializations.**

> **Decided (2026-08-11), supersedes the plain-column form agreed 2026-07-28.** The
> container+member analysis above stands; what changed is how the three target shapes are
> expressed. Instead of three nullable column groups on one flat table guarded by a `CHECK`
> constraint, the link becomes an entity hierarchy — the generic-type + specialization pattern
> this codebase already uses for `ArrData`, `ArrItem`/`ArrDescItem` and `ArrRequest`. JOINED
> tables were chosen over SINGLE_TABLE deliberately: consistency with those precedents, and each
> subtype table can declare real NOT NULL constraints on its target columns, which one shared
> table of nullable groups never could. (SINGLE_TABLE + `@DiscriminatorFormula` was evaluated as
> the lighter alternative — same schema as the plain-column form, no cache impact — and rejected
> in favour of schema-level typing.)

- `ArrDaoLink` becomes the **abstract base** (`@Inheritance(JOINED)`) on table `arr_dao_link`,
  keeping the shared spine: `node_id`, `create_change_id`, `delete_change_id`, `link_type`.
  `link_type` stays on the base — granularity is a cross-backend concept — and needs only
  backend-neutral naming (`CONTAINER` / `PART` / `COMPONENT`, retaining the existing values as
  aliases so DA rows need no data migration).
- `ArrLegacyDaoLink` → table `arr_legacy_dao_link` (`dao_id` **NOT NULL**, `scenario`) — the
  transitional shape; Phase 5 deletes the class and drops the table.
- `ArrDaLink` → table `arr_da_link` (`aip_id` **NOT NULL**, `da_dao_id` nullable — NULL means the
  whole AIP, consistent with today's `LinkType.AIP`).
- `ArrFsLink` → table `arr_fs_link` (`digital_repository_id` **NOT NULL**, `path` nullable — NULL
  means the repository root).

**Exclusivity becomes structural.** A link's shape is which subtype row exists, so the previously
planned `CHECK` constraint over nullable column groups is replaced by the per-subtype NOT NULL
constraints — a stronger guarantee. This also retired the former "nullable `dao_id`" risk at the
root: `dao_id` is NOT NULL again, inside `arr_legacy_dao_link`. One residual gap: nothing at the
DB level prevents child rows in *two* subtype tables for one `dao_link_id` (Hibernate never
creates that state, and JOINED type resolution would break on it) — Phase 5 validation keeps a
verification query for it.

**IMPLEMENTED 2026-08-11** (changesets `20260811180000-180005`, in the last-included changelog
file per the 2026-08-12 reorganization). Notes that survived implementation and still matter:

- The step 3b wave is guarded by HALT preconditions (no legacy data under DA-type repositories,
  exactly one target group per row) — a halt means "inspect the data", see `da-migration.md`.
- Cached link JSON binds to fully-qualified class names (`@JsonTypeInfo(Id.CLASS)`, the `ArrData`
  precedent) — renaming the link entity classes invalidates `arr_cached_node` rows; pair any
  rename with a cache-invalidation changeset (the 3b wave shows the pattern; the startup sync
  rebuilds dropped rows).
- The filesystem `ArrDao` anchor rows and their packages initially remained as unreferenced
  orphans (still visible in the package and unassigned-entity tabs); cleanup changeset
  `20260812110000` removed them together with their request references — filesystem repositories
  now persist **zero** entities.
- `LinkType` still carries `AIP`/`PART_AIP`/`COMPONENT_AIP` — the backend-neutral rename was
  deferred because the enum is part of the OpenAPI contract (see `da-migration.md`, deferred
  cleanups).
- Node deletion now severs links of all three subtypes; before the split, DA links silently
  survived node deletion (the legacy fetch's `JOIN FETCH dl.dao` filtered them out).

---

## 6. Frontend weaknesses

Partially resolved: the *list* now reloads on sort/filter changes, after link/unlink, and via the
`refreshCounter` prop (the D1 symptom "only reopening the dialog shows new files" is fixed for the
list pane). Still open:

### D1 (tree half) — tree children are fetched once per component lifetime

`Tree.tsx` holds `workingTree` in component state and only ever splices into it; `expandItem`
early-returns when `expandedItems[fullPath] != undefined` (`Tree.tsx:109-110`), so
collapse-then-expand never re-fetches and there is no tree refresh path. Fix: a keyed cache with
explicit `invalidate(path)` / `invalidateSubtree(path)` and re-fetch on expand when stale.

### D2 — Stale-closure races

Both components read state from the closure and write spreads back
(`FileSystemBrowser.tsx:179-180`, `:479`; `Tree.tsx:110`, `:118-128`). Two concurrent expands lose
one another's results. Use functional `setState` updaters.

### D3 — Paths are `/`-joined strings with the repository id prefixed

`extractRepoIdFromFullPath` + `parseInt(repoId, 10)` (`FileSystemBrowser.tsx:154`, `Tree.tsx:58`);
repository id and path stay conflated. Model as `{repoId: number, segments: string[]}`.

### D4 — Missing `key` props on breadcrumb fragments

The breadcrumb row was rebuilt in #9944 (overflow ellipsis, `ResizeObserver`-style fitting) but
the `map` at `FileSystemBrowser.tsx:360-392` still returns keyless `<>…</>` fragments.

### D5 — No loading or error state

Every `await Api.funds…` in both components is unguarded — there is not a single `catch` in either
file (re-verified 2026-08-14) — so a failed listing leaves the tree/list silently empty. Only one
failure mode is now explained rather than swallowed: an unavailable repository, which the server
reports as data (`FsRepo.available`) instead of an error, so the file pane can state the reason
(§1.1). Everything else — a listing that 500s, a download that 404s — is still silent.

### D6 — Two sources of truth for expansion

`Tree.tsx` keeps its own `expandedItems` state (`:44`) *and* receives `expandedItems` /
`onExpandChange` props (`:14-15`), using the props only for notification.

### D7 — The repository list is fetched once and never refreshed *(new 2026-08-14)*

`FileSystemBrowser` calls `fundFsRepos` in a `useEffect` with an empty dependency array, and
`Tree.tsx` rebuilds its `workingTree` on `[repos.length]` — the *count*, not the content. So after
an administrator fixes a broken path, the repository stays greyed until the dialog is reopened;
and any future change that alters a repository's fields without changing how many there are (an
availability flip, a rename) would not reach the tree at all. The `refreshCounter` prop already
exists for the item list — extending it to the repository fetch, and keying the tree effect on the
repository identities rather than their count, is the same fix.

---

## 7. Sequencing

Status of the original delivery plan:

| Phase | Content | Status |
|---|---|---|
| 0 — stop the bleeding | A1–A9, C6 | **Done** (#9944) |
| 1 — give the feature a home | Extract browser service, delete cache, containment-checked resolve; *plus the `arr_dao_link` schema change* | **Done** — the schema change shipped as `da-migration.md` step 3b (2026-08-11) |
| 2 — the contract | Sort/paging/filter parameters, `truncated`, link-state filter, link-operation result shape | **Done.** `linkState` is global (§8 item 3); the link-result shape resolved itself differently — `skippedEntries` shipped with the interim A7 fix and was removed again in step 3a, because linking no longer walks anything |
| 3 — backend SPI | `DigitalRepositoryBackend` + backend-neutral link-status query | **Open** (see §5.3 points 2 and 5) |
| 4 — frontend | Refresh, functional updates, structured paths, loading/error states | **Partial** — list refresh done; D1(tree)/D2/D3/D4/D5/D6/D7 open (§6) |
| 5 — scale and robustness | Keyset cursor (C2), single-pass attributes (C5), async link creation, range requests | **Partial** — range requests and async linking are moot (C7 resolved by `FileSystemResource`; C9 by step 3a); C5 done 2026-08-12; C2 remains, and N6 partly undoes C5 |
| 6 — optional | "Ingest folder as AIP" (§5.3 point 4) | **Open, only if wanted** |

### Recommended next steps *(reviewed 2026-08-14)*

Ordered by value per effort, not by phase number. Nothing here blocks anything else except where
stated.

1. **N6 — bound the `hasChildren` probe.** Highest value: it is a live regression of a fix that
   was just made (C5), it hits exactly the deployment the feature targets (network shares), and it
   is a contained change inside `browseItems`. Probing only entries that reach the returned page is
   the smallest honest fix.
2. **N7 — bound the availability check.** Small, and it protects the endpoint every user hits from
   a single dead mount. Do it together with 1 — both are about not letting the filesystem dictate
   request latency.
3. **§9 coverage gap — test `createFsDaoLink` / `moveFsDaoLink`.** These carry the
   `multiple_links` enforcement (N3's resolution) and the newest feature (link move), and neither
   has a single automated test. Cheapest insurance on this list.
4. **D7 + D5 — refresh and error state.** D7 makes the §1.1 diagnostics actually reach the user
   without reopening the dialog; D5 stops every other failure from looking like an empty folder.
   Together they are what makes the feature feel finished rather than merely correct.
5. **D1(tree)/D2/D6 — the tree's state model.** One coherent rewrite: a keyed cache with explicit
   invalidation, functional `setState` updaters, and a single owner of the expansion state. Doing
   them separately is more work than doing them at once.
6. **C2 — keyset cursor.** Only matters for directories large enough to page *and* mutating while
   browsed. Real, but the rarest of the scale items; needs the sort key and direction encoded in
   the cursor.
7. **N5 — batch the per-DAO disk walks.** Still deferred; revisit only if nodes with many
   filesystem links appear in practice.
8. **D3/D4 — structured paths, breadcrumb keys.** Cleanups; fold into whichever frontend task
   touches those files next.
9. **Phase 3 — the SPI** (`DigitalRepositoryBackend` + `RepoItem`/`RepoItems`, §5.3 points 2 and
   3). Unblocked, but it is an investment that pays off only when a second backend is actually
   written against it. Deliberately last among the code items: doing it now means designing an
   abstraction from one implementation.
10. **Phase 6 — optional ingest-as-AIP** (§5.3 point 4), the only sanctioned way a filesystem
    folder gets real `DaAip` semantics. Only if wanted.
11. **`da-migration.md` Phases 4–5** — the WSDL decision and the removal of the `arr_dao` family,
    tracked there; independent of everything above.

---

## 8. Decisions

All architectural questions raised by this analysis are settled.

1. **Filesystem repositories stay specialized** and are not migrated onto `DaAip`/`DaDao`
   (§5.3, agreed 2026-07-28). `da-migration.md` Phase 3 was revised accordingly; both its steps
   are implemented (3a 2026-08-10, 3b 2026-08-11). The two documents are consistent.

2. **Links reuse the `arr_dao_link` model, specialized by JOINED inheritance** (§5.4; the
   plain-column form agreed 2026-07-28 was revised 2026-08-11 to the entity hierarchy
   `ArrLegacyDaoLink` / `ArrDaLink` / `ArrFsLink` under an abstract `ArrDaoLink` base). Still no
   `arr_dao_target` normalization; `arr_fs_link` now exists as a table, but as a JOINED subtype
   of the one link model — not the parallel link model §5.4 rejected. *Implemented 2026-08-11
   (`da-migration.md` step 3b).*

3. **"Unlinked" is scoped globally, not per-fund** (agreed 2026-07-28). *Implemented:*
   `findLinksByDigitalRepository` filters on the repository and `delete_change_id IS NULL` only —
   no fund predicate. A user browsing fund A sees paths linked from fund B as linked, and
   `UNLINKED` hides them; the question answered is "has this file already been catalogued
   anywhere". The implementation additionally exposes the linking nodes cross-fund (clickable, with
   `FsLink.readable` marking unresolvable funds) — a deliberate departure from the original
   preference to omit node references.

4. **Repository visibility stays as it is — C8 is not actioned** (agreed 2026-07-28). Every
   configured filesystem repository remains visible to any user who can read a fund. This does not
   relax A3/A4, which are fixed.

5. **A filesystem repository carries no external-system settings** (2026-08-14). The fields that
   address a *remote* repository are not merely hidden for `FILESYSTEM` — they are cleared, because
   a value that is invisible but still consulted is worse than one that is visible. Per-field
   evidence: `sendNotification` — both notification sites skip filesystem repositories explicitly
   (`DaoService`); `viewDaoUrl` — `getDaoUrl` returns `null` for them before reading it;
   `username`/`password` — read only by `DaConnector`, i.e. the DA/WSDL connection. `viewFileUrl`
   and `viewThumbnailUrl` *were* honoured as an override of the built-in `item-data` URL; that
   override was given up deliberately — it cannot be administered through a hidden field, and the
   built-in endpoint is the one correct answer for a repository ELZA serves itself. What stays
   configurable: `url`, `elzaCode` (templated-root parameters) and `multipleLinks` (enforced by
   `createFsDaoLink`).

---

## 9. Verification

Automated coverage today (counted 2026-08-14): `FileSystemRepoServiceTest` (10 tests — path
containment incl. traversal inputs, MIME detection, inline-renderable classification),
`FileSystemRepoBrowserTest` (37 tests — browse sort/filter/paging incl. Czech collation, link
resolution incl. multi-fund rows, `listDaoFiles` recursive/cap/missing-path, repository
availability incl. the templated-root asymmetry, `testRepository` per failure mode, name ordering,
`normalizeRelatPath` and the templated-URL helpers), `ArrDigitalRepositoryVOTest` (3 tests —
clearing of settings that do not apply, pass-through for other repository types, `file://` strip),
and `DaoCoreServiceTest` (SOAP DAO flow, unaffected by the fs changes; its repository fixture was
aligned when N4 was resolved, 2026-08-11).

**Coverage gap (2026-08-14):** `DaoService.createFsDaoLink` and `moveFsDaoLink` have **no
automated test at all** — no test source references either. That leaves the `multiple_links`
enforcement (the whole of N3's resolution), the idempotent re-link on the same node, the
containment check on the link target, and the entire link-move feature resting on manual testing.
Worth closing before any refactor touches `DaoService`.

**For the remaining work:**

- **Step 3b — done.** Automated: `FileSystemRepoBrowserTest` covers link resolution incl.
  multi-fund rows against the `arr_fs_link` query; `DaoCoreServiceTest` exercises the SOAP flow
  on the hierarchy; `ChangelogRelocationTest` proves changeset-identity preservation for the
  changelog reorganization. Post-deploy check per installation: the migration guard passed, and
  `SELECT count(*) FROM arr_legacy_dao_link ll JOIN arr_fs_link fl USING (dao_link_id)` is 0.
- **Phase 4 (frontend).** Manual: add a file on disk, refresh, confirm it appears without
  reopening the dialog; collapse and re-expand a tree folder and confirm children re-fetch (the
  D1 tree half — currently broken); point a repository at a missing path and confirm it appears
  greyed with an explanation, then fix the path and confirm the dialog must be reopened before it
  goes live again (D7 — currently the expected, wrong behaviour).
- **Phase 5.** Benchmark a 50 000-entry directory on a network share — C5's single-pass rewrite
  landed unmeasured, and N6's per-folder probe should be measured on the same fixture, with a
  folder containing many files and no subfolder as the worst case. Confirm paging is stable under
  concurrent modification once C2's keyset cursor lands.
- **Repository configuration.** Manual: a templated root (`{fundId}`) that resolves for one fund
  and not another stays hidden for the second; the administration test reports each failure mode
  distinctly; saving a filesystem repository clears the settings of §8 item 5.

**Running the application:** standard Maven/Spring Boot launch for `elza-core` plus the
`elza-react` dev server; the browser is reached from a fund's DAO panel and needs at least one
external system of type `FILESYSTEM` whose `url` holds a plain path to a readable directory
(the `file://` prefix was removed by N4's normalization, changeset `20260811135500`).
