package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.GisSystemType;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.GisExternalSystem;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí Gis systém.
 */
public class GisExternalSystemVO extends SysExternalSystemVO {

    private GisSystemType type;

    public GisSystemType getType() {
        return type;
    }

    public void setType(GisSystemType type) {
        this.type = type;
    }

    @Override
    public SysExternalSystem createEntity(ApScope scope) {
        GisExternalSystem entity = new GisExternalSystem();
        entity.setType(type);
        this.fillEntity(entity);
        return entity;
    }

    /**
     * Creates value object from AP external system.
     */
    public static GisExternalSystemVO newInstance(GisExternalSystem src) {
        GisExternalSystemVO vo = new GisExternalSystemVO();
        vo.setCode(src.getCode());
        vo.setElzaCode(src.getElzaCode());
        vo.setId(src.getExternalSystemId());
        vo.setName(src.getName());
        vo.setPassword(src.getPassword());
        vo.setType(src.getType());
        vo.setUrl(src.getUrl());
        vo.setUsername(src.getUsername());
        vo.setApiKeyId(src.getApiKeyId());
        vo.setApiKeyValue(src.getApiKeyValue());
        return vo;
    }
}
