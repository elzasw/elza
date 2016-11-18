package cz.tacr.elza.exception;

import cz.tacr.elza.exception.codes.ErrorCode;

/**
 * Vyjímka při mazání - typicky při existujících vazbách.
 *
 * @author Martin Šlapa
 * @since 09.11.2016
 */
public class DeleteException extends AbstractException {

    public DeleteException(final ErrorCode errorCode) {
        super(errorCode);
    }
}
