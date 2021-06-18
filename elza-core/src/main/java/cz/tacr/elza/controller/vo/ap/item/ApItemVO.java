package cz.tacr.elza.controller.vo.ap.item;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;

import java.util.Objects;

import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;

/**
 * @since 18.07.2018
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, property = "@class")
public abstract class ApItemVO {

    /**
     * identifikátor
     */
    private Integer id;

    /**
     * identifikátor hodnoty atributu
     */
    private Integer objectId;

    /**
     * pozice
     */
    private Integer position;

    /**
     * typ atributu
     */
    private Integer typeId;

    /**
     * specifikace atributu
     */
    private Integer specId;

    public ApItemVO() {
    }

    public ApItemVO(final ApItem item) {
        id = item.getItemId();
        objectId = item.getObjectId();
        position = item.getPosition();
        specId = item.getItemSpecId();
        typeId = item.getItemTypeId();
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getObjectId() {
        return objectId;
    }

    public void setObjectId(final Integer objectId) {
        this.objectId = objectId;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(final Integer position) {
        this.position = position;
    }

    public Integer getSpecId() {
        return specId;
    }

    public void setSpecId(final Integer specId) {
        this.specId = specId;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(final Integer typeId) {
        this.typeId = typeId;
    }

    /**
     * Fill correspoding Aptem with values from this object
     *
     * @param item target item to be filled
     */
    public void fill(ApItem item) {
        item.setItemId(id);
        item.setObjectId(objectId);
        item.setPosition(position);
        //spec id cannot be set explicitly
        //item.setItemSpecId(specId);
    }

    /**
     * Create data entity from value object
     *
     * @param em
     * @return data
     */
    public abstract ArrData createDataEntity(EntityManager em);

    /**
     * Comparison of values ApItemVO and ApItem
     * 
     * @param apItem
     * @return true if equals
     */
    public abstract boolean equalsValue(ApItem apItem);

    /**
     * Comparison of base fields: position, typeId, specId
     * 
     * @param apItem
     * @return true if equals in three fields
     */
    public boolean equalsBase(@NotNull ApItem apItem) {
        return Objects.equals(position, apItem.getPosition()) 
                && Objects.equals(typeId, apItem.getItemTypeId())
                && Objects.equals(specId, apItem.getItemSpecId());
    }
}
