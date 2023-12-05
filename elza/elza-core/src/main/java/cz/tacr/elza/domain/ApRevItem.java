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
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Item in the revision
 *
 * New item has valid value in objectId.
 * Updated item has valid value in origObjectId
 */
@Entity
@Table(name = "ap_rev_item")
@JsonIgnoreProperties(ignoreUnknown = true, value = { "hibernateLazyInitializer", "handler" })
public class ApRevItem implements AccessPointItem {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    protected Integer itemId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "create_change_id", nullable = false)
    protected ApChange createChange;

    @Column(name = "create_change_id", nullable = false, updatable = false, insertable = false)
    protected Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "delete_change_id")
    protected ApChange deleteChange;

    @Column(name = "delete_change_id", updatable = false, insertable = false)
    protected Integer deleteChangeId;

    /**
     * ID of new item
     */
    @Column
    protected Integer objectId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "item_type_id", nullable = false)
    protected RulItemType itemType;

    @Column(name = "item_type_id", nullable = false, updatable = false, insertable = false)
    protected Integer itemTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemSpec.class)
    @JoinColumn(name = "item_spec_id")
    protected RulItemSpec itemSpec;

    @Column(name = "item_spec_id", updatable = false, insertable = false)
    protected Integer itemSpecId;

    @Column(nullable = false)
    protected Integer position;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrData.class)
    @JoinColumn(name = "data_id")
    protected ArrData data;

    @Column(name = "data_id", updatable = false, insertable = false)
    protected Integer dataId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApRevPart.class)
    @JoinColumn(name = "part_id", nullable = false)
    protected ApRevPart part;

    @Column(name = "part_id", nullable = false, updatable = false, insertable = false)
    private Integer partId;

    /**
     * ID of original object (@see ApItem)
     */
    @Column
    private Integer origObjectId;

    @Column(name = "deleted", nullable = false)
    private Boolean deleted;

    public ApRevItem() {

    }

    public ApRevItem(final ApRevItem other) {
        this.createChange = other.createChange;
        this.createChangeId = other.createChangeId;
        this.deleteChange = other.deleteChange;
        this.deleteChangeId = other.deleteChangeId;
        this.objectId = other.objectId;
        this.itemType = other.itemType;
        this.itemTypeId = other.itemTypeId;
        this.itemSpec = other.itemSpec;
        this.itemSpecId = other.itemSpecId;
        this.position = other.position;
        this.data = other.data;
        this.dataId = other.dataId;
        this.part = other.part;
        this.partId = other.partId;
        this.origObjectId = other.origObjectId;
        this.deleted = other.deleted;
    }

    public ApRevItem copy() {
        return new ApRevItem(this);
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(final Integer itemId) {
        this.itemId = itemId;
    }

    public ApChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(final ApChange createChange) {
        this.createChange = createChange;
        this.createChangeId = createChange != null ? createChange.getChangeId() : null;
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public ApChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(final ApChange deleteChange) {
        this.deleteChange = deleteChange;
        this.deleteChangeId = deleteChange != null ? deleteChange.getChangeId() : null;
    }

    public Integer getObjectId() {
        return objectId;
    }

    public void setObjectId(final Integer objectId) {
        this.objectId = objectId;
    }

    public RulItemType getItemType() {
        return itemType;
    }

    public Integer getItemTypeId() {
        return itemTypeId;
    }

    public void setItemType(final RulItemType itemType) {
        this.itemType = itemType;
        this.itemTypeId = itemType == null ? null : itemType.getItemTypeId();
    }

    public RulItemSpec getItemSpec() {
        return itemSpec;
    }

    public void setItemSpec(final RulItemSpec itemSpec) {
        this.itemSpec = itemSpec;
        this.itemSpecId = itemSpec == null ? null : itemSpec.getItemSpecId();
    }

    public Integer getItemSpecId() {
        return itemSpecId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(final Integer position) {
        this.position = position;
    }

    public ArrData getData() {
        return data;
    }

    public void setData(final ArrData data) {
        this.data = data;
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }

    public ApRevPart getPart() {
        return part;
    }

    public void setPart(ApRevPart part) {
        this.part = part;
        this.partId = part != null ? part.getPartId() : null;
    }

    public Integer getPartId() {
        if (partId != null) {
            return partId;
        } else if (part != null) {
            return part.getPartId();
        } else {
            return null;
        }
    }

    public Integer getOrigObjectId() {
        return origObjectId;
    }

    public void setOrigObjectId(Integer origObjectId) {
        this.origObjectId = origObjectId;
    }

    public Boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
