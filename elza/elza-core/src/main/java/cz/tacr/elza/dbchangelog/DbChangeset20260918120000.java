package cz.tacr.elza.dbchangelog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import cz.tacr.elza.service.dms.DmsStorageLayout;
import cz.tacr.elza.service.SpringContext;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;

/**
 * One-off migration from the legacy flat DMS layout ({@code dms/<file_id>})
 * to {@code dms/<yyyy>/<MM>/<block>/<id>.<ext>}. Runs once via Liquibase,
 * paired in the same changeset with the schema additions
 * (created_at / storage_path / checksum). Row/file states — MIGRATED,
 * RECOVERED, MISSING, DUPLICATE, FAILED — are logged; anomalies also land
 * in {@code dms/_migration-<timestamp>.txt} for operator review.
 *
 * At Liquibase phase JPA is not yet initialized, so this changeset cannot
 * touch beans that transitively depend on {@code EntityManagerFactory}
 * (that includes {@code ResourcePathResolver} and {@code DmsTrashService}).
 * The DMS root is read directly from {@link Environment} and trash-move is
 * inlined below.
 */
public class DbChangeset20260918120000 extends BaseTaskChange {

    private static final Logger log = LoggerFactory.getLogger(DbChangeset20260918120000.class);

    private static final Pattern YEAR_DIR = Pattern.compile("^\\d{4}$");
    private static final DateTimeFormatter LOG_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter TRASH_COLLISION_SUFFIX = DateTimeFormatter.ofPattern("HHmmss");
    private static final String WORKING_DIR_PROPERTY = "elza.workingDir";
    private static final String DMS_SUBDIR = "dms";
    private static final String TRASH_REPORT_FILE = "report.txt";

