package cz.tacr.elza.common;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Uzavíratelný resource pro input stream, který umožňuje definici jména souboru a případně jeho délky.
 *
 */
public class CloseablePathResource
    implements Closeable
{
    private final Path resourceFile;

    public CloseablePathResource(final Path resourceFile) {
        this.resourceFile = resourceFile;
    }

    public Path getPath() {
        return resourceFile;
    }

    public String getFilename() {
        return resourceFile.getFileName().toString();
    }

    public long contentLength() throws IOException {
        return resourceFile.toFile().length();
    }

    public ReadableByteChannel getByteChannel() throws IOException {
        return Files.newByteChannel(resourceFile);
    }

    @Override
    public void close() throws IOException {
        // This implementation has
        // nothing to close
    }

    public void writeTo(OutputStream out) throws IOException {
        Files.copy(resourceFile, out);
    }
}
