# FileSystemRepoService — architectural analysis and modernization plan

**Status:** analysis + agreed architectural direction — all decisions settled 2026-07-28 (§8);
sections §1–§4 and §6 describe the pre-#9944 state and are kept as the review record
**Implementation status (2026-08-07):** the #9944 series (2026-07-28 → 2026-08-06) delivered §7
Phase 0 in full (A1–A9 fixed, incl. repair changeset `20260728120000`; C6 became
`readOnly = true` rather than removal), Phase 1 except the `arr_dao_link` schema change
(`FileSystemRepoBrowser` extracted, `FileSystemImage` + Guava cache deleted, containment-checked
resolve; **the §5.4 columns were deferred** — see `da-migration.md` revised Phase 3, step 3b),
most of Phase 2 (`pageSize`/`foldersFirst`/`truncated`, sort enum with `Collator`,
`LINKED`/`UNLINKED` filter, substring filter; the link-state question was decided the other way
than §7 Phase 2 preferred — `FsItem.links` exposes node references cross-fund, clickable in the
UI), and parts of Phase 4 (refresh after link/unlink + refresh trigger, popover, draggable
separator). New since this analysis: `arr_digital_repository.multiple_links` flag (changeset
`20260806120000`, admin UI only, not yet enforced at link creation) and '/' path-separator
normalization with data migration (`20260803120000`). Tests added:
`FileSystemRepoBrowserTest`, `FileSystemRepoServiceTest` — §9's "no automated coverage" no longer
holds.
**Step 3a of `da-migration.md` revised Phase 3 shipped 2026-08-10:** filesystem linking no longer
persists `ArrDaoFile`/`ArrDaoFileGroup` trees (the `ArrDao` anchor remains), the arrangement DAO
panel reads files live via `FileSystemRepoBrowser.listDaoFiles`, and `arr_dao_file_group` was
dropped entirely (changesets `20260810120000`/`20260810120001`).
**Scope:** `FileSystemRepoService` and the filesystem-repository browser feature
**Related:** `elza/da-migration.md` — its Phase 3 was revised to match §5.3 of this document

---

## Context

The filesystem-repository browser (`file://` digital repositories) was originally built as a quick
experiment. It became popular with users and is now to be promoted to a permanent, supported part of
Elza, with new functionality: **sorting of files**, **filtering to show only unlinked DAOs**, and
**refresh/reload of listings** (which does not work at all today).

Before adding features it is worth recording what the current implementation actually is, because the
feature is not one class — it is a thin vertical slice spread across four layers, and most of the
logic lives in a controller rather than in the service. That distribution is the root cause of most
of the weaknesses below, and it is precisely the code that the new features must extend.

A second question drove this analysis: whether the filesystem repository should keep its own
specialized implementation, or be migrated onto the newer DA/AIP (EARK) architecture — which is what
`da-migration.md` §4 Phase 3 originally scheduled. §5 answers that in detail; the conclusion (§5.3)
is that it stays specialized, and `da-migration.md` Phase 3 has been rewritten to match.

**A note on phase numbering.** Both documents use "Phase N" for different things. Throughout this
document, *unqualified* phase numbers refer to the delivery sequencing in §7 of this document;
migration phases are always written as "`da-migration.md` Phase N".

---

## 1. What the feature is today

| Layer | Location | Role |
|---|---|---|
| Contract | `elza-development/typespec/main.tsp:183-255`, `:2987-3033` | `FsRepo`, `FsItem`, `FsItems`, 4 ops |
| Browsing | `FundController.java:305-337` (`fundFsRepos`), `:359-480` (`fundFsRepoItems`), `:480-495` (`fundFsRepoItemData`) | **All listing/sorting/paging logic lives in the controller** |
| Linking | `FundController.java:497+` (`fundFsCreateDAOLink`) → `FileSystemRepoService.createDao()` | Creates the `ArrDao`/`ArrDaoFile` tree |
| Preview | `DmsController.java:201-228` (`/api/digirepo/{repoId}/{*filePath}`) | A second, parallel file-serving endpoint |
| UI | `elza-react/src/components/arr/daos/file-system-browser/{FileSystemBrowser,Tree,types}.tsx` | Tree + list on `VirtualList` |

`FileSystemRepoService` itself is only a path-resolution utility plus a DAO-creation routine. The
actual repository-browsing domain logic was never given a home.

---

## 2. Correctness defects

These are worth fixing regardless of which architecture is chosen.

### A1 — `createDao` never assigns a parent file group (dead code + data loss)

`FileSystemRepoService.java:212-227`:

```java
ArrDaoFileGroup parentFileGroup = null;          // declared null
if (!fp.equals(srcItemPath) && !skipItems.contains(parentPath)) {
    ArrDaoFileGroup parentDaoFileGroup = existingFileGroups.get(parentName);   // shadowing local
    if (parentDaoFileGroup == null) { throw ... }
}
ArrDaoFile dff = daoServiceInternal.createDaoFile(relatPath, fileName, parentFileGroup, dao);  // always null
```

