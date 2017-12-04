package cz.tacr.elza.print;

import java.util.ArrayList;
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

    private final int recordId;

    private final String record;

    private final String characteristics;

    private final RecordType recordType;

    private final String externalId;

    private final List<String> variantRecords;

    private Record(RegRecord regRecord, RecordType recordType, List<String> variantRecords) {
        this.externalId = regRecord.getExternalId();
        this.recordId = regRecord.getRecordId();
        this.record = regRecord.getRecord();
        this.characteristics = regRecord.getCharacteristics();
        this.recordType = recordType;
        this.variantRecords = variantRecords;
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
        this.variantRecords = new ArrayList<>(srcRecord.variantRecords);
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
        return variantRecords;
    }

    public String getExternalId() {
        return externalId;
    }

    /**
     * Return new instance of Record. Variant names are required (fetched from database if not initialized).
     */
    public static Record newInstance(RegRecord regRecord, RecordType recordType) {
        List<String> variantNames = regRecord.getVariantRecordList().stream()
                .map(RegVariantRecord::getRecord)
                .collect(Collectors.toList());
        Record record = new Record(regRecord, recordType, variantNames);
        return record;
    }

}
