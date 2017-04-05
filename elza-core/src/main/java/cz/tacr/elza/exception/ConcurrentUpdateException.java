package cz.tacr.elza.exception;


import cz.tacr.elza.exception.codes.ErrorCode;

/**
 * Výjimka pro současnou aktualizace položky více uživateli.
 *
 * @since 26. 8. 2015
 */
public class ConcurrentUpdateException extends AbstractException {

    public ConcurrentUpdateException(String message, Exception e, ErrorCode errorCode) {
        super(message, e, errorCode);
    }
}
