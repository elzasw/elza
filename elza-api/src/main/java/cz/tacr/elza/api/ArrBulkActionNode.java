package cz.tacr.elza.api;

/**
 * Vazba nad kterými hromadná akce byla spuštěna - odkaz na root node podstromu
 *
 * @author Martin Šlapa
 * @since 04.04.2016
 */
public interface ArrBulkActionNode<N extends ArrNode, BAR extends ArrBulkActionRun> {

    /**
     * @return identifikátor entity
     */
    Integer getBulkActionNodeId();

    /**
     * @param bulkActionNodeId identifikátor entity
     */
    void setBulkActionNodeId(Integer bulkActionNodeId);

    /**
     * @return vazba na root podstromu
     */
    N getNode();

    /**
     * @param node vazba na root podstromu
     */
    void setNode(N node);

    /**
     * @return odkaz na dokončenou hromadnou akci
     */
    BAR getBulkActionRun();

    /**
     * @param bulkActionRun odkaz na dokončenou hromadnou akci
     */
    void setBulkActionRun(BAR bulkActionRun);
}
