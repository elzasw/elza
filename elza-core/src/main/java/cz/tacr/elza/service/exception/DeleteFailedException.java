package cz.tacr.elza.service.exception;

/**
 * Výjimka pro mazání záznamů, které kvůli závislosti nejdou smazat.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 15.04.2016
 */
public class DeleteFailedException extends RuntimeException {

    public DeleteFailedException() {
    }

    public DeleteFailedException(final String message) {
        super(message);
    }

    public DeleteFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public DeleteFailedException(final Throwable cause) {
        super(cause);
    }

    public DeleteFailedException(final String message,
                                 final Throwable cause,
                                 final boolean enableSuppression,
                                 final boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
