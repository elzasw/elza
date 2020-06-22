package cz.tacr.elza.controller.vo;

import java.time.LocalDateTime;

public class ExtSyncsQueueItemVO {

    private Integer id;
    private Integer scopeId;
    private ExtAsyncQueueState state;
    private String stateMessage;
    private Integer accessPointId;
    private String accessPointName;
    private LocalDateTime date;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Integer getScopeId() {
        return scopeId;
    }

    public void setScopeId(final Integer scopeId) {
        this.scopeId = scopeId;
    }

    public ExtAsyncQueueState getState() {
        return state;
    }

    public void setState(final ExtAsyncQueueState state) {
        this.state = state;
    }

    public String getStateMessage() {
        return stateMessage;
    }

    public void setStateMessage(final String stateMessage) {
        this.stateMessage = stateMessage;
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public void setAccessPointId(final Integer accessPointId) {
        this.accessPointId = accessPointId;
    }

    public String getAccessPointName() {
        return accessPointName;
    }

    public void setAccessPointName(final String accessPointName) {
        this.accessPointName = accessPointName;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(final LocalDateTime date) {
        this.date = date;
    }
}
