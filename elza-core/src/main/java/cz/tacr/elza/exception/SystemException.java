package cz.tacr.elza.exception;

import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ErrorCode;

/**
 * Výjimka pro systémovou chybu.
 *
 * @since 09.11.2016
 */
public class SystemException extends AbstractException {

    public SystemException(String message) {
        super(message, BaseCode.SYSTEM_ERROR);
    }

    public SystemException(final String message, final ErrorCode errorCode) {
        super(message, errorCode);
    }

    public SystemException(final Throwable cause) {
        super(cause.getMessage(), cause, BaseCode.SYSTEM_ERROR);
    }

    public SystemException(final String message, final Throwable cause) {
        super(message, cause, BaseCode.SYSTEM_ERROR);
    }

    public SystemException(final String message, final Throwable cause, final ErrorCode parseError) {
        super(message, cause, parseError);
    }
}
