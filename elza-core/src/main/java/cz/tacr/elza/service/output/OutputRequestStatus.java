package cz.tacr.elza.service.output;

/**
 * Stav spuštění generátoru.
 */
@Deprecated
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
    RECOMMENDED_ACTION_NOT_RUN,

    /**
     * A recommended action respecting output filters was run only with another output filter
     * than the output has.
     */
    RECOMMENDED_ACTION_FILTER_CHANGED
}