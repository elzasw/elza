package cz.tacr.elza.controller.vo;

import java.util.List;

public class ApCreateTypeVO {

    /**
     * Identifikátor typu atributu
     */
    private Integer itemTypeId;

    /**
     * RequiredType
     */
    private RequiredType requiredType;

    /**
     * Seznam povolených specifikací typu atributu
     */
    private List<Integer> itemSpecIds = null;

    /**
     * Jedná se o opakovatelný typ prvku popisu?
     */
    private Boolean repeatable;

    public Integer getItemTypeId() {
        return itemTypeId;
    }

    public void setItemTypeId(Integer itemTypeId) {
        this.itemTypeId = itemTypeId;
    }

    public RequiredType getRequiredType() {
        return requiredType;
    }

    public void setRequiredType(RequiredType requiredType) {
        this.requiredType = requiredType;
    }

    public List<Integer> getItemSpecIds() {
        return itemSpecIds;
    }

    public void setItemSpecIds(List<Integer> itemSpecIds) {
        this.itemSpecIds = itemSpecIds;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(Boolean repeatable) {
        this.repeatable = repeatable;
    }
}
