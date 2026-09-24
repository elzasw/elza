# Filesystem repository — architecture and decisions

**The filesystem-repository feature is finished (2026-08-18, #9944 + #9971).** It is fully on its
target architecture: browsing, panel files and downloads are live disk reads; links are `ArrFsLink`
rows targeting `(repository, path)`; `multiple_links` is enforced; filesystem repositories persist
**zero** entities. No in-scope item is open.

**What this document is.** Not a plan and not a status report — those did their job and are gone.
What remains is the part the code cannot state for itself: the invariants new code must respect
(§1), the behaviours that look like bugs until you know why they were chosen (§1.1, §1.2), the
notes that outlive the delivery (§2), the reasoning behind the central architectural decision
(§3), and the decisions themselves (§5). §4 records what the feature deliberately leaves for
later and §6 how it is verified.

**Scope:** `FileSystemRepoService`, `FileSystemRepoBrowser` and the filesystem-repository browser.
**Related:** `elza/da-migration.md` — its Phase 3 was revised to match §3.3 of this document and is
fully implemented (steps 3a + 3b); remaining DA-side work (Phases 4–5) is tracked there.

*The delivery history is not repeated here. The resolved correctness defects (A1–A9), the deleted
image cache, the resolved structural items (C1–C7, C9), the findings (N1, N3, N4, N6, N7), the
frontend items (D1–D7) with the `VirtualList` replacement, and the design iterations behind the
link model all live in this file's git history, in the changesets, and in `da-migration.md`.
Finding ids are never reused or renumbered, so a reference from a commit message or from the other
document keeps resolving — `git log -p -- elza/fs-repo-analysis.md` is the way to read one back.*

---

## 1. What the feature is today

| Layer | Location | Role |
|---|---|---|
| Contract | `elza-development/typespec/main.tsp` (`FsRepo`, `FsItem`, `FsItems`, `FsLink`, `DigitalRepositoryTestResult`) | Paged, sorted, filtered browse contract; `FsItemSortType` covers name/size/last-change × asc/desc (last-change added by #9971, 2026-08-18); `FsItems.lastKey` is an opaque keyset cursor (§1.2); `FsItem.links` carries node references incl. a `readable` flag for funds the resolver could not read; `FsItem.hasChildren` drives the tree's expand marker; `FsRepo.available` marks a repository whose root is not reachable |
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
  by changeset `20260812120000`. Rationale and the per-field evidence: §5 item 5.

### 1.2 Paging semantics *(C2, closed 2026-08-18)*

`lastKey` used to be an offset, so a file created between two "load more" calls shifted every
following entry and the client silently skipped or duplicated rows. It is now a real **keyset
cursor**: a base64url-encoded JSON blob carrying the last emitted entry's sort key
(`lastName`, `lastSize`, `lastChange`, `lastWasFolder`) plus the sort parameters it was minted
under. Three properties follow, and new work must not regress them:

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

No backend defect is open. What follows is the residue that outlives the delivery: one decision
taken and recorded so it is not re-raised, one documented behaviour new client code must respect,
and one optimization deliberately deferred.

### C8 — No authorization on repository visibility *(accepted, not a defect)*

`fundFsRepos` returns every `FILESYSTEM`-typed external system to anyone who can read the fund.
**Decision (2026-07-28): accepted, will not be changed** — see §5 item 4. Recorded so it is not
re-raised as a finding later. It does not relax the containment check on path resolution or the
single authorized download endpoint.

Re-confirmed 2026-08-14 with one note: since §1.1, the response also includes repositories whose
root is missing, still carrying the resolved server path in `FsRepo.path`. The audience is
unchanged (any fund reader could always see `path` for a working repository), so this does not
reopen C8 — but it does mean a misconfigured path is now visible to non-administrators too.

### N2 — Live-listed files have synthetic negative ids

`ClientFactoryVO` assigns `-N` wire ids (unique within one response) to files without a persistent
id. The panel's selection round-trips these ids through component state; they are **not stable
across requests** — client code must not persist them.

### N5 — One disk walk per fs DAO per `findDaos(detail)` request

Acceptable for typical per-node link counts; revisit (batching or caching) only if nodes with many
filesystem links become common. *Still deferred (re-verified 2026-08-18).*

---

## 3. Filesystem repository vs. the DA/AIP architecture

The central architectural question was whether to keep the filesystem repository as a specialized
implementation or migrate it onto the DA/AIP model. It is **settled — it stays specialized**
(§3.3), and both steps of the resulting plan (3a and 3b) are implemented. This section stays as the
decision record, because the conclusion ran against what `da-migration.md` originally scheduled and
the reasoning should be reviewable rather than taken on trust.

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

### 3.2 What migration would actually cost

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
it occurs at four sites (`DaConnector.java:106`, `DaScheduler.java:51`,
`FileSystemRepoService.isFileSystemRepository`, and `ArrDigitalRepositoryVO.java:87` for the
administration save path; re-verified 2026-08-18). DA, FILESYSTEM and WSDL are three disjoint
parallel code paths sharing one entity table — not polymorphic siblings. The filesystem branch is
the one that grew: it is now consulted from browsing, linking, the DAO panel, URL construction and
the settings-clearing save path, which is the concrete shape the SPI of §3.3 point 2 would absorb.

### 3.3 Decision: a specialized backend behind a shared browsing contract

> **Agreed 2026-07-28.** The filesystem repository requires specialization and cannot be treated
> as just another implementation of a generic DAO/AIP repository. `da-migration.md` Phase 3 was
> revised to match.

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
   `truncated`) were designed to be backend-neutral, and the opaque `lastKey` cursor of §1.2 keeps
   that property — its contents are the backend's business, never the client's.*

4. **Where a filesystem folder genuinely *is* an ingest candidate**, provide an explicit,
   user-initiated "ingest this folder as an AIP" action that mints a real `DaAip` with a
   deliberate code and version: a `FilesystemAipProcessor` sibling to `DaoProcessor`, reusing the
   generic `DaService` factories plus a non-PREMIS variant of `PackageInfoService`.
   *Status: open, optional (§4 item 3).*

5. **Link-status queries.** One batch query per repository, **globally scoped** (no fund predicate,
   §5 item 3). The original preference to omit node references was decided the other way in
   implementation: `FsItem.links` exposes `nodeId`/`fundId`/`fundName`/`nodeLabel`/`nodePath`
   cross-fund, rendered as clickable references in the popover, with `FsLink.readable = false`
   marking links whose fund/nodes the resolver could not read.

### 3.4 Why filesystem links needed no specialized link model

*(Rationale record for `da-migration.md` step 3b, implemented 2026-08-11; the implemented model is
documented in `da-migration.md` §2.)*

Filesystem linking was never a special case. `ArrDaoLink.LinkType` already encoded *what level of
the repository hierarchy a link points at* — `AIP` (whole package, member reference null) versus
`PART_AIP`/`COMPONENT_AIP` (something inside it) — i.e. **container reference + optional member
reference**. A filesystem target is the same shape: whether the user selects the repository root, a
folder or a single file, it is always *(repository, relative path)*. DA links never address an
individual file either — the finest DA granularity is a `DaDao` of type `FILE`, a node in the
hierarchy, not a byte stream — so both models stop linking at the same level.

The result is one link model: an abstract `ArrDaoLink` base (`@Inheritance(JOINED)`) carrying the
shared spine, with `ArrLegacyDaoLink`, `ArrDaLink` and `ArrFsLink` as JOINED subtypes. JOINED was
chosen over SINGLE_TABLE deliberately — consistency with the `ArrData` / `ArrItem` / `ArrRequest`
precedents, and each subtype table declares real NOT NULL constraints on its target columns, so a
link's shape is *which subtype row exists* rather than a `CHECK` over nullable column groups.

Two consequences that still bind new work:

- **Cached link JSON binds to fully-qualified class names** (`@JsonTypeInfo(Id.CLASS)`, the
  `ArrData` precedent) — renaming a link entity class invalidates `arr_cached_node` rows; pair any
  rename with a cache-invalidation changeset (the step 3b wave shows the pattern; the startup sync
  rebuilds dropped rows).
- **Nothing at the DB level prevents child rows in *two* subtype tables** for one `dao_link_id`.
  Hibernate never creates that state and JOINED type resolution would break on it; `da-migration.md`
  Phase 5 validation keeps a verification query for it.

---

## 4. What this leaves for later

None of this is owed by the filesystem feature — it is finished. These are the follow-ons it
leaves behind, in the order they become worth doing, and nothing here blocks anything else.

1. **N5 — batch the per-DAO disk walks** (§2). Deferred by choice; revisit only if nodes carrying
   many filesystem links appear in practice.
2. **The `DigitalRepositoryBackend` SPI** (§3.3 points 2 and 3) — the backend interface plus the
   backend-neutral `RepoItem`/`RepoItems` the `FsItem`/`FsItems` contract was shaped to become.
   Unblocked, but it pays off only when a second backend is actually written against it: doing it
   now means designing an abstraction from a single implementation. `FileSystemRepoBrowser` is
   already the shape a `FileSystemBackend` would take, so the cost of waiting is low.
3. **Ingest-as-AIP** (§3.3 point 4) — the only sanctioned way a filesystem folder acquires real
   `DaAip` semantics. Optional; only if the need is asked for.
4. **`da-migration.md` Phases 4–5** — the WSDL decision and the removal of the `arr_dao` family,
   tracked there; independent of everything above.

---

## 5. Decisions

All architectural questions raised by this analysis are settled.

1. **Filesystem repositories stay specialized** and are not migrated onto `DaAip`/`DaDao`
   (§3.3, agreed 2026-07-28). `da-migration.md` Phase 3 was revised accordingly; both its steps
   are implemented (3a 2026-08-10, 3b 2026-08-11). The two documents are consistent.

2. **Links reuse the `arr_dao_link` model, specialized by JOINED inheritance** (§3.4) — the entity
   hierarchy `ArrLegacyDaoLink` / `ArrDaLink` / `ArrFsLink` under an abstract `ArrDaoLink` base.
   `arr_fs_link` is a JOINED subtype of the one link model, not a parallel link model for
   filesystem repositories. *Implemented 2026-08-11 (`da-migration.md` step 3b).*

3. **"Unlinked" is scoped globally, not per-fund** (agreed 2026-07-28). *Implemented:*
   `findLinksByDigitalRepository` filters on the repository and `delete_change_id IS NULL` only —
   no fund predicate. A user browsing fund A sees paths linked from fund B as linked, and
   `UNLINKED` hides them; the question answered is "has this file already been catalogued
   anywhere". The implementation additionally exposes the linking nodes cross-fund (clickable, with
   `FsLink.readable` marking unresolvable funds) — a deliberate departure from the original
   preference to omit node references.

