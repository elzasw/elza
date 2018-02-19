package cz.tacr.elza.print;

import java.util.List;
import java.util.stream.Collectors;

import cz.tacr.elza.domain.ApRecord;
import cz.tacr.elza.domain.ApVariantRecord;

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
    private final List<ApVariantRecord> srcVariantRecords;

    private List<String> variantRecords;

    private Record(ApRecord apRecord, RecordType recordType) {
        this.externalId = apRecord.getExternalId();
        this.recordId = apRecord.getRecordId();
        this.record = apRecord.getRecord();
        this.characteristics = apRecord.getCharacteristics();
        this.recordType = recordType;
        this.srcVariantRecords = apRecord.getVariantRecordList();
    }

    /**
     * Copy constructor
     *
     * @param record
     */
    protected Record(Record srcRecord) {
        this.recordId = srcRecord.recordId;
        this.record = srcRecord.record;
        this.characteristics = srcRecord.characteristics;
        this.recordType = srcRecord.recordType;
        this.externalId = srcRecord.externalId;
        this.srcVariantRecords = srcRecord.srcVariantRecords;
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
            variantRecords = srcVariantRecords.stream()
                    .map(ApVariantRecord::getRecord)
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
    public static Record newInstance(ApRecord apRecord, RecordType recordType) {
        Record record = new Record(apRecord, recordType);
        return record;
    }

}
