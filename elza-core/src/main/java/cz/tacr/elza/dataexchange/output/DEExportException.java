package cz.tacr.elza.dataexchange.output;

import cz.tacr.elza.exception.AbstractException;
import cz.tacr.elza.exception.codes.BaseCode;

/**
 * General exception for DataExchange export.
 */
public class DEExportException extends AbstractException {

    private static final long serialVersionUID = 1L;

    public DEExportException(String message) {
        super(message, BaseCode.EXPORT_FAILED);
    }

    public DEExportException(String message, Throwable cause) {
        super(message, cause, BaseCode.EXPORT_FAILED);
    }
}