`parentDaoFileGroup` shadows `parentFileGroup`, so the outer variable is never assigned. Every
`ArrDaoFile` is persisted with `dao_file_group_id = NULL` even though the groups themselves are
created correctly. **The folder hierarchy of a linked DAO is silently lost in the database.** Since
"show only unlinked DAOs" will be built on this data, this must be fixed first — and existing rows
will need a repair migration.

### A2 — `getMimetype` recognises only JPEG

`FileSystemRepoService.java:250-256` returns `image/jpeg` for `jpg`/`jpeg` and `null` for everything
else. `DmsController.java:218` then downgrades `null` to `application/binary` and adds
`Content-Disposition: attachment`, so PNG/TIFF/PDF previews are impossible by construction. This is
almost certainly why the preview is commented out in `FileSystemBrowser.tsx:156-161`.

Fix: `Files.probeContentType` with a `URLConnection.guessContentTypeFromName` (or Tika) fallback, and
an explicit allowlist of types rendered inline rather than as attachments.

### A3 — Path traversal (security)

`resolvePath(digiRepo, filePath)` (`FileSystemRepoService.java:272-284`) does
`rootPath.resolve(filePath)` with **no containment check**. `DmsController.java:201` passes a
user-supplied `{*filePath}` path segment straight in, guarded only by a global `FUND_RD_ALL`
permission. `../../../etc/passwd` escapes the repository root. The same applies to
`fundFsRepoItems`, `fundFsRepoItemData` and `fundFsCreateDAOLink`.

Fix: every resolve must end with `normalize()` plus a `startsWith(root)` assertion, and there must be
an explicit symlink policy (`toRealPath()` and re-check, or refuse symlinks).

### A4 — Two download endpoints with different security

`fundFsRepoItemData` (`FundController.java:480`) takes a `fundId` but does not use it for
authorization. `/api/digirepo/{repoId}/{*filePath}` (`DmsController.java:201`) requires global
`FUND_RD_ALL`. Neither checks per-fund read permission against the repository. Consolidate to one
endpoint with one authorization rule.

### A5 — File size truncated to 32 bits

`FundController.java:413` casts `(int) attrs.size()`, so **files over 2 GB report negative or
wrapped sizes**. Archival TIFF and video content routinely exceeds this.

The generated Java type is the binding constraint, not the TypeSpec: `main.tsp:245` declares
`size?: integer`, which is arbitrary-precision in TypeSpec but emits `Integer` here. Fix both —
declare `size?: int64` explicitly and drop the cast — so the contract states the intent rather than
relying on emitter defaults. Note `ArrDaoFile.size` is already `long`, so only this path truncates.

### A6 — Unguarded iterator and silent truncation

`FundController.java:396-400` calls `Files.walk(itemPath, 1)` then `it.next()` to skip the root
without checking `hasNext()` — a directory that becomes unreadable mid-request throws
`NoSuchElementException`. Separately, `Files.walk` swallows per-entry `IOException` as silent
omission rather than as an error, so a partially-unreadable directory looks like a complete listing.

### A7 — `syncFilesAndFolders` aborts on non-regular files

`FileSystemRepoService.java:188-190` throws `BusinessException` for anything that is neither a
regular file nor a directory, aborting the entire link operation rather than skipping the entry.

Note both `Files.isDirectory` and `Files.isRegularFile` follow symlinks by default, so an intact
symlink to a normal file or directory is classified correctly and does **not** hit this branch. What
does: broken symlinks, symlinks whose target is unreadable, device and pipe files, and — on Windows —
some reparse points. Rare, but a single such entry anywhere in the subtree fails the whole link.

Fix: skip unrecognised entries with a logged warning, and report them in the result so the user knows
the DAO is incomplete rather than silently partial.

### A8 — `Collectors.toMap` on non-unique keys

`FileSystemRepoService.java:141-144` collects by `getCode()`. Duplicates throw
`IllegalStateException` (an opaque 500) rather than a `BusinessException`. Nothing currently
enforces uniqueness of `ArrDaoFile.code` within a DAO, so this is reachable.

### A9 — `createDao` picks the wrong package in multi-fund deployments

`FileSystemRepoService.java:101-108` calls `findAllByDigitalRepository(digiRep)` and takes
`daoPackages.get(0)`. The query is not fund-scoped, so a DAO can be attached to a different fund's
package. Should be a `findByDigitalRepositoryAndFund` query.

---

## 3. The cache — why refresh does not work

`FileSystemRepoService.java:51-54`:

```java
private Cache<String, FileSystemImage> images = CacheBuilder.newBuilder()
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .removalListener(this)
        .build();
```

Four compounding problems:

**B1 — The cache caches nothing.** `FileSystemImage` (`FileSystemImage.java:20-67`) holds a
`repoPath`, a `rootPath` and a `digiRepId`. No directory listing, no metadata. Every `walk()` and
`getInputStream()` hits the disk anyway. The cache buys zero performance while costing a 5-minute
staleness window on the one thing it does memoize — the `Files.isDirectory(rootPath)` validation in
the constructor. A repository that goes offline still resolves for 5 minutes; one that comes online
is rejected for up to 5 minutes.

