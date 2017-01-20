package cz.tacr.elza.exception;

import cz.tacr.elza.exception.codes.ErrorCode;

/**
 * Výjimka pro business.
 *
 * @author Martin Šlapa
 * @since 09.11.2016
 */
public class BusinessException extends AbstractException {

    public BusinessException(final String message, final ErrorCode errorCode) {
        super(message, errorCode);
    }
}
