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

import cz.tacr.elza.service.cache.NodeCacheSerializable;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.NumericField;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;


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

    public static final String ITEM_SPEC = "itemSpec";
    public static final String ITEM_TYPE = "itemType";

    public ArrItem() {

    }

    public ArrItem(final Class<? extends ArrItemData> clazz) throws IllegalAccessException, InstantiationException {
        this.item = clazz.newInstance();
    }

    public ArrItem(final ArrItemData item) {
        this.item = item;
    }

    @Transient
    protected ArrItemData item;

    public void setItem(final ArrItemData item) {
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

    @Column(name = "itemTypeId", nullable = false, updatable = false, insertable = false)
    private Integer itemTypeId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = RulItemSpec.class)
    @JoinColumn(name = "itemSpecId")
    private RulItemSpec itemSpec;

    @Column(name = "itemSpecId", updatable = false, insertable = false)
    private Integer itemSpecId;

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

    public ArrItemData getItem() {
        return item;
    }

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
}
