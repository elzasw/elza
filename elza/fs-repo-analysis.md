# FileSystemRepoService — architectural analysis and current state

**Status:** original analysis 2026-07-28 (decisions settled the same day, §8); updated 2026-08-10
to the implemented state — resolved defect details removed, open items kept, new findings added.
The #9944 series (2026-07-28 → 2026-08-06) delivered §7 Phases 0–2 and part of Phase 4; step 3a of
`da-migration.md` revised Phase 3 (2026-08-10) removed the persisted file trees and dropped
`arr_dao_file_group`.
**Scope:** `FileSystemRepoService` and the filesystem-repository browser feature
**Related:** `elza/da-migration.md` — its Phase 3 was revised to match §5.3 of this document;
its step 3b carries the remaining `arr_dao_link` schema change (§5.4)

---

## Context

The filesystem-repository browser (`file://` digital repositories) was originally built as a quick
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
| Contract | `elza-development/typespec/main.tsp` (`FsRepo`, `FsItem`, `FsItems`, `FsLink`) | Paged, sorted, filtered browse contract; `FsItem.links` carries node references incl. a `readable` flag for funds the resolver could not read |
| Browsing | `FileSystemRepoBrowser` (`browseItems`, `listRepos`, `listDaoFiles`) | All listing logic; live disk reads, nothing persisted |
| Primitives | `FileSystemRepoService` | Containment-checked path resolution, MIME detection, input streams, and the `ArrDao` anchor creation (`createDao` — path in `code`, no per-file entities since step 3a) |
| Linking | `FundController.fundFsCreateDAOLink` → `createDao` + `DaoService.createDaoLink` | Creates/reuses the anchor and the `arr_dao_link` row; returns `daoLinkId` |
| Panel files | `DaoService.getDaoFiles` / `countDaoFiles` | Branch per repository type: filesystem DAOs are listed live via `listDaoFiles` (recursive, flat, path-ordered, capped at `DAO_FILE_LIMIT` = 1000, transient entities); other types read persisted `arr_dao_file` rows |
| Download | `FundController.fundFsRepoItemData` | Single authorized endpoint (`FUND_RD`-checked); streams a `FileSystemResource`, so Content-Length and HTTP Range work; MIME-typed, inline for renderable types |
| UI | `elza-react/src/components/arr/daos/file-system-browser/{FileSystemBrowser,Tree}.tsx`; DAO panel `ArrDaos.jsx`/`ArrDao.jsx` | Tree + list with toolbar filters and link popovers; panel viewer consumes `ArrDaoVO.fileList` |

Link state is resolved by `DaoLinkRepository.findLinksByDigitalRepository` — a single query per
repository with **no fund predicate** (§8 item 3), joining through `arr_dao_link.dao_id` and
`arr_dao.code` until `da-migration.md` step 3b converts links to `(digital_repository_id, path)`.

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

### C5 — Per-entry attribute reads

`browseItems` uses `Files.list` + one `Files.readAttributes` per entry. On a network share — the
realistic deployment for a `file://` archival repository — the per-entry round trip is the
dominant cost. `Files.newDirectoryStream` with attributes read once per batch would reduce it.
`listDaoFiles` (the DAO panel walk) reads attributes from the visitor callback, which is already
single-pass.

### C8 — No authorization on repository visibility *(accepted, not a defect)*

`fundFsRepos` returns every `file://` external system to anyone who can read the fund.
**Decision (2026-07-28): accepted, will not be changed** — see §8 item 4. Recorded so it is not
re-raised as a finding later. It does not relax A3/A4, which are fixed.

### New findings (2026-08-10)

- **N1 — The DAO panel's live listing cap is silent.** `DaoService.getDaoFiles` caps at
  `DAO_FILE_LIMIT` (1000) and `ArrDaoVO` has no `truncated` indicator, so a linked folder with
  more files shows exactly 1000 path-ordered entries and `fileCount` tops out at 1000 with no hint
  that the list is incomplete. Same family as the fixed A6/C2 concerns; fix when the panel gains
  paging (§7 Phase 5) or add a flag to `ArrDaoVO`.
- **N2 — Live-listed files have synthetic negative ids.** `ClientFactoryVO` assigns
  `-N` wire ids (unique within one response) to files without a persistent id. The panel's
  selection round-trips these ids through component state; they are **not stable across
  requests** — client code must not persist them.
- **N3 — `multiple_links` is declared but not enforced.** The flag exists on
  `arr_digital_repository` and in the admin UI; link creation does not check it. Implement as
  "is `(repository, path)` already live-linked" so the check survives step 3b unchanged (also
  recorded in `da-migration.md` §6).
