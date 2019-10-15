package cz.tacr.elza.bulkaction.generator.multiple;

/**
 * Typ akce pro vícenásobnou hromadnou akci.
 *
 * @author Martin Šlapa
 * @since 29.06.2016
 */
public enum  ActionType {

    /**
     * Rozsah datace
     */
    DATE_RANGE,

    /**
     * Počet JP
     */
    NODE_COUNT,

    /**
     * Agregace textových hodnot
     */
    TEXT_AGGREGATION,

    /**
     * DISTINCT tabulka hodnot
     */
    TABLE_STATISTIC,

    /**
     * Kopie prvků archivního popisu
     */
    COPY,

    /**
     * Počet EJ
     */
    UNIT_COUNT,

    /**
     * Odstranění hodnot
     */
    DELETE_ITEM,

}
