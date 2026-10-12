package cz.tacr.elza.dbchangelog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import cz.tacr.elza.service.SpringContext;
import cz.tacr.elza.service.dms.DmsStorageLayout;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;

/**
 * Migration of the DMS storage from the legacy flat layout ({@code dms/<file_id>})
 * to {@code dms/<yyyy>/<MM>/<block>/<id>.<ext>} (#10028).
 *
 * Replaces the migration of changeset 20260918120000 (3.4.9), which parked every
 * legacy file with a four-digit id (1000-9999) in {@code _migrating/} as if it
 * collided with a year directory, never took it from there and marked the row
 * MISSING. The migration is idempotent and repairs that state too:
 * <ol>
 * <li>Stage: every legacy file at the root ({@code dms/<digits>}) is moved to
 * {@code _migrating/<digits>}. Afterwards no legacy file can collide with a year
 * directory and every legacy file is at one place.</li>
 * <li>Row pass over all rows of dms_file: when the file is not at its
 * storage_path (or at the path computed from created_at for a row without one),
 * it is moved there from {@code _migrating/}.</li>
 * <li>Files left in {@code _migrating/} belong to no row. They stay there for
 * the operator; nothing is deleted.</li>
 * </ol>
 * Row states (MISSING, FAILED, ORPHAN, ...) land in
 * {@code dms/_migration-<timestamp>.txt} for operator review.
 *
 * At Liquibase phase JPA is not yet initialized; the DMS root is read directly
 * from {@link Environment}.
 */
public class DbChangeset20261012120000 extends BaseTaskChange {

    private static final Logger log = LoggerFactory.getLogger(DbChangeset20261012120000.class);

    private static final DateTimeFormatter LOG_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String WORKING_DIR_PROPERTY = "elza.workingDir";
    private static final String DMS_SUBDIR = "dms";

    @Override
    public void execute(Database database) throws CustomChangeException {
        try {
            Connection conn = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();
            migrate(conn, resolveDmsRoot());
        } catch (Exception e) {
            throw new CustomChangeException(e);
        }
    }

    private static Path resolveDmsRoot() {
        Environment env = SpringContext.getBean(Environment.class);
        String workDir = env.getProperty(WORKING_DIR_PROPERTY);
        if (workDir == null || workDir.isBlank()) {
            throw new IllegalStateException("Configuration property '" + WORKING_DIR_PROPERTY + "' is not set");
        }
        return Paths.get(workDir, DMS_SUBDIR);
    }

    static Counters migrate(Connection conn, Path dmsRoot) throws IOException, SQLException {
        Path migratingDir = dmsRoot.resolve(DmsStorageLayout.MIGRATING_DIR);
        Files.createDirectories(dmsRoot);
        Files.createDirectories(dmsRoot.resolve(DmsStorageLayout.TRASH_DIR));
        Files.createDirectories(migratingDir);
        copyReadme(dmsRoot);

        List<String> problems = new ArrayList<>();
        Counters c = new Counters();

        stage(dmsRoot, migratingDir, problems, c);
        rowPass(conn, dmsRoot, migratingDir, problems, c);
        reportOrphans(migratingDir, problems, c);

        if (!problems.isEmpty()) {
            String name = DmsStorageLayout.MIGRATION_LOG_PREFIX + LOG_STAMP.format(LocalDateTime.now()) + ".txt";
            Files.write(dmsRoot.resolve(name), problems);
        }

        log.info("DMS migration: staged={}, migrated={}, inPlace={}, missing={}, failed={}, orphans={}",
                c.staged, c.migrated, c.inPlace, c.missing, c.failed, c.orphans);
        if (c.failed > 0) {
            log.warn("DMS migration left {} FAILED rows, see {}*.txt in {}.",
                    c.failed, DmsStorageLayout.MIGRATION_LOG_PREFIX, dmsRoot);
        }
        return c;
    }

