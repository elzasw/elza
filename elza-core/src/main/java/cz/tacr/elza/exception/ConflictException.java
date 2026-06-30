package cz.tacr.elza.exception;

import cz.tacr.elza.exception.codes.ErrorCode;

/**
 * Indicates that the request conflicts with the current state of a resource
 * — e.g. an ambiguous lookup that resolves to more rows than expected.
 * Mapped to HTTP 409 by {@code ControllerExceptionHandler}.
 */
public class ConflictException extends BusinessException {

    private static final long serialVersionUID = 1L;

    public ConflictException(final String message, final ErrorCode errorCode) {
        super(message, errorCode);
    }

    public ConflictException(final String message, final Throwable cause, final ErrorCode errorCode) {
        super(message, cause, errorCode);
    }
}
