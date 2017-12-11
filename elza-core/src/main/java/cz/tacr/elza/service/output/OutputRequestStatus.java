package cz.tacr.elza.service.output;

/**
 * Stav spuštění generátoru.
 */
public enum OutputRequestStatus {
    /**
     * V pořádku.
     */
    OK,

    /**
     * Byl detekovány změny v Pořádání.
     */
    DETECT_CHANGE,

    /**
     * Nebyly spuštěny všechny doporučené akce.
     */
    RECOMMENDED_ACTION_NOT_RUN
}