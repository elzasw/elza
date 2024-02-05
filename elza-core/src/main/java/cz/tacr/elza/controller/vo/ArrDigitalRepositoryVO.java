package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí systém - uložiště digitalizátů.
 *
 */
public class ArrDigitalRepositoryVO extends SysExternalSystemVO {

    private String viewDaoUrl;

    private String viewFileUrl;

    private String viewThumbnailUrl;

    private Boolean sendNotification;

    public String getViewDaoUrl() {
        return viewDaoUrl;
    }

    public void setViewDaoUrl(final String viewDaoUrl) {
        this.viewDaoUrl = viewDaoUrl;
    }

    public String getViewFileUrl() {
        return viewFileUrl;
    }

    public void setViewFileUrl(final String viewFileUrl) {
        this.viewFileUrl = viewFileUrl;
    }

    public String getViewThumbnailUrl() {
        return viewThumbnailUrl;
    }

    public void setViewThumbnailUrl(String viewThumbnailUrl) {
        this.viewThumbnailUrl = viewThumbnailUrl;
    }

    public Boolean getSendNotification() {
        return sendNotification;
    }

    public void setSendNotification(final Boolean sendNotification) {
        this.sendNotification = sendNotification;
    }

    @Override
    public SysExternalSystem createEntity(ApScope scope) {
        ArrDigitalRepository entity = new ArrDigitalRepository();
        this.fillEntity(entity);

        entity.setViewDaoUrl(viewDaoUrl);
        entity.setViewFileUrl(viewFileUrl);
        entity.setViewThumbnailUrl(viewThumbnailUrl);
        entity.setSendNotification(sendNotification);

        return entity;
    }

    public static ArrDigitalRepositoryVO newInstance(ArrDigitalRepository src) {
    	ArrDigitalRepositoryVO vo = new ArrDigitalRepositoryVO();
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
    	// ArrDigitalRepositoryVO
        vo.setViewDaoUrl(src.getViewDaoUrl());
        vo.setViewFileUrl(src.getViewFileUrl());
        vo.setViewThumbnailUrl(src.getViewThumbnailUrl());
        vo.setSendNotification(src.getSendNotification());
        return vo;
    }
}
