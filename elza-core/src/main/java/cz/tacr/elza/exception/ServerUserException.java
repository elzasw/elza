package cz.tacr.elza.exception;

/**
 * Uživatelská výjimka, která je ošetřená v klientské části.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 18.03.2016
 */
public class ServerUserException extends RuntimeException {

    public ServerUserException() {
    }

    public ServerUserException(final String message) {
        super(message);
    }

    public ServerUserException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public ServerUserException(final Throwable cause) {
        super(cause);
    }
}
