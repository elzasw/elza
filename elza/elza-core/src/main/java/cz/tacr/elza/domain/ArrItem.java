package cz.tacr.elza.domain;

import java.beans.Transient;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.service.cache.NodeCacheSerializable;

/**
 * Abstraktní prvek popisu
 *
 */
@Entity(name = "arr_item")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Inheritance(strategy = InheritanceType.JOINED)
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
public abstract class ArrItem implements NodeCacheSerializable, Item {

    public static final String TABLE_NAME = "arr_item";

    public static final String FIELD_DATA = "data";
    public static final String FIELD_ITEM_SPEC = "itemSpec";
    public static final String FIELD_ITEM_TYPE = "itemType";
    public static final String FIELD_POSITION = "position";
    public static final String FIELD_CREATE_CHANGE_ID = "createChangeId";
    public static final String FIELD_DELETE_CHANGE_ID = "deleteChangeId";
    public static final String FIELD_READ_ONLY = "readOnly";
    public static final String DELETE_CHANGE_ID_MAX = "" + Integer.MAX_VALUE;

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
	protected Integer itemId;

    @JsonIgnore
    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_CREATE_CHANGE_ID, nullable = false)
	protected ArrChange createChange;

    @Column(name = FIELD_CREATE_CHANGE_ID, nullable = false, updatable = false, insertable = false)
	protected Integer createChangeId;

    @JsonIgnore
    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrChange.class)
    @JoinColumn(name = FIELD_DELETE_CHANGE_ID, nullable = true)
	protected ArrChange deleteChange;

    @Column(name = FIELD_DELETE_CHANGE_ID, nullable = true, updatable = false, insertable = false)
	protected Integer deleteChangeId;

    @Column(nullable = false)
	protected Integer descItemObjectId;

    @JsonIgnore
    @RestResource(exported = false)
	@ManyToOne(fetch=FetchType.LAZY, targetEntity = RulItemType.class)
    @JoinColumn(name = "itemTypeId", nullable = false)
	protected RulItemType itemType;

    @Column(name = "itemTypeId", nullable = false, updatable = false, insertable = false)
	protected Integer itemTypeId;

    @JsonIgnore
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

    @Column(name = "dataId", nullable = false, updatable = false, insertable = false)
    private Integer dataId;

    @Column(name = FIELD_READ_ONLY, nullable = false)
    private Boolean readOnly = false;

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
		this.readOnly = src.readOnly;
	}

    @GenericField
    public Integer getCreateChangeId() {
        return createChangeId;
    }

    @GenericField(indexNullAs = DELETE_CHANGE_ID_MAX)
    public Integer getDeleteChangeId() {
        return deleteChangeId;
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

    /**
     * Setter which will not touch ID in createChange
     *
     * @param createChange
     * @param createChangeId
     */
    public void setCreateChange(final ArrChange createChange,
                                final Integer createChangeId) {
        this.createChange = createChange;
        this.createChangeId = createChangeId;

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

    public Integer getItemTypeId() {
        if (itemTypeId == null) {
            if (itemType != null) {
                return itemType.getItemTypeId();
            }
        }
        return itemTypeId;
    }

    public void setItemTypeId(final Integer itemTypeId) {
        this.itemTypeId = itemTypeId;
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

    public Integer getItemSpecId() {
        if (itemSpecId == null) {
            if (itemSpec != null) {
                return itemSpec.getItemSpecId();
            }
        }
        return itemSpecId;
    }

    public void setItemSpecId(final Integer itemSpecId) {
        this.itemSpecId = itemSpecId;
    }

    @Override
    public String toString() {
        return "ArrItem pk=" + itemId;
    }

    public abstract Integer getNodeId();

    public abstract Integer getFundId();

    public abstract ArrNode getNode();

    public abstract ArrOutput getOutput();

    public void setCreateChangeId(final Integer createChangeId) {
        this.createChangeId = createChangeId;
    }

    public void setDeleteChangeId(final Integer deleteChangeId) {
        this.deleteChangeId = deleteChangeId;
    }

    public ArrData getData() {
        return data;
    }

    public void setData(final ArrData data) {
        this.data = data;
    }

    @JsonIgnore
    public boolean isUndefined() {
        return data == null;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
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