4. **Repository visibility stays as it is — C8 is not actioned** (agreed 2026-07-28). Every
   configured filesystem repository remains visible to any user who can read a fund.

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

## 6. Verification

Where the automated coverage lives, and what each class is responsible for — extend the matching
one rather than starting somewhere new:

| Test class | Covers |
|---|---|
| `FileSystemRepoServiceTest` | Path containment incl. traversal inputs, MIME detection, inline-renderable classification |
| `FileSystemRepoBrowserTest` | Browse sort/filter/paging incl. Czech collation, last-change ordering, keyset paging across an insert or delete between pages (§1.2); link resolution incl. multi-fund rows; `listDaoFiles` recursive/cap/missing-path; repository availability incl. the templated-root asymmetry; `testRepository` per failure mode; `normalizeRelatPath` and the templated-URL helpers |
| `DaoServiceFsLinkTest` | `createFsDaoLink` and `moveFsDaoLink` — `multiple_links` enforcement, idempotent re-link on the same node, containment check on the link target, the link-move flow |
| `ArrDigitalRepositoryVOTest` | Clearing of the settings of §5 item 5, pass-through for other repository types, `file://` strip |
| `DaoCoreServiceTest` | SOAP DAO flow — unaffected by the filesystem work, kept as the regression guard that it stayed that way |

