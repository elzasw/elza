package cz.tacr.elza.service.dms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Pure-logic tests for {@link DmsStorageLayout}. The server-zone field is
 * pinned to UTC for the whole class so time-zone-dependent expectations are
 * reproducible.
 */
public class DmsStorageLayoutTest {

    private static ZoneId originalZone;

    @BeforeAll
    static void pinZone() {
        originalZone = DmsStorageLayout.SERVER_ZONE;
        DmsStorageLayout.SERVER_ZONE = ZoneOffset.UTC;
    }

    @AfterAll
    static void restoreZone() {
        DmsStorageLayout.SERVER_ZONE = originalZone;
    }

    @Test
    void sanitizedExtension_examples() {
        assertEquals("pdf", DmsStorageLayout.sanitizedExtension("report.PDF"));
        assertEquals("gz", DmsStorageLayout.sanitizedExtension("scan.tar.gz"));
        assertEquals("json5", DmsStorageLayout.sanitizedExtension("data.json5"));
        assertEquals("name", DmsStorageLayout.sanitizedExtension("weird.name"));
        assertEquals("", DmsStorageLayout.sanitizedExtension("no-dot-here"));
        assertEquals("hidden", DmsStorageLayout.sanitizedExtension(".hidden"));
        assertEquals("", DmsStorageLayout.sanitizedExtension("bad.<script>"));
        assertEquals("", DmsStorageLayout.sanitizedExtension("long.abcdefghijk"));
        assertEquals("", DmsStorageLayout.sanitizedExtension("trailing."));
        assertEquals("", DmsStorageLayout.sanitizedExtension(null));
    }

    @Test
    void relativePath_monthBoundary() {
        OffsetDateTime dec = OffsetDateTime.parse("2025-12-31T23:59:59Z");
        OffsetDateTime jan = OffsetDateTime.parse("2026-01-01T00:00:01Z");
        assertTrue(DmsStorageLayout.relativePath(dec, 1, "a.pdf").startsWith("2025/12/"));
        assertTrue(DmsStorageLayout.relativePath(jan, 1, "a.pdf").startsWith("2026/01/"));
    }

    @Test
    void relativePath_blockBoundary() {
        OffsetDateTime at = OffsetDateTime.parse("2026-09-14T12:00:00Z");
        assertEquals("2026/09/000051/51999.pdf",
                DmsStorageLayout.relativePath(at, 51999, "x.pdf"));
        assertEquals("2026/09/000052/52000.pdf",
                DmsStorageLayout.relativePath(at, 52000, "x.pdf"));
    }

    @Test
    void relativePath_sameBlock() {
        OffsetDateTime at = OffsetDateTime.parse("2026-09-14T12:00:00Z");
        String a = DmsStorageLayout.relativePath(at, 51230, "x.pdf");
        String b = DmsStorageLayout.relativePath(at, 51231, "x.pdf");
        assertEquals("2026/09/000051/51230.pdf", a);
        assertEquals("2026/09/000051/51231.pdf", b);
    }

    @Test
    void relativePath_noExtensionWhenSanitizerRejects() {
        OffsetDateTime at = OffsetDateTime.parse("2026-09-14T12:00:00Z");
        assertEquals("2026/09/000000/17",
                DmsStorageLayout.relativePath(at, 17, "no-dot-here"));
        assertEquals("2026/09/000000/17",
                DmsStorageLayout.relativePath(at, 17, "bad.<script>"));
    }

    @Test
    void isLegacyName_distinguishesFromNewName() {
        assertTrue(DmsStorageLayout.isLegacyName("48213"));
        assertFalse(DmsStorageLayout.isLegacyName("48213.pdf"));
        assertFalse(DmsStorageLayout.isLegacyName("_trash"));
        assertFalse(DmsStorageLayout.isLegacyName("README.txt"));
        assertFalse(DmsStorageLayout.isLegacyName(""));
        assertFalse(DmsStorageLayout.isLegacyName(null));
    }

    @Test
    void tmpPath_sameDirectory() {
        Path target = Paths.get("dms", "2026", "09", "000051", "51230.pdf");
        Path tmp = DmsStorageLayout.tmpPath(target);
        assertEquals(target.getParent(), tmp.getParent());
        assertEquals("51230.pdf.tmp", tmp.getFileName().toString());
    }

    @Test
    void trashDir_dateFormat() {
        Path root = Paths.get("dms");
        assertEquals(Paths.get("dms", "_trash", "2026-09-13"),
                DmsStorageLayout.trashDir(root, LocalDate.of(2026, 9, 13)));
    }
}
