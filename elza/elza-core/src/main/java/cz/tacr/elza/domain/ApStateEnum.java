package cz.tacr.elza.domain;

/**
 * Stav entity.
 *
 * Používané pro entity:
 *  - {@link ApAccessPoint}
 *  - {@link ApPart}
 */
public enum ApStateEnum {

    /**
     * V pořádku, platné.
     */
    OK,

    /**
     * Chyba při generování.
     */
    ERROR,

    /**
     * Dočasná entita, bude odstraněno.
     */
    TEMP,

    /**
     * Připraveno k přegenerování.
     */
    INIT

}
