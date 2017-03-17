package cz.tacr.elza.domain;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Vazba nad kterými hromadná akce byla spuštěna - odkaz na root node podstromu.
 *
 * @author Martin Šlapa
 * @since 04.04.2016
 */
@Entity(name = "arr_bulk_action_node")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrBulkActionNode {

    @Id
    @GeneratedValue
    private Integer bulkActionNodeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrBulkActionRun.class)
    @JoinColumn(name = "bulkActionRunId", nullable = false)
    private ArrBulkActionRun bulkActionRun;

    /**
     * @return identifikátor entity
     */
    public Integer getBulkActionNodeId() {
        return bulkActionNodeId;
    }

    /**
     * @param bulkActionNodeId identifikátor entity
     */
    public void setBulkActionNodeId(final Integer bulkActionNodeId) {
        this.bulkActionNodeId = bulkActionNodeId;
    }

    /**
     * @return vazba na root podstromu
     */
    public ArrNode getNode() {
        return node;
    }

    /**
     * @param node vazba na root podstromu
     */
    public void setNode(final ArrNode node) {
        this.node = node;
    }

    /**
     * @return odkaz na dokončenou hromadnou akci
     */
    public ArrBulkActionRun getBulkActionRun() {
        return bulkActionRun;
    }

    /**
     * @param bulkActionRun odkaz na dokončenou hromadnou akci
     */
    public void setBulkActionRun(final ArrBulkActionRun bulkActionRun) {
        this.bulkActionRun = bulkActionRun;
    }
}
