package cz.tacr.elza.utils;

import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * Resource pro input stream, který umožňuje definici jména souboru a případně jeho délky.
 *
 * @author Pavel Stánek [pavel.stanek@marbes.cz]
 * @since 15.11.2017
 */
public class NamedInputStreamResource extends InputStreamResource {
    /**
     * Název souboru.
     */
    private String filename;
    /**
     * Délka souboru, pokud je null, není definovaná.
     */
    private Long contentLength;

    public NamedInputStreamResource(String filename, Long length, InputStream inputStream) {
        super(inputStream);
        this.filename = filename;
        this.contentLength = length;
    }
    public NamedInputStreamResource(String filename, InputStream inputStream) {
        super(inputStream);
        this.filename = filename;
    }

    public NamedInputStreamResource(String filename, Long length, InputStream inputStream, String description) {
        super(inputStream, description);
        this.filename = filename;
        this.contentLength = length;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public long contentLength() throws IOException {
        return contentLength != null ? contentLength : -1;
    }
}
