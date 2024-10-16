package cz.tacr.elza.service.cache;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApStateEnum;
import cz.tacr.elza.domain.RevStateApproval;

public class CachedAccessPoint {

    @JsonIgnore
    private Integer accessPointId;

    /**
     * Verze AP by neměla být serializována
     */
    @JsonIgnore
    private Integer accessPointVersion;

    @JsonIgnore
    private String uuid;

    @JsonIgnore
    private ApStateEnum state;

    @JsonIgnore
    private String errorDescription;

    @JsonIgnore
    private LocalDateTime lastUpdate;

    @JsonIgnore
    private Integer preferredPartId;

    private ApState apState;

    private List<CachedPart> parts;

    /**
     * Optional field with list of prepared ApParts (based on CachedPart)
     */
    @JsonIgnore
    private List<ApPart> apParts;

    private List<CachedBinding> bindings;

    private List<Integer> replacedAPIds;

    private RevStateApproval revState;

    private String createUsername;

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public void setAccessPointId(Integer accessPointId) {
        this.accessPointId = accessPointId;
    }

    public Integer getAccessPointVersion() {
        return accessPointVersion;
    }

    public void setAccessPointVersion(Integer accessPointVersion) {
        this.accessPointVersion = accessPointVersion;
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

    public void addPart(CachedPart cachedPart) {
        if (this.parts == null) {
            parts = new ArrayList<>();
        }
        parts.add(cachedPart);

    }

    public void addBinding(CachedBinding cb) {
        if (this.bindings == null) {
            bindings = new ArrayList<>();
        }
        bindings.add(cb);
    }

    public List<Integer> getReplacedAPIds() {
        return replacedAPIds;
    }

    public void setReplacedAPIds(List<Integer> replacedAPIds) {
        this.replacedAPIds = replacedAPIds;
    }

    public RevStateApproval getRevState() {
		return revState;
	}

	public void setRevState(RevStateApproval revState) {
		this.revState = revState;
	}

	public String getCreateUsername() {
		return createUsername;
	}

	public void setCreateUsername(String createUsername) {
		this.createUsername = createUsername;
	}

	public void addReplacedId(Integer id) {
        if (replacedAPIds == null) {
            replacedAPIds = new ArrayList<>();
        }
        replacedAPIds.add(id);        
    }

    public List<ApPart> getApParts() {
        if (apParts != null) {
            return apParts;
        }
        if (CollectionUtils.isEmpty(parts)) {
            return Collections.emptyList();
        }
        // should not happen
        Validate.isTrue(false);
        return null;
    }

    public void setApParts(final List<ApPart> apParts) {
        this.apParts = apParts;
    }

    public Map<Integer, List<ApItem>> getApItemMap() {
        if (parts == null) {
            return Collections.emptyMap();
        }
        Map<Integer, List<ApItem>> result = new HashMap<>();
        for(CachedPart part: parts) {
            result.put(part.getPartId(), part.getItems());
        }
        return result;
    }
}
