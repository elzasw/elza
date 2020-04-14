package cz.tacr.elza.controller.vo.ap;

import cz.tacr.elza.controller.vo.ApAccessPointVO;

/**
 * Stav entity.
 *
 * Používané pro entity:
 *  - {@link ApAccessPointVO}
 *  - {@link ApFragmentVO}
 */
public enum ApStateVO {

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
