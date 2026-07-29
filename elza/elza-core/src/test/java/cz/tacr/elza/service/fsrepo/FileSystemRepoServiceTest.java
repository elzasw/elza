package cz.tacr.elza.service.fsrepo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.service.dao.FileSystemRepoService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileSystemRepoServiceTest {

    private final FileSystemRepoService svc = new FileSystemRepoService();

    // ---------- A3: resolvePath containment ----------

    @Test
    void resolve_blank_returnsRoot(@TempDir Path root) {
        assertEquals(root.normalize(), svc.resolvePath(root, null));
        assertEquals(root.normalize(), svc.resolvePath(root, ""));
        assertEquals(root.normalize(), svc.resolvePath(root, "   "));
    }

    @Test
    void resolve_legitimateChild(@TempDir Path root) {
        Path resolved = svc.resolvePath(root, "sub/file.jpg");
        assertTrue(resolved.startsWith(root.normalize()));
        assertTrue(resolved.endsWith(Path.of("sub", "file.jpg")));
    }

    @Test
    void resolve_dotDot_escapes_throws(@TempDir Path root) {
        assertThrows(BusinessException.class,
                () -> svc.resolvePath(root, "../etc/passwd"));
        assertThrows(BusinessException.class,
                () -> svc.resolvePath(root, "a/b/../../../etc/passwd"));
    }

    @Test
    void resolve_absolute_replacesRoot_throws(@TempDir Path root) {
        String absolute = Path.of("/", "etc", "passwd").toString();
        assertThrows(BusinessException.class, () -> svc.resolvePath(root, absolute));
    }

    @Test
    void resolve_dotSegments_allowed(@TempDir Path root) {
        Path resolved = svc.resolvePath(root, "a/./b");
        assertTrue(resolved.endsWith(Path.of("a", "b")));
    }

    // ---------- A2: getMimetype by extension ----------

    @Test
    void mimetype_byExtension_knownTypes() {
        assertEquals("image/jpeg",       svc.getMimetype("photo.JPG"));
        assertEquals("image/jpeg",       svc.getMimetype("photo.jpeg"));
        assertEquals("image/png",        svc.getMimetype("scan.png"));
        assertEquals("image/tiff",       svc.getMimetype("archival.tif"));
        assertEquals("image/tiff",       svc.getMimetype("archival.tiff"));
        assertEquals("application/pdf",  svc.getMimetype("finding-aid.pdf"));
    }

    @Test
    void mimetype_byExtension_unknown_returnsNull() {
        assertNull(svc.getMimetype("something.zzzz"));
        assertNull(svc.getMimetype("noext"));
    }

    @Test
    void mimetype_byPath_realFile(@TempDir Path tmp) throws IOException {
        Path png = Files.createFile(tmp.resolve("x.png"));
        String type = svc.getMimetype(png);
        // Files.probeContentType may return a system-specific alias
        // (e.g. "image/png" or "image/vnd.microsoft.icon" for weird types);
        // for .png every mainstream JDK returns something starting with "image/"
        assertTrue(type != null && type.startsWith("image/"),
                "Expected image/*, got: " + type);
    }

    // ---------- A2: isInlineRenderable ----------

    @Test
    void inlineRenderable_allowsImagesPdfText() {
        assertTrue(FileSystemRepoService.isInlineRenderable("image/jpeg"));
        assertTrue(FileSystemRepoService.isInlineRenderable("IMAGE/PNG"));   // case-insensitive
        assertTrue(FileSystemRepoService.isInlineRenderable("application/pdf"));
        assertTrue(FileSystemRepoService.isInlineRenderable("text/plain"));
    }

    @Test
    void inlineRenderable_rejectsBinaryAndUnknown() {
        assertFalse(FileSystemRepoService.isInlineRenderable("application/octet-stream"));
        assertFalse(FileSystemRepoService.isInlineRenderable("video/mp4"));
        assertFalse(FileSystemRepoService.isInlineRenderable(null));
    }
}