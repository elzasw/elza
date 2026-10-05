package cz.tacr.elza.service.dms;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.core.ResourcePathResolver;
import cz.tacr.elza.domain.DmsFile;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.FileRepository;

/**
 * Compares the on-disk {@code dms/} tree against {@code dms_file} and
 * categorises every discrepancy. Optionally moves orphans and stale
 * temporary files into {@link DmsTrashService the trash}.
 */
@Service
public class DmsConsistencyService {

    private static final Logger logger = LoggerFactory.getLogger(DmsConsistencyService.class);

    private static final Pattern YEAR_DIR = Pattern.compile("^\\d{4}$");
    private static final Pattern MONTH_DIR = Pattern.compile("^\\d{2}$");
    private static final Pattern BLOCK_DIR = Pattern.compile("^\\d{6}$");

    @Autowired
    private ResourcePathResolver resourcePathResolver;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private DmsTrashService dmsTrashService;

    @Autowired
    private PlatformTransactionManager txManager;

    private static final int SNAPSHOT_BATCH_SIZE = 500;

    @Value("${elza.dms.orphanMinAgeMinutes:60}")
    private int orphanMinAgeMinutes;

    /** Scheduled weekly run with full verification and trash moves. */
    @Scheduled(cron = "${elza.dms.check.cron:0 30 3 ? * SUN}")
    public void scheduledCheck() {
        try {
            check(true, true);
        } catch (Exception e) {
            logger.error("Scheduled DMS consistency check failed.", e);
        }
    }

    public DmsConsistencyReport check(boolean verifyChecksums, boolean moveOrphansToTrash) {
        long start = System.currentTimeMillis();
        Path dmsRoot = resourcePathResolver.getDmsDir();
        Snapshot snapshot = takeSnapshot();
        DmsConsistencyReport report = new DmsConsistencyReport();

        // notMigrated is snapshot state, populated before the walk mutates the map
        for (Integer legacyId : snapshot.byLegacyId.keySet()) {
            report.notMigrated.add(String.valueOf(legacyId));
        }

        if (Files.exists(dmsRoot)) {
            walkTree(dmsRoot, snapshot, report, verifyChecksums, moveOrphansToTrash);
        }
        reconcileMissing(snapshot, report);

        if (moveOrphansToTrash) {
            report.trashDirUsed = DmsStorageLayout.TRASH_DIR + "/" + LocalDate.now();
        }
        report.durationMillis = System.currentTimeMillis() - start;
        logger.info("DMS consistency check: missing={}, orphans={}, sizeMismatch={}, corrupted={}, notMigrated={}, staleTmp={}, foreign={}, {}ms",
                report.missing.count, report.orphans.count, report.sizeMismatch.count,
                report.corrupted.count, report.notMigrated.count, report.staleTmp.count,
                report.foreign.count, report.durationMillis);
        return report;
    }

    private Snapshot takeSnapshot() {
        Snapshot snap = new Snapshot();
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.setReadOnly(true);
        int page = 0;
        while (true) {
            final int p = page;
            List<DmsFile> rows = tt.execute(status ->
                    fileRepository.findAll(PageRequest.of(p, SNAPSHOT_BATCH_SIZE)).getContent());
            if (rows == null || rows.isEmpty()) {
                break;
            }
            for (DmsFile row : rows) {
                Row r = new Row(row.getFileId(), row.getStoragePath(), row.getFileSize(), row.getChecksum());
                if (r.storagePath != null) {
                    snap.byPath.put(r.storagePath, r);
                } else {
                    snap.byLegacyId.put(r.fileId, r);
                }
            }
            if (rows.size() < SNAPSHOT_BATCH_SIZE) {
                break;
            }
            page++;
        }
        return snap;
    }

