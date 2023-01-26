package cz.tacr.elza.dataexchange.output;

public class RefRecordsFromIds {

    private final Integer recordId;
    private final Integer accessPointId;

    public RefRecordsFromIds(Integer recordId, Integer accessPointId) {
        this.recordId = recordId;
        this.accessPointId = accessPointId;
    }

    public Integer getRecordId() {
        return recordId;
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }
}
