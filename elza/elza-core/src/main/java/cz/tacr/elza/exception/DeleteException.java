package cz.tacr.elza.exception;

import cz.tacr.elza.exception.codes.ErrorCode;

/**
 * Vyjímka při mazání - typicky při existujících vazbách.
 *
 * @since 09.11.2016
 */
public class DeleteException extends AbstractException {

    public DeleteException(final String message, final ErrorCode errorCode) {
        super(message, errorCode);
    }
}
