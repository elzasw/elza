package cz.tacr.elza.controller.vo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.tacr.elza.domain.SysExternalSystem;

/**
 * VO pro externí systém.
 *
 * @author Martin Šlapa
 * @since 05.12.2016
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class SysExternalSystemVO {

    private Integer id;

    private String code;

    private String name;

    private String url;

    private String username;

    private String password;

    private String elzaCode;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

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

    /**
     * Convert VO object to the domain object
     * 
     * @return
     */
    abstract public SysExternalSystem createEntity();

    protected void fillEntity(SysExternalSystem entity) {
        entity.setCode(code);
        entity.setElzaCode(elzaCode);
        entity.setExternalSystemId(id);
        entity.setName(name);
        entity.setPassword(password);
        entity.setUrl(url);
        entity.setUsername(username);
    }
}
