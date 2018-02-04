package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Hodnota atributu archivního popisu typu RegRecord.
 */
@Entity(name = "arr_data_record_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataRecordRef extends ArrData {

    public static final String RECORD = "record";

    @RestResource(exported = false)
	@ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRecord.class)
    @JoinColumn(name = "recordId", nullable = false)
    private RegRecord record;

    @Column(name = "recordId", updatable = false, insertable = false)
    private Integer recordId;

	public ArrDataRecordRef() {

	}

	protected ArrDataRecordRef(ArrDataRecordRef src) {
		super(src);
		this.record = src.record;
		this.recordId = src.recordId;
	}

    public RegRecord getRecord() {
        return record;
    }

    public void setRecord(final RegRecord record) {
        this.record = record;
        this.recordId = record == null ? null : record.getRecordId();
    }

    public Integer getRecordId() {
        return recordId;
    }

    @Override
    public String getFulltextValue() {
        return record.getRecord();
    }

	@Override
	public ArrDataRecordRef makeCopy() {
		return new ArrDataRecordRef(this);
	}

    @Override
    protected boolean isEqualValueInternal(ArrData srcData) {
        ArrDataRecordRef src = (ArrDataRecordRef)srcData;
        return recordId.equals(src.recordId);
    }
}
