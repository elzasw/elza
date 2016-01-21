package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.search.annotations.Indexed;

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
public class ArrDataRecordRef extends ArrData implements cz.tacr.elza.api.ArrDataRecordRef {

    @Column(nullable = false)
    private Integer recordId;

    @Override
    public Integer getRecordId() {
        return recordId;
    }

    @Override
    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }

    @Override
    public String getFulltextValue() {
//        return (record != null ) ? record.getRecord() : null;
        return null;
    }
}
