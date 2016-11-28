package cz.tacr.elza.exception.codes;

/**
 * Základní kódy.
 *
 * @author Martin Šlapa
 * @since 09.11.2016
 */
public enum BaseCode implements ErrorCode {

    /**
     * Nedostatečná oprávnění.
     */
    INSUFFICIENT_PERMISSIONS,

    /**
     * Systémová chyba.
     */
    SYSTEM_ERROR,

    /**
     * Chyba optimistických zámků - při ukládání změněné entity.
     */
    OPTIMISTIC_LOCKING_ERROR,

    /**
     * Chyba při převodu JSON objektu.
     */
    JSON_PARSE

}
