package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.RulPartType;

public class RulPartTypeVO extends BaseCodeVo {

    private Integer childPartId;
    private Boolean repeatable;

    public Integer getChildPartId() {
        return childPartId;
    }

    public void setChildPartId(Integer childPartId) {
        this.childPartId = childPartId;
    }

    public Boolean getRepeatable() {
        return repeatable;
    }

    public void setRepeatable(final Boolean repeatable) {
        this.repeatable = repeatable;
    }

    public static RulPartTypeVO newInstance(final RulPartType partType) {
    	RulPartTypeVO result = new RulPartTypeVO();
    	result.setId(partType.getPartTypeId());
    	result.setName(partType.getName());
    	result.setCode(partType.getCode());
    	result.setChildPartId(partType.getChildPart() != null? partType.getChildPart().getPartTypeId() : null);
    	result.setRepeatable(partType.getRepeatable());
    	return result;
    }
}
