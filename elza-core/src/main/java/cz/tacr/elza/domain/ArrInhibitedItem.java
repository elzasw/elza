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

@Entity(name = "arr_inhibited_item")
public class ArrInhibitedItem {

    public final static String TABLE_NAME = "arr_inhibited_item";

    public final static String FIELD_CREATE_CHANGE_ID = "createChangeId";

    public final static String FIELD_DELETE_CHANGE_ID = "deleteChangeId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer inhibitedItemId;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class)
	@JoinColumn(name = "nodeId", nullable = false)
    private ArrNode node;

    @Column(name = "nodeId", updatable = false, insertable = false)
    private Integer nodeId;

    @Column(nullable = false)
	protected Integer descItemObjectId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_CREATE_CHANGE_ID, nullable = false)
    private ArrChange createChange;

    @Column(name = FIELD_CREATE_CHANGE_ID, nullable = false, updatable = false, insertable = false)
    private Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_DELETE_CHANGE_ID, nullable = true)
    private ArrChange deleteChange;

    @Column(name = FIELD_DELETE_CHANGE_ID, nullable = true, updatable = false, insertable = false)
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

	public Integer getDescItemObjectId() {
		return descItemObjectId;
	}

	public void setDescItemObjectId(Integer descItemObjectId) {
		this.descItemObjectId = descItemObjectId;
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
