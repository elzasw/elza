package cz.tacr.elza.exception;

/**
 * Neplatný lucene dotaz.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 22.04.2016
 */
public class InvalidQueryException extends ServerUserException {

    public InvalidQueryException() {
    }

    public InvalidQueryException(final String message) {
        super(message);
    }

    public InvalidQueryException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public InvalidQueryException(final Throwable cause) {
        super(cause);
    }
}
