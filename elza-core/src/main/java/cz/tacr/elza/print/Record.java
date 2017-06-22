package cz.tacr.elza.print;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;

/**
 * One record from registry
 * 
 * Each record has its type, record name and characteristics
 */
public class Record implements Comparable<Record> {

    final int recordId;

	private String record;
    private String characteristics;
    private List<String> variantRecords = new ArrayList<>();
    private RecordType recordType;    

    private Record(RegRecord regRecord, RecordType recordType) {
        this.recordId = regRecord.getRecordId(); 
        this.record = regRecord.getRecord();
        this.characteristics = regRecord.getCharacteristics();
        this.recordType = recordType;
        regRecord.getVariantRecordList().forEach(regVariantRecord -> variantRecords.add(regVariantRecord.getRecord()));
    }
    
    /**
     * Copy constructor
     * @param record
     */
    protected Record(Record srcRecord) {
    	this.recordId = srcRecord.recordId;
    	this.record = srcRecord.record;
    	this.characteristics = srcRecord.characteristics;
    	this.recordType = srcRecord.recordType;
    	this.variantRecords.addAll(srcRecord.variantRecords);
    }

    public int getRecordId() {
		return recordId;
	}

    
    public RecordType getRecordType() {
        return recordType;
    }

    public void setRecordType(final RecordType recordType) {
        this.recordType = recordType;
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

    @Override
    public String toString() {
        return new ToStringBuilder(this).append("record", record).append("characteristics", characteristics).toString();
    }

    @Override
    public int compareTo(final Record o) {
        return CompareToBuilder.reflectionCompare(this, o);
    }

    /**
     * Return value of RegRecord
     * @param regRecord
     * @param output
     * @return
     */
	public static Record newInstance(Output output, RegRecord regRecord) {
        // set register type
        RegRegisterType dbRegisterType = regRecord.getRegisterType();
		RecordType recordType = RecordType.getInstance(output, dbRegisterType);

		Record record = new Record(regRecord, recordType);

        return record;
	}
}
