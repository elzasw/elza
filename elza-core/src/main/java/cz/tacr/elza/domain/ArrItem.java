package cz.tacr.elza.domain;

import java.beans.Transient;

import javax.persistence.Access;
import javax.persistence.AccessType;
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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.NumericField;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.service.cache.NodeCacheSerializable;

/**
 * Nadřízená položka.
 *
 * @author Martin Šlapa
 * @since 19.06.2016
 */
@Entity(name = "arr_item")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Inheritance(strategy = InheritanceType.JOINED)
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class ArrItem implements NodeCacheSerializable {

    public static final String DATA = "data";
    public static final String ITEM_SPEC = "itemSpec";
    public static final String ITEM_TYPE = "itemType";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
	protected Integer itemId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
	protected ArrChange createChange;

    @Column(name = "createChangeId", nullable = false, updatable = false, insertable = false)
	protected Integer createChangeId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = "deleteChangeId", nullable = true)
	protected ArrChange deleteChange;

    @Column(name = "deleteChangeId", nullable = true, updatable = false, insertable = false)
	protected Integer deleteChangeId;

    @Column(nullable = false)
	protected Integer descItemObjectId;

    @RestResource(exported = false)
	@ManyToOne(fetch=FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "itemTypeId", nullable = false)
	protected RulItemType itemType;

    @Column(name = "itemTypeId", nullable = false, updatable = false, insertable = false)
	protected Integer itemTypeId;

    @RestResource(exported = false)
	@ManyToOne(fetch=FetchType.LAZY, targetEntity = RulItemSpec.class)
    @JoinColumn(name = "itemSpecId")
	protected RulItemSpec itemSpec;

    @Column(name = "itemSpecId", updatable = false, insertable = false)
	protected Integer itemSpecId;

    @Column(nullable = false)
	protected Integer position;

	@ManyToOne(fetch=FetchType.LAZY, targetEntity = ArrData.class)
    @JoinColumn(name = "dataId")
	protected ArrData data;

	/**
	 * Default constructor
	 */
	protected ArrItem() {

	}

	/**
	 * Copy constructor for ArrItem
	 *
	 * @param src
	 *            Source item
	 */
	public ArrItem(ArrItem src) {
		this.createChange = src.createChange;
		this.createChangeId = src.createChangeId;
		this.data = src.data;
		this.deleteChange = src.deleteChange;
		this.deleteChangeId = src.deleteChangeId;
		this.descItemObjectId = src.descItemObjectId;
		this.itemId = src.itemId;
		this.itemSpec = src.itemSpec;
		this.itemSpecId = src.itemSpecId;
		this.itemType = src.itemType;
		this.itemTypeId = src.itemTypeId;
		this.position = src.position;
	}

	@JsonIgnore
    @Field
    @NumericField
    public Integer getCreateChangeId() {
        return createChangeId;
    }

    @JsonIgnore
    @Field
    @NumericField
    public Integer getDeleteChangeId() {
        return deleteChangeId == null ? Integer.MAX_VALUE : deleteChangeId;
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
        this.createChangeId = createChange == null ? null : createChange.getChangeId();
    }

    public ArrChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(final ArrChange deleteChange) {
        this.deleteChange = deleteChange;
        this.deleteChangeId = deleteChange == null ? null : deleteChange.getChangeId();
    }

    /**
     * @return identifikátor hodnoty atrributu, který se nemění při verzované změně hodnoty.
     */
    public Integer getDescItemObjectId() {
        return descItemObjectId;
    }

    /**
     * Nastaví identifikátor hodnoty atrributu, který se nemění při verzované změně hodnoty.
     *
     * @param descItemObjectId identifikátor hodnoty atrributu, který se nemění při verzované změně hodnoty.
     */
    public void setDescItemObjectId(final Integer descItemObjectId) {
        this.descItemObjectId = descItemObjectId;
    }

    /**
    *
    * @return pořadí atributu v rámci shodného typu a specifikace atributu. U neopakovatelných
    *         atributů bude hodnota vždy 1, u opakovatelných dle skutečnosti.).
    */
    public Integer getPosition() {
        return position;
    }

    /**
     * Nastaví pořadí atributu v rámci shodného typu a specifikace atributu. U neopakovatelných
     * atributů bude hodnota vždy 1, u opakovatelných dle skutečnosti.).
     *
     * @param position pořadí atributu v rámci shodného typu a specifikace atributu.
     */
    public void setPosition(final Integer position) {
        this.position = position;
    }

    /**
    *
    * @return Odkaz na typ atributu.
    */
    public RulItemType getItemType() {
        return itemType;
    }

    /**
     * Nastaví odkaz na typ atributu.
     *
     * @param itemType odkaz na typ atributu.
     */
    public void setItemType(final RulItemType itemType) {
        this.itemType = itemType;
        this.itemTypeId = itemType == null ? null : itemType.getItemTypeId();
    }

    /**
     * @return Odkaz na podtyp atributu.
     */
    public RulItemSpec getItemSpec() {
        return itemSpec;
    }

    /**
     * Nastaví odkaz na podtyp atributu.
     *
     * @param itemSpec odkaz na podtyp atributu.
     */
    public void setItemSpec(final RulItemSpec itemSpec) {
        this.itemSpec = itemSpec;
        this.itemSpecId = itemSpec == null ? null : itemSpec.getItemSpecId();
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

    public void setCreateChangeId(final Integer createChangeId) {
        this.createChangeId = createChangeId;
    }

    public void setDeleteChangeId(final Integer deleteChangeId) {
        this.deleteChangeId = deleteChangeId;
    }

    public Integer getItemTypeId() {
        return itemTypeId;
    }

    public void setItemTypeId(final Integer itemTypeId) {
        this.itemTypeId = itemTypeId;
    }

    public Integer getItemSpecId() {
        return itemSpecId;
    }

    public void setItemSpecId(final Integer itemSpecId) {
        this.itemSpecId = itemSpecId;
    }

    public ArrData getData() {
        return data;
    }

    public void setData(final ArrData data) {
        this.data = data;
    }

    public boolean isUndefined() {
        return data == null;
    }

    /**
     * Prepare copy of the item object
     *
     * Method returns pure item copy of the source object without saving it to
     * the DB
     *
     * @return Return copy of the object
     */
    @Transient
    abstract public ArrItem makeCopy();
}
