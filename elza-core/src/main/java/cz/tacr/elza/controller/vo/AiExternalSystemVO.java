package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.AiExternalSystem;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí AI systém (AI provider).
 */
public class AiExternalSystemVO extends SysExternalSystemVO {

    @Override
    public SysExternalSystem createEntity(ApScope scope) {
        AiExternalSystem entity = new AiExternalSystem();
        this.fillEntity(entity);
        return entity;
    }

    /**
     * Creates value object from AI external system.
     */
    public static AiExternalSystemVO newInstance(AiExternalSystem src) {
        AiExternalSystemVO vo = new AiExternalSystemVO();
        vo.setCode(src.getCode());
        vo.setElzaCode(src.getElzaCode());
        vo.setId(src.getExternalSystemId());
        vo.setName(src.getName());
        vo.setPassword(src.getPassword());
        vo.setUrl(src.getUrl());
        vo.setUsername(src.getUsername());
        vo.setApiKeyId(src.getApiKeyId());
        vo.setApiKeyValue(src.getApiKeyValue());
        return vo;
    }
}
