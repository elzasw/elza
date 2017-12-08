package cz.tacr.elza.service.output.dev;

public enum OutputInterruptReason {

    ERROR,

    /**
     * Byl detekovány změny v Pořádání.
     */
    DETECT_CHANGE,

    /**
     * Nebyly spuštěny všechny doporučené akce.
     */
    RECOMMENDED_ACTION_NOT_RUN
}
