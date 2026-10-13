# DMS storage

How Elza stores managed binary files on disk: attachments (`ArrFile`),
generated outputs (`ArrOutputFile`), publication exports (`arr_export.file_id`)
and import batch sources (`imp_item.dms_id`). Every such file has one
`dms_file` row. The administrator's view is in the admin guide
(`02-installation.rst`, `04-configuration.rst`, `09-backup.rst`); the
operator-facing summary is `dms/README.txt`.

Code: `cz.tacr.elza.service.dms` — `DmsStorageLayout` (path rules),
`DmsService` (write, read, delete), `DmsTrashService`,
`DmsConsistencyService`; migration in `dbchangelog/DbChangeset20261012120000`.

## Why this layout

The original storage was one flat directory `dms/<file_id>`, which had these
problems:

- **Scaling** — every publication and import batch added files to a single
  directory; listing and backup scans grew with the installation.
- **Opacity** — `dms/48213` says nothing about the content type.
- **Orphans** — a rollback after a successful write, an in-place overwrite
  crossing a rollback, and a silently failed `File.delete()` all left files
  behind.
- **Torn writes** — a crash mid-stream left a truncated file under the final
  name while the row looked valid.
- **No integrity check** — after a restore there was no way to verify content.

## Directory layout

```
work/dms/
├── README.txt
├── 2026/
│   └── 09/
│       ├── 000050/
│       │   └── 50990.pdf
│       └── 000051/               # ids 51000..51999
│           ├── 51230.pdf
│           └── 51231.zip
├── _trash/
│   └── 2026-09-14/
│       ├── report.txt
│       └── 50871.xml
├── _migrating/                   # legacy files no row claimed (migration)
└── _migration-20261013-101500.txt
```

### Path rule

`dms_file.storage_path` is a POSIX-style path relative to `dms/`
(`DmsStorageLayout.relativePath`):

| segment | source |
|---|---|
| `yyyy` / `MM` | `dms_file.created_at` in the server time zone |
| `bbbbbb` | `file_id / 1000`, zero-padded to six digits |
| `<id>.<ext>` | `file_id` + sanitized extension of `file_name` |

Example: `file_id=51230`, `file_name="report.PDF"`, created in September 2026
→ `2026/09/000051/51230.pdf`.

- A block directory holds at most 1000 ids, so no leaf directory grows beyond
  1000 files regardless of installation size. Ids come from one sequence, so
  files of one month cluster in one or two blocks.
- `storage_path` is written once, when the row is created, and never changed.
  The stored path is authoritative: a later change of the rule (or of the
  server time zone) affects only new rows.
- `storage_path IS NULL` means the file is at the legacy flat location
  `dms/<file_id>`; `DmsService.getFilePath` (and the static
  `DmsService.newInputStream` used by `DmsFileLoader`) fall back to it.
- The physical path never surfaces through the REST API.

### Extension sanitization

The extension is only a hint for the administrator; the database identifies
the file. Take the text after the last `.` of `file_name`, lower-case it, and
use it if it matches `^[a-z0-9]{1,10}$`; otherwise the name is the bare id.
`scan.tar.gz` → `.gz`; `.hidden` → `.hidden`; `bad.<script>`,
`long.abcdefghijk`, `no-dot` → no extension.

Renaming `file_name` does not rename the file — the extension is fixed for the
lifetime of the row. Replacing an attachment's content creates a new row (see
below), whose extension comes from the new `file_name`.

### Special names

- `<target>.tmp` — a write in progress, in the same directory as the target
  so the final atomic move stays on one filesystem.
- `_trash/<yyyy-MM-dd>/` — one directory per day of removal. Files keep their
  leaf name (`-HHmmss` appended on collision); `report.txt` gets one line per
  file: `<leaf>;<size>;<mtime ISO>;<reason>`. Reasons are short free-form
  slugs (`delete-attachment`, `delete-output`, `delete-plain`,
  `rollback-create`, `write-failed`, `output-regenerated`,
  `revert-attachment`, `fund-delete-output`, `fund-history-delete-*`,
  `consistency-orphan`, `consistency-stale-tmp`).
- `_migrating/` — legacy files the migration could not assign to a row; never
  purged automatically.
