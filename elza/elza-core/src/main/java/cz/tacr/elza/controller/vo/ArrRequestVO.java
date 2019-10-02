package cz.tacr.elza.controller.vo;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import cz.tacr.elza.domain.ArrRequest;

import java.util.Date;

/**
 * Value objekt {@link cz.tacr.elza.domain.ArrRequest}
 *
 * @author Martin Šlapa
 * @since 07.12.2016
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.MINIMAL_CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
public abstract class ArrRequestVO {

    private Integer id;

    private String code;

    private ArrRequest.State state;

    private String rejectReason;

    private String username;

    private Date create;

    private Date queued;

    private Date send;

    private Date responseExternalSystem;

    private String externalSystemCode;

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

    public ArrRequest.State getState() {
        return state;
    }

    public void setState(final ArrRequest.State state) {
        this.state = state;
    }

    public String getRejectReason() {
        return rejectReason;
    }

    public void setRejectReason(final String rejectReason) {
        this.rejectReason = rejectReason;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public Date getCreate() {
        return create;
    }

    public void setCreate(final Date create) {
        this.create = create;
    }

    public Date getResponseExternalSystem() {
        return responseExternalSystem;
    }

    public void setResponseExternalSystem(final Date responseExternalSystem) {
        this.responseExternalSystem = responseExternalSystem;
    }

    public Date getQueued() {
        return queued;
    }

    public void setQueued(final Date queued) {
        this.queued = queued;
    }

    public Date getSend() {
        return send;
    }

    public void setSend(final Date send) {
        this.send = send;
    }

    public String getExternalSystemCode() {
        return externalSystemCode;
    }

    public void setExternalSystemCode(final String externalSystemCode) {
        this.externalSystemCode = externalSystemCode;
    }
}
