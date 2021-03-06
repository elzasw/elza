package cz.tacr.elza.service.cache;

import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApStateEnum;

import java.time.LocalDateTime;
import java.util.List;

public class CachedAccessPoint {

    public static final String ACCESS_POINT_ID = "id";

    private Integer accessPointId;

    private String uuid;

    private ApStateEnum state;

    private String errorDescription;

    private LocalDateTime lastUpdate;

    private Integer preferredPartId;

    private ApState apState;

    private List<CachedPart> parts;

    private List<CachedBinding> bindings;

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public void setAccessPointId(Integer accessPointId) {
        this.accessPointId = accessPointId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public ApStateEnum getState() {
        return state;
    }

    public void setState(ApStateEnum state) {
        this.state = state;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Integer getPreferredPartId() {
        return preferredPartId;
    }

    public void setPreferredPartId(Integer preferredPartId) {
        this.preferredPartId = preferredPartId;
    }

    public ApState getApState() {
        return apState;
    }

    public void setApState(ApState apState) {
        this.apState = apState;
    }

    public List<CachedPart> getParts() {
        return parts;
    }

    public void setParts(List<CachedPart> parts) {
        this.parts = parts;
    }

    public List<CachedBinding> getBindings() {
        return bindings;
    }

    public void setBindings(List<CachedBinding> bindings) {
        this.bindings = bindings;
    }
}
