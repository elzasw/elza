package cz.tacr.elza.service.dms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.apache.commons.codec.binary.Hex;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import cz.tacr.elza.AbstractTest;
import cz.tacr.elza.domain.DmsFile;

public class DmsServiceStorageTest extends AbstractTest {

    @Test
    void create_commit_writesFileAndRow() throws IOException {
        DmsFile file = plainDms("hello.txt", "text/plain");
        byte[] content = "hello world".getBytes(StandardCharsets.UTF_8);

        txTemplate().executeWithoutResult(status -> writeFile(file, content));

        assertNotNull(file.getStoragePath(), "storagePath populated");
        assertNotNull(file.getCreatedAt(), "createdAt populated");
        assertEquals(content.length, file.getFileSize().longValue());
        assertEquals(sha256Hex(content), file.getChecksum());
        assertTrue(file.getStoragePath().matches("\\d{4}/\\d{2}/\\d{6}/\\d+\\.txt"),
                "storagePath shape: " + file.getStoragePath());

        Path target = dmsRoot().resolve(file.getStoragePath());
        assertTrue(Files.exists(target), "file exists on disk");
        assertEquals(content.length, Files.size(target));

        DmsFile loaded = dmsService.getFile(file.getFileId());
        assertEquals(file.getStoragePath(), loaded.getStoragePath());
    }

    @Test
    void create_rollback_movesNewFileToTrash() {
        DmsFile file = plainDms("rollback.txt", "text/plain");
        byte[] content = "will be rolled back".getBytes(StandardCharsets.UTF_8);

        assertThrows(RuntimeException.class, () ->
                txTemplate().executeWithoutResult(status -> {
                    writeFile(file, content);
                    throw new RuntimeException("force rollback");
                }));

        // storagePath was set by writeFile even though tx rolled back;
        // we use its leaf to locate the trashed copy
        String leaf = Path.of(file.getStoragePath()).getFileName().toString();
        assertFalse(Files.exists(dmsRoot().resolve(file.getStoragePath())),
                "target absent after rollback");
        assertTrue(Files.exists(trashDayDir().resolve(leaf)),
                "trashed copy present with reason rollback-create");
    }

    @Test
    void delete_movesToTrash() throws IOException {
        DmsFile file = plainDms("bye.txt", "text/plain");
        txTemplate().executeWithoutResult(status ->
                writeFile(file, "bye".getBytes(StandardCharsets.UTF_8)));

        Path target = dmsRoot().resolve(file.getStoragePath());
        String leaf = target.getFileName().toString();
        int fileId = file.getFileId();

        txTemplate().executeWithoutResult(status ->
                dmsService.deleteFile(dmsService.getFile(fileId)));

        assertFalse(Files.exists(target), "target removed from live tree");
        assertTrue(Files.exists(trashDayDir().resolve(leaf)), "file present in trash");
    }

    @Test
    void delete_missingFile_isNotAnError() throws IOException {
        DmsFile file = plainDms("ghost.txt", "text/plain");
        txTemplate().executeWithoutResult(status ->
                writeFile(file, "gone".getBytes(StandardCharsets.UTF_8)));

        Path target = dmsRoot().resolve(file.getStoragePath());
        Files.delete(target); // physically remove behind the service's back
        int fileId = file.getFileId();

        // must not throw
        txTemplate().executeWithoutResult(status ->
                dmsService.deleteFile(dmsService.getFile(fileId)));

        // row gone; missing file logged by dmsTrashService but no exception
    }

    @Test
    void read_legacyRow_usesFlatFallback() throws IOException {
        DmsFile legacy = plainDms("legacy.txt", "text/plain");
        legacy.setFileSize(4);
        legacy.setCreatedAt(OffsetDateTime.now());
        // storagePath deliberately NOT set

        txTemplate().executeWithoutResult(status -> {
            em.persist(legacy);
            em.flush();
        });

        Path legacyPath = dmsRoot().resolve(String.valueOf(legacy.getFileId()));
        Files.createDirectories(legacyPath.getParent());
        Files.write(legacyPath, "leg1".getBytes(StandardCharsets.UTF_8));

        Path resolved = dmsService.getFilePath(legacy);
        assertEquals(legacyPath, resolved,
                "getFilePath falls back to dms/<id> when storagePath is NULL");
    }

    @Test
    void create_pdfPagesCounted() throws IOException {
        DmsFile file = plainDms("doc.pdf", DmsService.MIME_TYPE_APPLICATION_PDF);
        byte[] pdf = threePagePdf();

        txTemplate().executeWithoutResult(status -> writeFile(file, pdf));

        assertEquals(Integer.valueOf(3), file.getPagesCount());
    }

    // --------- helpers ---------

    private TransactionTemplate txTemplate() {
        return new TransactionTemplate(txManager);
    }

    private Path dmsRoot() {
        return resourcePathResolver.getDmsDir();
    }

    private Path trashDayDir() {
        return dmsRoot()
                .resolve(DmsStorageLayout.TRASH_DIR)
                .resolve(LocalDate.now().toString());
    }

    private static DmsFile plainDms(String fileName, String mimeType) {
        DmsFile f = new DmsFile();
        f.setName(fileName);
        f.setFileName(fileName);
        f.setMimeType(mimeType);
        return f;
    }

    private void writeFile(DmsFile file, byte[] content) {
        try {
            dmsService.createFile(file, new ByteArrayInputStream(content));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String sha256Hex(byte[] content) {
        MessageDigest d;
        try {
            d = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return Hex.encodeHexString(d.digest(content));
    }

    private static byte[] threePagePdf() throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            doc.addPage(new PDPage());
            doc.addPage(new PDPage());
            doc.save(out);
            return out.toByteArray();
        }
    }
}
