package cz.tacr.elza.exception;


import cz.tacr.elza.exception.codes.ErrorCode;

/**
 * Entita nenalezena.
 *
 * @author Martin Šlapa
 * @since 09.11.2016
 */
public class ObjectNotFoundException extends AbstractException {

    public ObjectNotFoundException(final ErrorCode errorCode) {
        super(errorCode);
    }
}
