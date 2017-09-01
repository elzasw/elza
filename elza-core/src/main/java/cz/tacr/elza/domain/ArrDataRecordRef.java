package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.search.annotations.Indexed;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.search.IndexArrDataWhenHasDescItemInterceptor;


/**
 * Hodnota atributu archivního popisu typu RegRecord.
 *
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Indexed(interceptor = IndexArrDataWhenHasDescItemInterceptor.class)
@Entity(name = "arr_data_record_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataRecordRef extends ArrData {

    public static final String RECORD = "record";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.EAGER, targetEntity = RegRecord.class)
    @JoinColumn(name = "recordId", nullable = false)
    private RegRecord record;

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
    }

    @Override
    public String getFulltextValue() {
        if (fulltextValue != null) {
            return fulltextValue;
        }
        return record.getRecord();
    }
}
