package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.service.importnodes.vo.NodeRegister;

/**
 * Přiřazení rejstříkových hesel k jednotce archivního popisu.
 */
@Entity(name = "arr_node_register")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ArrNodeRegister implements NodeRegister {

    public static final String TABLE_NAME = "arr_node_register";

    public static final String FIELD_NODE_ID = "nodeId";
    public static final String FIELD_NODE = "node";
    public static final String FIELD_RECORD = "record";
    public static final String FIELD_CREATE_CHANGE_ID = "createChangeId";
    public static final String FIELD_DELETE_CHANGE = "deleteChange";
    public static final String FIELD_DELETE_CHANGE_ID = "deleteChangeId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer nodeRegisterId;

    @RestResource(exported = false)
	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @Column(name = "nodeId", updatable = false, insertable = false)
    private Integer nodeId;

    @RestResource(exported = false)
	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "recordId", nullable = false)
    private ApAccessPoint record;

    @Column(name = "recordId", updatable = false, insertable = false)
    private Integer recordId;

    @RestResource(exported = false)
	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_CREATE_CHANGE_ID, nullable = false)
    private ArrChange createChange;

    @Column(name = FIELD_CREATE_CHANGE_ID, updatable = false, insertable = false)
    private Integer createChangeId;

    @RestResource(exported = false)
	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_DELETE_CHANGE_ID, nullable = true)
    private ArrChange deleteChange;

    @Column(name = FIELD_DELETE_CHANGE_ID, updatable = false, insertable = false)
    private Integer deleteChangeId;

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

    public ApAccessPoint getRecord() {
        return record;
    }

    public void setRecord(final ApAccessPoint record) {
        this.record = record;
        this.recordId = record != null ? record.getAccessPointId() : null;
    }

    public ArrChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
        this.createChangeId = createChange != null ? createChange.getChangeId() : null;
    }

    public ArrChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(final ArrChange deleteChange) {
        this.deleteChange = deleteChange;
        this.deleteChangeId = deleteChange != null ? deleteChange.getChangeId() : null;
    }

    @Override
    public String toString() {
        return "ArrNodeRegister pk=" + nodeRegisterId;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
    }

    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(final Integer recordId) {
        this.recordId = recordId;
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public void setCreateChangeId(final Integer createChangeId) {
        this.createChangeId = createChangeId;
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }

    public void setDeleteChangeId(final Integer deleteChangeId) {
        this.deleteChangeId = deleteChangeId;
    }
}