    @Override
    public void execute(Database database) throws CustomChangeException {
        try {
            Path dmsRoot = resolveDmsRoot();

            prepareRoot(dmsRoot);

            List<String> problems = new ArrayList<>();
            Counters c = new Counters();

            Connection conn = ((JdbcConnection) database.getConnection()).getUnderlyingConnection();

            rootCollisionPrePass(dmsRoot, problems);
            rowPass(conn, dmsRoot, problems, c);
            rootScan(dmsRoot, c);

            if (!problems.isEmpty()) {
                writeReportFile(dmsRoot, problems);
            }

            log.info("DMS migration: migrated={}, recovered={}, missing={}, duplicate={}, failed={}, legacyOrphans={}, unexpected={}",
                    c.migrated, c.recovered, c.missing, c.duplicate, c.failed, c.legacyOrphans, c.unexpected);
            if (c.failed > 0) {
                log.warn("DMS migration left {} FAILED rows.", c.failed);
            }
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

    private static void prepareRoot(Path dmsRoot) throws IOException {
        Files.createDirectories(dmsRoot);
        Files.createDirectories(dmsRoot.resolve(DmsStorageLayout.TRASH_DIR));
        Files.createDirectories(dmsRoot.resolve(DmsStorageLayout.MIGRATING_DIR));
        copyReadme(dmsRoot);
    }

    private static void copyReadme(Path dmsRoot) throws IOException {
        try (InputStream in = DbChangeset20260918120000.class.getResourceAsStream(DmsStorageLayout.README_CLASSPATH)) {
            if (in == null) {
                log.warn("Classpath resource {} not found; skipping README.txt refresh.",
                        DmsStorageLayout.README_CLASSPATH);
                return;
            }
            Files.copy(in, dmsRoot.resolve(DmsStorageLayout.README_FILE), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Legacy flat file whose name looks like a year (e.g. {@code dms/2026})
     * would collide with a year directory we are about to create. Park such
     * entries in {@code _migrating/} before the row pass runs.
     */
    private static void rootCollisionPrePass(Path dmsRoot, List<String> problems) throws IOException {
        try (Stream<Path> stream = Files.list(dmsRoot)) {
            for (Path entry : (Iterable<Path>) stream::iterator) {
                String name = entry.getFileName().toString();
                if (Files.isRegularFile(entry) && YEAR_DIR.matcher(name).matches()) {
                    Path parked = dmsRoot.resolve(DmsStorageLayout.MIGRATING_DIR).resolve(name);
                    try {
                        Files.move(entry, parked, StandardCopyOption.ATOMIC_MOVE);
                        log.info("Parked legacy flat file {} in _migrating/ to avoid year-dir collision.", name);
                    } catch (IOException e) {
                        log.warn("Failed to park root entry {}: {}", name, e.toString());
                        problems.add(name + "\tPARK_FAILED\t" + e.getMessage());
                    }
                }
            }
        }
    }

    private static void rowPass(Connection conn, Path dmsRoot, List<String> problems, Counters c) throws SQLException {
        List<PendingRow> pending = readPending(conn);
        try (PreparedStatement upd = conn.prepareStatement(
                "UPDATE dms_file SET storage_path = ? WHERE file_id = ?")) {
            for (PendingRow row : pending) {
                String rel = DmsStorageLayout.relativePath(row.createdAt, row.fileId, row.fileName);
                Path source = dmsRoot.resolve(String.valueOf(row.fileId));
                Path target = dmsRoot.resolve(rel);
                boolean sExists = Files.exists(source);
                boolean tExists = Files.exists(target);
                try {
                    if (sExists && !tExists) {
                        Files.createDirectories(target.getParent());
                        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
                        setStoragePath(upd, row.fileId, rel);
                        c.migrated++;
                    } else if (!sExists && tExists) {
                        setStoragePath(upd, row.fileId, rel);
                        c.recovered++;
                    } else if (!sExists && !tExists) {
                        setStoragePath(upd, row.fileId, rel);
                        c.missing++;
                        problems.add(row.fileId + "\tMISSING\t" + rel);
                    } else {
                        // both exist — keep target, trash source
                        moveToTrash(dmsRoot, source, "duplicate");
                        setStoragePath(upd, row.fileId, rel);
                        c.duplicate++;
                        problems.add(row.fileId + "\tDUPLICATE\t" + rel);
                    }
                } catch (Exception e) {
                    log.warn("Migration FAILED for fileId {}: {}", row.fileId, e.toString());
                    c.failed++;
                    problems.add(row.fileId + "\tFAILED\t" + e.getMessage());
                }
            }
        }
    }

    private static List<PendingRow> readPending(Connection conn) throws SQLException {
        List<PendingRow> rows = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT file_id, file_name, created_at FROM dms_file WHERE storage_path IS NULL ORDER BY file_id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                PendingRow r = new PendingRow();
                r.fileId = rs.getInt("file_id");
                r.fileName = rs.getString("file_name");
                r.createdAt = rs.getObject("created_at", OffsetDateTime.class);
                rows.add(r);
            }
        }
        return rows;
    }

    private static void setStoragePath(PreparedStatement upd, int fileId, String path) throws SQLException {
        upd.setString(1, path);
        upd.setInt(2, fileId);
        upd.executeUpdate();
    }

    /**
     * After the row pass drains, walk the root at depth 1 and trash anything
     * that shouldn't be there (leftover flat legacy files, operator garbage).
     */
    private static void rootScan(Path dmsRoot, Counters c) throws IOException {
        try (Stream<Path> stream = Files.list(dmsRoot)) {
            for (Path entry : (Iterable<Path>) stream::iterator) {
                String name = entry.getFileName().toString();
                if (isKnownRootName(name)) {
                    continue;
                }
                if (Files.isRegularFile(entry) && DmsStorageLayout.isLegacyName(name)) {
                    moveToTrash(dmsRoot, entry, "legacy-orphan");
                    c.legacyOrphans++;
                } else if (Files.isRegularFile(entry)) {
                    moveToTrash(dmsRoot, entry, "unexpected");
                    c.unexpected++;
                }
                // unknown directories at root left alone — operator's territory
            }
        }
    }

    private static boolean isKnownRootName(String name) {
        return name.equals(DmsStorageLayout.README_FILE)
                || name.equals(DmsStorageLayout.TRASH_DIR)
                || name.equals(DmsStorageLayout.MIGRATING_DIR)
                || (name.startsWith(DmsStorageLayout.MIGRATION_LOG_PREFIX) && name.endsWith(".txt"))
                || YEAR_DIR.matcher(name).matches();
    }

    /**
     * Inlined counterpart of {@code DmsTrashService.moveToTrash}: the service
     * bean cannot be resolved at Liquibase phase because it transitively pulls
     * in JPA. Semantics stay identical: move {@code source} to
     * {@code _trash/<today>/} under its own leaf name (collision suffix
     * {@code -HHmmss}) and append one line to {@code report.txt}.
     */
    private static void moveToTrash(Path dmsRoot, Path source, String reason) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        Path trashDir = dmsRoot.resolve(DmsStorageLayout.TRASH_DIR).resolve(LocalDate.now().toString());
        Files.createDirectories(trashDir);
        String leaf = source.getFileName().toString();
        Path target = trashDir.resolve(leaf);
        if (Files.exists(target)) {
            target = trashDir.resolve(leaf + "-" + TRASH_COLLISION_SUFFIX.format(LocalTime.now()));
        }
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        String line = String.format("%s;%d;%s;%s%n",
                target.getFileName(),
                Files.size(target),
                Files.getLastModifiedTime(target).toInstant(),
                reason);
        Files.writeString(trashDir.resolve(TRASH_REPORT_FILE), line, StandardCharsets.UTF_8,
                StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    }

    private static void writeReportFile(Path dmsRoot, List<String> problems) throws IOException {
        String name = DmsStorageLayout.MIGRATION_LOG_PREFIX + LOG_STAMP.format(LocalDateTime.now()) + ".txt";
        Files.write(dmsRoot.resolve(name), problems);
    }

    private static final class Counters {
        int migrated;
        int recovered;
        int missing;
        int duplicate;
        int failed;
        int legacyOrphans;
        int unexpected;
    }

    private static final class PendingRow {
        int fileId;
        String fileName;
        OffsetDateTime createdAt;
    }
}
