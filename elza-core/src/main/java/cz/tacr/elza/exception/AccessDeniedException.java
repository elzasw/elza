package cz.tacr.elza.exception;

/**
 * Výjimka pro neautorizovaný přístup.
 *
 * @author Martin Šlapa
 * @since 27.04.2016
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(final String text) {
        super(text);
    }
}