- `_migration-*.txt` — migration problem reports.
- `README.txt` — copied from the classpath resource `dms/README.txt` at
  startup (`DmsService.initDmsStorage`) whenever the on-disk copy differs, so
  edits do not survive.

Nothing else is ever written under `dms/`.

## Database

Changeset `20260918120000` (`db.elza-3-part-03.xml`) adds to `dms_file`:

| column | type | |
|---|---|---|
| `created_at` | timestamptz NOT NULL | source of `yyyy/MM` |
| `storage_path` | nvarchar(255) NULL | authoritative relative path; NULL = legacy flat |
| `checksum` | nvarchar(64) NULL | SHA-256 hex of the content |

Existing rows got `created_at` from their back-references, in this order:
attachments via `arr_file.create_change_id` → `arr_change.change_date`;
outputs via `arr_output_result.change_id`; publication exports
`MIN(arr_export.created_at)` (one file may back several exports); import
sources `MIN(imp_item.created_at)`; `CURRENT_TIMESTAMP` for unreferenced rows.
All updates use portable correlated subqueries (no `UPDATE ... FROM`).

`storage_path` has no index: lookups go by `file_id`, and a unique index with
many NULLs would need dialect-specific filtered indexes on MSSQL.

## Write path

`DmsService.createFile` (both the `InputStream` and the
`Consumer<OutputStream>` variant) → `writeFile`:

1. Set `created_at = now`, save the row to allocate `file_id`, compute
   `storage_path`.
2. Stream to `<target>.tmp` through a SHA-256 `DigestOutputStream`, flush and
   `fsync`.
3. Set `file_size` (`Math.toIntExact` — a file over 2 GiB fails loudly instead
   of overflowing), `checksum`, and the PDF page count read from the tmp file.
4. `Files.move(tmp, target, ATOMIC_MOVE)` — the final name never holds a
   partial file.
5. Register an `afterCompletion` hook: on **rollback** the new file is moved to
   the trash (`rollback-create`), so a rolled-back create leaves no orphan.
6. Save the row, publish `FILES_CHANGE`.

Any failure while writing moves the tmp file to the trash (`write-failed`)
and rethrows; the row is not committed.

## Replacing attachment content

Content is never overwritten in place. `DmsService.replaceArrFileContent`
creates an `arr_change` of type `UPDATE_ATTACHMENT`, marks the old `ArrFile`
deleted by it, and creates a new `ArrFile` (new `dms_file` row, new
`storage_path`) through the normal write path. The old row and file stay
untouched, so the replacement is revertible through `RevertingChangesService`
like any other change; reverting removes the new file
(`revert-attachment`). The cost is disk space: old content stays until the
history is dropped (fund deletion, history deletion).

`DmsService.updateFile` changes metadata only (name, file name, MIME type,
size).

## Delete path

Files are never deleted directly; they are moved to the trash **after
commit**, so a rolled-back delete keeps its file:

- `deleteFile(DmsFile)` resolves the path *before* deleting the row (the path
  lives on the row), deletes the row and registers an `afterCommit` move
  (`delete-attachment|output|plain`).
- `deleteFilesAfterCommit(files, reason)` does the same for a collection
  (fund deletion, history deletion, output regeneration, revert). Callers must
  load the entities before bulk-deleting the rows.
- Without an active transaction synchronization the move happens at once.
- A failed move is logged as WARN, never silently ignored; the consistency
  check finds the leftover file later.
- Publication exports may share one file (`PublicationService.copy`);
  `deleteFileIfUnreferenced` deletes it only with the last reference.

`DmsTrashService.purgeExpired` runs from `ScheduledCleanupWorker` (the
`elza.cleanup.cron` job) and deletes day directories older than
`elza.dms.trashRetentionDays`. Until then every delete can be undone by hand.

## Migration of the flat layout

A Liquibase custom change (`DbChangeset20261012120000`, `runInTransaction="false"`),
so it runs once, at the first startup after the upgrade, before the
application serves requests. JPA is not available yet; it uses JDBC and reads
`elza.workingDir` from the Spring `Environment`.

1. **Stage** — every legacy file `dms/<digits>` is moved to
   `_migrating/<digits>`. Afterwards no legacy name (e.g. a file `2026`) can
   collide with a year directory. A name already present in `_migrating/` is
   left in place and reported (`STAGE_CONFLICT`).
