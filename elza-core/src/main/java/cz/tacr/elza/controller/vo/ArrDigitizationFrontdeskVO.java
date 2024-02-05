package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrDigitizationFrontdesk;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí systém - seznam digitalizačních linek.
 *
 */
public class ArrDigitizationFrontdeskVO extends SysExternalSystemVO {

    @Override
    public SysExternalSystem createEntity(ApScope scope) {
        ArrDigitizationFrontdesk entity = new ArrDigitizationFrontdesk();
        this.fillEntity(entity);

        return entity;
    }

	public static ArrDigitizationFrontdeskVO newInstance(ArrDigitizationFrontdesk src) {
		ArrDigitizationFrontdeskVO vo = new ArrDigitizationFrontdeskVO();
        // BaseCodeVo
        vo.setId(src.getExternalSystemId());
        vo.setCode(src.getCode());
        vo.setName(src.getName());
        // SysExternalSystemVO
        vo.setUrl(src.getUrl());
        vo.setUsername(src.getUsername());
        vo.setPassword(src.getPassword());
        vo.setElzaCode(src.getElzaCode());
        vo.setApiKeyId(src.getApiKeyId());
        vo.setApiKeyValue(src.getApiKeyValue());
    	return vo;
	}

}
