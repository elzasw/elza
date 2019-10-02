package cz.tacr.elza.dataexchange.output.writer.xml;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.commons.lang3.Validate;

/**
 * Simple representation of file as xml fragment. Implementation is not thread-safe.
 */
public class XmlFragment implements Closeable {

    private static final XMLOutputFactory OUTPUT_FACTORY = XMLOutputFactory.newInstance();

    private final Path directory;

    /**
     * Path to the fragment
     * 
     * If this value is null then fragment is closed
     */
    private Path file = null;

    private OutputStream outputStream;

    private XMLStreamWriter streamWriter;

    /**
     * Creates a new xml fragment in the specified directory, using the "xml-fragment-" prefix to
     * generate its name.
     */
    public XmlFragment(Path directory) {
        this.directory = Validate.notNull(directory);
    }

    /**
     * @return Path to xml fragment or null if does not exist (never been opened).
     */
    public Path getPath() {
        return file;
    }

    public boolean isExist() {
        return file != null;
    }

    public boolean isOpen() {
        boolean open = streamWriter != null;
        Validate.isTrue(open == (outputStream != null));
        return open;
    }

    public XMLStreamWriter openStreamWriter() throws IOException, XMLStreamException {
        Validate.isTrue(!isOpen());

        try {
            prepareFileForWrite();
            streamWriter = OUTPUT_FACTORY.createXMLStreamWriter(outputStream);
        } catch (IOException | XMLStreamException e) {
            delete();
            throw e;
        }
        return streamWriter;
    }

    public XMLStreamWriter getStreamWriter() {
        Validate.isTrue(isOpen());

        return streamWriter;
    }

    @Override
    public void close() throws IOException {
        if (isExist()) {
            try {
                if (streamWriter != null) {
                    try {
                        streamWriter.close();
                    } catch (XMLStreamException e) {
                        throw new IOException("Failed to close stream writter", e);
                    }
                    streamWriter = null;
                }
            } finally {
                closeOutputStream();
            }
        }
    }

    /**
     * Delete fragment
     * 
     * If fragment is opened it closed first
     * 
     * @throws IOException
     */
    public void delete() throws IOException {
        if (isExist()) {
            streamWriter = null; // w/o close, we don't need to write open elements
            closeOutputStream();
            Files.delete(file);
            file = null;
        }
    }

    private void closeOutputStream() throws IOException {
        if (outputStream != null) {
            outputStream.close();
            outputStream = null;
        }
    }

    private void prepareFileForWrite() throws IOException {
        if (file == null) {
            file = Files.createTempFile(directory, "xml-fragment-", null);
        }
        outputStream = Files.newOutputStream(file, StandardOpenOption.APPEND);
    }
}
