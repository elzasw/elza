package cz.tacr.elza.exception;

import cz.tacr.elza.service.FilterTreeService;


/**
 * Chyba když nejsou nastavené filtry stromu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @see FilterTreeService
 * @since 18.03.2016
 */
public class FilterExpiredException extends ServerUserException {

    public FilterExpiredException() {
    }

    public FilterExpiredException(final String message) {
        super(message);
    }

    public FilterExpiredException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public FilterExpiredException(final Throwable cause) {
        super(cause);
    }
}