- **N4 — Repository type enum and URL can disagree.** `DaoCoreServiceTest` creates a repository
  with `DigitalRepositoryType.FILESYSTEM` but a NULL `url`; `isFileSystemRepository()` (URL-prefix
  check) then treats it as non-filesystem, which is why the SOAP fixture works at all. The
  `digital_repository_type` column was heuristically backfilled and is **not** a reliable
  discriminator — code and migrations must key off `url LIKE 'file://%'` (the step 3a changesets
  do). Unifying the two is a candidate cleanup for `da-migration.md` step 3b.
- **N5 — One disk walk per fs DAO per `findDaos(detail)` request.** Acceptable for typical
  per-node link counts; revisit (batching or caching) only if nodes with many filesystem links
  become common.

---

## 5. Filesystem repository vs. the DA/AIP architecture

The central architectural question was whether to keep the filesystem repository as a specialized
implementation or migrate it onto the DA/AIP model. It is **settled — it stays specialized**
(§5.3), and step 3a of the resulting plan is implemented. This section stays as the decision
record, because the conclusion ran against what `da-migration.md` originally scheduled and the
reasoning should be reviewable rather than taken on trust.

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
it occurs at exactly two sites (`DaConnector.java:106`, `DaScheduler.java:51`; re-verified
2026-08-07). DA, FILESYSTEM and WSDL are three disjoint parallel code paths sharing one entity
table — not polymorphic siblings.

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
   resolver could not read. The query joins through `arr_dao.code` today; `da-migration.md`
   step 3b changes its `WHERE` clause to `(digital_repository_id, path)` — same shape, no join.

### 5.4 How filesystem links are represented — no specialization needed

*(This is the specification for `da-migration.md` step 3b — still pending.)*

An earlier draft proposed either adding `path` columns to `arr_dao_link` or creating a separate
link table for filesystem repositories. **Both were wrong.** Re-examining how DA links address
sub-AIP content shows that `arr_dao_link` already has the abstraction filesystem linking needs.

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

**Filesystem linking is the same shape.** `fundFsCreateDAOLink` creates an `ArrDao` whose `code`
*is* the repository-relative path (since step 3a that anchor is all it creates), then links to it.
Whether the user selects the repository root, a folder, or a single file, the target is always
*(repository, relative path)* — a container plus a member, exactly like *(AIP, DaDao)*.

Note also that **DA links never address an individual file either**: there is no `DaDaoFile`
reference anywhere on `ArrDaoLink`. The finest DA granularity is a `DaDao` of type `FILE`, which
is a *node in the hierarchy*, not a byte stream. A filesystem path naming a single file is the
direct analogue. The two models agree on where linking stops.

**Consequence: keep one link table and one link model.**

- `arr_dao_link` keeps `node_id`, `create_change_id`, `delete_change_id` and `link_type` as the
  shared spine — `link_type` already carries the granularity distinction and needs only
  backend-neutral naming (e.g. `CONTAINER` / `PART` / `COMPONENT`, retaining the existing values
  as aliases so no data migration is needed for DA rows).
- The container reference generalizes: `aip_id` for DA, `digital_repository_id` for filesystem.
- The member reference generalizes: `da_dao_id` for DA, a relative path for filesystem — nullable
  in both, with `NULL` meaning "the whole container", consistent with today's `LinkType.AIP`.

That is **one additional nullable column** (`path`) alongside the `digital_repository_id` the
filesystem case needs, not a specialized table and not a parallel link model.

**Enforce the exclusivity in the schema.** Add a `CHECK` constraint asserting that exactly one
container-reference group is populated per row. The current table relies entirely on convention,
which is what made `ArrDaoLink.dao` becoming nullable a documented risk (`da-migration.md` §2.4)
rather than a schema-enforced invariant.

**Decided (2026-07-28): a plain `path` column on `arr_dao_link`.** The alternative considered was
normalizing the container+member pair into a small `arr_dao_target` table referenced by both
backends. That only pays off if a third path-addressed backend appears, and adopting it later is a
mechanical change behind the link-status query — so the simpler form wins now. See §8 item 2.

**Conversion of existing rows** stays mechanical: for each filesystem `ArrDaoLink`, read the path
from the `ArrDao.code` it points at (canonical '/' form since changeset `20260803120000`), write
it to `path`, set `digital_repository_id` from the DAO's package, set `link_type`, clear `dao_id`.
Step 3a already deleted the fs `arr_dao_file` rows and the `arr_dao_file_group` table, so the
conversion touches only `arr_dao_link` and `arr_dao`.

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

Every `await Api.funds…` in both components is unguarded; a failed listing leaves the tree/list
silently empty.

### D6 — Two sources of truth for expansion

`Tree.tsx` keeps its own `expandedItems` state (`:44`) *and* receives `expandedItems` /
`onExpandChange` props (`:14-15`), using the props only for notification.

---

## 7. Sequencing

Status of the original delivery plan:

