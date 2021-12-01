package cz.tacr.elza.domain;

public enum RevStateApproval {

    /**
     * Nová revize přístupového bodu.
     */
    ACTIVE,

    /**
     * Připraven ke schválení.
     */
    TO_APPROVE,

    /**
     * K doplnění.
     */
    TO_AMEND;
}
