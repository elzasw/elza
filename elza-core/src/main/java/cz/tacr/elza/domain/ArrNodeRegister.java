package cz.tacr.elza.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Přiřazení rejstříkových hesel k jednotce archivního popisu.
 */
@Entity(name = "arr_node_register")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrNodeRegister implements Serializable {

    public static final String NODE_ID = "nodeId";
    public static final String NODE = "node";
    public static final String RECORD = "record";
    public static final String DELETE_CHANGE = "deleteChange";

    @Id
    @GeneratedValue
    private Integer nodeRegisterId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRecord.class)
    @JoinColumn(name = "recordId", nullable = false)
    private RegRecord record;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "deleteChangeId", nullable = true)
    private ArrChange deleteChange;

    @Column(insertable = false, updatable = false)
    @ReadOnlyProperty
    private Integer nodeId;

    public Integer getNodeRegisterId() {
        return nodeRegisterId;
    }

    public void setNodeRegisterId(final Integer nodeRegisterId) {
        this.nodeRegisterId = nodeRegisterId;
    }

    public ArrNode getNode() {
        return node;
    }

    public void setNode(final ArrNode node) {
        this.node = node;
        this.nodeId = node != null ? node.getNodeId() : null;
    }

    public RegRecord getRecord() {
        return record;
    }

    public void setRecord(final RegRecord record) {
        this.record = record;
    }

    public ArrChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
    }

    public ArrChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(final ArrChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    @Override
    public String toString() {
        return "ArrNodeRegister pk=" + nodeRegisterId;
    }

    public Integer getNodeId() {
        return nodeId;
    }
}
