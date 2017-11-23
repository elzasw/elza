package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.filter.condition.LuceneDescItemCondition;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;


/**
 * Hodnota atributu archivního popisu typu omezený textový řetězec.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Entity(name = "arr_data_string")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataString extends ArrData {

    @Column(length = StringLength.LENGTH_1000, nullable = false)
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
        ArrDataString data = new ArrDataString();
        data.setDataType(this.getDataType());
        data.setValue(this.getValue());
        return data;
    }

    @Override
    public void merge(final ArrData data) {
        this.setValue(((ArrDataString) data).getValue());
    }
}