**B2 — No invalidation API exists.** There is no `evict(repoId)`, no `refresh()`. `onRemoval` is an
empty stub carrying the comment `// todo cleanup on removal / is it needed?`
(`FileSystemRepoService.java:88-92`).

Important nuance: **the browsing path never touches this cache.** `fundFsRepoItems` calls
`resolvePath(digiRepo, fund, path)`, not `getFileSystemImage`. So the server does re-read the disk on
every listing request — the missing refresh is a **frontend** problem (see D1). The cache only
affects the `DmsController` preview path.

**B3 — Cache key collides across funds.** The key is `digiRep.getUrl().substring(7)` — the
*unexpanded* URL. But `getPath(digiRepo, fund)` expands `${fundId}`, `${institutionCode}` etc. via
`ElzaTools.bindingUrlParams` (`FileSystemRepoService.java:293-306`). For a templated repository URL
all funds therefore share one cache entry pointing at the unexpanded path, and `FileSystemImage`'s
constructor validates `file://.../${fundId}/...` as a directory — which fails. **Templated
repositories and the preview endpoint are mutually incompatible today.**

**B4 — `synchronized` on `getFileSystemImage`** serializes every preview request across all
repositories and all users, for a method that performs only a map lookup.

**Recommendation: delete `FileSystemImage` and the Guava cache entirely.** It is a 40-line class
providing `resolve` + `newInputStream`, both of which the service already does. If caching is wanted
later, cache the *listing* (the expensive part), keyed by `(expandedRootPath, relativePath)`, with an
explicit invalidation endpoint and a `Files.getLastModifiedTime` validation stamp.

---

## 4. Structural weaknesses

### C1 — Domain logic lives in `FundController`

`fundFsRepoItems` is roughly 120 lines of directory walking, filtering, sorting, in-memory pagination
and attribute reading. It is unreachable from any other caller, not unit-testable without MockMvc,
and it is exactly the code that must now grow sorting and link-status filtering. **This is the single
most important refactor.**

### C2 — Pagination is offset-as-`lastKey`, recomputed from scratch

`FundController.java:383-389` and `:438-451`. Every "load more" re-walks the whole directory,
re-reads every `BasicFileAttributes`, re-sorts, then slices. For a 10 000-entry directory paged at
1 000 that is ten full walks. It is also **not stable**: a file added between pages shifts entries so
the client silently skips or duplicates rows. The hard cap at `counter < 10000`
(`FundController.java:402`) **silently truncates** with no flag in `FsItems` to tell the client the
listing is incomplete.

The name `lastKey` suggests a keyset cursor was the original intent. Since the sort is already
`(type, name)`, an honest cursor is `"FOLDER\u0000lastName"`.

### C3 — `maxItems = 2` triggered by a repository code ending in `_DEBUG`

`FundController.java:377-382`, commented `// hack for debugging client`. Production behaviour keyed
off a naming convention in configuration data. Replace with a `pageSize` request parameter validated
against a configured maximum — which the new UI needs anyway.

### C4 — Sorting is hardcoded and locale-naive

`FundController.java:424-437`: folders first, then `String.compareTo` — byte order, so `Z` sorts
before `a` and Czech diacritics are wrong (`Č` after `Z`). Since sorting is a required new feature:
the sort key must become a request parameter (`name` | `size` | `lastChange` × `asc` | `desc`, plus a
folders-first toggle) and name comparison must use a `Collator`. Note this couples to C2 — the cursor
encoding depends on the sort key.

### C5 — Per-entry attribute reads

`FundController.java:404-410` calls `Files.getFileAttributeView(...).readAttributes()` per item, and
the `acceptor` performs another `isRegularFile`/`isDirectory` stat. Use `Files.newDirectoryStream` and
read attributes once, deriving the type from the same `BasicFileAttributes`. On a network share —
the realistic deployment for a `file://` archival repository — this is the dominant cost.

### C6 — `@Transactional` on pure filesystem reads

`fundFsRepos`, `fundFsRepoItems` and `fundFsRepoItemData` are all annotated. The last holds a
database transaction open for the entire duration of a large file download to the client.

### C7 — No range requests on download

`DmsController.getFile` uses `IOUtils.copy` into the servlet stream with no `Content-Length` and no
range support: no seeking, no resumable download, no browser progress indication. For an archival
browser serving large TIFFs and video this is a real limitation.

### C8 — No authorization on repository visibility *(accepted, not a defect)*

`fundFsRepos` returns every `file://` external system to anyone who can read the fund. There is no
per-repository or per-fund ACL.

**Decision (2026-07-28): this is accepted and will not be changed** — see §8 item 4. Recorded here as
a property of the design so it is not re-raised as a finding later. It does not relax A3 or A4, which
remain Phase 0 fixes.

### C9 — `createDao` does heavy I/O inside the request transaction

A full recursive `Files.walk` of the selected subtree plus one `save()` per file. Linking a folder
with 50 000 scans will time out. Needs batching and plausibly an async job with progress — Elza
already has `ArrBulkActionRun` / async request infrastructure to model this on.

