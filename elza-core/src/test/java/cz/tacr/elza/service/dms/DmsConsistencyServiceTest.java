package cz.tacr.elza.service.dms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.domain.DmsFile;

public class DmsConsistencyServiceTest extends AbstractTest {

    @Autowired
    private DmsConsistencyService consistencyService;

    @Test
    void check_reportsMissing() throws IOException {
        DmsFile file = createDms("m.txt", "text/plain", "x".getBytes());
        Files.delete(dmsRoot().resolve(file.getStoragePath()));

        DmsConsistencyReport report = consistencyService.check(false, false);

        // Package loading in AbstractTest.setUp creates other dms_file rows whose
        // files never land on disk, so we can only assert on our specific id.
        assertTrue(report.missing.sample.contains(String.valueOf(file.getFileId())),
                "our missing file id is in the sample");
    }

    @Test
    void check_reportsOrphans_andTrashesWhenAsked() throws IOException {
        Path orphan = dmsRoot().resolve("2020/01/000000/99999.txt");
        Files.createDirectories(orphan.getParent());
        Files.write(orphan, "orphan".getBytes());
        ageFile(orphan, 120);

        DmsConsistencyReport readonly = consistencyService.check(false, false);
        assertEquals(1, readonly.orphans.count);
        assertTrue(Files.exists(orphan), "read-only run does not touch");

        DmsConsistencyReport moving = consistencyService.check(false, true);
        assertEquals(1, moving.orphans.count);
        assertFalse(Files.exists(orphan), "trashed when moveOrphansToTrash=true");
        assertTrue(Files.exists(trashDayDir().resolve("99999.txt")));
    }

    @Test
    void check_reportsSizeMismatch() {
        DmsFile file = createDms("size.txt", "text/plain", "hello".getBytes());

        txTemplate().executeWithoutResult(status -> {
            DmsFile loaded = em.find(DmsFile.class, file.getFileId());
            loaded.setFileSize(9999);
            em.flush();
        });

        DmsConsistencyReport report = consistencyService.check(false, false);
        assertEquals(1, report.sizeMismatch.count);
    }

    @Test
    void check_reportsCorrupted_onlyIfVerify() throws IOException {
        DmsFile file = createDms("crpt.txt", "text/plain", "aaaaa".getBytes());
        Path target = dmsRoot().resolve(file.getStoragePath());
        Files.write(target, "bbbbb".getBytes()); // same size, different bytes

        assertEquals(0, consistencyService.check(false, false).corrupted.count,
                "size matches, checksum not verified");
        assertEquals(1, consistencyService.check(true, false).corrupted.count,
                "sha-256 differs, corrupted");
    }

    @Test
    void check_reportsNotMigrated() throws IOException {
        DmsFile legacy = seedLegacyRow("legacy.txt", "leg".getBytes());
        Path legacyPath = dmsRoot().resolve(String.valueOf(legacy.getFileId()));
        Files.write(legacyPath, "leg".getBytes());
        ageFile(legacyPath, 120);

        DmsConsistencyReport report = consistencyService.check(false, false);
        // Package loading may leave other legacy rows too; assert only ours.
        assertTrue(report.notMigrated.sample.contains(String.valueOf(legacy.getFileId())),
                "our legacy row is in notMigrated sample");
    }

    @Test
    void check_reportsStaleTmp_respectsAge() throws IOException {
        Path stale = dmsRoot().resolve("2020/01/000000/1.txt.tmp");
        Files.createDirectories(stale.getParent());
        Files.write(stale, "old".getBytes());
        ageFile(stale, 120);

        Path young = dmsRoot().resolve("2020/01/000000/2.txt.tmp");
        Files.write(young, "new".getBytes()); // mtime = now → below 60-min guard

        DmsConsistencyReport report = consistencyService.check(false, false);
        assertEquals(1, report.staleTmp.count, "young .tmp protected by age guard");
    }

    @Test
    void check_reportsForeign_neverTouches() throws IOException {
        Path foreign = dmsRoot().resolve("notes.txt");
        Files.createDirectories(dmsRoot());
        Files.write(foreign, "operator notes".getBytes());

        DmsConsistencyReport report = consistencyService.check(false, true);
        assertEquals(1, report.foreign.count);
        assertTrue(Files.exists(foreign), "foreign never touched");
    }

    @Test
    void check_backfillsChecksumForLegacy() throws IOException {
        byte[] content = "abc".getBytes();
        DmsFile legacy = seedLegacyRow("cs.txt", content);
        Path legacyPath = dmsRoot().resolve(String.valueOf(legacy.getFileId()));
        Files.write(legacyPath, content);
        ageFile(legacyPath, 120);

        consistencyService.check(true, false);

        DmsFile reloaded = txTemplate().execute(status -> dmsService.getFile(legacy.getFileId()));
        assertNotNull(reloaded.getChecksum(), "checksum backfilled");
        assertEquals(64, reloaded.getChecksum().length(), "sha-256 hex length");
    }

    @Test
    void check_snapshotWalkRace_isProtectedByMinAge() throws IOException {
        Path youngOrphan = dmsRoot().resolve("2020/01/000000/77777.txt");
        Files.createDirectories(youngOrphan.getParent());
        Files.write(youngOrphan, "fresh".getBytes());
        // mtime = now → under 60-min guard

        DmsConsistencyReport report = consistencyService.check(false, true);
        assertEquals(0, report.orphans.count, "young file protected by age guard");
        assertTrue(Files.exists(youngOrphan));
    }

    // ---------- helpers ----------

    private TransactionTemplate txTemplate() {
        return new TransactionTemplate(txManager);
    }

    private Path dmsRoot() {
        return resourcePathResolver.getDmsDir();
    }

    private Path trashDayDir() {
        return dmsRoot().resolve(DmsStorageLayout.TRASH_DIR).resolve(LocalDate.now().toString());
    }

    private DmsFile createDms(String name, String mime, byte[] content) {
        DmsFile f = new DmsFile();
        f.setName(name);
        f.setFileName(name);
        f.setMimeType(mime);
        txTemplate().executeWithoutResult(status -> {
            try {
                dmsService.createFile(f, new ByteArrayInputStream(content));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return f;
    }

    private DmsFile seedLegacyRow(String name, byte[] content) {
        DmsFile f = new DmsFile();
        f.setName(name);
        f.setFileName(name);
        f.setMimeType("text/plain");
        f.setFileSize(content.length);
        f.setCreatedAt(OffsetDateTime.now());
        // storagePath deliberately NOT set → legacy row
        txTemplate().executeWithoutResult(status -> {
            em.persist(f);
            em.flush();
        });
        return f;
    }

    private static void ageFile(Path p, int minutesOld) throws IOException {
        Files.setLastModifiedTime(p, FileTime.from(Instant.now().minus(minutesOld, ChronoUnit.MINUTES)));
    }
}
