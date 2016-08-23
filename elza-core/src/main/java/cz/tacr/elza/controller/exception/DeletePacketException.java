package cz.tacr.elza.controller.exception;

import org.springframework.http.HttpStatus;

/**
 * Vyjímka při mazání obalů - typicky při existujících vazbách.
 *
 * @author Martin Šlapa
 * @since 23.08.2016
 */
public class DeletePacketException extends AbstractControllerException {

    private static final HttpStatus CODE = HttpStatus.METHOD_NOT_ALLOWED;

    public DeletePacketException() {
        super(CODE);
    }

    public DeletePacketException(final String message) {
        super(message, CODE);
    }

    public DeletePacketException(final String message,
                                 final Throwable cause) {
        super(message, cause, CODE);
    }

    public DeletePacketException(final Throwable cause) {
        super(cause, CODE);
    }

    public DeletePacketException(final String message,
                                 final Throwable cause,
                                 final boolean enableSuppression,
                                 final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace, CODE);
    }
}
