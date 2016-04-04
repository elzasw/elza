package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Implementace {@link cz.tacr.elza.api.ArrBulkActionNode}
 *
 * @author Martin Šlapa
 * @since 04.04.2016
 */
@Entity(name = "arr_bulk_action_node")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrBulkActionNode implements cz.tacr.elza.api.ArrBulkActionNode<ArrNode, ArrBulkActionRun>, Serializable {

    @Id
    @GeneratedValue
    private Integer bulkActionNodeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrBulkActionRun.class)
    @JoinColumn(name = "bulkActionRunId", nullable = false)
    private ArrBulkActionRun bulkActionRun;

    @Override
    public Integer getBulkActionNodeId() {
        return bulkActionNodeId;
    }

    @Override
    public void setBulkActionNodeId(final Integer bulkActionNodeId) {
        this.bulkActionNodeId = bulkActionNodeId;
    }

    @Override
    public ArrNode getNode() {
        return node;
    }

    @Override
    public void setNode(final ArrNode node) {
        this.node = node;
    }

    @Override
    public ArrBulkActionRun getBulkActionRun() {
        return bulkActionRun;
    }

    @Override
    public void setBulkActionRun(final ArrBulkActionRun bulkActionRun) {
        this.bulkActionRun = bulkActionRun;
    }
}
