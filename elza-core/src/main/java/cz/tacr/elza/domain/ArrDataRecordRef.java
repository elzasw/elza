package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Entity(name = "arr_data_record_ref")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataRecordRef extends ArrData implements cz.tacr.elza.api.ArrDataRecordRef {

    @Column(nullable = false)
    private Integer recordId;

    @Override
    public String getData() {
        return getRecordId() + "";
    }

    @Override
    public Integer getRecordId() {
        return recordId;
    }

    @Override
    public void setRecordId(Integer recordId) {
        this.recordId = recordId;
    }
}