    private void walkTree(Path dmsRoot, Snapshot snap, DmsConsistencyReport report,
                          boolean verifyChecksums, boolean moveOrphansToTrash) {
        try {
            Files.walkFileTree(dmsRoot, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(dmsRoot)) {
                        return FileVisitResult.CONTINUE;
                    }
                    String name = dir.getFileName().toString();
                    if (dir.getParent().equals(dmsRoot)
                            && (name.equals(DmsStorageLayout.TRASH_DIR)
                                    || name.equals(DmsStorageLayout.MIGRATING_DIR))) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    visit(dmsRoot, file, attrs, snap, report, verifyChecksums, moveOrphansToTrash);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    logger.warn("Cannot visit {}: {}", file, exc.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new SystemException("Failed to walk DMS root " + dmsRoot, e);
        }
    }

    private void visit(Path dmsRoot, Path file, BasicFileAttributes attrs, Snapshot snap,
                       DmsConsistencyReport report, boolean verifyChecksums, boolean moveOrphansToTrash) throws IOException {
        String leaf = file.getFileName().toString();
        Path rel = dmsRoot.relativize(file);
        String relStr = rel.toString().replace(File.separatorChar, '/');

        // Root-level well-known names
        if (rel.getNameCount() == 1) {
            if (leaf.equals(DmsStorageLayout.README_FILE)) {
                return;
            }
            if (leaf.startsWith(DmsStorageLayout.MIGRATION_LOG_PREFIX) && leaf.endsWith(".txt")) {
                return;
            }
        }

        // .tmp anywhere — age-guarded trash
        if (leaf.endsWith(DmsStorageLayout.TMP_SUFFIX)) {
            if (isTooYoung(attrs)) {
                return;
            }
            report.staleTmp.add(relStr);
            if (moveOrphansToTrash) {
                dmsTrashService.moveToTrash(file, "consistency-stale-tmp");
            }
            return;
        }

        // Match by new-layout storagePath
        Row byPath = snap.byPath.remove(relStr);
        if (byPath != null) {
            checkRow(file, byPath, report, verifyChecksums);
            return;
        }

        // Match legacy (root-level digits-only leaf)
        if (rel.getNameCount() == 1 && DmsStorageLayout.isLegacyName(leaf)) {
            int id = Integer.parseInt(leaf);
            Row legacy = snap.byLegacyId.remove(id);
            if (legacy != null) {
                checkRow(file, legacy, report, verifyChecksums);
                return;
            }
            if (isTooYoung(attrs)) {
                return;
            }
            report.orphans.add(relStr);
            if (moveOrphansToTrash) {
                dmsTrashService.moveToTrash(file, "consistency-orphan");
            }
            return;
        }

        // Looks-like-new-layout leaf (yyyy/MM/block/*) but no matching row → orphan
        if (looksLikeLayoutPath(rel)) {
            if (isTooYoung(attrs)) {
                return;
            }
            report.orphans.add(relStr);
            if (moveOrphansToTrash) {
                dmsTrashService.moveToTrash(file, "consistency-orphan");
            }
            return;
        }

        // Everything else — foreign, never touch
        report.foreign.add(relStr);
    }

    private void checkRow(Path file, Row row, DmsConsistencyReport report, boolean verifyChecksums) throws IOException {
        long actualSize = Files.size(file);
        if (row.fileSize == null || row.fileSize.longValue() != actualSize) {
            report.sizeMismatch.add(String.valueOf(row.fileId));
            return;
        }
        if (!verifyChecksums) {
            return;
        }
        String actualHex = sha256Hex(file);
        if (row.checksum == null) {
            backfillChecksum(row.fileId, actualHex);
        } else if (!row.checksum.equalsIgnoreCase(actualHex)) {
            report.corrupted.add(String.valueOf(row.fileId));
        }
    }

    private void backfillChecksum(int fileId, String checksum) {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        tt.executeWithoutResult(status -> fileRepository.findById(fileId).ifPresent(entity -> {
            if (entity.getChecksum() == null) {
                entity.setChecksum(checksum);
                fileRepository.save(entity);
            }
        }));
    }

    private void reconcileMissing(Snapshot snap, DmsConsistencyReport report) {
        for (Row r : snap.byPath.values()) {
            report.missing.add(String.valueOf(r.fileId));
        }
        for (Row r : snap.byLegacyId.values()) {
            report.missing.add(String.valueOf(r.fileId));
        }
    }

    private boolean isTooYoung(BasicFileAttributes attrs) {
        Instant mtime = attrs.lastModifiedTime().toInstant();
        Instant threshold = Instant.now().minus(orphanMinAgeMinutes, ChronoUnit.MINUTES);
        return mtime.isAfter(threshold);
    }

    private static boolean looksLikeLayoutPath(Path rel) {
        if (rel.getNameCount() != 4) {
            return false;
        }
        return YEAR_DIR.matcher(rel.getName(0).toString()).matches()
                && MONTH_DIR.matcher(rel.getName(1).toString()).matches()
                && BLOCK_DIR.matcher(rel.getName(2).toString()).matches();
    }

    private static String sha256Hex(Path file) throws IOException {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new SystemException("SHA-256 not available", e);
        }
        try (InputStream in = Files.newInputStream(file);
             DigestInputStream dis = new DigestInputStream(in, digest)) {
            byte[] buf = new byte[8192];
            while (dis.read(buf) != -1) {
                // consume
            }
        }
        return Hex.encodeHexString(digest.digest());
    }

    private static final class Snapshot {
        final Map<String, Row> byPath = new HashMap<>();
        final Map<Integer, Row> byLegacyId = new HashMap<>();
    }

    private static final class Row {
        final int fileId;
        final String storagePath;
        final Integer fileSize;
        final String checksum;

        Row(int fileId, String storagePath, Integer fileSize, String checksum) {
            this.fileId = fileId;
            this.storagePath = storagePath;
            this.fileSize = fileSize;
            this.checksum = checksum;
        }
    }
}
