package cz.tacr.elza.controller.exception;

import org.springframework.http.HttpStatus;

/**
 * Abstraktní výjimka pro typované chyby na kontroleru.
 *
 * @author Martin Šlapa
 * @since 23.08.2016
 */
public abstract class AbstractControllerException extends RuntimeException {

    /**
     * Status kód, jakým se má odpověď přenášet.
     */
    private HttpStatus code;

    public AbstractControllerException(final HttpStatus code) {
        this.code = code;
    }

    public AbstractControllerException(final String message,
                                       final HttpStatus code) {
        super(message);
        this.code = code;
    }

    public AbstractControllerException(final String message,
                                       final Throwable cause,
                                       final HttpStatus code) {
        super(message, cause);
        this.code = code;
    }

    public AbstractControllerException(final Throwable cause,
                                       final HttpStatus code) {
        super(cause);
        this.code = code;
    }

    public AbstractControllerException(final String message,
                                       final Throwable cause,
                                       final boolean enableSuppression,
                                       final boolean writableStackTrace,
                                       final HttpStatus code) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
    }

    public HttpStatus getCode() {
        return code;
    }
}
