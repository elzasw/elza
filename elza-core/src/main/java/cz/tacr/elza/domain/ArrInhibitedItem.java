package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "arr_inhibited_item")
public class ArrInhibitedItem {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer inhibitedItemId;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
	@JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @Column(name = "nodeId", updatable = false, insertable = false)
    private Integer nodeId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrDescItem.class)
    @JoinColumn(name = "desc_item_id", nullable = false)
    private ArrDescItem descItem;

    @Column(name = "desc_item_id", updatable = false, insertable = false)
    private Integer descItemId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @Column(name = "createChangeId", nullable = false, updatable = false, insertable = false)
    private Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "deleteChangeId", nullable = true)
    private ArrChange deleteChange;

    @Column(name = "deleteChangeId", nullable = true, updatable = false, insertable = false)
    private Integer deleteChangeId;

	public Integer getInhibitedItemId() {
		return inhibitedItemId;
	}

	public void setInhibitedItemId(Integer inhibitedItemId) {
		this.inhibitedItemId = inhibitedItemId;
	}

	public ArrNode getNode() {
		return node;
	}

	public void setNode(ArrNode node) {
		this.node = node;
		this.nodeId = node != null ? node.getNodeId() : null;
	}

	public Integer getNodeId() {
		return nodeId;
	}

	public ArrDescItem getDescItem() {
		return descItem;
	}

	public void setDescItem(final ArrDescItem descItem) {
		this.descItem = descItem;
		this.descItemId = descItem != null ? descItem.getItemId() : null;
	}

	public Integer getDescItemId() {
		return descItemId;
	}

	public ArrChange getCreateChange() {
		return createChange;
	}

	public void setCreateChange(ArrChange createChange) {
		this.createChange = createChange;
		this.createChangeId = createChange != null ? createChange.getChangeId() : null;
	}

	public Integer getCreateChangeId() {
		return createChangeId;
	}

	public ArrChange getDeleteChange() {
		return deleteChange;
	}

	public void setDeleteChange(ArrChange deleteChange) {
		this.deleteChange = deleteChange;
		this.deleteChangeId = deleteChange != null ? deleteChange.getChangeId() : null;
	}

	public Integer getDeleteChangeId() {
		return deleteChangeId;
	}
}
