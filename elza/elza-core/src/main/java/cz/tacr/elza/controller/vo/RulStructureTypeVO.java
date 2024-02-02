package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.RulStructuredType;

/**
 * VO pro typ strukt. typu.
 *
 * @since 10.11.2017
 */
public class RulStructureTypeVO extends BaseCodeVo {

    private Boolean anonymous;

    public Boolean getAnonymous() {
        return anonymous;
    }

    public void setAnonymous(final Boolean anonymous) {
        this.anonymous = anonymous;
    }

    public static RulStructureTypeVO newInstance(final RulStructuredType structureType) {
    	RulStructureTypeVO result = new RulStructureTypeVO();
    	result.setId(structureType.getStructuredTypeId());
    	result.setName(structureType.getName());
    	result.setCode(structureType.getCode());
    	result.setAnonymous(structureType.getAnonymous());
    	return result;
    }
}