---

## 5. Filesystem repository vs. the DA/AIP architecture

The central architectural question was whether to keep the filesystem repository as a specialized
implementation or migrate it onto the DA/AIP model. It is **settled — it stays specialized** (§5.3).

This section records the evidence, because the conclusion runs against what `da-migration.md`
originally scheduled and the reasoning should be reviewable rather than taken on trust.

### 5.1 What the DA model actually requires

| Entity | Key constraints |
|---|---|
| `DaAip` (`DaAip.java:12`) | `code` NOT NULL, `digitalRepository` NOT NULL, unique `(code, digital_repository_id)`. **No change columns — a thin identity row.** |
| `DaAipState` (`DaAipState.java:18`) | `daAip` NOT NULL, `createChange` NOT NULL, `deleteChange` nullable, `aipVersion` NOT NULL. Holds all versioned metadata. |
| `DaDao` (`DaDao.java:19`) | **`aip` NOT NULL (`:27`)**, `createChange` NOT NULL (`:31`), `type` NOT NULL, `code` NOT NULL. |
| `DaDaoRelation` (`DaDaoRelation.java:15`) | `dao` + `parentDao` both NOT NULL, change-versioned. No ordering column. |
| `DaDaoFileFolder` (`DaDaoFileFolder.java:17`) | `label` NOT NULL (one path *segment*), `representationDao` NOT NULL, `parentFileFolder` nullable self-FK — **a genuine recursive tree**. |
| `DaDaoFile` (`DaDaoFile.java:19`) | `dao` NOT NULL, `createChange` NOT NULL, checksum/mime/size/image-dimensions/duration. **No `code`, no `created` timestamp.** |
| `DaChange` / `DaChangeType` | Only two values: `AIP_CREATE`, `AIP_UPDATE`. |

Two structural facts dominate the decision:

1. **`DaDao.aip` is `nullable = false`.** There is no way to have a DA-model DAO without inventing an
   AIP for it. A filesystem folder is not an AIP — it has no `aipVersion`, no PREMIS provenance, no
   ingestion event, no fixity manifest.
2. **The DA model is write-once + change-versioned.** All seven mutable entities carry
   `create_change_id NOT NULL` / `delete_change_id NULL`, and `DaoProcessor.java:227-240` shows the
   discipline: content changes close old rows and insert new ones. **A filesystem repository is
   mutable by nature** — files appear, change and vanish outside Elza's control. Mirroring a live
   mutable directory into a write-once versioned store means generating a `DaChange` on every
   detected difference, which is a preservation-grade audit trail for something that is not a
   preservation store.

This is the substance of the concern: the AIP model *can* express a hierarchy, but its hierarchy
carries semantics (representation vs. logical structure, versioned fixity, ingestion events) that a
filesystem cannot supply. Synthesising those fields produces a record that looks like preservation
metadata but is not.

### 5.2 What migration would actually cost

The optimistic reading is that `DaService`'s entity factories are generic — and they genuinely are.
`createDaDao` (`DaService.java:893`), `createDaDaoRelation` (`:903`), `createDaDaoFileFolder`
(`:911`), `createDaDaoFile` (`:920`), `createDaDaoItem` (`:944`) all take plain Java types with no
METS in their signatures. `DaoProcessor.findOrCreateFileFolder` (`:528-561`) already splits an
`href` on `/` and builds a folder chain segment by segment — that logic maps directly onto a
filesystem path.

But everything *around* those factories is METS/PREMIS-bound:

- **`DaAip` can only be created by parsing PREMIS.** `PackageInfoService.processPackageInfo`
  (`PackageInfoService.java:69`, `new DaAip()` at `:106`) is the **only** `new DaAip()` in the main
  source tree. It requires a `PACKAGE-INFO.xml` from which it reads `AIP_ID`, `FONDS_ID`,
  `INSTITUTION_ID`, `AIP_VERSION`, `AIP_SIZE`.
- **`DaDao` can only be created by `DaoProcessor`**, whose constructor takes `MetsType` and
  `PremisComplexType` (`DaoProcessor.java:148`). `DaService.doCreateDaoStructure` (`:287`) hard-requires
  `METS.xml` (`:325`) and `PREMIS.xml` (`:330`) inside a ZIP registered in `DaLocalCache`.
- **`getComponent` (`DaService.java:1586`) unzips the entire AIP into a fresh temp directory on every
  single file request**, then `Files.walk`s it for a name suffix match — and never cleans the temp
  directory up. For a filesystem repository, where the bytes are *already on disk in their final
  place*, this is strictly worse than the current direct read, so it would have to be branched
  around anyway.
- **The sync machinery is hard-filtered to DA.** `DaScheduler.java:51` filters
  `getDigitalRepositoryType() == DigitalRepositoryType.DA`, and `DaConnector.get()`
  (`DaConnector.java:106`) throws `IllegalArgumentException("Externí systém není typu DA")` for
  anything else. `DaRemoteRepositorySync.nextQuery` is an opaque *remote* cursor; a filesystem needs
  mtime scanning instead. So the attractive "reuse the refresh queue" argument does not hold without
  modification at three separate points.