2. **Row pass** over all `dms_file` rows: the target is `storage_path` or, if
   NULL, the path computed from `created_at`. If the target exists, the row is
   in place (a leftover legacy copy is reported `DUPLICATE` and stays in
   `_migrating/`). Otherwise the file is moved there from `_migrating/` (or
   the flat root). If no source exists the row is `MISSING`. `storage_path`
   is written in every case except an I/O error (`FAILED`, path left unset).
3. **Orphans** — whatever remains in `_migrating/` is reported `ORPHAN` and
   left for the administrator; nothing is deleted.

Problem rows go to `dms/_migration-<yyyyMMdd-HHmmss>.txt`
(`<id>\t<state>\t<detail>`); a summary is logged at INFO, FAILED rows at
WARN. The step is idempotent: running it again on a migrated tree only
confirms rows in place, and it also repairs the state left by the first
migration (changeset `20260918120000`, now a no-op), which parked legacy
files with four-digit ids in `_migrating/` and marked them MISSING.

## Consistency check

`DmsConsistencyService.check(verifyChecksums, moveOrphansToTrash)`:

- scheduled by `elza.dms.check.cron` (default Sunday 03:30) with both options
  on; on demand via `POST /api/v1/admin/dms/consistency-check` (permission
  `ADMIN`, both options default false; `POST` because it can move files and
  write checksums).
- **Snapshot** — pages through `dms_file` (500 rows per read-only
  transaction) into maps by `storage_path` and by id for legacy rows (roughly
  30 bytes per row, ~100 MB for a million files).
- **Walk** `dms/`, skipping `_trash/`, `_migrating/`, `README.txt` and
  `_migration-*.txt`:
  - `*.tmp` older than `orphanMinAgeMinutes` → `staleTmp` (trashed if asked);
  - a file matching a row → size compare (`sizeMismatch`); with
    `verifyChecksums` a SHA-256 compare (`corrupted`), or a **checksum
    backfill** when the row has none;
  - a root `<digits>` file or a `yyyy/MM/bbbbbb/*` file without a row, older
    than `orphanMinAgeMinutes` → `orphans` (trashed if asked);
  - anything else → `foreign`, reported only, never touched.
- Rows not seen in the walk → `missing`; rows with NULL `storage_path` →
  `notMigrated`.
- The report gives the true count and the first 500 items per category.

The age guard protects a file whose row was committed after the snapshot was
taken; without it the walk would trash a valid new file.

## Configuration

| key | default | |
|---|---|---|
| `elza.dms.trashRetentionDays` | 30 | trash purge (`ScheduledCleanupWorker`) |
| `elza.dms.orphanMinAgeMinutes` | 60 | age guard of the consistency check |
| `elza.dms.check.cron` | `0 30 3 ? * SUN` | scheduled check; `-` disables it |

The DMS root is always `${elza.workingDir}/dms`; there is no switch for the
migration (it is part of the upgrade) and the checksum algorithm is fixed
(SHA-256).

## Deployment notes

- **Backup.** The database and `work/dms` are one unit: back up and restore
  them together, from the same moment.
- **Downgrade** to a version before the migration requires restoring both
  from before the upgrade; the older version cannot find files in the new tree.
- **One filesystem.** Atomic moves (tmp → target, migration, trash) need
  source and target on one filesystem. A subdirectory of `dms/` symlinked
  elsewhere breaks this.
- **FAILED migration rows** are not retried automatically (the changeset has
  run). Their file is typically in `_migrating/` while `storage_path` is NULL,
  so it is unreadable until moved to its computed path and `storage_path` set
  by hand; check `_migration-*.txt` after the upgrade.
- **Windows.** Antivirus or an open handle can block a move; a failed trash
  move leaves the file in place for the consistency check.
- **Trash disk usage.** Every deletion is kept for `trashRetentionDays`;
  installations with heavy publication/import churn should shorten it or plan
  headroom.
- **Replaced attachments** keep their old content on disk for as long as the
  history exists.

## Open work

- **`dms_file.file_size` is `int`:** files over 2 GiB fail at write time.
  Widening it touches the entity, VOs with a size field and the REST clients.
- **`GET /api/dms/{fileId}`** checks fund read permission with
  `file.getFundId()`, which is null for plain `DmsFile` rows (publication
  exports, import sources), so their download fails. They need their own
  permission gate or a dedicated download endpoint.
