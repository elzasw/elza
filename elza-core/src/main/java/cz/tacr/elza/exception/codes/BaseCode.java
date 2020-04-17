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
    JSON_PARSE,

    /**
     * Identifikátor entity nesmí existovat.
     */
    ID_EXIST,

    /**
     * Identifikátor entity musí existovat.
     */
    ID_NOT_EXIST,

    /**
     * Pole entity musí být vyplněno.
     */
    PROPERTY_NOT_EXIST,

    /**
     * Pole entity musí být správně vyplněno.
     */
    PROPERTY_IS_INVALID,

    /**
     * Problém s integritou databáze.
     */
    DB_INTEGRITY_PROBLEM,

    /**
     * Neplatný stav.
     */
    INVALID_STATE,

    /**
     * Byla detekována cyklická závislost.
     */
    CYCLE_DETECT,

    /**
     * Pole entity není správného datového typu.
     */
    PROPERTY_HAS_INVALID_TYPE,

    /**
     * Import failed.<br>
     * Code used by DataExchange import exception.
     */
    IMPORT_FAILED,

    /**
     * Export failed.<br>
     * Code used by DataExchange export exception.
     */
    EXPORT_FAILED,

    /**
     * Přílišná délka
     */
    INVALID_LENGTH

}
