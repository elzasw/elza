# DMS storage rework — implementation notes

This document is a per-file, per-line implementation guide for the DMS storage
rework described in the design memo `260913 - Přepracování DMS.odt`. It targets
the developer who will land the changes on branch `3.4.x`. Paths are relative
to the repository root; line numbers are as of the branch tip at drafting time
and may drift — re-read the file if a hunk does not match.

## 1. Current state

### 1.1 Filesystem layout

DMS is a single flat directory resolved from `elza.workingDir`:

- [ResourcePathResolver.java:73](elza-core/src/main/java/cz/tacr/elza/core/ResourcePathResolver.java#L73)
  `getDmsDir()` returns `${workDir}/dms`.
- [ResourcePathResolver.java:91](elza-core/src/main/java/cz/tacr/elza/core/ResourcePathResolver.java#L91)
  `getDmsFile(String)` resolves `${workDir}/dms/<fileName>`; the argument is the
  numeric `file_id` in string form, without an extension. Only call site to be
  removed in §5.

Every managed binary — `ArrFile` attachment, `ArrOutputFile` output, publication
export referenced from `arr_export.file_id`, import batch source referenced from
`imp_item.dms_id` — sits directly under `dms/` under that name. There are no
subdirectories, no `.tmp` files, no trash.

### 1.2 Database schema

`dms_file` is defined in
[db.elza-init.xml:562-581](elza-core/src/main/resources/db/changelog/db.elza-init.xml#L562):

| Column        | Type            | Nullable |
|---------------|-----------------|----------|
| `file_id`     | `int` (PK)      | no       |
| `name`        | `nvarchar(250)` | no       |
| `file_name`   | `nvarchar(250)` | no       |
| `file_size`   | `int`           | no       |
| `mime_type`   | `nvarchar(250)` | no       |
| `pages_count` | `int`           | yes      |

No creation timestamp, no path, no checksum. The entity mirrors the table 1:1 at
[DmsFile.java:28-58](elza-core/src/main/java/cz/tacr/elza/domain/DmsFile.java#L28)
and is a `JOINED` inheritance root; concrete subclasses `ArrFile` and
`ArrOutputFile` add fund/output context. Plain `DmsFile` rows exist for
publication exports and import sources.

### 1.3 Write path

[DmsService.createFile(DmsFile, InputStream):101](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L101)
and its `Consumer<OutputStream>` twin at
[:127](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L127) both
call `fileRepository.save(dmsFile)` to allocate `file_id`, resolve the final
path with `getFilePath(dmsFile)`, and then write the payload straight to that
final path via
[saveFile:374](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L374):

- [:377](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L377)
  `FileUtils.touch(outputFile)` creates the target file eagerly.
- [:379](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L379)
  `new BufferedOutputStream(new FileOutputStream(outputFile, false))` writes in
  place under the final name; there is no temporary file and no `fsync`.
- [:383](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L383)
  the `catch (IOException)` deletes the target on stream failure only. A
  successful stream followed by a transaction rollback leaves the file behind:
  no `TransactionSynchronization` is registered for creation.
- [:388-395](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L388)
  size is written back as `(int) outputFile.length()` — silent overflow past
  2 GiB — and PDF page count is read from the final file with PDFBox.

### 1.4 Read path

[DmsService.getFilePath(DmsFile):405](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L405)
delegates to
[getFilePath(int):416](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L416),
which returns `resourcePathResolver.getDmsDir().resolve(String.valueOf(fileId))`.
A second static reader,
[newInputStream(ResourcePathResolver, DmsFile):236](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L236),
rebuilds the same path independently — it is the only caller of
`ResourcePathResolver.getDmsFile` outside the resolver itself and is invoked
from a place that cannot easily reach `DmsService` (documented in §5).

### 1.5 Update path

[DmsService.updateFile:192](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L192)
overwrites the target in place: on
[:216](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L216) it
calls `outputFile.delete()` unconditionally and immediately writes the new
payload with `saveFile`. There is no backup and no rollback compensation, so a
transaction abort between old-content deletion and new-content commit leaves
the row pointing at fresh bytes that will never appear in the database.

### 1.6 Delete path

[DmsService.deleteFile:298](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L298)
deletes the row and schedules the on-disk delete via
[deleteFilesAfterCommitByIds:322](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L322).
Inside the `afterCommit` block at
[:331-338](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L331)
the code calls `file.delete()` with no result check: a delete blocked by an
antivirus scan or an open handle (Windows) is lost silently. There is no
audit trail of what was removed.

### 1.7 Consequences that motivate the rework

- **Scaling.** Every publication and every import batch adds files to a single
  directory. Listing and backup scan cost grows with total install size.
- **Opacity.** `dms/48213` gives the administrator no hint of content type.
- **Orphans.** Three independent leaks:
  - Write path leaks on rollback after a successful stream (§1.3).
  - Update path leaks the old content on any I/O boundary crossing a rollback
    (§1.5).
  - Delete path leaks on silent `File.delete()` failure (§1.6).
- **Torn writes.** A crash mid-stream leaves the final name pointing at a
  truncated payload; the database still says the row is valid.
- **No integrity check.** After a restore from backup, there is no way to
  confirm the on-disk file matches the intended content.

These are the properties the rework must fix; §2 states the target layout,
§3-§9 the implementation, §10 the sequencing.

## 2. Target layout

### 2.1 Directory tree

```
work/dms/
├── README.txt
├── 2019/
│   └── 03/
│       └── 000000/
│           ├── 17.pdf
│           └── 18.docx
├── 2026/
│   ├── 08/
│   │   └── 000050/
│   │       ├── 50871.pdf
│   │       └── 50872.xml
│   └── 09/
│       ├── 000050/
│       │   └── 50990.pdf
│       └── 000051/               # holds id 51000..51999
│           ├── 51230.pdf
│           └── 51231.zip
└── _trash/
    ├── 2026-09-13/
    │   ├── report.txt
    │   ├── 777                    # legacy-orphan found by migration
    │   └── 50871.xml              # removed by publication retention
    └── 2026-09-14/
```

Nothing else may ever be written under `dms/`. Any name that does not match a
rule below is treated as foreign by the consistency check (§4, §5).

### 2.2 Path rule

`storage_path` is a POSIX-style relative path stored in `dms_file` (§3). Its
segments are, in order:

| Segment      | Source                                                          |
|--------------|-----------------------------------------------------------------|
| `yyyy`       | year of `dms_file.created_at` in the server time zone           |
| `MM`         | month of `dms_file.created_at`, zero-padded                     |
| `bbbbbb`     | `file_id / 1000`, zero-padded to six digits                     |
| `<id>.<ext>` | `file_id` and the sanitized extension of `file_name` (§2.3)     |

Example: a row with `file_id=51230`, `file_name="report.PDF"`,
`created_at=2026-09-14T…` stores `storage_path = "2026/09/000051/51230.pdf"`.

Rules that fall out of the design:

- One row of `dms_file` is created per file. Ids are allocated from the shared
  sequence that already backs the entity; files created in one month therefore
  cluster in one block directory or two.
- The block directory is capped by construction: any block `bbbbbb` holds at
  most 1 000 ids, so the leaf directory never exceeds 1 000 files regardless of
  install size.
- `storage_path` is written once, in the transaction that creates the row, and
  never mutated. A `NULL` value means the row still lives at the legacy flat
  location `dms/<file_id>` and is subject to migration (§5).
- The path is authoritative. If the deriving rule ever changes, only new rows
  see the new rule; existing rows keep the path they were assigned.

### 2.3 Extension sanitization

The extension in the on-disk name is a hint for the human administrator; the
database identifies the file. The rule:

1. Let `raw` be the substring of `file_name` after the last `.`, or empty if
   there is no `.`.
2. Lower-case `raw`.
3. If the result matches `^[a-z0-9]{1,10}$`, use it as the extension.
4. Otherwise, emit no extension: the on-disk name is bare `<file_id>`.

Examples:

| `file_name`              | On-disk name  |
|--------------------------|---------------|
| `report.PDF`             | `51230.pdf`   |
| `scan.tar.gz`            | `51230.gz`    |
| `data.json5`             | `51230.json5` |
| `weird.name`             | `51230.name`  |
| `no-dot-here`            | `51230`       |
| `.hidden`                | `51230.hidden`|
| `bad.<script>`           | `51230`       |
| `long.abcdefghijk`       | `51230`       |

Renaming `file_name` does not rename the on-disk file. The extension is fixed
at creation for the same reason `storage_path` is fixed: history stays
readable.

The extension freeze applies for the lifetime of one `dms_file` row.
Replacing an attachment's content (§5.2) creates a fresh row with a fresh
`storage_path`, and the extension of the new row is derived from the new
`file_name` at that time.

### 2.4 Temporary and trash names

- `<target>.tmp` — a write in progress. It lives in the same directory as the
  final target so that the finishing `Files.move(..., ATOMIC_MOVE)` stays on
  one filesystem (§10 risks). A `.tmp` file older than `orphanMinAgeMinutes`
  (§7) is moved to trash by the consistency check.
- `_trash/<yyyy-MM-dd>/` — one directory per day of removal. Every file inside
  keeps its original leaf name; on collision the mover appends `-HHmmss`. Each
  directory contains a `report.txt` (§4.2) written line by line as files
  arrive.

### 2.5 README.txt

Written by the application on startup. Roughly fifteen Czech lines: purpose of
the directory, structure rule with one worked example, note that `<id>` equals
`dms_file.file_id` and `dms_file.storage_path` is authoritative, warning not
to rename or delete files by hand, explanation of `_trash/<date>/`,
`_migration-*.txt` and `*.tmp`, note that the directory backs up together with
the database. The exact text lives on the classpath and is copied over on
every startup so an operator's edits do not survive an upgrade.

## 3. Database

### 3.1 Column additions

Three columns are added to `dms_file`:

| Column         | SQL type (Liquibase)             | Nullable at end | Purpose                             |
|----------------|----------------------------------|-----------------|-------------------------------------|
| `created_at`   | `timestamp with time zone`       | no              | source of `yyyy/MM` in `storage_path` |
| `storage_path` | `nvarchar(255)`                  | yes             | authoritative on-disk path; NULL = legacy flat |
| `checksum`     | `nvarchar(64)`                   | yes             | SHA-256 of content, hex             |

Type notes:

- `timestamp with time zone` is already in use on `arr_change.change_date`
  ([db.elza-init.xml:968](elza-core/src/main/resources/db/changelog/db.elza-init.xml#L968)),
  `arr_export.created_at`
  ([db.elza-3-part-01.xml:1053](elza-core/src/main/resources/db/changelog/db.elza-3-part-01.xml#L1053))
  and `imp_item.created_at`
  ([db.elza-3-part-03.xml:469](elza-core/src/main/resources/db/changelog/db.elza-3-part-03.xml#L469)),
  so PostgreSQL, MSSQL and H2 already carry the mapping.
- `storage_path` stores POSIX-style paths (`/` separator) — never Windows
  backslashes; 255 is comfortably above the observed max (year/month/block/id
  plus extension ≤ 30 bytes).
- `checksum` is fixed-width hex (SHA-256 = 64 hex characters), single-byte —
  no `nvarchar` savings, but consistent with the surrounding columns.
- `storage_path` gets no index. Lookups happen by `file_id`; a unique index
  cannot be created on MSSQL because MSSQL treats every `NULL` as distinct
  only under a filtered index, which Liquibase would have to emit
  dialect-specifically.

### 3.2 Changeset placement

New changeset is appended to
[db.elza-3-part-03.xml](elza-core/src/main/resources/db/changelog/db.elza-3-part-03.xml)
(the file's header at
[:1-13](elza-core/src/main/resources/db/changelog/db.elza-3-part-03.xml#L1)
marks it OPEN — the only file that accepts new changesets on 3.4.x). The
changeset id is a fresh 14-digit UTC timestamp, matching the style of
[:19](elza-core/src/main/resources/db/changelog/db.elza-3-part-03.xml#L19)
(`id="20260812110000" author="ppy"`). The changeset opens with a Czech-style
English rationale comment, one paragraph, mirroring the neighboring blocks.

Do **not** add an `insert` into `db_hibernate_sequences` for the new columns —
the sequence backing `dms_file.file_id` is untouched, and the codebase has
recently standardized on omitting that insert for new columns.

### 3.3 `created_at` backfill

Every existing `dms_file` row is one of four kinds. The backfill applies four
correlated `UPDATE`s in order, then a fallback for the residual, then
promotes the column to `NOT NULL`. All four queries use portable
correlated-subquery syntax accepted by PostgreSQL, MSSQL and H2 — no
`UPDATE ... FROM`, no dialect-specific `MERGE`.

```sql
-- 1) Attachments: dms_file <- arr_file.create_change_id -> arr_change.change_date
UPDATE dms_file
   SET created_at = (
       SELECT c.change_date
         FROM arr_file f
         JOIN arr_change c ON c.change_id = f.create_change_id
        WHERE f.file_id = dms_file.file_id)
 WHERE created_at IS NULL
   AND EXISTS (SELECT 1 FROM arr_file f WHERE f.file_id = dms_file.file_id);

-- 2) Output files: dms_file <- arr_output_file -> arr_output_result.change_id -> arr_change.change_date
UPDATE dms_file
   SET created_at = (
       SELECT c.change_date
         FROM arr_output_file ofile
         JOIN arr_output_result ores ON ores.output_result_id = ofile.output_result_id
         JOIN arr_change c ON c.change_id = ores.change_id
        WHERE ofile.file_id = dms_file.file_id)
 WHERE created_at IS NULL
   AND EXISTS (SELECT 1 FROM arr_output_file ofile WHERE ofile.file_id = dms_file.file_id);

-- 3) Publication exports: one dms_file may back several arr_export rows (copies)
UPDATE dms_file
   SET created_at = (
       SELECT MIN(e.created_at)
         FROM arr_export e
        WHERE e.file_id = dms_file.file_id)
 WHERE created_at IS NULL
   AND EXISTS (SELECT 1 FROM arr_export e WHERE e.file_id = dms_file.file_id);

-- 4) Import sources: one dms_file per imp_item, MIN() for safety on any repeats
UPDATE dms_file
   SET created_at = (
       SELECT MIN(i.created_at)
         FROM imp_item i
        WHERE i.dms_id = dms_file.file_id)
 WHERE created_at IS NULL
   AND EXISTS (SELECT 1 FROM imp_item i WHERE i.dms_id = dms_file.file_id);

-- 5) Fallback: an unreferenced dms_file row is a bug elsewhere; give it *some*
--    timestamp so NOT NULL survives, and let the migration protocol surface it.
UPDATE dms_file
   SET created_at = CURRENT_TIMESTAMP
 WHERE created_at IS NULL;
```

Table-column dependencies for the backfill queries:

| Query | Depends on                                                                  |
|-------|------------------------------------------------------------------------------|
| 1     | [arr_file.create_change_id:1035](elza-core/src/main/resources/db/changelog/db.elza-init.xml#L1035), [arr_change.change_date:968](elza-core/src/main/resources/db/changelog/db.elza-init.xml#L968) |
| 2     | [arr_output_file:1687](elza-core/src/main/resources/db/changelog/db.elza-init.xml#L1687), [arr_output_result.change_id:1666](elza-core/src/main/resources/db/changelog/db.elza-init.xml#L1666) |
| 3     | [arr_export.file_id:1086](elza-core/src/main/resources/db/changelog/db.elza-3-part-01.xml#L1086), [arr_export.created_at:1053](elza-core/src/main/resources/db/changelog/db.elza-3-part-01.xml#L1053) |
| 4     | [imp_item.dms_id:459](elza-core/src/main/resources/db/changelog/db.elza-3-part-03.xml#L459), [imp_item.created_at:469](elza-core/src/main/resources/db/changelog/db.elza-3-part-03.xml#L469) |

### 3.4 NOT NULL promotion

After the five `UPDATE`s, a single `addNotNullConstraint` on
`dms_file.created_at` closes the column. `storage_path` and `checksum` remain
nullable: they fill in as the migration (§5) processes each row and the
consistency check (§4) fills in missing checksums on legacy content.

### 3.5 Entity mapping

`DmsFile` at
[DmsFile.java:28](elza-core/src/main/java/cz/tacr/elza/domain/DmsFile.java#L28)
gains three fields:

- `createdAt : OffsetDateTime` — mapped to `created_at`, `nullable = false`.
  Set exclusively by `DmsService.createFile` (§5); no `@PrePersist` and no
  default in the entity — the NOT NULL constraint catches any code path that
  forgets. `OffsetDateTime` matches the Hibernate 6 default mapping for
  `timestamp with time zone`.
- `storagePath : String` — mapped to `storage_path`, `nullable = true`.
  Written once by `DmsService.createFile` (§5) and by
  `DmsStorageMigrationService` (§5); never mutated afterwards.
- `checksum : String` — mapped to `checksum`, `nullable = true`. Written by
  `DmsService.createFile` and by the consistency check (§5).

The transient `File file` field at
[DmsFile.java:57](elza-core/src/main/java/cz/tacr/elza/domain/DmsFile.java#L57)
stays untouched — it is a Jackson-ignored scratch slot used by callers that
package a temporary file before calling `createFile`.

VO classes (`DmsFileVO`, `ArrFileVO`, `ArrOutputFileVO`) are **not** changed:
the physical path is an internal concern and never surfaces through the REST
API.

## 4. New classes

All four classes live under `elza-core/src/main/java/cz/tacr/elza/service/dms/`,
alongside the existing `DmsService` (moved into the same sub-package).
Package: `cz.tacr.elza.service.dms`.

### 4.1 `DmsStorageLayout`

Pure static utility. No Spring bean, no fields, no I/O; testable without a
container. Encodes the path rules of §2 and the extension whitelist of §2.3
in one place so the rest of the code never concatenates DMS paths.

```java
public final class DmsStorageLayout {

    public static final String TRASH_DIR         = "_trash";
    public static final String MIGRATING_DIR     = "_migrating";
    public static final String README_FILE       = "README.txt";
    public static final String TMP_SUFFIX        = ".tmp";
    public static final String MIGRATION_LOG_PREFIX = "_migration-";

    private static final DateTimeFormatter YEAR   = DateTimeFormatter.ofPattern("yyyy");
    private static final DateTimeFormatter MONTH  = DateTimeFormatter.ofPattern("MM");
    private static final DateTimeFormatter DAY    = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern EXT_ALLOWED      = Pattern.compile("^[a-z0-9]{1,10}$");
    private static final Pattern LEGACY_LEAF_NAME = Pattern.compile("^\\d+$");
    private static final int  BLOCK_SIZE = 1000;
    private static final ZoneId SERVER_ZONE = ZoneId.systemDefault();

    private DmsStorageLayout() {}

    /** Relative POSIX path used as {@code dms_file.storage_path}. */
    public static String relativePath(OffsetDateTime createdAt, int fileId, String fileName);

    /** Lower-cased extension of {@code fileName} if it matches {@code ^[a-z0-9]{1,10}$}, else empty. */
    public static String sanitizedExtension(String fileName);

    /** True for the flat legacy leaf name ({@code "<digits>"} with no extension). */
    public static boolean isLegacyName(String leafName);

    /** {@code <target>.tmp} in the same directory as {@code target}. */
    public static Path tmpPath(Path target);

    /** {@code <dmsRoot>/_trash/<yyyy-MM-dd>/}. */
    public static Path trashDir(Path dmsRoot, LocalDate day);
}
```

Behaviour notes:

- `relativePath` uses the server's default time zone at call time. The path
  is written to `storage_path` once (§2.2), so a later time-zone change on
  the host does not move the file.
- The block segment is `String.format("%06d", fileId / 1000)` — this is the
  only place the constant `1000` appears; a future re-parameterisation is
  one edit here plus a new unit-test row.
- Extension sanitisation is delegated to `sanitizedExtension` and returns the
  empty string, never `null`; the caller composes `<id>` or `<id>.<ext>` by
  string concat and an `isEmpty()` check.
- `SERVER_ZONE` is a field so `DmsStorageLayoutTest` can override via a
  package-private setter if a time-zone-dependent test needs it — otherwise
  the class is a bare `final` with a private constructor.

Unit test: `DmsStorageLayoutTest` in
`elza-core/src/test/java/cz/tacr/elza/service/`. Coverage:
`sanitizedExtension` for each row of the §2.3 example table; `relativePath`
for month boundaries, two adjacent ids in the same block, adjacent ids across
a block boundary (`51999` → `000051`, `52000` → `000052`); `isLegacyName`
distinguishing `48213` from `48213.pdf` and from `_trash`.

### 4.2 `DmsTrashService`

Spring bean. Holds the on-disk trash and the retention purge. Never asks the
database anything; the caller passes a `Path` and a reason string.

```java
@Service
public class DmsTrashService {

    @Autowired private ResourcePathResolver resourcePathResolver;

    /**
     * Move {@code source} into {@code _trash/<today>/} under its own leaf name.
     * On name collision inside the day directory, appends {@code -HHmmss}.
     * Appends one line to {@code report.txt}.
     *
     * @return path in the trash, or {@code null} if the source did not exist
     *         (already gone counts as success)
     */
    public Path moveToTrash(Path source, String reason);

    /**
     * Delete every {@code _trash/<yyyy-MM-dd>/} directory whose date is
     * strictly older than {@code today - retentionDays}.
     *
     * @return number of directories deleted
     */
    public int purgeExpired(int retentionDays);
}
```

`report.txt` line format, one line appended per moved file, UTF-8, `\n`:

```
<leaf-name-in-trash>;<size-bytes>;<mtime-ISO-INSTANT>;<reason>
```

Example:
```
50871.xml;73412;2026-09-13T14:22:07.331Z;publication-retention
17.pdf-142207;18234;2026-09-13T14:22:07.412Z;consistency-orphan
```

The write is `Files.writeString(..., APPEND, CREATE)`. No header row (a
day's `report.txt` grows over the day; no consumer parses it as CSV — it is
an operator log).

Behaviour notes:

- The move uses `Files.move(source, target, StandardCopyOption.ATOMIC_MOVE)`.
  Trash directories live under `dms/`, so target and source are always on
  the same filesystem for files created by this application.
- Missing-source is not an error: some callers (consistency check) race with
  legitimate deletes. `null` return means "nothing to do".
- The reason is a short slug from a small vocabulary: `publication-retention`,
  `import-retention`, `attachment-delete`, `output-regenerated`,
  `consistency-orphan`, `consistency-stale-tmp`, `legacy-orphan`, `duplicate`,
  `unexpected`. The vocabulary is a `String` — no enum, so a future caller
  adds one without regenerating an enum contract.
- `purgeExpired` uses `Files.walk` scoped to the trash root, filters by
  `LocalDate.parse(dirName)`, and deletes recursively. A day directory whose
  name does not parse is skipped and logged as WARN.

Unit tests: `DmsTrashServiceTest` — move happy path, collision suffix
appended, missing source returns null, `report.txt` line format, purge
respects retention, purge tolerates non-date directory names.

### 4.3 `DmsStorageMigrationService`

Spring bean, no `@Transactional` on the class — the migration opens its own
short-lived transactions batch by batch, so an unrelated startup task cannot
be dragged into the migration transaction and vice versa.

```java
@Service
public class DmsStorageMigrationService {

    /** Called once per startup, from StartupService (§5.8). Idempotent. */
    public MigrationReport migrateAll();
}
```

Algorithm, in the order the code runs it:

1. **Prepare.** Ensure `dms/` exists; write `README.txt` from the classpath
   resource (overwriting if present). Ensure `_trash/` and `_migrating/`
   exist.
2. **Root-collision pre-pass.** For every entry in `dms/` whose leaf name
   equals a year we may write to (any four-digit numeric name), rename the
   entry into `_migrating/<name>` before the migration proper starts. This
   is the only case where a legacy flat name (a plain number like `2026`)
   collides with a new-layout directory (`2026/`). The moved entry is later
   processed by the row-by-row pass or, if unreferenced, ends in trash as
   `legacy-orphan`.
3. **Row pass.** In batches of `migrationBatchSize` (§7), open a fresh
   read-write transaction and stream rows with `storage_path IS NULL`
   ordered by `file_id`. For each row:

   | Source | Target | State      | Action                                                                                 |
   |--------|--------|------------|----------------------------------------------------------------------------------------|
   | exists | absent | MIGRATED   | rename source → target; set `storage_path`, `created_at`-derived path is already stored |
   | absent | exists | RECOVERED  | previous run crashed between rename and commit; set `storage_path` from the target it found |
   | absent | absent | MISSING    | set `storage_path` anyway (so DB has one shape) and record the id for the log         |
   | exists | exists | DUPLICATE  | keep target, move source to trash with reason `duplicate`, set `storage_path`         |
   | I/O err| —      | FAILED     | log WARN, leave row untouched (retry on next startup)                                 |

   The source is `resolveLegacyPath(fileId)` = `dms/<file_id>`; the target
   is `dms/<relativePath(created_at, file_id, file_name)>`. Target parents
   are created lazily. The rename uses `ATOMIC_MOVE`. `storage_path` is
   assigned after the rename succeeds; on transaction rollback the file is
   already at the target and the next run treats it as RECOVERED — that is
   why RECOVERED is a normal state, not an anomaly.
4. **Root scan.** After the row pass drains, walk `dms/` at depth 1 and
   flag every unexpected top-level entry:
   - Numeric-only leaf names (`isLegacyName`) whose id is not in `dms_file`
     → trash with reason `legacy-orphan`.
   - Any other unexpected file/directory (not `README.txt`, not a
     `yyyy/` year directory, not `_trash`, not `_migrating`, not
     `_migration-*.txt`) → trash with reason `unexpected`.
5. **Report.** If any row had state MISSING, DUPLICATE or FAILED, write
   `dms/_migration-<yyyyMMdd-HHmmss>.txt` — one line per problem row,
   tab-separated `<file_id>\t<state>\t<detail>`. Log a summary line at INFO
   in all cases; log at WARN if any FAILED rows remain.

Reason `legacy-orphan` is a **persistent** trash entry, not migration-only:
the same reason surfaces from `DmsConsistencyService` (§4.4) whenever a
plain-number leaf is found in the tree after migration. See §5.8 for how the
migration wires into `StartupService`.

Unit tests: `DmsStorageMigrationServiceTest` — happy path (single row,
MIGRATED); missing source (MISSING); orphan file (routed to trash on root
scan); root-name collision with a year directory (`_migrating/` fallback);
second run with all rows already migrated is a no-op; RECOVERED path with a
pre-existing target.

### 4.4 `DmsConsistencyService`

Spring bean. Runs on a schedule (§5.9) and, via the admin endpoint (§6),
on demand.

```java
@Service
public class DmsConsistencyService {

    public DmsConsistencyReport check(boolean verifyChecksums,
                                      boolean moveOrphansToTrash);
}
```

Sequence, top-down:

1. **Snapshot the database.** Read-only paged streaming over `dms_file`
   projecting `(file_id, storage_path, file_size, checksum)`; page size is
   the same `migrationBatchSize`. Build two in-memory sets:
   `pathsByStoragePath` (the live-file view) and `legacyIds` (rows with
   `storage_path IS NULL`, expected under `dms/<file_id>`). At ~30 bytes
   per entry, one million files sits around 100 MB — sized for a big
   installation (§10 risks).
2. **Walk the tree.** `Files.walkFileTree` rooted at `dms/`, depth
   ≤ 3, skipping `_trash`, `_migrating`, `README.txt` and any
   `_migration-*.txt` at the root. For every regular file encountered:
   - If its path matches a `storage_path` snapshot entry: check size; if
     `verifyChecksums`, stream SHA-256 and compare. Emit `sizeMismatch` or
     `corrupted` on divergence.
   - If its relative name equals `dms/<digits>` and the id is in
     `legacyIds`: same size/checksum comparison against the legacy row.
   - Else if the leaf ends with `.tmp` and its `mtime` is older than
     `orphanMinAgeMinutes` (§7): emit `staleTmp`; if `moveOrphansToTrash`,
     move to trash with reason `consistency-stale-tmp`.
   - Else if the leaf is numeric-only and not in `legacyIds`, or its
     `storage_path` shape does not resolve: emit `orphans`; if
     `moveOrphansToTrash`, move to trash with reason `consistency-orphan`.
   - Else: emit `foreign` (something no rule created) and only record — do
     not touch. Foreign files are the operator's; consistency reports them
     and stops.
3. **Reconcile the snapshot residual.** Every `storage_path` and every
   `legacyId` still unaccounted for after the walk is a `missing` row.
4. **Checksum backfill.** For every legacy row whose `checksum` is null and
   whose file exists (verifyChecksums implies this pass — otherwise skip
   it), compute SHA-256 and update `dms_file.checksum` in the same paged
   batches. This is the only write the consistency check performs
   unconditionally on legacy rows.
5. **Return.**

Report shape returned to the caller (also §6):

```java
public final class DmsConsistencyReport {
    public int  missingCount, orphansCount, sizeMismatchCount,
                corruptedCount, notMigratedCount, staleTmpCount,
                foreignCount;
    public List<String> missing, orphans, sizeMismatch,
                        corrupted, notMigrated, staleTmp, foreign; // each ≤ 500
    public String trashDirUsed;   // e.g. "_trash/2026-09-14"
    public long   durationMillis;
}
```

Each list holds the first 500 entries (id for row-anchored categories, path
for tree-anchored ones); counts are the true totals.

The `orphanMinAgeMinutes` guard protects a file whose row was committed
between snapshot and walk — its `storage_path` is not yet in the snapshot
map, and without the age guard the walk would trash a valid file. Sixty
minutes (§7 default) is comfortably longer than any single write.

Unit tests: `DmsConsistencyServiceTest` — the seven categories, each with a
one-file fixture; snapshot vs. walk race (row committed after snapshot); an
existing `.tmp` younger than the guard is left alone; checksum backfill
writes the row.

## 5. Changes to existing classes

### 5.1 `DmsService.createFile` — new write path

Both `createFile` overloads at
[DmsService.java:101](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L101)
and
[DmsService.java:127](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L127)
collapse onto one private `writeFile(DmsFile, Consumer<OutputStream>)`
helper; the `InputStream` overload becomes a thin wrapper that delegates
using the same `IOUtils.copy` glue currently at
[:352-360](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L352).

`writeFile` does, in order:

1. `fileRepository.save(dmsFile)` — allocate `file_id`.
2. `dmsFile.setCreatedAt(OffsetDateTime.now())` — the only place `created_at`
   is written by application code.
3. `String rel = DmsStorageLayout.relativePath(createdAt, fileId, fileName);`
   `dmsFile.setStoragePath(rel);` — path is now known before any I/O.
4. `Path target = dmsService.getFilePath(dmsFile);` (§5.4 fallback aware).
   `Path tmp = DmsStorageLayout.tmpPath(target);`
5. `Files.createDirectories(target.getParent());`
6. Stream to `tmp` through a `DigestOutputStream(BufferedOutputStream(FileOutputStream(tmp)), SHA-256)`;
   on `close`, `((FileOutputStream) fos.getFD()).sync()` before returning.
7. `dmsFile.setFileSize(Math.toIntExact(Files.size(tmp)));` — replaces the
   silent `(int) length` cast at
   [:388](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L388);
   an export larger than 2 GiB now throws `ArithmeticException` visibly (see
   §11 Out of scope).
8. `dmsFile.setChecksum(Hex.encodeHexString(digest.digest()));`
9. PDF page count block from
   [:390-395](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L390)
   moves to run on `tmp`, not `outputFile`, so it never touches the final
   name.
10. `Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE);`
11. Register a `TransactionSynchronization.afterCompletion(status)` handler:
    if `STATUS_ROLLED_BACK`, `dmsTrashService.moveToTrash(target, "rollback-create")`.
    This is new — the current code has no rollback compensation for a
    successful stream write (§1.3).
12. `fileRepository.save(dmsFile);` (second save, mirrors current
    behaviour at
    [:113](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L113)).
13. `publishFileChange(dmsFile);`

The `Consumer<OutputStream>` variant hands the digested/buffered stream to
the provider unchanged; the provider still owns whatever writes into it.
The `IOException` catch in the current `saveFile` at
[:381-385](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L381)
collapses to a `try (…)` block: on failure, `tmp` is trashed with reason
`write-failed`, the exception propagates, and no row-side compensation is
needed because the row has not been committed yet.

### 5.2 Attachment content replacement — new row via arr_change tracking

Replacing an `ArrFile` attachment's content is a **reversible operation**
modelled through the existing `arr_change` machinery, mirroring how attachment
deletion has always worked. Replacement never overwrites a `dms_file` in
place: instead, a fresh `dms_file` row is created for the new content and the
old row (with its physical file) stays put. Both are recoverable through
`RevertingChangesService`.

Flow (owned by the attachment controller / arrangement service, not by
`DmsService`):

1. Create a new `arr_change` of type `UPDATE_ATTACHMENT`.
2. Mark the old `ArrFile` with `delete_change_id = newChange` — same call as
   `DmsService.deleteArrFile`
   ([DmsService.java:275](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L275)).
   No file movement, no trash entry.
3. Instantiate a new `ArrFile` with the same `fund`,
   `create_change_id = newChange`, and the new `name` / `fileName` / `mimeType`.
4. Call `dmsService.createFile(newArrFile, inputStream)` — the standard
   create pipeline (§5.1). This allocates a fresh `dms_file.file_id`,
   computes a fresh `storage_path` (with the current time and the extension
   derived from the new `fileName`), and writes the content through the
   tmp + digest + fsync + atomic-move machinery.
5. Old `dms_file` row and physical file: **untouched**. Consistency check
   (§4.4) will match them normally — the row still points to a valid file.

Reversal via
[RevertingChangesService](elza-core/src/main/java/cz/tacr/elza/service/RevertingChangesService.java):
`revert(newChange)` reopens the old `ArrFile` (clears its `delete_change_id`)
and removes the new `ArrFile` (which schedules the new `dms_file` for trash
via §5.3 with reason `revert-attachment`). The two states are exact mirrors.

Consequences:

- **Extension always matches content**: each replace gets a fresh
  `storage_path` derived from the new `fileName` at replace time. The
  extension-freeze note in §2.3 applies only within the lifetime of one row.
- **Full audit trail**: every replacement is a distinct `arr_change`
  attributable to a user, timestamped, revertible.
- **Disk growth**: each replace keeps the old file on disk until an archive
  admin operation (fund deletion, revert-and-purge) frees it. This is the
  intended cost of a reversible design; retention of old physical files
  scales with the history depth of the attachment tree.
- **No `DmsService.updateFile` content-replace path**. The former in-place
  overwrite behaviour (with `update-supersedes` / `rollback-update` trash
  reasons) is removed. The method survives only for metadata-only updates
  (name / mime / display name without a stream); or is deleted entirely if
  no caller keeps that use.

### 5.3 `DmsService.deleteFile` and `deleteFilesAfterCommit`

Current shape:
- `deleteFile(DmsFile)`:
  [:298-305](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L298)
  deletes the row synchronously and defers file removal via
  `deleteFilesAfterCommitByIds`.
- `deleteFilesAfterCommit(List<ArrOutputFile>)`:
  [:312-315](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L312)
  extracts ids and delegates to the same helper.
- `deleteFilesAfterCommitByIds(List<Integer>)`:
  [:322-340](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L322)
  resolves paths by id, registers a synchronization, and calls
  `file.delete()` inside `afterCommit` — silently ignoring failure
  ([:335](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L335)).

New shape:

- Keep `deleteFile(DmsFile)` — same signature, same call sites — but its body
  becomes:
  ```java
  Path path = getFilePath(dmsFile);       // resolve BEFORE the row is deleted
  fileRepository.delete(dmsFile);
  registerAfterCommitTrashMove(path, "delete-" + reasonSuffix(dmsFile));
  publishFileChange(dmsFile);
  ```
  where `reasonSuffix` returns `attachment` / `output` / `plain` based on
  the concrete class.
- Delete `deleteFilesAfterCommitByIds(List<Integer>)`. It resolves paths by
  id **after** the row is gone (currently at
  [:325-327](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L325)),
  which will not work once `storage_path` lives on the row — a deleted row
  cannot answer where its file lived. The id-only entry point is unused
  outside `DmsService` itself; nothing external needs to be updated.
- Replace `deleteFilesAfterCommit(List<ArrOutputFile>)` with a generic
  `deleteFilesAfterCommit(Collection<? extends DmsFile>, String reason)`:
  ```java
  public void deleteFilesAfterCommit(
          Collection<? extends DmsFile> files, String reason) {
      List<Path> paths = files.stream().map(this::getFilePath).toList();
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
              @Override public void afterCommit() {
                  for (Path p : paths) dmsTrashService.moveToTrash(p, reason);
              }
          });
  }
  ```
  Callers pass a reason slug from §4.2. Call sites and their reasons:
  - `DeleteFundAction.dropOutputs()` (§5.7) — `"fund-delete-output"`
  - `DeleteFundHistoryAction` — `"fund-history-delete-output"`
  - `RevertingChangesService` — `"revert-output"`
  - `OutputServiceInternal` (output regeneration) — `"output-regenerated"`
  - `PublicationService.sweepRetention`
    ([PublicationService.java:371](elza-core/src/main/java/cz/tacr/elza/service/PublicationService.java#L371),
    [:424](elza-core/src/main/java/cz/tacr/elza/service/PublicationService.java#L424))
    — `"publication-retention"`
  - `ImpBatchService.cleanupOldFiles` — `"import-retention"`
  - `ImpItemService` — `"import-item-delete"`

  Existing call sites of `deleteFile(DmsFile)` in `PublicationService` at
  [:424](elza-core/src/main/java/cz/tacr/elza/service/PublicationService.java#L424)
  and in `ImpBatchService` / `ImpItemService` stay put — they still hand a
  single `DmsFile` at a time; the reason slug is a compile-time constant in
  each caller.

Neither variant hides an I/O failure any more: `moveToTrash` logs at WARN on
failure and the consistency check (§4.4) picks the file up next run.

### 5.4 `DmsService.getFilePath` — fallback

[DmsService.java:405](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L405)
becomes the only path resolver:

```java
public Path getFilePath(DmsFile file) {
    String rel = file.getStoragePath();
    if (rel != null) {
        return resourcePathResolver.getDmsDir().resolve(rel);
    }
    return resourcePathResolver.getDmsDir()
                              .resolve(String.valueOf(file.getFileId())); // legacy
}
```

`getFilePath(int)` at
[:416](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L416)
is deleted — it cannot honour `storage_path` (no way to look it up without
another repository hit, and every current caller has the entity in hand).
Its two internal callers (both in `DmsService` itself) switch to
`getFilePath(file)`.

The static `newInputStream(ResourcePathResolver, DmsFile)` at
[:236](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L236)
is deleted; the instance overload at
[:263](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L263)
rebuilds directly through `getFilePath`. See §5.6 for its one external
caller.

### 5.5 `ResourcePathResolver.getDmsFile(String)` — removed

[ResourcePathResolver.java:91](elza-core/src/main/java/cz/tacr/elza/core/ResourcePathResolver.java#L91)
is deleted; the resolver keeps `getDmsDir()` at
[:73](elza-core/src/main/java/cz/tacr/elza/core/ResourcePathResolver.java#L73)
as the single root reference. `getDmsFile(String)` cannot honour
`storage_path` — it takes a `String` name, has no row context, and would
have to rebuild the layout by itself.

### 5.6 `AsyncOutputGeneratorWorker`

[AsyncOutputGeneratorWorker.java:222](elza-core/src/main/java/cz/tacr/elza/service/output/AsyncOutputGeneratorWorker.java#L222):

```java
Path dmsFilePath = resourcePathResolver.getDmsFile(String.valueOf(file.getFileId()));
```

becomes:

```java
Path dmsFilePath = dmsService.getFilePath(file);
```

The worker already holds the `DmsFile` (the surrounding validation loop
uses `file` for logging). Inject `DmsService` (Spring will resolve; no
circular dependency — `DmsService` does not use `AsyncOutputGeneratorWorker`).

This is the sole surviving call site of the removed
`ResourcePathResolver.getDmsFile(String)` and of the removed static
`DmsService.newInputStream(ResourcePathResolver, DmsFile)`.

### 5.7 `DeleteFundAction.dropOutputs()`

Current shape at
[DeleteFundAction.java:346](elza-core/src/main/java/cz/tacr/elza/service/arrangement/DeleteFundAction.java#L346):
bulk-deletes `arr_output_file` rows with
`outputFileRepository.deleteByOutputResultOutputFund(fund)` at
[:349](elza-core/src/main/java/cz/tacr/elza/service/arrangement/DeleteFundAction.java#L349),
then the downstream cascade — no on-disk cleanup. Every physical output for
a deleted fund becomes an orphan (§1.7 second bullet).

New shape:

```java
private void dropOutputs() {
    List<ArrOutputFile> outputFiles =
        outputFileRepository.findByOutputResultOutputFund(fund);
    dmsService.deleteFilesAfterCommit(outputFiles, "fund-delete-output");

    outputFileRepository.deleteByOutputResultOutputFund(fund);
    outputResultRepository.deleteByOutputFund(fund);
    itemSettingsRepository.deleteByOutputFund(fund);
    outputItemRepository.deleteByOutputFund(fund);
    nodeOutputRepository.deleteByOutputFund(fund);
    outputTemplateRepository.deleteByFund(fund);
    outputRepository.deleteByFund(fund);

    em.flush();
}
```

The load-then-schedule pattern must run **before** the bulk delete — after
the row is gone the entity is unreachable. `OutputFileRepository` gains a
`findByOutputResultOutputFund` method mirroring the existing
`deleteByOutputResultOutputFund`.

`DeleteFundAction:258` at
[:258](elza-core/src/main/java/cz/tacr/elza/service/arrangement/DeleteFundAction.java#L258)
already delegates to `dmsService.deleteFilesByFund(fund)` for attachments,
which internally calls `deleteFile` per row; no change needed there once
`deleteFile` goes through trash (§5.3).

### 5.8 `StartupService.startNow` — migration wiring

[StartupService.java:223](elza-core/src/main/java/cz/tacr/elza/service/StartupService.java#L223)
runs three ordered stages. The migration slots between stage 2 and stage 3:

```java
tt.executeWithoutResult(r -> startInTransaction());       // :241, unchanged
syncNodeCacheService();                                   // :242
syncApCacheService();                                     // :243

// System security context is available from here on for the migration too.
SecurityContextHolder.setContext(userService.createSecurityContextSystem()); // :246
packageService.autoImportPackages(resourcePathResolver.getDpkgDir());        // :247

//----- stage 2b (new) -----
dmsStorageMigrationService.migrateAll();                  // idempotent, own tx per batch

//----- stage 3 ------
tt.executeWithoutResult(r -> startInTransaction2());      // :250, unchanged
...
asyncRequestService.start();                              // :256, unchanged
```

The migration must precede `asyncRequestService.start()` at
[:256](elza-core/src/main/java/cz/tacr/elza/service/StartupService.java#L256):
async workers can create new `DmsFile` rows, and the migration expects a
consistent starting set of legacy rows. It runs *after* `startInTransaction`
and cache warmups because it uses `DmsFile` entities and the surrounding
JPA infrastructure.

The migration is called from outside any transaction on purpose — it opens
its own per-batch transactions (§4.3). No `@Transactional` on `startNow()`
itself changes.

HTTP is already accepting requests by the time this line runs; reads honour
the fallback in §5.4, and a request racing a single row's rename may retry
next time it opens the file. The consistency check (§4.4) will not see the
inflight rename because `orphanMinAgeMinutes` protects fresh files.

### 5.9 `ScheduledCleanupWorker.scheduledCleanup`

[ScheduledCleanupWorker.java:68](elza-core/src/main/java/cz/tacr/elza/service/ScheduledCleanupWorker.java#L68)
currently has two guarded blocks — `arr_data` cleanup at
[:71-82](elza-core/src/main/java/cz/tacr/elza/service/ScheduledCleanupWorker.java#L71)
and `impBatchService.cleanupOldFiles()` at
[:85-89](elza-core/src/main/java/cz/tacr/elza/service/ScheduledCleanupWorker.java#L85).
A third guarded block is appended:

```java
// trash purge
try {
    dmsTrashService.purgeExpired(trashRetentionDays);
} catch (Exception e) {
    log.error("Error purging DMS trash. ", e);
}
```

Where `trashRetentionDays` is `@Value("${elza.dms.trashRetentionDays:30}")`
(§7). Each guarded block runs independently on the cron — failure of one
does not skip the others.

The consistency check (§4.4) does **not** ride this worker; it has its own
cron `elza.dms.check.cron` (§7) and its own `@Scheduled` on
`DmsConsistencyService` — different cadence (weekly vs. daily) and
different failure semantics (a consistency error must not stop nightly data
cleanup).

## 6. API — TypeSpec contract

### 6.1 Placement

The consistency-check operation lives in the existing `ElzaAPI.Admin`
namespace at
[typespec/main.tsp:2915](elza-development/typespec/main.tsp#L2915)
(the namespace already carries `@route("/admin")` on the preceding line
[:2914](elza-development/typespec/main.tsp#L2914) and hosts sibling
`arrangement/…` housekeeping ops). No new namespace is created; DMS
administration is administration, not a category of its own.

### 6.2 Model

Appended inside the `ElzaAPI.Admin` namespace, next to the existing inline
`model CopyPermissionParams` at
[typespec/main.tsp:2936](elza-development/typespec/main.tsp#L2936):

```tsp
/**
 * Result of one DMS consistency check run. Every count is the true total;
 * each list holds at most the first 500 entries in that category.
 */
model DmsConsistencyReport {
    /** Rows whose file could not be located on disk. */
    missing: DmsConsistencyEntry;

    /** Files on disk that no row claims. */
    orphans: DmsConsistencyEntry;

    /** Rows whose file exists but with a different size. */
    sizeMismatch: DmsConsistencyEntry;

    /** Rows whose file exists but whose SHA-256 does not match `dms_file.checksum`. */
    corrupted: DmsConsistencyEntry;

    /** Rows still on the legacy flat path (`storage_path IS NULL`). */
    notMigrated: DmsConsistencyEntry;

    /** Abandoned `<target>.tmp` files older than `orphanMinAgeMinutes`. */
    staleTmp: DmsConsistencyEntry;

    /** Files under `dms/` that no rule created; reported only, never touched. */
    foreign: DmsConsistencyEntry;

    /** `_trash/<yyyy-MM-dd>` used if `moveOrphansToTrash` was true and moves happened. */
    trashDirUsed?: string;

    /** Wall-clock duration of the check. */
    durationMillis: integer;
}

/** One category of the report: a true total and a truncated sample. */
model DmsConsistencyEntry {
    /** Total number of items in this category. */
    count: integer;

    /**
     * At most the first 500 items in this category. `id` for row-anchored
     * categories (missing, notMigrated), path relative to `dms/` otherwise.
     */
    sample: string[];
}
```

The nested `DmsConsistencyEntry` avoids a flat schema with seven paired
`Count`/`List<String>` fields; consumers get one type to render.

### 6.3 Operation

```tsp
/**
 * Run a DMS consistency check.
 *
 * Compares `dms_file` against the on-disk tree under `${workDir}/dms/`,
 * categorises every discrepancy, and optionally moves orphans and stale
 * temporary files to the trash. Requires the ADMIN permission.
 *
 * @param verifyChecksums re-compute SHA-256 for every checked file; when
 *        false, only size is compared. Also drives the checksum backfill
 *        for legacy rows.
 * @param moveOrphansToTrash actually move orphan / stale-tmp files to
 *        `_trash/<today>/`. When false, the check is read-only and
 *        merely reports.
 */
@route("dms/consistency-check")
@post op dmsConsistencyCheck(
    @query verifyChecksums?: boolean = false,
    @query moveOrphansToTrash?: boolean = false,
): DmsConsistencyReport;
```

`POST` is deliberate: the operation is not idempotent under
`moveOrphansToTrash=true` (each run may move a different set of files, once
their trash retention window opens), and the read-only variant still writes
`checksum` values when `verifyChecksums=true`. No path suffix beyond
`dms/consistency-check` — the report is the whole response.

### 6.4 Regeneration pipeline

The mechanical chain, unchanged from every prior TypeSpec-first change:

1. **Compile.** In `elza-development/typespec/`:
   ```bash
   npm run compile
   ```
   emits the updated OpenAPI YAML.
2. **Copy.** The new operation and model land in
   [elza-core/src/main/resources/rest/elza-openapi.yml](elza-core/src/main/resources/rest/elza-openapi.yml).
   Diff review: exactly one new path
   `/admin/dms/consistency-check`, one new tag section (if any), two new
   `#/components/schemas` entries (`DmsConsistencyReport`,
   `DmsConsistencyEntry`).
3. **Backend build.** `mvn install -Pskiptest` in the root regenerates the
   `AdminApi` interface with `dmsConsistencyCheck(...)` as a new method to
   implement.
4. **Implement in `AdminController`.** Below the existing
   [AdminController.java:55](elza-core/src/main/java/cz/tacr/elza/controller/AdminController.java#L55)
   class body, inject `DmsConsistencyService` and add:

   ```java
   @Autowired
   private DmsConsistencyService dmsConsistencyService;

   @Override
   @AuthMethod(permission = UsrPermission.Permission.ADMIN)
   public ResponseEntity<DmsConsistencyReport> dmsConsistencyCheck(
           Boolean verifyChecksums, Boolean moveOrphansToTrash) {
       boolean vc = Boolean.TRUE.equals(verifyChecksums);
       boolean mt = Boolean.TRUE.equals(moveOrphansToTrash);
       return ResponseEntity.ok(dmsConsistencyService.check(vc, mt));
   }
   ```

   No `@Transactional` on this method: the service opens its own paged
   read-only transactions (§4.4). The `@AuthMethod(permission = ADMIN)` is
   the sole authorization gate — the interceptor already installed on other
   Admin operations covers it (compare
   [AdminController.java:80](elza-core/src/main/java/cz/tacr/elza/controller/AdminController.java#L80)
   for the surrounding annotation style; `copyPermissions` uses
   `@Transactional` because it actually writes user rows, which the
   consistency-check does not).

   `DmsConsistencyReport` in the generated Java model maps 1:1 to the
   service DTO from §4.4; either the controller returns the service's
   report directly (add a Jackson-visible mirror in a lightweight VO if
   the generated type differs on nullability), or a two-line mapper does
   the copy. Prefer the direct return.
5. **Frontend regeneration.** In `elza-react/`:
   ```bash
   mvn exec:exec -Pnpm-install
   ```
   runs the OpenAPI Generator over the updated YAML and refreshes
   `elza-react/src/api/generated/`. Nothing under `generated/` is edited by
   hand (CLAUDE.md). This rework ships **no** React UI; the operation is
   only exposed to administrator-tooling callers of the REST API.

### 6.5 Not in this contract

- No paged variant of the report — 500-item lists are truncated in place
  (§4.4). If the operator needs the full list, they consult the logs
  written by the same run.
- No progress endpoint — the check is expected to finish in seconds on
  installations sized within the design envelope; the report's
  `durationMillis` is the after-the-fact record.
- No cancellation — the check is short-running by design; a cluster of
  overlapping calls is handled by the ADMIN role scoping the caller.

## 7. Configuration

Four new keys under `elza.dms`. All have safe production defaults; none is
required in a fresh install. The block sits below the existing `cleanup`
block in
[elza-web/config/elza.yaml.template:71](elza-web/config/elza.yaml.template#L71)
so an operator reads DMS retention next to the arr_data retention they
already know.

### 7.1 Keys

| Key                                | Type    | Default             | Consumer                                       |
|------------------------------------|---------|---------------------|------------------------------------------------|
| `elza.dms.trashRetentionDays`      | int     | `30`                | `DmsTrashService.purgeExpired` via `ScheduledCleanupWorker` (§5.9) |
| `elza.dms.migrationBatchSize`      | int     | `500`               | `DmsStorageMigrationService.migrateAll` (§4.3) and `DmsConsistencyService.check` snapshot paging (§4.4) |
| `elza.dms.orphanMinAgeMinutes`     | int     | `60`                | `DmsConsistencyService.check` file-age guard (§4.4) |
| `elza.dms.check.cron`              | string  | `0 30 3 ? * SUN`    | `DmsConsistencyService` `@Scheduled` (weekly, Sunday 03:30) |

Injection sites use Spring `@Value` with in-place default, matching
`ScheduledCleanupWorker` at
[ScheduledCleanupWorker.java:35](elza-core/src/main/java/cz/tacr/elza/service/ScheduledCleanupWorker.java#L35)
(`@Value("${elza.cleanup.maxBatch:150000}")`):

```java
@Value("${elza.dms.trashRetentionDays:30}")     private int trashRetentionDays;
@Value("${elza.dms.migrationBatchSize:500}")    private int migrationBatchSize;
@Value("${elza.dms.orphanMinAgeMinutes:60}")    private int orphanMinAgeMinutes;
```

`elza.dms.check.cron` is consumed by a class-level `@Scheduled(cron =
"${elza.dms.check.cron:0 30 3 ? * SUN}")` on `DmsConsistencyService`,
matching the pattern at
[ScheduledCleanupWorker.java:67](elza-core/src/main/java/cz/tacr/elza/service/ScheduledCleanupWorker.java#L67).

The cron uses Spring's 6-field syntax with the `?` placeholder — same form
as the comment example at
[elza-web/config/elza.yaml.template:54](elza-web/config/elza.yaml.template#L54)
(`0 0 4 ? * SAT`). Weekly is deliberate: consistency-check I/O is bounded
by the DMS tree size, and running it against a live installation should
displace one workday's cleanup, not compete with it. An operator wanting
daily reassurance sets `0 30 3 * * *`; wanting off sets the Spring
convention `-`.

### 7.2 Template block

Appended to
[elza-web/config/elza.yaml.template](elza-web/config/elza.yaml.template),
below the existing `cleanup` block at
[:71-73](elza-web/config/elza.yaml.template#L71) and above the
`attachment:` block at
[:75](elza-web/config/elza.yaml.template#L75). All comments in Czech, one
paragraph per key, mirroring the tone of the surrounding template:

```yaml
  # Nastavení úložiště DMS (viz service/dms/DmsService.java, DmsTrashService.java,
  # DmsConsistencyService.java):
  #
  # trashRetentionDays – po kolika dnech se adresář _trash/<datum>/ ve
  #   work/dms/ fyzicky smaže. Do koše se přesouvá každý mazaný obsah
  #   DMS (retence publikací a importů, smazání příloh, přegenerování
  #   výstupu, nálezy kontroly konzistence). V průběhu retenční lhůty
  #   je každé smazání vratné. Výchozí 30 dní.
  #
  # migrationBatchSize – kolik řádků dms_file zpracuje jedna transakce
  #   při migraci ze starého plochého uspořádání na nové (spouští se
  #   automaticky při prvním startu po aktualizaci) a při stránkovaném
  #   snímku databáze v kontrole konzistence. Výchozí 500.
  #
  # orphanMinAgeMinutes – minimální stáří souboru v DMS, aby ho
  #   kontrola konzistence mohla považovat za osiřelý nebo za
  #   zapomenutý *.tmp. Ochrana proti tomu, aby check smazal soubor
  #   právě dokončované operace, jejíž řádek se ještě neobjevil ve
  #   snímku. Výchozí 60 minut.
  #
  # check.cron – kdy se má kontrola konzistence spustit; formát cron
  #   viz sekci reindex výše. Výchozí každou neděli ve 03:30. Pro
  #   vypnutí nastavte hodnotu -.
  #
  #dms:
  #  trashRetentionDays: 30
  #  migrationBatchSize: 500
  #  orphanMinAgeMinutes: 60
  #  check:
  #    cron: 0 30 3 ? * SUN
```

### 7.3 What the block deliberately omits

- **No `enabled` toggle for migration.** Migration is part of the upgrade,
  driven off `storage_path IS NULL`; a config switch would only mask a
  broken deployment (§4.3 note "no configuration switch").
- **No `enabled` toggle for the consistency check.** Setting `check.cron`
  to `-` disables the scheduled run; the manual endpoint (§6) stays
  available to an ADMIN caller.
- **No `dmsRoot` override.** The root stays `${elza.workingDir}/dms`; a
  separate DMS location would need a full move (§10 risks — atomic move
  requires the same filesystem for the trash directory too).
- **No `checksumAlgorithm`.** SHA-256 is hard-coded (§4.1); a change would
  invalidate every stored `checksum` value.

## 8. Tests

### 8.1 New tests

Six test classes, all under `elza-core/src/test/java/cz/tacr/elza/`.

#### `service/DmsStorageLayoutTest`

Pure unit test — no Spring, no filesystem. One `@Test` per rule:

| Test method                                         | Verifies                                          |
|-----------------------------------------------------|---------------------------------------------------|
| `sanitizedExtension_examples()`                     | every row of the §2.3 example table               |
| `relativePath_monthBoundary()`                      | `2025-12-31T23:59:59Z` vs `2026-01-01T00:00:01Z` in the server zone lands in `2025/12/…` vs `2026/01/…` |
| `relativePath_blockBoundary()`                      | `51999` → `.../000051/51999`, `52000` → `.../000052/52000` |
| `relativePath_sameBlock()`                          | `51230` and `51231` land in the same block dir    |
| `isLegacyName_distinguishesFromNewName()`           | `"48213"` yes, `"48213.pdf"` no, `"_trash"` no    |
| `tmpPath_sameDirectory()`                           | tmp lives in `target.getParent()` (`ATOMIC_MOVE` requirement) |
| `trashDir_dateFormat()`                             | `LocalDate.of(2026,9,13)` → `"_trash/2026-09-13"` |

#### `service/DmsTrashServiceTest`

Uses `@TempDir` for the DMS root; no Spring context needed beyond a stub
`ResourcePathResolver` returning that temp path.

| Test method                                     | Verifies                                                          |
|-------------------------------------------------|-------------------------------------------------------------------|
| `moveToTrash_happy()`                           | file appears under `_trash/<today>/` with original leaf name      |
| `moveToTrash_collisionAppendsHHmmss()`          | second move of same name gains `-HHmmss` suffix                   |
| `moveToTrash_missingSource_returnsNull()`       | absent source is not an error; return is `null`; no `report.txt` line |
| `moveToTrash_reportLineFormat()`                | `<leaf>;<size>;<mtime-instant>;<reason>` — regex match            |
| `purgeExpired_dropsOldDays()`                   | with a fixed clock, day `today-31` is deleted, `today-29` kept    |
| `purgeExpired_toleratesNonDateDir()`            | a directory named `garbage` is skipped and logged                 |

#### `service/DmsServiceStorageTest`

Spring context (uses the existing test harness that wires JPA + H2). Backed
by `@TempDir` for the DMS root, injected through a test override of
`ResourcePathResolver`.

Each test is one transaction driven by `TransactionTemplate` from the test
code — so commit / rollback are explicit, not implicit from method return.

| Test method                                       | Verifies                                                                        |
|---------------------------------------------------|---------------------------------------------------------------------------------|
| `create_commit_writesFileAndRow()`                | after commit, target exists at `yyyy/MM/000nnn/<id>.<ext>`; `storage_path`, `checksum`, `file_size`, `created_at` are populated |
| `create_rollback_movesNewFileToTrash()`           | after rollback, target absent; a file with same content sits under `_trash/<today>/` with reason `rollback-create` |
| `delete_movesToTrash_notFilesystemDelete()`       | after commit of `deleteFile`, target absent; trash entry present; `dms_file` row absent |
| `delete_missingFile_isNotAnError()`               | `deleteFile` on a row whose file was already gone still commits; no exception; consistency check would later report the row as `missing` |
| `update_rollback_restoresOriginalContent()`       | pre-existing content is preserved on rollback (moved back from trash), new content is trashed as `rollback-create` |
| `read_legacyRow_usesFlatFallback()`               | a row with `storage_path IS NULL` and a file at `dms/<id>` reads correctly via `getFilePath` |
| `create_pdfPagesCountedOnTmp()`                   | for a PDF payload, `pagesCount` matches PDFBox's count of the tmp file         |
| `create_fileSizeOverflow_throwsArithmetic()`      | when payload > `Integer.MAX_VALUE`, `Math.toIntExact` throws (§11 out of scope) |

The rollback tests exercise `TransactionSynchronization.afterCompletion(STATUS_ROLLED_BACK)`
— which does not fire under `@Transactional(propagation = NEVER)` — so the
harness must open a real transaction and abort it.

#### `service/DmsStorageMigrationServiceTest`

Spring context; the temp `dms/` root is seeded manually with a mix of files
before each test. `DmsFile` rows are inserted via repository fixtures.

| Test method                                       | Verifies                                                                        |
|---------------------------------------------------|---------------------------------------------------------------------------------|
| `migrate_happyRow_movesAndSetsPath()`             | flat `dms/17` with a matching row moves to `2019/03/000000/17.pdf`; `storage_path` set |
| `migrate_missingSource_marksMissing()`            | row exists, file gone → `storage_path` populated anyway; `_migration-*.txt` lists id |
| `migrate_recovered_pathAlreadyThere()`            | file already at target from a prior aborted run; `storage_path` catches up; state RECOVERED |
| `migrate_duplicate_bothPresent()`                 | source and target both exist; target kept, source trashed with reason `duplicate` |
| `migrate_rootYearCollision_movesToMigrating()`    | flat `dms/2026` collides with year dir; moved to `_migrating/2026` before pass  |
| `migrate_rootScan_legacyOrphanTrashed()`          | numeric leaf without a row is trashed with reason `legacy-orphan`               |
| `migrate_rootScan_unexpectedTrashed()`            | non-numeric leaf that is not `README.txt` / `_trash` / `yyyy/` is trashed with reason `unexpected` |
| `migrate_secondRun_isNoOp()`                      | after a full first run, second call moves nothing and writes no `_migration-*.txt` |

#### `service/DmsConsistencyServiceTest`

Spring context; seeds the tree and the database directly. One `@Test` per
category from §4.4 plus the race guard.

| Test method                                       | Verifies                                                                        |
|---------------------------------------------------|---------------------------------------------------------------------------------|
| `check_reportsMissing()`                          | a row with a well-formed `storage_path` but no file → `missingCount`, `missing[0]` = id |
| `check_reportsOrphans_andTrashesWhenAsked()`      | with `moveOrphansToTrash=true`, an untracked file lands in `_trash/`; without it, only reported |
| `check_reportsSizeMismatch()`                     | mismatched `file_size` counted                                                  |
| `check_reportsCorrupted_onlyIfVerify()`           | tampered content flagged only when `verifyChecksums=true`                       |
| `check_reportsNotMigrated()`                      | a legacy row (`storage_path IS NULL`) counted                                   |
| `check_reportsStaleTmp_respectsAge()`             | `.tmp` file younger than `orphanMinAgeMinutes` is left alone; older is flagged  |
| `check_reportsForeign_neverTouches()`             | an operator-authored file at `dms/notes.txt` is reported as `foreign`, never trashed |
| `check_backfillsChecksumForLegacy()`              | with `verifyChecksums=true`, a legacy row without `checksum` receives one after the check |
| `check_snapshotWalkRace_isProtectedByMinAge()`    | a valid row committed after the snapshot with a young file is not trashed as `orphans` |

#### `controller/AdminControllerDmsConsistencyTest`

Wraps the endpoint with the existing controller test harness (same style as
`PublicationControllerTest`). One happy-path call, one 403 for
non-ADMIN, one payload assertion mapping the service report to the JSON
shape.

| Test method                                       | Verifies                                                                        |
|---------------------------------------------------|---------------------------------------------------------------------------------|
| `consistencyCheck_asAdmin_returnsReport()`        | 200 with expected top-level fields                                              |
| `consistencyCheck_asNonAdmin_forbidden()`         | 403 via `@AuthMethod(permission = ADMIN)`                                       |
| `consistencyCheck_flagsPropagate()`               | `verifyChecksums=true&moveOrphansToTrash=true` reach the service call          |

### 8.2 Adjustments to existing tests

`HelperTestService.deleteTables` at
[HelperTestService.java:303](elza-core/src/test/java/cz/tacr/elza/other/HelperTestService.java#L303)
becomes the single place that also cleans the DMS tree between tests.
Append to the end of the method body (after `deleteTablesInternal()` at
[:308](elza-core/src/test/java/cz/tacr/elza/other/HelperTestService.java#L308)):

```java
Path dmsRoot = resourcePathResolver.getDmsDir();
if (Files.exists(dmsRoot)) {
    FileUtils.cleanDirectory(dmsRoot.toFile());
}
```

Ad-hoc cleaners in individual controller tests are then removed:

- [OutputGenerationTest.java:49](elza-core/src/test/java/cz/tacr/elza/controller/OutputGenerationTest.java#L49)
  — the `cleanDmsDirectory()` call plus the private method at
  [:55-56](elza-core/src/test/java/cz/tacr/elza/controller/OutputGenerationTest.java#L55)
  are deleted; the `resourcePathResolver` field at
  [:42](elza-core/src/test/java/cz/tacr/elza/controller/OutputGenerationTest.java#L42)
  becomes unused and is deleted too.
- [PublicationControllerTest.java:347-349](elza-core/src/test/java/cz/tacr/elza/controller/PublicationControllerTest.java#L347)
  — the `FileUtils.cleanDirectory(dmsDir)` block is deleted; the
  surrounding tearDown/setUp keeps whatever else it does.
- `PublicationPublicApiTest` (same package) has the same ad-hoc cleaner —
  delete it in parallel; the pattern is a copy.

Retention assertions that currently check for a file's **absence** switch
to check for its presence in `_trash/<today>/`:

- Publication retention tests (`PublicationControllerTest`, sweep-retention
  scenarios) — after sweep, the file must be at
  `dms/_trash/<yyyy-MM-dd>/<id>.<ext>` with a corresponding line in
  `report.txt` whose reason is `publication-retention`. The trash location
  is stable across runs because tests use `HelperTestService`'s cleaner,
  which runs before each test — not after.
- Import retention tests (in the ImpBatch/ImpItem test suites) — same
  pattern with reason `import-retention` (or `import-item-delete`).

The `hasDownloadableFile` assertions at
[PublicationControllerTest.java:173,256,282,294](elza-core/src/test/java/cz/tacr/elza/controller/PublicationControllerTest.java#L173)
are unaffected — they check the row's business state, not the file's
on-disk presence.

## 9. Documentation

Three touchpoints. Content is Czech; language of surrounding text is
authoritative (`README.md` has Czech sections, `readme.txt` is Czech
reStructuredText, the classpath resource ships in Czech).

### 9.1 `README.md` — Pracovní adresář

Existing section header at
[README.md:133](README.md#L133). The section currently states only that
`elza.workingDir` sets the location. Append two paragraphs after the
current YAML example at
[:143](README.md#L143):

```markdown
Pod pracovním adresářem vzniká podadresář `dms/`, do kterého Elza ukládá
všechny binární soubory: přílohy archivních souborů, vygenerované výstupy,
publikační exporty a zdrojové soubory importních dávek. Struktura je
`dms/<rok>/<měsíc>/<blok>/<id>.<přípona>` a nesmí se ručně upravovat —
autoritativní cestu drží sloupec `dms_file.storage_path` a jakákoli změna
mimo aplikaci se projeví buď jako osiřelý soubor, nebo jako chybějící obsah
u existujícího řádku. Adresář `dms/_trash/<datum>/` obsahuje soubory
čekající na fyzické smazání (viz `elza.dms.trashRetentionDays`); dokud
jsou v koši, je smazání vratné.

Databáze a `${elza.workingDir}/dms` tvoří jeden celek a musí se zálohovat i
obnovovat společně. Doporučený postup je zálohovat nejdřív databázi
(`pg_dump`), pak `dms/`, nebo obojí se zastavenou aplikací. Před každou
aktualizací pořiďte zálohu obojího z jednoho okamžiku — návrat na starší
verzi po proběhlé migraci úložiště (§Migrace v [`docs/dms-storage.md`](docs/dms-storage.md))
vyžaduje obnovu obou z doby *před* aktualizací; starší verze by soubory
uspořádané v novém stromě nenašla.
```

Adds no headings, no code fences beyond the existing one; keeps the
section digestible.

### 9.2 `distrib/distribution/src/assembly/readme.txt`

The installer's operator-facing README. reStructuredText, Czech, current
`work/` reference at
[readme.txt:40](distrib/distribution/src/assembly/readme.txt#L40). Insert
a new sub-block below the working-directory description:

```rst
- work/dms – úložiště všech binárních souborů spravovaných Elzou
  (přílohy, výstupy, publikace, zdroje importů). Adresář má vlastní
  vnitřní strukturu popsanou v souboru work/dms/README.txt, který Elza
  sama založí a udržuje. Ručně tento adresář neupravujte.

Zálohování a obnova
-------------------

Databáze a adresář work/dms tvoří jeden celek a musí se zálohovat
i obnovovat společně. Před každou aktualizací pořiďte zálohu obojího
z jednoho okamžiku; návrat na starší verzi po proběhlé migraci
úložiště vyžaduje obnovu obou.
```

Section-title underline (`===` / `---`) must match the neighboring
convention; check the actual file at the insertion point before writing —
reST fails silently on mismatched lengths.

### 9.3 `dms/README.txt` — classpath resource

New file, shipped from
[elza-core/src/main/resources/dms/README.txt](elza-core/src/main/resources/dms/README.txt),
overwritten under `${elza.workingDir}/dms/README.txt` on every startup by
`DmsStorageMigrationService.migrateAll` (§4.3 step 1). About fifteen
Czech lines; edited copies do not survive an upgrade — that is the whole
reason it is a classpath resource.

```text
Úložiště DMS aplikace Elza
==========================

Do tohoto adresáře ukládá Elza všechny binární soubory, které
spravuje: přílohy archivních souborů (arr_file), vygenerované
výstupy (arr_output_file), publikační exporty a zdrojové soubory
importních dávek.

Struktura je pevná:

  dms/<rok>/<měsíc>/<blok>/<id>.<přípona>

kde <id> je hodnota sloupce dms_file.file_id. Autoritativní cestu
drží sloupec dms_file.storage_path; přípona v názvu je jen vodítko
pro obsluhu. Soubory ručně nepřejmenovávejte, nepřesouvejte ani
nemažte — Elza je najde jen tam, kam si je sama uložila.

Zvláštní jména:

  _trash/<datum>/    soubory čekající na fyzické smazání spolu
                     s report.txt (název;velikost;čas;důvod).
                     Automaticky se mažou po uplynutí retence
                     (elza.dms.trashRetentionDays, výchozí 30 dní).
                     V průběhu retenční lhůty je smazání vratné.
  _migration-*.txt   protokoly migrace ze staré ploché struktury;
                     ponechejte je pro případnou analýzu.
  *.tmp              rozpracovaný zápis; osamocené *.tmp starší než
                     elza.dms.orphanMinAgeMinutes uklidí kontrola
                     konzistence (výchozí 60 minut).

Adresář dms/ a databáze Elzy tvoří jeden zálohovací celek a musí
se zálohovat a obnovovat společně.
```

Sixteen useful lines plus the title underline; every rule an operator
needs to make sense of the tree, and no reference to Java internals that
would rot.

### 9.4 What is deliberately not documented user-facing

- **`_migrating/` directory.** An implementation detail of the collision
  pre-pass (§4.3 step 2); appears only when a legacy flat file's numeric
  name matched a year directory. Documenting it would explain a case
  most installations never see.
- **The admin consistency-check endpoint (§6).** Not exposed in the UI
  (§6.4 step 5); its consumers are administrators and integrators who
  read the OpenAPI spec directly. Adding it to the user README would
  imply a user-facing feature that does not exist.
- **The `checksum` column.** Not visible in any operator workflow —
  useful only to restore-verification tooling that would run against the
  database directly.

## 10. Implementation order and deployment risks

### 10.1 Order of work

Target release line: 3.4.x. Steps below can each be reviewed and merged as
one PR; each step compiles, passes tests, and leaves the tree in a shippable
state. Later steps assume earlier ones.

1. **Database and entity.** §3.1-§3.5. New changeset in
   `db.elza-3-part-03.xml`; `DmsFile` gains `createdAt` / `storagePath` /
   `checksum`. Repository additions if any (`countByStoragePathIsNull`,
   `findWithoutStoragePath(Pageable)`, `findByOutputResultOutputFund` on
   `OutputFileRepository`). No behaviour change — all writes still go
   through the current code paths.

2. **`DmsStorageLayout` and `DmsTrashService`.** §4.1-§4.2. Pure new
   classes plus their unit tests; not yet wired to `DmsService`.

3. **`DmsService` rewrite.** §5.1-§5.5. `createFile` → new `writeFile`
   pipeline (tmp + digest + fsync + atomic move + rollback trash). `updateFile`
   via trash with rollback restore. `deleteFile` /
   `deleteFilesAfterCommit` via trash with checked outcome. Remove
   `getFilePath(int)`, `deleteFilesAfterCommitByIds`, static
   `newInputStream`. `getFilePath(DmsFile)` gains the legacy fallback.
   Delete `ResourcePathResolver.getDmsFile(String)`. This is the moment
   `storage_path` starts being written on every new row.

4. **Orphan-source fixes.** §5.6, §5.7. `AsyncOutputGeneratorWorker`
   switches to `dmsService.getFilePath(file)`. `DeleteFundAction.dropOutputs()`
   loads output files and schedules trash moves before the bulk row
   delete.

5. **Migration.** §4.3, §5.8. `DmsStorageMigrationService.migrateAll`
   with the row pass, root-collision pre-pass, root scan, and
   `_migration-*.txt` protocol. Wire into `StartupService.startNow`
   between stage 2 and stage 3. First startup after this step is when the
   flat tree becomes the new tree.

6. **Consistency check.** §4.4, §6. `DmsConsistencyService` with the
   snapshot / walk / reconcile / checksum-backfill algorithm. TypeSpec
   operation, generated OpenAPI, `AdminController` implementation,
   regenerated TS client. Class-level `@Scheduled` for the weekly run.

7. **Trash purge on cron.** §5.9. Third guarded block in
   `ScheduledCleanupWorker.scheduledCleanup` calling
   `dmsTrashService.purgeExpired(trashRetentionDays)`. Independent from
   the other blocks — its failure does not skip them and vice versa.

8. **Configuration and documentation.** §7, §9. Template block in
   `elza.yaml.template`; `README.md` and installer `readme.txt` additions;
   the classpath `dms/README.txt` resource. Documentation lands with
   configuration so an operator upgrading reads everything at once.

9. **Test adjustments.** §8.2. Move DMS cleanup into
   `HelperTestService.deleteTables`; delete ad-hoc cleaners in
   `OutputGenerationTest`, `PublicationControllerTest`,
   `PublicationPublicApiTest`; update retention assertions to expect the
   file in trash rather than absent. Landing this last keeps the existing
   suite green through each earlier step (steps 1-7 each add or update
   their own tests as they go — this step retires the leftovers).

Steps 1-7 each grow the test suite (§8.1); step 9 shrinks it where
duplication has become obvious.

### 10.2 Deployment risks

| Topic                              | Note                                                                                                                                                                                                                    |
|------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Backup before upgrade              | Back up both the database and `work/dms` before applying the release; migration only renames files, so disk usage does not grow, but the DB and DMS become a tighter pair from this release on.                          |
| Downgrade                          | Rolling back to a pre-migration release means restoring **both** backups from before the upgrade — the older version does not know about `storage_path` or the new tree shape.                                            |
| Same filesystem                    | `Files.move(ATOMIC_MOVE)` requires source and target on one filesystem. New directories are created under `dms/`, so the constraint fails only if a subdirectory of `dms/` is a symlink elsewhere — such a row is reported as FAILED and left for the next startup. |
| MSSQL type coverage                | `timestamp with time zone` is already in use on `usr_api_key` / `arr_change` / `arr_export`. `storage_path` has no unique index (§3.1 — MSSQL treats every `NULL` as duplicate under a plain unique index).               |
| Windows                            | Antivirus or an in-flight download can block a rename; the row stays with `storage_path IS NULL` and retries on next startup. The remainder is picked up by the consistency check.                                        |
| Requests during startup            | The migration runs before `asyncRequestService.start()` but after HTTP is accepting requests. Reads honour the fallback in §5.4; a download racing a single row's rename may fail once and succeed on retry.              |
| Consistency check memory           | Snapshot map is roughly 30 bytes per row; one million rows is about 100 MB. Sized comfortably for the largest expected installation.                                                                                     |
| Trash disk usage                   | Trash holds every deletion for `trashRetentionDays` (default 30). Large installations that churn a lot of files (heavy publication or import volume) should tune the retention down or plan the extra headroom.          |
| Shared publication files           | `PublicationService.copy` binds one `dms_file` to several `arr_export` rows (a publication copy shares its source file). Nothing changes; `deleteFileIfUnreferenced` still gates the physical delete on the last reference. |
| `file_name` change                 | The stored file is not renamed when a user edits `file_name`; the extension in `storage_path` was fixed at creation. This is intentional — see §2.3.                                                                    |

## 11. Out of scope

Landed only if a follow-up ticket picks them up; each is called out where it
touches this rework so a reviewer knows why it is not fixed here.

- **`dms_file.file_size` → `bigint`.** Exports larger than 2 GiB overflow
  the `int` column. This rework only turns the silent
  `(int) outputFile.length()` cast at
  [DmsService.java:388](elza-core/src/main/java/cz/tacr/elza/service/dms/DmsService.java#L388)
  into a loud `Math.toIntExact` throw (§5.1 step 7). Widening the column
  touches the entity, every VO with a size field, and the REST clients;
  it belongs to its own ticket.

- **`DeleteFundAction` and `arr_export`.** `DeleteFundAction` does not
  drop `arr_export` rows (they carry `fund_version_id` with a FK to
  `arr_fund_version`), so an archival fund with associated publications
  cannot be deleted at all — the FK aborts the delete. Not caused by this
  rework and not addressed here; the trash-based delete path in §5.3 is a
  no-op for a delete that never runs.

- **`GET /api/dms/{fileId}` permission check.** The endpoint calls the
  fund-read permission check with `fundId == null` for plain `DmsFile`
  rows (publication exports, import sources), and fails. This is a
  pre-existing bug independent of the storage layout; the rework touches
  neither the endpoint nor the permission model. A separate ticket should
  either give plain `DmsFile` rows their own permission gate or route
  their downloads through a dedicated endpoint.
