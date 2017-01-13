package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.search.IndexArrDataWhenHasDescItemInterceptor;


/**
 * Hodnota atributu archivního popisu typu Integer.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.8.2015
 */
@Indexed(interceptor = IndexArrDataWhenHasDescItemInterceptor.class)
@Entity(name = "arr_data_integer")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataInteger extends ArrData {

    @Column(nullable = false)
    private Integer value;

    @Field(name = "valueInt", store = Store.YES)
    @NumericField
    public Integer getValue() {
        return value;
    }

    public void setValue(final Integer value) {
        this.value = value;
    }

    @Override
    public String getFulltextValue() {
        return value.toString();
    }
}
