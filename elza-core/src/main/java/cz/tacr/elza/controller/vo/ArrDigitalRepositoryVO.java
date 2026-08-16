package cz.tacr.elza.controller.vo;

import static cz.tacr.elza.service.dao.FileSystemRepoService.FILE_URI_PREFIX;

import cz.tacr.elza.api.DigitalRepositoryType;
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

    private DigitalRepositoryType digitalRepositoryType;

    private Boolean multipleLinks;

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

    public DigitalRepositoryType getDigitalRepositoryType() {
        return digitalRepositoryType;
    }

    public void setDigitalRepositoryType(DigitalRepositoryType digitalRepositoryType) {
        this.digitalRepositoryType = digitalRepositoryType;
    }

    public Boolean getMultipleLinks() {
		return multipleLinks;
	}

	public void setMultipleLinks(Boolean multipleLinks) {
		this.multipleLinks = multipleLinks;
	}

    @Override
    public SysExternalSystem createEntity(ApScope scope) {
        ArrDigitalRepository entity = new ArrDigitalRepository();
        this.fillEntity(entity);

        // Defensive strip of a leftover file:// prefix if the admin pasted a full URI.
        if (entity.getUrl() != null && entity.getUrl().startsWith(FILE_URI_PREFIX)) {
            entity.setUrl(entity.getUrl().substring(FILE_URI_PREFIX.length()));
        }

        entity.setDigitalRepositoryType(digitalRepositoryType);
        if (digitalRepositoryType == DigitalRepositoryType.FILESYSTEM) {
            // Settings addressing an external repository system have no meaning for a
            // filesystem repository. They are hidden in the UI and cleared here, so a value
            // left over from an earlier configuration cannot keep influencing anything.
            entity.setViewDaoUrl(null);
            entity.setViewFileUrl(null);
            entity.setViewThumbnailUrl(null);
            entity.setUsername(null);
            entity.setPassword(null);
            entity.setSendNotification(false);
        } else {
            entity.setViewDaoUrl(viewDaoUrl);
            entity.setViewFileUrl(viewFileUrl);
            entity.setViewThumbnailUrl(viewThumbnailUrl);
            entity.setSendNotification(sendNotification);
        }
        if (multipleLinks != null) {
            entity.setMultipleLinks(multipleLinks);
        }

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
        vo.setDigitalRepositoryType(src.getDigitalRepositoryType());
        vo.setMultipleLinks(src.getMultipleLinks());
        return vo;
    }
}
