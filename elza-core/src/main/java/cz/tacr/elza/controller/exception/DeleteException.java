package cz.tacr.elza.controller.exception;

import org.springframework.http.HttpStatus;

/**
 * Vyjímka při mazání obalů - typicky při existujících vazbách.
 *
 * @author Martin Šlapa
 * @since 23.08.2016
 */
public class DeleteException extends AbstractControllerException {

    private static final HttpStatus CODE = HttpStatus.METHOD_NOT_ALLOWED;

    public DeleteException() {
        super(CODE);
    }

    public DeleteException(final String message) {
        super(message, CODE);
    }

    public DeleteException(final String message,
                           final Throwable cause) {
        super(message, cause, CODE);
    }

    public DeleteException(final Throwable cause) {
        super(cause, CODE);
    }

    public DeleteException(final String message,
                           final Throwable cause,
                           final boolean enableSuppression,
                           final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace, CODE);
    }
}
