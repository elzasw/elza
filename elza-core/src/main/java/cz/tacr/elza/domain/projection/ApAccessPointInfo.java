package cz.tacr.elza.domain.projection;

public class ApAccessPointInfo {

    // --- fields ---

    private final int accessPointId;
    private final String uuid;
    private final int scopeId;
    private final Integer apTypeId;

    // --- getters/setters ---

    public int getAccessPointId() {
        return accessPointId;
    }

    public String getUuid() {
        return uuid;
    }

    public int getScopeId() {
        return scopeId;
    }

    public Integer getApTypeId() {
        return apTypeId;
    }

    // --- constructor ---

    public ApAccessPointInfo(int accessPointId, String uuid, int scopeId, Integer apTypeId) {
        this.accessPointId = accessPointId;
        this.uuid = uuid;
        this.scopeId = scopeId;
        this.apTypeId = apTypeId;
    }
}
