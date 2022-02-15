package cz.tacr.elza.controller.vo;

import cz.tacr.elza.api.ApExternalSystemType;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí systém.
 *
 */
public class ApExternalSystemVO extends SysExternalSystemVO {

    private ApExternalSystemType type;

    private Integer scopeId;

    private String userInfo;

	private Boolean publishOnlyApproved;

    public ApExternalSystemType getType() {
        return type;
    }

    public void setType(final ApExternalSystemType type) {
        this.type = type;
    }

    public Integer getScope() {
        return scopeId;
    }

    public void setScope(Integer scopeId) {
        this.scopeId = scopeId;
    }
    
    public Boolean getPublishOnlyApproved() {
		return publishOnlyApproved;
	}
    
	private void setPublishOnlyApproved(final Boolean publishOnlyApproved) {
		this.publishOnlyApproved = publishOnlyApproved;		
	}    

    public String getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public SysExternalSystem createEntity(ApScope scope) {
        ApExternalSystem entity = new ApExternalSystem();
        entity.setType(type);
        entity.setScope(scope);
        entity.setUserInfo(userInfo);
        entity.setPublishOnlyApproved(publishOnlyApproved);
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
        vo.setScope(src.getScopeId());
        vo.setUrl(src.getUrl());
        vo.setUsername(src.getUsername());
        vo.setApiKeyId(src.getApiKeyId());
        vo.setApiKeyValue(src.getApiKeyValue());
        vo.setPublishOnlyApproved(src.getPublishOnlyApproved());
        vo.setUserInfo(src.getUserInfo());
        return vo;
    }
}
