package cz.tacr.elza.domain.projection;

public class ApAccessPointInfo {

    // --- fields ---

    private final int accessPointId;
    private final String uuid;
    private final int apStateId;
    private final int apScopeId;
    private final Integer apTypeId;

    // --- getters/setters ---

    public int getAccessPointId() {
        return accessPointId;
    }

    public String getUuid() {
        return uuid;
    }

    public int getApScopeId() {
        return apScopeId;
    }

    public Integer getApTypeId() {
        return apTypeId;
    }

    // --- constructor ---

    public ApAccessPointInfo(int accessPointId, String uuid, int apStateId, int apScopeId, Integer apTypeId) {
        this.accessPointId = accessPointId;
        this.uuid = uuid;
        this.apStateId = apStateId;
        this.apScopeId = apScopeId;
        this.apTypeId = apTypeId;
    }
}
