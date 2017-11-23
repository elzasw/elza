package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.hibernate.search.annotations.Indexed;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;


/**
 * Hodnota atributu archivního popisu typu RegRecord.
 *
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Entity(name = "arr_data_record_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataRecordRef extends ArrData {

    public static final String RECORD = "record";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = RegRecord.class)
    @JoinColumn(name = "recordId", nullable = false)
    private RegRecord record;

    @Column(name = "recordId", updatable = false, insertable = false)
    private Integer recordId;

    @Transient
    private final String fulltextValue;

    /**
     * Sets fulltext value index when record is only reference (detached hibernate proxy).
     */
    public ArrDataRecordRef(String fulltextValue) {
        this.fulltextValue = fulltextValue;
    }

    public ArrDataRecordRef() {
        this(null);
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
        if (fulltextValue != null) {
            return fulltextValue;
        }
        return record.getRecord();
    }

    @Override
    public ArrData copy() {
        ArrDataRecordRef data = new ArrDataRecordRef();
        data.setDataType(this.getDataType());
        data.setRecord(this.getRecord());
        return data;
    }

    @Override
    public void merge(final ArrData data) {
        this.setRecord(((ArrDataRecordRef) data).getRecord());
    }
}
