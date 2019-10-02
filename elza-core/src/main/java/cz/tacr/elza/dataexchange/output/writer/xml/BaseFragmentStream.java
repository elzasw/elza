package cz.tacr.elza.dataexchange.output.writer.xml;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * Base implementation for fragment stream
 * 
 *
 */
public abstract class BaseFragmentStream {

    protected final XmlFragment fragment;

    private boolean processed = false;

    protected BaseFragmentStream(Path tempDirectory) {
        fragment = new XmlFragment(tempDirectory);
    }

    protected boolean isProcessed() {
        return processed;
    }

    /**
     * Save fragment
     */
    protected void finishFragment() {
        Validate.isTrue(!isProcessed());

        try {
            fragment.close();
        } catch (IOException e) {
            throw new SystemException("Failed to close fragment", BaseCode.EXPORT_FAILED);
        }
        processed = true;
    }

    protected void closeFragment() {
        if (isProcessed()) {
            return;
        }
        try {
            fragment.delete();
        } catch (IOException e) {
            throw new SystemException(e);
        }
    }
}
