package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí systém.
 *
 */
public class ApExternalSystemVO extends SysExternalSystemVO {

    private ApExternalSystemType type;

    public ApExternalSystemType getType() {
        return type;
    }

    public void setType(final ApExternalSystemType type) {
        this.type = type;
    }

    @Override
    public SysExternalSystem createEntity() {
        ApExternalSystem entity = new ApExternalSystem();
        entity.setType(type);
        this.fillEntity(entity);
        return entity;
    }
    
    /**
     * Creates value object from AP external system.
     */
    public static ApExternalSystemVO newInstance(ApExternalSystem src) {
        ApExternalSystemVO vo = new ApExternalSystemVO();
        vo.setCode(src.getCode());
        vo.setElzaCode(src.getElzaCode());
        vo.setId(src.getExternalSystemId());
        vo.setName(src.getName());
        vo.setPassword(src.getPassword());
        vo.setType(src.getType());
        vo.setUrl(src.getUrl());
        vo.setUsername(src.getUsername());
        return vo;
    }
}
