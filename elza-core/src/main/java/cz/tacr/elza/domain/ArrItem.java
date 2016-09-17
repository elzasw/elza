package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.NumericField;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


/**
 * Implementace {@link cz.tacr.elza.api.ArrItem}
 *
 * @author Martin Šlapa
 * @since 19.06.2016
 */
@Entity(name = "arr_item")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Inheritance(strategy = InheritanceType.JOINED)
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class ArrItem<T extends ArrItemData> implements cz.tacr.elza.api.ArrItem<T, ArrChange, RulItemType, RulItemSpec> {

    public static final String ITEM_SPEC = "itemSpec";
    public static final String ITEM_TYPE = "itemType";

    public ArrItem() {

    }

    public ArrItem(final Class<T> clazz) throws IllegalAccessException, InstantiationException {
        this.item = clazz.newInstance();
    }

    public ArrItem(final T item) {
        this.item = item;
    }

    @Transient
    protected T item;

    public void setItem(final T item) {
        this.item = item;
    }

    @Id
    @GeneratedValue
    private Integer itemId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ArrChange createChange;

    @Column(name = "createChangeId", nullable = false, updatable = false, insertable = false)
    private Integer createChangeId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "deleteChangeId", nullable = true)
    private ArrChange deleteChange;

    @Column(name = "deleteChangeId", nullable = true, updatable = false, insertable = false)
    private Integer deleteChangeId;

    @Column(nullable = false)
    private Integer descItemObjectId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = RulItemType.class)
    @JoinColumn(name = "itemTypeId", nullable = false)
    private RulItemType itemType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = RulItemSpec.class)
    @JoinColumn(name = "itemSpecId", nullable = true)
    private RulItemSpec itemSpec;

    @Column(nullable = false)
    private Integer position;

    @Field
    @NumericField
    public Integer getCreateChangeId() {
        return createChangeId;
    }

    @Field
    @NumericField
    public Integer getDeleteChangeId() {
        return deleteChangeId == null ? Integer.MAX_VALUE : deleteChangeId;
    }

    @Override
    public Integer getItemId() {
        return itemId;
    }

    @Override
    public void setItemId(final Integer itemId) {
        this.itemId = itemId;
    }

    @Override
    public ArrChange getCreateChange() {
        return createChange;
    }

    @Override
    public void setCreateChange(final ArrChange createChange) {
        this.createChange = createChange;
    }

    @Override
    public ArrChange getDeleteChange() {
        return deleteChange;
    }

    @Override
    public void setDeleteChange(final ArrChange deleteChange) {
        this.deleteChange = deleteChange;
        if (deleteChange != null) {
            deleteChangeId = deleteChange.getChangeId();
        }
    }

    @Override
    public Integer getDescItemObjectId() {
        return descItemObjectId;
    }

    @Override
    public void setDescItemObjectId(final Integer descItemObjectId) {
        this.descItemObjectId = descItemObjectId;
    }

    @Override
    public Integer getPosition() {
        return position;
    }

    @Override
    public void setPosition(final Integer position) {
        this.position = position;
    }

    @Override
    public RulItemType getItemType() {
        return itemType;
    }

    @Override
    public void setItemType(final RulItemType itemType) {
        this.itemType = itemType;
    }

    @Override
    public RulItemSpec getItemSpec() {
        return itemSpec;
    }

    @Override
    public void setItemSpec(final RulItemSpec itemSpec) {
        this.itemSpec = itemSpec;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ArrItem)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        if (getItemId() == null) {
            return false;
        }

        ArrItem other = (ArrItem) obj;

        return new EqualsBuilder().append(itemId, other.getItemId()).isEquals();
    }

    @Override
    public String toString() {
        return "ArrItem pk=" + itemId;
    }

    public abstract Integer getNodeId();

    public abstract Integer getFundId();

    public abstract ArrNode getNode();

    public abstract ArrOutputDefinition getOutputDefinition();

    @Override
    public T getItem() {
        return item;
    }
}
