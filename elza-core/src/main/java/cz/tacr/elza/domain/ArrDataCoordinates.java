package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * popis {@link cz.tacr.elza.api.ArrDataCoordinates}.
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Entity(name = "arr_data_coordinates")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataCoordinates extends ArrData implements cz.tacr.elza.api.ArrDataCoordinates {

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
