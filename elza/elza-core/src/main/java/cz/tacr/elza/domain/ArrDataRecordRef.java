package cz.tacr.elza.domain;

import org.apache.commons.lang3.Validate;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;


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

    @Column(name = "bindingId", updatable = false, insertable = false)
    private Integer bindingId;

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
        if (recordId == null) {
            if (record != null) {
                return record.getAccessPointId();
            }
        }
        return recordId;
    }

    public ApBinding getBinding() {
        return binding;
    }

    public void setBinding(ApBinding binding) {
        this.binding = binding;
        this.bindingId = binding == null ? null : binding.getBindingId();
    }

    public Integer getBindingId() {
        if (bindingId == null) {
            if (binding != null) {
                return binding.getBindingId();
            }
        }
        return bindingId;
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
        ArrDataRecordRef src = (ArrDataRecordRef) srcData;
        if (recordId == null && src.getRecordId() == null) {
            return Objects.equal(bindingId, src.getBindingId());
        }
        return Objects.equal(recordId, src.getRecordId());
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
        if (record == null) {
            Validate.isTrue(recordId == null);
        } else {
            Validate.notNull(recordId);
        }
    }
}
