package cz.tacr.elza.domain;

import javax.persistence.*;

/**
 * Prvek popisu pro přístupové body.
 * Prvek popisu je abstraktní a z něj jsou odvozeny jeho jednotlivé varianty.
 *
 * @since 17.07.2018
 */
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@Table(name = "ap_item")
public class ApItem implements Item {

    public static final String PART_ID = "partId";
    public static final String PART = "part";
    public static final String DELETE_CHANGE_ID = "deleteChangeId";
    public static final String FIELD_DATA = "data";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    protected Integer itemId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    protected ApChange createChange;

    @Column(name = "createChangeId", nullable = false, updatable = false, insertable = false)
    protected Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "deleteChangeId")
    protected ApChange deleteChange;

    @Column(name = "deleteChangeId", updatable = false, insertable = false)
    protected Integer deleteChangeId;

    @Column(nullable = false)
    protected Integer objectId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "itemTypeId", nullable = false)
    protected RulItemType itemType;

    @Column(name = "itemTypeId", nullable = false, updatable = false, insertable = false)
    protected Integer itemTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulItemSpec.class)
    @JoinColumn(name = "itemSpecId")
    protected RulItemSpec itemSpec;

    @Column(name = "itemSpecId", updatable = false, insertable = false)
    protected Integer itemSpecId;

    @Column(nullable = false)
    protected Integer position;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrData.class)
    @JoinColumn(name = "dataId")
    protected ArrData data;

    @Column(name = "dataId", updatable = false, insertable = false)
    protected Integer dataId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApPart.class)
    @JoinColumn(name = "partId", nullable = false)
    protected ApPart part;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer partId;

    public ApItem() {

    }

    public ApItem(final ApItem other) {
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
    }

    public ApItem copy() {
        return new ApItem(this);
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
    }

    public ApChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(final ApChange deleteChange) {
        this.deleteChange = deleteChange;
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

    public ApPart getPart() {
        return part;
    }

    public void setPart(ApPart part) {
        this.part = part;
        this.partId = part != null ? part.getPartId() : null;
    }

    public Integer getPartId() {
        return partId;
    }

    public boolean isUndefined() {
        return data == null;
    }
}
