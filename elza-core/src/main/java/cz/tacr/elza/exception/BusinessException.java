package cz.tacr.elza.exception;

import cz.tacr.elza.exception.codes.ErrorCode;

/**
 * Výjimka pro business.
 *
 * @since 09.11.2016
 */
public class BusinessException extends AbstractException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public BusinessException(final String message, final ErrorCode errorCode) {
        super(message, errorCode);
    }

    public BusinessException(final String message, final Throwable throwable, final ErrorCode errorCode) {
        super(message, throwable, errorCode);
    }
}
