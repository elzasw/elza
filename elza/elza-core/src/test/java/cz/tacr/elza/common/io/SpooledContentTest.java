package cz.tacr.elza.common.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class SpooledContentTest {

    private static byte[] bytes(int size) {
        byte[] b = new byte[size];
        for (int i = 0; i < size; i++) {
            b[i] = (byte) i;
        }
        return b;
    }

    @Test
    void smallContentStaysInMemory() throws IOException {
        byte[] payload = bytes(100);
        try (SpooledContent content = SpooledContent.readFrom(new ByteArrayInputStream(payload), 1000)) {
            assertTrue(content.isInMemory());
            assertNull(content.getFile());
            assertEquals(100, content.size());
            try (InputStream in = content.openStream()) {
                assertArrayEquals(payload, in.readAllBytes());
            }
            // readable more than once
            try (InputStream in = content.openStream()) {
                assertArrayEquals(payload, in.readAllBytes());
            }
        }
    }

    @Test
    void largeContentIsSpooledToFileDeletedOnClose() throws IOException {
        byte[] payload = bytes(5000);
        Path file;
        try (SpooledContent content = SpooledContent.readFrom(new ByteArrayInputStream(payload), 1000)) {
            assertFalse(content.isInMemory());
            file = content.getFile();
            assertNotNull(file);
            assertTrue(Files.exists(file));
            assertEquals(5000, content.size());
            try (InputStream in = content.openStream()) {
                assertArrayEquals(payload, in.readAllBytes());
            }
        }
        assertFalse(Files.exists(file));
    }

    @Test
    void closedContentCannotBeRead() throws IOException {
        SpooledContent content = SpooledContent.readFrom(new ByteArrayInputStream(bytes(10)));
        content.close();
        assertThrows(IOException.class, content::openStream);
        content.close(); // idempotent
    }

    @Test
    void ofTempFile_deletesFileOnClose(@TempDir Path dir) throws IOException {
        Path file = Files.write(dir.resolve("a.zip"), bytes(10));
        try (SpooledContent content = SpooledContent.ofTempFile(file)) {
            assertEquals(10, content.size());
        }
        assertFalse(Files.exists(file));
    }

    @Test
    void ofFile_keepsFileOnClose(@TempDir Path dir) throws IOException {
        Path file = Files.write(dir.resolve("a.zip"), bytes(10));
        try (SpooledContent content = SpooledContent.ofFile(file)) {
            assertEquals(10, content.size());
        }
        assertTrue(Files.exists(file));
    }

    @Test
    void openStreamAndCloseOnEnd_closesContentWithStream() throws IOException {
        SpooledContent content = SpooledContent.readFrom(new ByteArrayInputStream(bytes(5000)), 1000);
        Path file = content.getFile();
        try (InputStream in = content.openStreamAndCloseOnEnd()) {
            assertEquals(5000, in.readAllBytes().length);
        }
        assertFalse(Files.exists(file));
        assertThrows(IOException.class, content::openStream);
    }
}
