package cz.tacr.elza.controller.vo.nodes.descitems;

import javax.persistence.EntityManager;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItem;


/**
 * Abstraktní VO hodnoty atributu.
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class ArrItemVO {

    /**
     * identifikátor
     */
    private Integer id;

    /**
     * identifikátor hodnoty atributu
     */
    private Integer descItemObjectId;

    /**
     * pozice
     */
    private Integer position;

    /**
     * nezjištěný (bez hodnoty)
     */
    private Boolean undefined;

    /**
     * typ atributu
     */
    private Integer itemTypeId;

    /**
     * specifikace atributu
     */
    private Integer descItemSpecId;

    /**
     * Příznak, zda je hodnota readonly
     */
    private Boolean readOnly;

    public ArrItemVO() {

    }

    public ArrItemVO(ArrItem item) {
        this.id = item.getItemId();
        this.descItemObjectId = item.getDescItemObjectId();
        this.position = item.getPosition();
        this.undefined = (item.getData() == null);
        this.itemTypeId = item.getItemTypeId();
        this.descItemSpecId = item.getItemSpecId();
        this.readOnly = item.getReadOnly();
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getDescItemObjectId() {
        return descItemObjectId;
    }

    public void setDescItemObjectId(final Integer descItemObjectId) {
        this.descItemObjectId = descItemObjectId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(final Integer position) {
        this.position = position;
    }

    public Integer getDescItemSpecId() {
        return descItemSpecId;
    }

    public void setDescItemSpecId(final Integer descItemSpecId) {
        this.descItemSpecId = descItemSpecId;
    }

    public Integer getItemTypeId() {
        return itemTypeId;
    }

    public void setItemTypeId(final Integer itemTypeId) {
        this.itemTypeId = itemTypeId;
    }

    public Boolean getUndefined() {
        return undefined;
    }

    public void setUndefined(final Boolean undefined) {
        this.undefined = undefined;
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    /**
     * Fill correspoding ArrDescItem with values from this object
     *
     * @param descItem
     *            target item to be filled
     */
	public void fill(ArrDescItem descItem) {
		descItem.setItemId(id);
		descItem.setDescItemObjectId(descItemObjectId);
		descItem.setPosition(position);
        descItem.setReadOnly(readOnly != null ? readOnly : false);
		//spec id cannot be set explicitly
		//descItem.setItemSpecId(descItemSpecId);
	}

    /**
     * Create data entity from value object
     *
     * @return
     */
    public abstract ArrData createDataEntity(EntityManager em);

    /**
     * Check if value is undefined
     * 
     * @return Return true if undefined flag is set to TRUE
     */
    public boolean isUndefined() {
        return (Boolean.TRUE.equals(undefined));
    }
}
