package cz.tacr.elza.domain;

import javax.persistence.*;

/**
 * Prvek popisu pro přístupové body.
 * Prvek popisu je abstraktní a z něj jsou odvozeny jeho jednotlivé varianty.
 *
 * @since 17.07.2018
 */
@Entity
@Table(name = "ap_item")
public abstract class ApItem {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    protected Integer itemId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    protected ArrChange createChange;

    @Column(name = "createChangeId", nullable = false, updatable = false, insertable = false)
    protected Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "deleteChangeId")
    protected ArrChange deleteChange;

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

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(final Integer itemId) {
        this.itemId = itemId;
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

    public Integer getObjectId() {
        return objectId;
    }

    public void setObjectId(final Integer objectId) {
        this.objectId = objectId;
    }

    public RulItemType getItemType() {
        return itemType;
    }

    public void setItemType(final RulItemType itemType) {
        this.itemType = itemType;
    }

    public RulItemSpec getItemSpec() {
        return itemSpec;
    }

    public void setItemSpec(final RulItemSpec itemSpec) {
        this.itemSpec = itemSpec;
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
}
