package cz.tacr.elza.exception;


import cz.tacr.elza.exception.codes.ErrorCode;

/**
 * Výjimka pro současnou aktualizace položky více uživateli.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 26. 8. 2015
 */
public class ConcurrentUpdateException extends AbstractException {

    public ConcurrentUpdateException(Exception e, ErrorCode errorCode) {
        super(e, errorCode);
    }
}
