package cz.tacr.elza.dataexchange.output;

public class RefRecordsFromIds {

    private final Integer recordId;
    private final Integer bindingId;
    private final Integer accessPointId;

    public RefRecordsFromIds(Integer recordId, Integer bindingId, Integer accessPointId) {
        this.recordId = recordId;
        this.bindingId = bindingId;
        this.accessPointId = accessPointId;
    }

    public Integer getRecordId() {
        return recordId;
    }

    public Integer getBindingId() {
        return bindingId;
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }
}
