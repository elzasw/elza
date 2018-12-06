package cz.tacr.elza.domain;

/**
 * Stav entity.
 *
 * Používané pro entity:
 *  - {@link ApAccessPoint}
 *  - {@link ApName}
 *  - {@link ApFragment}
 */
public enum ApState {

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
