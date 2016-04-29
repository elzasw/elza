package cz.tacr.elza.domain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.search.annotations.Indexed;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.search.IndexArrDataWhenHasDescItemInterceptor;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Indexed(interceptor = IndexArrDataWhenHasDescItemInterceptor.class)
@Entity(name = "arr_data_record_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataRecordRef extends ArrData implements cz.tacr.elza.api.ArrDataRecordRef<RegRecord> {

    public static final String RECORD = "record";

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRecord.class)
    @JoinColumn(name = "recordId", nullable = false)
    private RegRecord record;

    @Override
    public RegRecord getRecord() {
        return record;
    }

    @Override
    public void setRecord(final RegRecord record) {
        this.record = record;
    }

    @Override
    public String getFulltextValue() {
        RulDescItemSpec descItemSpec = getDescItem().getDescItemSpec();
        if (descItemSpec == null) {
            return record.getRecord();
        }

        return descItemSpec.getName() + ": " + record.getRecord();
    }
}
