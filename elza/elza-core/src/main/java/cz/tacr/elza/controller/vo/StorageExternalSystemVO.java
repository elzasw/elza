package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.StorageSystemType;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.StorageExternalSystem;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí úložiště.
 */
public class StorageExternalSystemVO extends SysExternalSystemVO {

    private StorageSystemType type;

    public StorageSystemType getType() {
        return type;
    }

    public void setType(StorageSystemType type) {
        this.type = type;
    }

    @Override
    public SysExternalSystem createEntity(ApScope scope) {
        StorageExternalSystem entity = new StorageExternalSystem();
        entity.setType(type);
        this.fillEntity(entity);
        return entity;
    }

    /**
     * Creates value object from AP external system.
     */
    public static StorageExternalSystemVO newInstance(StorageExternalSystem src) {
        StorageExternalSystemVO vo = new StorageExternalSystemVO();
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
