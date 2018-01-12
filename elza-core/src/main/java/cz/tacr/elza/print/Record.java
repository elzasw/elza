package cz.tacr.elza.print;

import java.util.List;
import java.util.stream.Collectors;

import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegVariantRecord;

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
    private final List<RegVariantRecord> srcVariantRecords;

    private List<String> variantRecords;

    private Record(RegRecord regRecord, RecordType recordType) {
        this.externalId = regRecord.getExternalId();
        this.recordId = regRecord.getRecordId();
        this.record = regRecord.getRecord();
        this.characteristics = regRecord.getCharacteristics();
        this.recordType = recordType;
        this.srcVariantRecords = regRecord.getVariantRecordList();
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
                    .map(RegVariantRecord::getRecord)
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
    public static Record newInstance(RegRecord regRecord, RecordType recordType) {
        Record record = new Record(regRecord, recordType);
        return record;
    }

}