    private static void copyReadme(Path dmsRoot) throws IOException {
        try (InputStream in = DbChangeset20261012120000.class.getResourceAsStream(DmsStorageLayout.README_CLASSPATH)) {
            if (in == null) {
                log.warn("Classpath resource {} not found; skipping README.txt refresh.",
                        DmsStorageLayout.README_CLASSPATH);
                return;
            }
            Files.copy(in, dmsRoot.resolve(DmsStorageLayout.README_FILE), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Moves every legacy file ({@code dms/<digits>}, a regular file) to
     * {@code _migrating/}. Directories at the root (year directories) are not
     * touched. A name already present in {@code _migrating/} is left at the
     * root and reported; the row pass takes the copy from {@code _migrating/}.
     */
    private static void stage(Path dmsRoot, Path migratingDir, List<String> problems, Counters c) throws IOException {
        List<Path> legacy = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dmsRoot)) {
            stream.filter(p -> Files.isRegularFile(p)
                    && DmsStorageLayout.isLegacyName(p.getFileName().toString()))
                    .forEach(legacy::add);
        }
        for (Path source : legacy) {
            String name = source.getFileName().toString();
            Path parked = migratingDir.resolve(name);
            if (Files.exists(parked)) {
                problems.add(name + "\tSTAGE_CONFLICT\t" + DmsStorageLayout.MIGRATING_DIR + "/" + name
                        + " already exists, " + name + " left in place");
                continue;
            }
            Files.move(source, parked, StandardCopyOption.ATOMIC_MOVE);
            c.staged++;
        }
    }

    private static void rowPass(Connection conn, Path dmsRoot, Path migratingDir, List<String> problems, Counters c)
            throws SQLException {
        List<Row> rows = readRows(conn);
        try (PreparedStatement upd = conn.prepareStatement(
                "UPDATE dms_file SET storage_path = ? WHERE file_id = ?")) {
            for (Row row : rows) {
                try {
                    migrateRow(row, dmsRoot, migratingDir, upd, problems, c);
                } catch (Exception e) {
                    log.warn("Migration FAILED for fileId {}: {}", row.fileId, e.toString());
                    c.failed++;
                    problems.add(row.fileId + "\tFAILED\t" + e);
                }
            }
        }
    }

    private static void migrateRow(Row row, Path dmsRoot, Path migratingDir, PreparedStatement upd,
                                   List<String> problems, Counters c) throws IOException, SQLException {
        String rel = row.storagePath != null
                ? row.storagePath
                : DmsStorageLayout.relativePath(row.createdAt, row.fileId, row.fileName);
        Path target = dmsRoot.resolve(rel);
        String name = String.valueOf(row.fileId);
        Path parked = migratingDir.resolve(name);
        Path flat = dmsRoot.resolve(name);

        if (Files.exists(target)) {
            if (!Files.isRegularFile(target)) {
                throw new IOException("Target is not a regular file: " + target);
            }
            c.inPlace++;
            if (Files.isRegularFile(parked)) {
                // file already at its place, the legacy copy stays in _migrating/ as an orphan
                problems.add(row.fileId + "\tDUPLICATE\t" + rel + " exists, legacy copy kept in "
                        + DmsStorageLayout.MIGRATING_DIR + "/" + name);
            }
        } else {
            Path source = Files.isRegularFile(parked) ? parked
                    : Files.isRegularFile(flat) ? flat
                    : null;
            if (source != null) {
                Files.createDirectories(target.getParent());
                Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
                c.migrated++;
            } else {
                c.missing++;
                problems.add(row.fileId + "\tMISSING\t" + rel);
            }
        }
        if (!Objects.equals(rel, row.storagePath)) {
            upd.setString(1, rel);
            upd.setInt(2, row.fileId);
            upd.executeUpdate();
        }
    }

    private static List<Row> readRows(Connection conn) throws SQLException {
        List<Row> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT file_id, file_name, created_at, storage_path FROM dms_file ORDER BY file_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Row r = new Row();
                r.fileId = rs.getInt("file_id");
                r.fileName = rs.getString("file_name");
                r.createdAt = rs.getObject("created_at", OffsetDateTime.class);
                r.storagePath = rs.getString("storage_path");
                rows.add(r);
            }
        }
        return rows;
    }

    /**
     * Files remaining in {@code _migrating/} after the row pass have no row
     * (or a row whose file was already in place). They are only reported.
     */
    private static void reportOrphans(Path migratingDir, List<String> problems, Counters c) throws IOException {
        try (Stream<Path> stream = Files.list(migratingDir)) {
            for (Path entry : (Iterable<Path>) stream::iterator) {
                c.orphans++;
                problems.add(entry.getFileName() + "\tORPHAN\t" + DmsStorageLayout.MIGRATING_DIR + "/"
                        + entry.getFileName());
            }
        }
    }

    static final class Counters {
        int staged;
        int migrated;
        int inPlace;
        int missing;
        int failed;
        int orphans;
    }

    private static final class Row {
        int fileId;
        String fileName;
        OffsetDateTime createdAt;
        String storagePath;
    }
}
