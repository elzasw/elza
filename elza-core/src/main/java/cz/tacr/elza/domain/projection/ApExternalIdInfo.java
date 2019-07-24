package cz.tacr.elza.domain.projection;

public class ApExternalIdInfo {

    // --- fields ---

    private final String value;

    private final ApAccessPointInfo accessPoint;

    // --- getters/setters ---

    public String getValue() {
        return value;
    }

    public ApAccessPointInfo getAccessPoint() {
        return accessPoint;
    }

    // --- constructor ---

    public ApExternalIdInfo(String value, int accessPointId, String uuid, int apStateId, int apScopeId, Integer apTypeId) {
        this.value = value;
        this.accessPoint = new ApAccessPointInfo(accessPointId, uuid, apStateId, apScopeId, apTypeId);
    }
}
