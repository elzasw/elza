package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.DigitalArchiveExternalSystem;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí digitální archiv.
 */
public class DigitalArchiveExternalSystemVO extends SysExternalSystemVO {

    @Override
    public SysExternalSystem createEntity(ApScope scope) {
        DigitalArchiveExternalSystem entity = new DigitalArchiveExternalSystem();
        this.fillEntity(entity);
        return entity;
    }

    /**
     * Creates value object from AP external system.
     */
    public static DigitalArchiveExternalSystemVO newInstance(DigitalArchiveExternalSystem src) {
        DigitalArchiveExternalSystemVO vo = new DigitalArchiveExternalSystemVO();
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
