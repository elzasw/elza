package cz.tacr.elza.service.output;

/**
 * Stav spuštění generátoru.
 *
 * @author Martin Šlapa
 * @since 01.12.2016
 */
public enum StatusGenerate {
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