- **The browse contracts do not converge.** `ExplorerTreeNode` (`main.tsp:257-313`) is an eager,
  complete, recursive tree with back-references, **no paging**, and two parallel hierarchies
  (`parentFolder` and `parentFolderLogical`). `FsItems` (`main.tsp:251`) is a flat, paged,
  one-level-at-a-time listing. A filesystem has exactly one hierarchy, so `parentFolderLogical` has
  no source; and `ExplorerTreeNode` would serialize an entire large directory tree in one response —
  the very thing `FsItems` paging exists to avoid.

There is also a general observation worth recording: **there is no abstraction over repository
backends at all.** `DigitalRepositoryType` is a bare 3-value enum, and behavioural dispatch on it
occurs at exactly two sites (`DaConnector.java:106`, `DaScheduler.java:51`). DA, FILESYSTEM and WSDL
are three disjoint parallel code paths sharing one entity table — not polymorphic siblings.

### 5.3 Decision: a specialized backend behind a shared browsing contract

> **Agreed 2026-07-28.** The filesystem repository requires specialization and cannot be treated as
> just another implementation of a generic DAO/AIP repository. `da-migration.md` Phase 3 has been
> revised to match; the two documents no longer diverge.

Do **not** migrate the filesystem repository onto `DaAip`/`DaDao`, and do **not** invent synthetic
AIPs for filesystem folders. Instead:

1. **Keep the filesystem repository specialized.** Its natural hierarchy is the directory tree, it is
   inherently mutable, and its content requires no fixity or preservation-event modelling. Forcing it
   through a write-once AIP model adds a `DaChange` per detected difference and a fabricated
   `aipVersion` without buying anything.

2. **Unify at the browsing contract and the SPI, not at the entity model.** Introduce a
   `DigitalRepositoryBackend` interface — `list(path, sort, filter, cursor)`, `open(path)`,
   `link(path, node)`, `refresh(path)` — with a `FileSystemBackend` and later a `DaBackend`. This
   gives the single implementation the user wants at the level where it is genuinely shared (the UI
   and the API), while letting each backend keep the storage model that fits it. It also fills the
   real architectural gap identified in 5.2: there is currently no such abstraction anywhere.

3. **Generalize the paged contract rather than adopting `ExplorerTreeNode`.** `FsItems` paging is the
   correct shape; `ExplorerTreeNode` will need a paged variant eventually for large AIPs regardless of
   backend. Extending the `FsItem`/`FsItems` models into a backend-neutral `RepoItem`/`RepoItems` is
   the convergence point.

4. **Where a filesystem folder genuinely *is* an ingest candidate** — i.e. the user wants it archived
   rather than merely browsed — provide an explicit, user-initiated "ingest this folder as an AIP"
   action that mints a real `DaAip` with a deliberate code and version. This is the case where the AIP
   semantics are true rather than synthesised, and it is a much smaller job than blanket migration:
   a `FilesystemAipProcessor` sibling to `DaoProcessor`, reusing the generic `DaService` factories
   plus a non-PREMIS variant of `PackageInfoService` to mint the `DaAip`/`DaAipState`.

5. **Isolate the link-status query behind an interface now.** "Show only unlinked DAOs" is today
   `DaoRepository.findDettachedByFund` (`DaoRepository.java:101-118`), which joins through
   `arr_dao_link.dao_id`. That column disappears in `da-migration.md` Phase 5, and filesystem links
   are addressed by `(digital_repository_id, path)` rather than `da_dao_id` (§5.4). So the query
   changes shape: "is this repository-relative path linked to a node?" rather than "does an `ArrDao`
   row exist without a link?".

   Put it behind a `DaoLinkStatusProvider` returning link state for a batch of paths, with an
   `ArrDao`-backed implementation now and a path-backed one after the §5.4 columns exist. Batching
   matters — the browser needs link state for a whole page of entries in one query, not per row.
   Because §5.4 keeps a single link table, this provider stays a single query with a different
   `WHERE` clause per backend, not a union across tables. Also note `ArrDaoLink.dao` is now nullable
   (`da-migration.md` §2.4) — any logic walking all links must null-check.

### 5.4 How filesystem links are represented — no specialization needed

An earlier draft of this section proposed either adding `path` columns to `arr_dao_link` or creating
a separate link table for filesystem repositories. **Both were wrong.** Re-examining how DA links
address sub-AIP content shows that `arr_dao_link` already has the abstraction filesystem linking
needs, and the two cases share one path.

**The existing DA link model is granularity-aware.** `ArrDaoLink.LinkType`
(`ArrDaoLink.java:85-89`) has three values, and they encode *what level of the repository hierarchy
the link points at*:

| `LinkType` | Set by | `aip` | `daDao` | Meaning |
|---|---|---|---|---|
| `AIP` | `DaService.connectToJP:1306` | set | **null** | whole package |
| `PART_AIP` | `DaService.connectPartToJP:1327` | set | set | a sub-node of the package |
| `COMPONENT_AIP` | `DaService.connectSelectedToJP:1371` | set | set | a leaf component |

