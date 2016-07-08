package cz.tacr.elza.exception;

import cz.tacr.elza.aop.MethodLoggerIgnoreException;


/**
 * Chyba pro vlastní porovnání verzí v db dvou objektů.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 09.12.2015
 */
public class LockVersionChangeException extends RuntimeException implements MethodLoggerIgnoreException {

    /**
     * @param message text chyby
     */
    public LockVersionChangeException(final String message) {
        super(message);
    }
}
