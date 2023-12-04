package cz.tacr.elza.controller.vo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí systém.
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class SysExternalSystemVO extends BaseCodeVo {

    private String url;

    private String username;

    private String password;

    private String elzaCode;

    private String apiKeyId;

    private String apiKeyValue;

    public String getUrl() {
        return url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getElzaCode() {
        return elzaCode;
    }

    public void setElzaCode(String elzaCode) {
        this.elzaCode = elzaCode;
    }

    public String getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(String apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public String getApiKeyValue() {
        return apiKeyValue;
    }

    public void setApiKeyValue(String apiKeyValue) {
        this.apiKeyValue = apiKeyValue;
    }

    /**
     * Convert VO object to the domain object
     * 
     * @return
     */
    abstract public SysExternalSystem createEntity(ApScope scope);

    protected void fillEntity(SysExternalSystem entity) {
        entity.setCode(getCode());
        entity.setElzaCode(elzaCode);
        entity.setExternalSystemId(getId());
        entity.setName(getName());
        entity.setPassword(password);
        entity.setUrl(url);
        entity.setUsername(username);
        entity.setApiKeyId(getApiKeyId());
        entity.setApiKeyValue(getApiKeyValue());
    }
}