**Manual acceptance — the checks worth repeating before a release:**

- **Frontend.** Hit "Obnovit" (or switch tabs onto the filesystem tab) and confirm the repository
  list and the tree collapse, then reload — the tree's children cache is wiped and the expansion
  markers stay in sync with it. Point a repository at a missing path, verify it appears greyed
  with an explanation (§1.1), fix the path from administration, hit "Obnovit" and confirm the tree
  updates *without* reopening the dialog. Disconnect the server or block the API and confirm both
  panels render error placeholders instead of empty space. Switch the sort selector across all six
  orders and confirm the listing restarts from the first page each time (§1.2).
- **Scale.** Benchmark a 50 000-entry directory on a network share — the single-pass rewrite, the
  paged `hasChildren` probe and the keyset slice all landed unmeasured; the worst case is a folder
  containing many files and no subfolder, with a page size that surfaces every folder. Since every
  page re-scans the directory (§1.2), the cost of "load more" is the number to watch.
- **Repository configuration.** Manual: a templated root (`{fundId}`) that resolves for one fund
  and not another stays hidden for the second; the administration test reports each failure mode
  distinctly; saving a filesystem repository clears the settings of §5 item 5.

**Running the application:** standard Maven/Spring Boot launch for `elza-core` plus the
`elza-react` dev server; the browser is reached from a fund's DAO panel and needs at least one
external system of type `FILESYSTEM` whose `url` holds a plain path to a readable directory.
