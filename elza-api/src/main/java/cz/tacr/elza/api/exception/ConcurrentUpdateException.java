package cz.tacr.elza.api.exception;


/**
 * Výjimka pro současnou aktualizace položky více uživateli.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 26. 8. 2015
 */
public class ConcurrentUpdateException extends RuntimeException {

    public ConcurrentUpdateException(Exception e) {
        super(e);
    }

    public ConcurrentUpdateException(String msg) {
        super(msg);
    }

}