So "link to the whole thing" versus "link to something inside it" is **already** a first-class
concept in this table, expressed as *"container reference + optional member reference"* — and the
member reference is nullable precisely so the whole-container case can reuse the same row shape.

**Filesystem linking is the same shape.** Today `fundFsCreateDAOLink`
(`FundController.java:497+`) creates an `ArrDao` whose `code` *is* the repository-relative path
(`FileSystemRepoService.java:119-120` passes `itemRelatPath` as both code and label), then links to
it. Whether the user selects the repository root, a folder, or a single file, the target is always
*(repository, relative path)* — a container plus a member, exactly like *(AIP, DaDao)*.

Note also that **DA links never address an individual file either**: there is no `DaDaoFile`
reference anywhere on `ArrDaoLink`. The finest DA granularity is a `DaDao` of type `FILE`, which is
a *node in the hierarchy*, not a byte stream. A filesystem path naming a single file is the direct
analogue. The two models agree on where linking stops.

**Consequence: keep one link table and one link model.** The generalization is to recognise that
`aip` + `daDao` is a *specific instance* of "container + member within container", and that the
filesystem needs the same pair with different column types. Concretely:

- `arr_dao_link` keeps `node_id`, `create_change_id`, `delete_change_id` and `link_type` as the
  shared spine — `link_type` already carries the granularity distinction and needs only
  backend-neutral naming (e.g. `CONTAINER` / `PART` / `COMPONENT`, retaining the existing values as
  aliases so no data migration is needed for DA rows).
- The container reference generalizes: `aip_id` for DA, `digital_repository_id` for filesystem.
- The member reference generalizes: `da_dao_id` for DA, a relative path for filesystem — nullable in
  both, with `NULL` meaning "the whole container", consistent with today's `LinkType.AIP`.

That is **one additional nullable column** (`path`) alongside the `digital_repository_id` the
filesystem case needs, not a specialized table and not a parallel link model. The mutual exclusivity
this introduces is not new — `arr_dao_link` already carries three mutually-exclusive target groups
(`dao_id`, `aip_id` + `da_dao_id`) and Phase 5 removes one of them, so the net column count barely
moves.

**Enforce the exclusivity in the schema.** The one thing worth adding that does not exist today is a
`CHECK` constraint asserting that exactly one container-reference group is populated per row. The
current table relies entirely on convention, which is what made `ArrDaoLink.dao` becoming nullable a
documented risk (`da-migration.md` §2.4) rather than a schema-enforced invariant.

**Decided (2026-07-28): a plain `path` column on `arr_dao_link`.** The alternative considered was
normalizing the container+member pair into a small `arr_dao_target` table referenced by both
backends. That only pays off if a third path-addressed backend appears, and adopting it later is a
mechanical change behind `DaoLinkStatusProvider` — so the simpler form wins now. See §8 item 2.

**Conversion of existing rows** stays mechanical: for each filesystem `ArrDaoLink`, read the path
from the `ArrDao.code` it points at, write it to `path`, set `digital_repository_id` from the DAO's
package, set `link_type`, clear `dao_id`.

One caveat for whoever writes that migration: A1 means the existing `ArrDaoFile` rows under those
DAOs have `dao_file_group_id = NULL`, so the folder structure cannot be recovered from the database.
It does not matter here — the link conversion needs only `ArrDao.code`, which is intact — but do not
attempt to reconstruct hierarchy from those rows.

---

## 6. Frontend weaknesses

### D1 — No refresh; the cause is structural

`Tree.tsx` holds `workingTree` in component state and only ever splices into it (`:118-121`, `:90-93`).
There is no invalidation path:

- `useEffect` re-seeds only on `[repos.length]` (`Tree.tsx:208`) — if the repository list changes
  content but not length, nothing happens.
- `expandItem` early-returns when `expandedItems[fullPath] != undefined` (`Tree.tsx:109-112`), so **a
  folder's children are fetched exactly once per component lifetime**; collapse-then-expand never
  re-fetches.
- `FileSystemBrowser.tsx:133-140` reloads the list only when `selectedTreeItemPath` *changes*, so
  re-clicking the same folder is a no-op.

Net effect: the only way to see new files is to close and reopen the dialog. This matches the
reported symptom exactly.

Fix: a keyed cache (`Map<path, {items, loadedAt}>`) with explicit `invalidate(path)` /
`invalidateSubtree(path)`, a toolbar refresh button, and re-fetch on expand when the entry is stale.
If React Query is already in the project it provides all of this directly.

### D2 — Stale-closure races

`loadLevel` in both components reads `childrenMap` / `workingTree` from the closure and writes
`setChildrenMap({...childrenMap, ...})` (`FileSystemBrowser.tsx:59-61`, `Tree.tsx:118-121`). Two
concurrent expands lose one another's results. Use functional `setState` updaters.

### D3 — Paths are `/`-joined strings with the repository id prefixed

