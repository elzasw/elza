package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.enumeration.StringLength;
import org.hibernate.search.annotations.Indexed;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;


/**
 * Hodnota atributu archivního popisu typu referenční označení.
 *
 * @author Martin Šlapa
 * @since 1.9.2015
 */
@Entity(name = "arr_data_unitid")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataUnitid extends ArrData {

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String value;

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    @Override
    public String getFulltextValue() {
        return value;
    }

    @Override
    public ArrData copy() {
        ArrDataUnitid data = new ArrDataUnitid();
        data.setDataType(this.getDataType());
        data.setValue(this.getValue());
        return data;
    }

    @Override
    public void merge(final ArrData data) {
        this.setValue(((ArrDataUnitid) data).getValue());
    }
}
