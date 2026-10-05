package cz.tacr.elza.service.dms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import cz.tacr.elza.core.ResourcePathResolver;

public class DmsTrashServiceTest {

    @TempDir
    Path dmsRoot;

    private DmsTrashService service;

    @BeforeEach
    void setUp() {
        ResourcePathResolver resolver = mock(ResourcePathResolver.class);
        when(resolver.getDmsDir()).thenReturn(dmsRoot);
        service = new DmsTrashService();
        ReflectionTestUtils.setField(service, "resourcePathResolver", resolver);
    }

    private Path createSource(String name, String content) throws IOException {
        Path p = dmsRoot.resolve(name);
        Files.writeString(p, content);
        return p;
    }

    private Path todayTrash() {
        return DmsStorageLayout.trashDir(dmsRoot, LocalDate.now());
    }

    @Test
    void moveToTrash_happy() throws IOException {
        Path source = createSource("51230.pdf", "hello");
        Path stored = service.moveToTrash(source, "attachment-delete");

        assertFalse(Files.exists(source));
        assertTrue(Files.exists(stored));
        assertEquals(todayTrash(), stored.getParent());
        assertEquals("51230.pdf", stored.getFileName().toString());

        List<String> lines = Files.readAllLines(todayTrash().resolve("report.txt"));
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).matches(
                "51230\\.pdf;5;\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.?\\d*Z;attachment-delete"));
    }

    @Test
    void moveToTrash_collisionAppendsHHmmss() throws IOException {
        Path first = createSource("17.pdf", "one");
        service.moveToTrash(first, "output-regenerated");

        Path second = createSource("17.pdf", "two");
        Path stored = service.moveToTrash(second, "output-regenerated");

        assertTrue(stored.getFileName().toString().matches("17\\.pdf-\\d{6}"));
        assertTrue(Files.exists(todayTrash().resolve("17.pdf")));
    }

    @Test
    void moveToTrash_missingSource_returnsNull() {
        Path stored = service.moveToTrash(dmsRoot.resolve("nope"), "consistency-orphan");
        assertNull(stored);
        assertFalse(Files.exists(todayTrash().resolve("report.txt")));
    }

    @Test
    void purgeExpired_dropsOldDays() throws IOException {
        Path trashRoot = dmsRoot.resolve(DmsStorageLayout.TRASH_DIR);
        Files.createDirectories(trashRoot);
        Path old = trashRoot.resolve(LocalDate.now().minusDays(31).toString());
        Path fresh = trashRoot.resolve(LocalDate.now().minusDays(29).toString());
        Files.createDirectories(old);
        Files.createDirectories(fresh);
        Files.writeString(old.resolve("report.txt"), "x\n");
        Files.writeString(fresh.resolve("report.txt"), "y\n");

        int purged = service.purgeExpired(30);

        assertEquals(1, purged);
        assertFalse(Files.exists(old));
        assertTrue(Files.exists(fresh));
    }

    @Test
    void purgeExpired_toleratesNonDateDir() throws IOException {
        Path trashRoot = dmsRoot.resolve(DmsStorageLayout.TRASH_DIR);
        Files.createDirectories(trashRoot.resolve("garbage"));
        Files.createDirectories(trashRoot.resolve(LocalDate.now().minusDays(31).toString()));

        int purged = service.purgeExpired(30);

        assertEquals(1, purged);
        assertTrue(Files.exists(trashRoot.resolve("garbage")));
    }

    @Test
    void purgeExpired_noTrashRootIsZero() {
        assertEquals(0, service.purgeExpired(30));
    }
}
