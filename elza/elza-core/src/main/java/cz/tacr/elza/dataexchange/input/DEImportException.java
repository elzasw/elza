package cz.tacr.elza.dataexchange.input;

import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * General exception for DataExchange import.
 */
public class DEImportException extends AbstractException {

    private static final long serialVersionUID = 1L;

    public DEImportException(String message) {
        super(message, BaseCode.IMPORT_FAILED);
    }

    public DEImportException(String message, Throwable cause) {
        super(message, cause, BaseCode.IMPORT_FAILED);
    }
}