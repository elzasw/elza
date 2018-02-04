package cz.tacr.elza.exception;

import cz.tacr.elza.exception.codes.BaseCode;


/**
 * Chyba pro vlastní porovnání verzí v db dvou objektů.
 *
 * @since 09.12.2015
 */
public class LockVersionChangeException extends AbstractException {

    private static final long serialVersionUID = 1L; // default id

    /**
     * @param message text chyby
     */
    public LockVersionChangeException(final String message) {
        super(message, BaseCode.OPTIMISTIC_LOCKING_ERROR);
    }
}