| Phase | Content | Status |
|---|---|---|
| 0 — stop the bleeding | A1–A9, C6 | **Done** (#9944) |
| 1 — give the feature a home | Extract browser service, delete cache, containment-checked resolve; *plus the `arr_dao_link` schema change* | **Done except the schema change**, which moved to `da-migration.md` Phase 3 step 3b |
| 2 — the contract | Sort/paging/filter parameters, `truncated`, link-state filter, link-operation result shape | **Done.** `linkState` is global (§8 item 3); the link-result shape resolved itself differently — `skippedEntries` shipped with the interim A7 fix and was removed again in step 3a, because linking no longer walks anything |
| 3 — backend SPI | `DigitalRepositoryBackend` + backend-neutral link-status query | **Open** (see §5.3 points 2 and 5) |
| 4 — frontend | Refresh, functional updates, structured paths, loading/error states | **Partial** — list refresh done; D1(tree)/D2/D3/D4/D5/D6 open (§6) |
| 5 — scale and robustness | Keyset cursor (C2), single-pass attributes (C5), async link creation, range requests | **Partial** — range requests and async linking are moot (C7 resolved by `FileSystemResource`; C9 by step 3a); C2 and C5 remain |
| 6 — optional | "Ingest folder as AIP" (§5.3 point 4) | **Open, only if wanted** |

Remaining work, in dependency order:

1. **`da-migration.md` step 3b** — the `arr_dao_link` schema change and link conversion (§5.4),
   including the `multiple_links` enforcement (N3) implemented path-based so it lands once.
2. **Phase 3 — the SPI**, once 3b has removed the `dao_id` join from the link-status query.
3. **Phase 4 leftovers** — §6 items; independent of 1–2.
4. **Phase 5 leftovers** — C2 keyset cursor and C5 single-pass attributes; N1 (panel cap
   indicator) belongs here too.
5. **Phase 6** — optional ingest-as-AIP.

---

## 8. Decisions

All architectural questions raised by this analysis are settled.

1. **Filesystem repositories stay specialized** and are not migrated onto `DaAip`/`DaDao`
   (§5.3, agreed 2026-07-28). `da-migration.md` Phase 3 was revised accordingly; its step 3a is
   implemented (2026-08-10). The two documents are consistent.

2. **Links reuse `arr_dao_link` with a plain `path` column** (§5.4, agreed 2026-07-28). No
   `arr_dao_target` normalization and no filesystem-specific link table. *Pending — this is
   `da-migration.md` step 3b.*

3. **"Unlinked" is scoped globally, not per-fund** (agreed 2026-07-28). *Implemented:*
   `findLinksByDigitalRepository` filters on the repository and `delete_change_id IS NULL` only —
   no fund predicate. A user browsing fund A sees paths linked from fund B as linked, and
   `UNLINKED` hides them; the question answered is "has this file already been catalogued
   anywhere". The implementation additionally exposes the linking nodes cross-fund (clickable, with
   `FsLink.readable` marking unresolvable funds) — a deliberate departure from the original
   preference to omit node references.

4. **Repository visibility stays as it is — C8 is not actioned** (agreed 2026-07-28). Every
   configured `file://` repository remains visible to any user who can read a fund. This does not
   relax A3/A4, which are fixed.

---

## 9. Verification

Automated coverage today: `FileSystemRepoServiceTest` (path containment incl. traversal inputs,
MIME detection, inline-renderable classification), `FileSystemRepoBrowserTest` (browse
sort/filter/paging incl. Czech collation, link resolution incl. multi-fund rows, `listDaoFiles`
recursive/cap/missing-path, `normalizeRelatPath`), and `DaoCoreServiceTest` (SOAP DAO flow,
unaffected by the fs changes — but see N4 about its repository fixture).

**For the remaining work:**

- **Step 3b (link conversion).** Verify `linkState=UNLINKED` against a fixture where some paths
  are linked and some not, including a two-fund case with **a path linked only from fund B,
  browsed from fund A**: it must report `LINKED` and be hidden by `UNLINKED` — the global-scoping
  decision (§8 item 3) made executable, and the assertion most likely to be written backwards by
  someone assuming fund-scoped semantics. Assert the converted query contains no join to
  `arr_dao`/`arr_node`/`arr_fund`. Migration test: fs `ArrDaoLink` rows end with
  `digital_repository_id` + `path` populated and `dao_id` NULL; DA rows untouched.
- **Phase 4 (frontend).** Manual: add a file on disk, refresh, confirm it appears without
  reopening the dialog; collapse and re-expand a tree folder and confirm children re-fetch (the
  D1 tree half — currently broken).
- **Phase 5.** Benchmark a 50 000-entry directory on a network share before and after C5, and
  confirm paging through it is stable under concurrent modification once C2's keyset cursor
  lands.

**Running the application:** standard Maven/Spring Boot launch for `elza-core` plus the
`elza-react` dev server; the browser is reached from a fund's DAO panel and needs at least one
external system of type `FILESYSTEM` configured with a `file://` URL pointing at a readable
directory.
