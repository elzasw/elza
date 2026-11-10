package cz.tacr.elza.service.da;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cz.tacr.elza.api.DigitalRepositoryType;
import cz.tacr.elza.common.io.SpooledContent;
import cz.tacr.elza.connector.DaConnector;
import cz.tacr.elza.domain.ArrDigitalRepository;

/**
 * A batch downloaded over File Transfer arrives as a directory tree, one directory per package,
 * as the DA communication standard prescribes; the processing of a batch reads the zip the
 * standard download returns, so the tree is packed into one with the same layout.
 */
public class DaServiceFileTransferDownloadTest {

    @TempDir
    Path tempDir;

    private DaService service;
    private DaConnector daConnector;
    private ArrDigitalRepository repository;

    @BeforeEach
    void setUp() {
        daConnector = mock(DaConnector.class);
        service = new DaService();
        setField(service, "daConnector", daConnector);
        repository = new ArrDigitalRepository();
        repository.setExternalSystemId(3);
        repository.setDigitalRepositoryType(DigitalRepositoryType.DA);
    }

    @Test
    void deliveredDirectoryTreeIsPackedWithEntriesRelativeToTheDownloadDirectory() throws IOException {
        delivering(dir -> {
            write(dir.resolve("329740/PACKAGE-INFO.xml"), "<pi/>");
            write(dir.resolve("404822/PACKAGE-INFO.xml"), "<pi/>");
            write(dir.resolve("404822/representations/rep1/data/a.txt"), "a");
        });

        try (SpooledContent content = service.downloadFileTransfer(repository, "batch-1")) {
            List<String> entries = entriesOf(content);
            assertTrue(entries.contains("329740/PACKAGE-INFO.xml"), entries.toString());
            assertTrue(entries.contains("404822/PACKAGE-INFO.xml"), entries.toString());
            assertTrue(entries.contains("404822/representations/rep1/data/a.txt"), entries.toString());
        }
    }

    @Test
    void aSingleDeliveredZipIsUsedAsItIs() throws IOException {
        delivering(dir -> {
            try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(dir.resolve("batch-1.zip")))) {
                zip.putNextEntry(new ZipEntry("329740/PACKAGE-INFO.xml"));
                zip.write("<pi/>".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        });

        try (SpooledContent content = service.downloadFileTransfer(repository, "batch-1")) {
            assertEquals(List.of("329740/PACKAGE-INFO.xml"), entriesOf(content));
        }
    }

    /**
     * A batch of one package whose content is one zip: the directory names the AIP, so the zip
     * is a file of the tree, not the batch.
     */
    @Test
    void aSingleZipInsideAPackageDirectoryIsPartOfTheTree() throws IOException {
        delivering(dir -> {
            try (ZipOutputStream zip = new ZipOutputStream(
                    Files.newOutputStream(write(dir.resolve("310415/dbdf00d0-85c4.zip"))))) {
                zip.putNextEntry(new ZipEntry("dbdf00d0-85c4/METS.xml"));
                zip.write("<mets/>".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        });

        try (SpooledContent content = service.downloadFileTransfer(repository, "batch-1")) {
            assertEquals(List.of("310415/dbdf00d0-85c4.zip"), entriesOf(content));
        }
    }

    @Test
    void nothingDeliveredIsAFailureOfTheDownload() {
        delivering(dir -> { });

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.downloadFileTransfer(repository, "batch-1"));
        assertTrue(e.getMessage().contains("File Transfer"), e.getMessage());
    }

    /** The download directory is temporary; it must not outlive the download, delivered or not. */
    @Test
    void downloadDirectoryIsRemovedAfterAFailedTransfer() throws IOException {
        List<Path> downloadDirs = new ArrayList<>();
        doAnswer(inv -> {
            downloadDirs.add(inv.getArgument(2, Path.class));
            throw new IllegalStateException("Přenos dávky batch-1 přes File Transfer selhal");
        }).when(daConnector).downloadFileTransfer(any(), eq("batch-1"), any());

        assertThrows(IllegalStateException.class, () -> service.downloadFileTransfer(repository, "batch-1"));

        assertEquals(1, downloadDirs.size());
        assertFalse(Files.exists(downloadDirs.get(0)));
    }

    private interface Delivery {
        void deliver(Path downloadDir) throws IOException;
    }

    private void delivering(Delivery delivery) {
        doAnswer(inv -> {
            delivery.deliver(inv.getArgument(2, Path.class));
            return null;
        }).when(daConnector).downloadFileTransfer(any(), eq("batch-1"), any());
    }

    private static void write(Path file, String content) throws IOException {
        Files.writeString(write(file), content);
    }

    /** @return the file, with its directory created */
    private static Path write(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        return file;
    }

    private static List<String> entriesOf(SpooledContent content) throws IOException {
        List<String> names = new ArrayList<>();
        try (InputStream in = content.openStream(); ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    names.add(entry.getName());
                }
            }
        }
        return names;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
