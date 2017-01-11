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
 * Implementace {@link cz.tacr.elza.api.ArrBulkActionNode}
 *
 * @author Martin Šlapa
 * @since 04.04.2016
 */
@Entity(name = "arr_bulk_action_node")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrBulkActionNode implements Serializable {

    @Id
    @GeneratedValue
    private Integer bulkActionNodeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrBulkActionRun.class)
    @JoinColumn(name = "bulkActionRunId", nullable = false)
    private ArrBulkActionRun bulkActionRun;

    public Integer getBulkActionNodeId() {
        return bulkActionNodeId;
    }

    public void setBulkActionNodeId(final Integer bulkActionNodeId) {
        this.bulkActionNodeId = bulkActionNodeId;
    }

    public ArrNode getNode() {
        return node;
    }

    public void setNode(final ArrNode node) {
        this.node = node;
    }

    public ArrBulkActionRun getBulkActionRun() {
        return bulkActionRun;
    }

    public void setBulkActionRun(final ArrBulkActionRun bulkActionRun) {
        this.bulkActionRun = bulkActionRun;
    }
}
