package cz.tacr.elza.exception;

import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.exception.codes.ErrorCode;

/**
 * Výjimka pro systémovou chybu.
 *
 * @author Martin Šlapa
 * @since 09.11.2016
 */
public class SystemException extends AbstractException {

    public SystemException() {
        super(BaseCode.SYSTEM_ERROR);
    }

    public SystemException(final ErrorCode errorCode) {
        super(errorCode);
    }

    public SystemException(final Throwable cause) {
        super(cause, BaseCode.SYSTEM_ERROR);
    }
}
