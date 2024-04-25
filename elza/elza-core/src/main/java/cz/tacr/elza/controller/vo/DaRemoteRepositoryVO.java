package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaRemoteRepository;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí digitální archiv.
 */
public class DaRemoteRepositoryVO extends SysExternalSystemVO {

    private Integer digitalRepositoryId;

    public Integer getDigitalRepositoryId() {
        return digitalRepositoryId;
    }

    public void setDigitalRepositoryId(Integer digitalRepositoryId) {
        this.digitalRepositoryId = digitalRepositoryId;
    }

    @Override
    public SysExternalSystem createEntity(ApScope scope, ArrDigitalRepository digitalRepository) {
        DaRemoteRepository entity = new DaRemoteRepository();
        entity.setDigitalRepository(digitalRepository);
        this.fillEntity(entity);
        return entity;
    }

    /**
     * Creates value object from AP external system.
     */
    public static DaRemoteRepositoryVO newInstance(DaRemoteRepository src) {
        DaRemoteRepositoryVO vo = new DaRemoteRepositoryVO();
        vo.setCode(src.getCode());
        vo.setElzaCode(src.getElzaCode());
        vo.setId(src.getExternalSystemId());
        vo.setName(src.getName());
        vo.setPassword(src.getPassword());
        vo.setUrl(src.getUrl());
        vo.setUsername(src.getUsername());
        vo.setApiKeyId(src.getApiKeyId());
        vo.setApiKeyValue(src.getApiKeyValue());
        vo.setDigitalRepositoryId(src.getDigitalRepository().getExternalSystemId());
        return vo;
    }
}
