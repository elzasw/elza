package cz.tacr.elza.exception;

import cz.tacr.elza.aop.MethodLoggerIgnoreException;


/**
 * Chyba pro vlastní porovnání verzí v db dvou objektů.
 *
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
