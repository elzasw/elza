package cz.tacr.elza.domain;

/**
 * Stav přístupového bodu.
 *
 * Používané pro entity:
 *  - {@link ApState}
 */
public enum ApStateApproval {

    /**
     * Nový přístupový bod.
     */
    NOVY,

    /**
     * Připraven ke schválení.
     */
    KE_SCHVALENI,

    /**
     * Schválený.
     */
    SCHVALENY,

    /**
     * K doplnění.
     */
    K_DOPLNENI

}
