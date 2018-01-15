package cz.tacr.elza.controller.vo.nodes.descitems;

import javax.persistence.EntityManager;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDescItem;


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
     * specifikace atributu
     */
    private Integer descItemSpecId;

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

    public Boolean getUndefined() {
        return undefined;
    }

    public void setUndefined(final Boolean undefined) {
        this.undefined = undefined;
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
		//spec id cannot be set explicitly
		//descItem.setItemSpecId(descItemSpecId);
	}

    /**
     * Create data entity from value object
     * 
     * @return
     */
    public abstract ArrData createDataEntity(EntityManager em);
}
