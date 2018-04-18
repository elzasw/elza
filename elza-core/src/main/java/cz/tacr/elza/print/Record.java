package cz.tacr.elza.print;

import java.util.List;
import java.util.stream.Collectors;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.service.vo.ApAccessPointData;

/**
 * One record from registry
 *
 * Each record has its type, record name and characteristics
 */
public class Record {

    private final Integer recordId;

    private final String record;

    private final String characteristics;

    private final String externalId;

    private final RecordType recordType;

    // can be proxy, initialized only when needed
    private final List<ApName> srcNames;

    private List<String> variantRecords;

    private Record(ApAccessPointData apRecord, RecordType recordType) {
        this.externalId = apRecord.getExternalId().getValue();
        this.recordId = apRecord.getAccessPointId();
        this.record = apRecord.getPreferredName().getName();
        this.characteristics = apRecord.getDescription().getDescription();
        this.recordType = recordType;
        this.srcNames = apRecord.getVariantRecordList();
    }

    /**
     * Copy constructor
     *
     * @param srcRecord
     */
    protected Record(Record srcRecord) {
        this.recordId = srcRecord.recordId;
        this.record = srcRecord.record;
        this.characteristics = srcRecord.characteristics;
        this.recordType = srcRecord.recordType;
        this.externalId = srcRecord.externalId;
        this.srcNames = srcRecord.srcNames;
    }

    public int getRecordId() {
        return recordId;
    }

    public RecordType getRecordType() {
        return recordType;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public String getRecord() {
        return record;
    }

    public List<String> getVariantRecords() {
        if (variantRecords == null) { // lazy initialization
            variantRecords = srcNames.stream()
                    .map(ApName::getName)
                    .collect(Collectors.toList());
        }
        return variantRecords;
    }

    public String getExternalId() {
        return externalId;
    }

    /**
     * Return new instance of Record. Variant names are required (fetched from database if not
     * initialized).
     */
    public static Record newInstance(ApAccessPoint apRecord, RecordType recordType) {
        ApAccessPointData apData = new ApAccessPointData(apRecord);
        Record record = new Record(apData, recordType);
        return record;
    }

}
