package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Entity(name = "arr_data_unitid")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataUnitid extends ArrData implements cz.tacr.elza.api.ArrDataUnitid {

    @Column(length = 250, nullable = false)
    private String value;

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String getData() {
        return getValue();
    }
}