`extractRepoIdFromFullPath` plus `fullPath: \`${fullPath}/${item.name}\``. Repository id and path are
conflated, breadcrumbs are rebuilt by string splitting (`FileSystemBrowser.tsx:163-189`), and a
filename that parses as a number at position 0 confuses `parseInt`. Model as
`{repoId: number, segments: string[]}`.

### D4 — Missing `key` props on breadcrumb fragments (`FileSystemBrowser.tsx:177`).

### D5 — No loading or error state. Every `await Api.funds...` is unguarded; a failed listing leaves
the tree silently empty.

### D6 — Two sources of truth for expansion. `Tree.tsx` keeps its own `expandedItems` state (`:44`)
*and* receives `expandedItems` / `onExpandChange` props (`:14-15`), using the props only for
notification. The parent writes the prop (`FileSystemBrowser.tsx:220`) and never reads it back.

---

## 7. Sequencing

Phases are grouped by dependency, not by size. Phase 0 is independent; Phases 1–3 are ordered and
each unblocks the next; Phases 4–6 can move relative to each other.

**Phase 0 — stop the bleeding.** Independent of every architectural decision; start here.

In rough priority order:

1. **A3 — path traversal.** Security; do this first and in isolation so it is easy to review.
2. **A1 — parent file group never assigned.** Code fix plus a repair migration for existing rows.
   Everything that reads DAO structure depends on this being correct.
3. **A5 — 32-bit size cast**, **A6 — unguarded iterator**, **A8 — duplicate-code crash**,
   **A9 — wrong package in multi-fund setups**. Small, unrelated, cheap.
4. **A2 — real mime detection** and **A4 — consolidate the two download endpoints**. Do these
   together: A4 decides which endpoint survives, and A2 changes how it sets `Content-Type`.
5. **C6 — drop `@Transactional` from filesystem reads.** One-line change, biggest effect on
   `fundFsRepoItemData`, which currently holds a transaction open for a whole download.

A7 (non-regular files) can also land here, but it changes link-operation behaviour from "fail" to
"skip and report", so it needs the result-reporting shape decided in Phase 2 to be done properly.
Deferring it is reasonable.

**Phase 1 — give the feature a home.** Extract `fundFsRepoItems` out of `FundController` into a
service (`FileSystemRepoBrowser`, or `FileSystemRepoService` itself) returning a domain result
object; leave the controller as a thin mapper. Delete `FileSystemImage` and the Guava cache. Add a
containment-checked `resolve`. No behaviour change, but everything after this becomes testable.

Also add the **filesystem link columns** to `arr_dao_link` here (§5.4): `path` plus
`digital_repository_id`, the `link_type` widening, and the `CHECK` constraint on target-group
exclusivity. Doing it now lets the conversion of existing filesystem `ArrDaoLink` rows be scripted
alongside the DA migration rather than retrofitted, and it unblocks `da-migration.md` Phase 5.

**Phase 2 — the contract, in one pass.** Sorting and cursors are coupled (C2/C4), so change
`main.tsp` once: `FsItemSort` enum, `sortBy` / `sortDir` / `foldersFirst` / `pageSize` parameters, a
`linkState` filter (`ALL` | `LINKED` | `UNLINKED`), and `truncated: boolean` on `FsItems`. Design the
models as backend-neutral (§5.3 point 3) even if only the filesystem backend exists initially.

Two points follow from the global-scoping decision (§8 item 3) and should be handled here rather than
discovered during implementation:

- **`linkState` is global, but the endpoint is fund-scoped** (`/{fundId}/fsrepo/...`). Document the
  asymmetry directly on the TypeSpec model or it will be read as a bug.
- **A path can be linked from more than one node**, possibly in different funds, so a single
  `nodeId` per item cannot represent link state faithfully. Either omit the node reference and expose
  only the boolean-ish `linkState`, or expose `linkedNodeIds` as a list. Prefer the former unless the
  UI has a concrete need to navigate to the linking node — it avoids leaking node ids from funds the
  user may not be able to read.

Also settle the **link-operation result shape** here: `fundFsCreateDAOLink` currently returns a bare
`daoLinkId` (`FundController.java:497+`), which cannot express "linked, but N entries were skipped".
A7 and C9 both need that — A7 to report unreadable entries, C9 to report async progress.

**Phase 3 — the backend SPI.** Introduce `DigitalRepositoryBackend` with `FileSystemBackend` as the
first implementation (§5.3 point 2). Implement link status behind `DaoLinkStatusProvider` (§5.3
point 5) — a batch query on `(digital_repository_id, path)` with `delete_change_id IS NULL` and **no
fund predicate** (§8 item 3).

**Phase 4 — frontend.** Keyed cache with invalidation and a refresh button (D1), functional state
updates (D2), structured paths (D3), loading and error states (D5), remove the duplicated expansion
state (D6).

**Phase 5 — scale and robustness.** Keyset cursor (C2), `newDirectoryStream` with single-pass
attributes (C5), async link creation for large trees (C9), range requests on download (C7), and A7
(skip-and-report unrecognised entries) if it was deferred from Phase 0 — it shares the result shape
defined in Phase 2 with C9.

