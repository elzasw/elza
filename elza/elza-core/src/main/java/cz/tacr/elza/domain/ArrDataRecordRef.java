package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.Validate;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Hodnota atributu archivního popisu typu ApAccessPoint.
 */
@Entity(name = "arr_data_record_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataRecordRef extends ArrData {

    public static final String FIELD_RECORD = "record";

    @RestResource(exported = false)
	@ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "recordId")
    @JsonIgnore
    private ApAccessPoint record;

    @Column(name = "recordId", updatable = false, insertable = false)
    private Integer recordId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApBinding.class)
    @JoinColumn(name = "bindingId")
    private ApBinding binding;

    private static ApFulltextProvider fulltextProvider;

	public ArrDataRecordRef() {

	}

	protected ArrDataRecordRef(ArrDataRecordRef src) {
		super(src);
        copyValue(src);
	}

    private void copyValue(ArrDataRecordRef src) {
        this.record = src.record;
        this.recordId = src.recordId;
        this.binding = src.binding;
    }

    public ApAccessPoint getRecord() {
        return record;
    }

    public void setRecord(final ApAccessPoint record) {
        this.record = record;
        this.recordId = record == null ? null : record.getAccessPointId();
    }

    public Integer getRecordId() {
        return recordId;
    }

    public ApBinding getBinding() {
        return binding;
    }

    public void setBinding(ApBinding binding) {
        this.binding = binding;
    }

    @Override
    public String getFulltextValue() {
        return fulltextProvider.getFulltext(record);
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

    @Override
    public void mergeInternal(final ArrData srcData) {
        ArrDataRecordRef src = (ArrDataRecordRef) srcData;
        this.copyValue(src);
    }

    public static void setFulltextProvider(ApFulltextProvider fullTextProvider) {
        ArrDataRecordRef.fulltextProvider = fullTextProvider;
    }

    @Override
    protected void validateInternal() {
        Validate.notNull(record);
        Validate.notNull(recordId);
    }
}
