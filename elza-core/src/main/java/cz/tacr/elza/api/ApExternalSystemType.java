package cz.tacr.elza.api;

/**
 * Výčet externích systémů pro rejstříky/osoby.
 *
 * @since 23. 11. 2016
 */
public enum ApExternalSystemType {

    CAM,
    CAM_V2,
    /**
     * Same as CAM except UUID is preferred as ID
     */
    CAM_UUID,
    CAM_UUID_V2,
    /**
     * Automatické přebírání nových záznamů z CAMu
     */
    CAM_COMPLETE,
    CAM_COMPLETE_V2;

    /**
     * Vrátí verzi protokolu.
     *
     * @return 1 pro CAM, CAM_UUID, CAM_COMPLETE; 2 pro CAM_V2, CAM_UUID_V2, CAM_COMPLETE_V2
     */
    public int getVersionApi() {
        switch (this) {
        case CAM:
        case CAM_UUID:
        case CAM_COMPLETE:
            return 1;
        case CAM_V2:
        case CAM_UUID_V2:
        case CAM_COMPLETE_V2:
            return 2;
        default:
            throw new IllegalStateException("Neznámý typ: " + this);
        }
    }

    /**
     * Zjistí, zda dva typy jsou stejného základního typu, ale různých verzí.
     * Např. CAM a CAM_V2 → true, CAM a CAM → false, CAM a CAM_UUID → false.
     *
     * @param other druhý typ k porovnání
     * @return true pokud jsou stejného základního typu, ale různých verzí
     */
    public boolean isSameType(ApExternalSystemType other) {
        if (other == null || this == other) {
            return false;
        }
        return getBaseType() == other.getBaseType();
    }

    /**
     * Vrátí základní typ bez ohledu na verzi.
     * Např. CAM_V2 → CAM, CAM_UUID_V2 → CAM_UUID, CAM_COMPLETE_V2 → CAM_COMPLETE.
     */
    public ApExternalSystemType getBaseType() {
        switch (this) {
        case CAM:
        case CAM_V2:
            return CAM;
        case CAM_UUID:
        case CAM_UUID_V2:
            return CAM_UUID;
        case CAM_COMPLETE:
        case CAM_COMPLETE_V2:
            return CAM_COMPLETE;
        default:
            throw new IllegalStateException("Neznámý typ: " + this);
        }
    }
}
