package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.service.importnodes.vo.NodeExtension;

/**
 * Přiřazení rozšížení k JP.
 */
@Entity(name = "arr_node_extension")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrNodeExtension implements NodeExtension {

    public static final String TABLE_NAME = "arr_node_extension";

    public static final String FIELD_CREATE_CHANGE = "createChange";
    public static final String FIELD_CREATE_CHANGE_ID = "createChangeId";

    public static final String FIELD_DELETE_CHANGE = "deleteChange";
    public static final String FIELD_DELETE_CHANGE_ID = "deleteChangeId";

    public static final String FIELD_NODE = "node";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer nodeExtensionId;

    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrNode.class)
    @JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @Column(name = "nodeId", updatable = false, insertable = false)
    private Integer nodeId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_CREATE_CHANGE_ID, nullable = false)
    private ArrChange createChange;

    @Column(name = FIELD_CREATE_CHANGE_ID, updatable = false, insertable = false)
    private Integer createChangeId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_DELETE_CHANGE_ID)
    private ArrChange deleteChange;

    @Column(name = FIELD_DELETE_CHANGE_ID, updatable = false, insertable = false)
    private Integer deleteChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulArrangementExtension.class)
    @JoinColumn(name = "arrangementExtensionId", nullable = false)
    private RulArrangementExtension arrangementExtension;

    @Column(name = "arrangementExtensionId", updatable = false, insertable = false)
    private Integer arrangementExtensionId;

    public Integer getNodeExtensionId() {
        return nodeExtensionId;
    }

    public void setNodeExtensionId(final Integer nodeExtensionId) {
        this.nodeExtensionId = nodeExtensionId;
    }

    public ArrNode getNode() {
        return node;
    }

    public void setNode(final ArrNode node) {
        this.node = node;
        this.nodeId = node != null ? node.getNodeId() : null;
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

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(final Integer nodeId) {
        this.nodeId = nodeId;
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

    public RulArrangementExtension getArrangementExtension() {
        return arrangementExtension;
    }

    public void setArrangementExtension(final RulArrangementExtension arrangementExtension) {
        this.arrangementExtension = arrangementExtension;
        this.arrangementExtensionId = arrangementExtension == null ? null : arrangementExtension.getArrangementExtensionId();
    }

    public Integer getArrangementExtensionId() {
        return arrangementExtensionId;
    }

    public void setArrangementExtension(final RulArrangementExtension arrangementExtension,
                                        final Integer arrangementExtensionId) {
        this.arrangementExtension = arrangementExtension;
        this.arrangementExtensionId = arrangementExtensionId;
    }

    public void setCreateChange(final ArrChange createChange,
                                final Integer createChangeId) {
        this.createChange = createChange;
        this.createChangeId = createChangeId;

    }
}
