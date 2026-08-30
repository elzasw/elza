package cz.tacr.elza.common.io;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.output.DeferredFileOutputStream;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Content of unknown size read from a stream and kept in memory only while it is small.
 *
 * Content up to {@link #DEFAULT_THRESHOLD} bytes stays in a byte array; anything larger is
 * spooled to a temporary file, so a download of arbitrary size never has to fit into the heap.
 * The content can be read repeatedly with {@link #openStream()} until {@link #close()}, which
 * deletes the temporary file. Instances are not thread safe.
 */
public final class SpooledContent implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(SpooledContent.class);

    /** Largest content kept in memory: 8 MiB. */
    public static final int DEFAULT_THRESHOLD = 8 * 1024 * 1024;

    private final byte[] data;

    private final Path file;

    private final boolean deleteFile;

    private boolean closed;

    private SpooledContent(byte[] data, Path file, boolean deleteFile) {
        this.data = data;
        this.file = file;
        this.deleteFile = deleteFile;
    }

    /**
     * Reads the stream to its end. The stream itself is not closed.
     */
    public static SpooledContent readFrom(InputStream in) throws IOException {
        return readFrom(in, DEFAULT_THRESHOLD);
    }

    /**
     * Reads the stream to its end, keeping at most {@code threshold} bytes in memory. The
     * stream itself is not closed.
     */
    public static SpooledContent readFrom(InputStream in, int threshold) throws IOException {
        Validate.notNull(in, "Input stream is required");
        Validate.isTrue(threshold >= 0, "Threshold must not be negative");
        DeferredFileOutputStream out = DeferredFileOutputStream.builder()
                .setThreshold(threshold)
                .setPrefix("elza-spool-")
                .setSuffix(".tmp")
                .get();
        try {
            in.transferTo(out);
            out.close();
        } catch (IOException | RuntimeException e) {
            out.close();
            deleteQuietly(out.getPath());
            throw e;
        }
        if (out.isInMemory()) {
            return new SpooledContent(out.getData(), null, false);
        }
        return new SpooledContent(null, out.getPath(), true);
    }

    /**
     * Wraps an existing file; the file is deleted by {@link #close()}.
     */
    public static SpooledContent ofTempFile(Path file) {
        Validate.notNull(file, "File is required");
        return new SpooledContent(null, file, true);
    }

    /**
     * Wraps an existing file that outlives this object.
     */
    public static SpooledContent ofFile(Path file) {
        Validate.notNull(file, "File is required");
        return new SpooledContent(null, file, false);
    }

    public boolean isInMemory() {
        return data != null;
    }

    public long size() throws IOException {
        ensureOpen();
        return data != null ? data.length : Files.size(file);
    }

    /**
     * @return the file holding the content, {@code null} when the content is in memory
     */
    public Path getFile() {
        return file;
    }

    /**
     * Opens a fresh stream over the content; the caller closes it.
     */
    public InputStream openStream() throws IOException {
        ensureOpen();
        return data != null ? new ByteArrayInputStream(data) : Files.newInputStream(file);
    }

    /**
     * Opens a stream whose {@code close()} also closes this content - for handing the content
     * over to a consumer that owns the stream (e.g. an HTTP response body).
     */
    public InputStream openStreamAndCloseOnEnd() throws IOException {
        return new FilterInputStream(openStream()) {
            @Override
            public void close() throws IOException {
                try {
                    super.close();
                } finally {
                    SpooledContent.this.close();
                }
            }
        };
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (deleteFile) {
            deleteQuietly(file);
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Spooled content is closed");
        }
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.warn("Failed to delete spool file {}: {}", path, e.getMessage());
        }
    }
}