**Phase 6 — optional, only if wanted.** Explicit "ingest folder as AIP" action producing a real
`DaAip` (§5.3 point 4).

---

## 8. Decisions

All architectural questions raised by this analysis are settled. Nothing here blocks Phase 0.

1. **Filesystem repositories stay specialized** and are not migrated onto `DaAip`/`DaDao`
   (§5.3, agreed 2026-07-28). `da-migration.md` Phase 3 was revised accordingly, along with its
   Phase 5 exit criterion, §2.2 status table, §5 key-files list and §6 risks. The two documents are
   consistent.

2. **Links reuse `arr_dao_link` with a plain `path` column** (§5.4, agreed 2026-07-28). No
   `arr_dao_target` normalization and no filesystem-specific link table. If a third path-addressed
   backend ever appears, normalizing then is a mechanical change behind `DaoLinkStatusProvider` —
   the cost of deferring is low, which is why the simpler form wins now.

3. **"Unlinked" is scoped globally, not per-fund** (agreed 2026-07-28). A filesystem path counts as
   linked if *any* live `ArrDaoLink` references it, regardless of which fund's node it is attached
   to. Consequences to implement:

   - The `DaoLinkStatusProvider` query filters on `(digital_repository_id, path)` and
     `delete_change_id IS NULL` only — **no fund predicate**, and therefore no join to
     `arr_node`/`arr_fund`. Simpler and cheaper than the per-fund alternative.
   - `linkState` in the contract (§7 Phase 2) means *globally linked*. Document this on the TypeSpec
     model, because the endpoint is fund-scoped (`/{fundId}/fsrepo/...`) while this particular field
     is not — that asymmetry will otherwise be read as a bug.
   - A user browsing fund A will see paths marked linked that are in fact linked from fund B, and
     `UNLINKED` will hide them. This is the intended behaviour: the question being answered is "has
     this file already been catalogued anywhere", which is what prevents duplicate cataloguing.
   - Because there is no fund predicate, this query is unaffected by the A9 package-scoping bug.
     The Phase 3 verification in §9 should still cover the two-fund case to confirm the global
     semantics hold rather than to guard against fund leakage.

4. **Repository visibility stays as it is — C8 is not actioned** (agreed 2026-07-28). There is
   deliberately no permission control over filesystem repositories beyond the existing fund-level
   checks; every configured `file://` repository remains visible to any user who can read a fund.
   C8 is retained in §4 as a documented property of the design, not as a defect to fix.

   Note this does **not** relax A3 (path traversal) or A4 (inconsistent authorization between the two
   download endpoints). Those remain Phase 0 fixes: the decision is that *which repositories a user
   may see* needs no new access control, not that a user may read arbitrary paths outside a
   repository root.

---

## 9. Verification

There is no automated coverage of this feature today; `DaoCoreServiceTest` covers only the
`ArrDao` service layer (`da-migration.md:47-50`).

**Per phase:**

- **Phase 0.** Unit-test `resolvePath` against traversal inputs (`../`, absolute paths, symlinks,
  URL-encoded variants) asserting `BusinessException`. Integration-test `createDao` on a temporary
  directory tree two levels deep and assert every `ArrDaoFile.daoFileGroupId` is non-null and matches
  the source parent — this is the regression test for A1. For A5, unit-test the size mapping with a
  stubbed `BasicFileAttributes.size()` of `3_000_000_000L` rather than creating a real file; a sparse
  file needs filesystem support the CI agent may not have.
- **Phase 1.** The extracted browser service becomes unit-testable against a temp directory with no
  Spring context — cover empty directory, single level, nested, unreadable entry, and a repository
  path that does not exist.
- **Phase 2.** Regenerate the client from `main.tsp` and confirm `elza-react` compiles. Contract test
  each sort key and direction, `pageSize` boundaries, and that `truncated` is set when the cap is hit.
- **Phase 3.** Verify `linkState=UNLINKED` against a fixture where some paths are linked and some are
  not. Include a two-fund case with **a path linked only from fund B, browsed from fund A**: it must
  report `LINKED` and be hidden by `UNLINKED`. That is the global-scoping decision (§8 item 3) made
  executable, and it is the assertion most likely to be written backwards by someone assuming
  fund-scoped semantics. Also assert the generated SQL contains no join to `arr_node`/`arr_fund`.
- **Phase 4.** Manual: open the browser, add a file to the repository directory on disk, press
  refresh, confirm it appears without reopening the dialog. Then collapse and re-expand a folder and
  confirm the children are re-fetched. Both are currently broken.
- **Phase 5.** Benchmark a directory of 50 000 entries on a network share before and after C5, and
  confirm paging through it issues one walk rather than N.

**Running the application:** the project's standard Maven/Spring Boot launch for `elza-core` plus the
`elza-react` dev server. The browser is reached from a fund's DAO panel, and needs at least one
external system of type `FILESYSTEM` configured with a `file://` URL pointing at a readable
directory.
