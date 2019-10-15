package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ApExternalId;

public class ApExternalIdVO {

    private Integer typeId;
    
    private String value;

    public Integer getTypeId() {
        return typeId;
    }

    public void setTypeId(Integer typeId) {
        this.typeId = typeId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    /**
     * Creates value object from AP external id.
     */
    public static ApExternalIdVO newInstance(ApExternalId src) {
        ApExternalIdVO vo = new ApExternalIdVO();
        vo.setTypeId(src.getExternalIdTypeId());
        vo.setValue(src.getValue());
        return vo;
    }
}
