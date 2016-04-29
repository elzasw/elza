package cz.tacr.elza.domain;

import java.math.BigDecimal;

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
 * @author Martin Šlapa
 * @since 12.10.2015
 */
@Indexed(interceptor = IndexArrDataWhenHasDescItemInterceptor.class)
@Entity(name = "arr_data_decimal")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataDecimal extends ArrData implements cz.tacr.elza.api.ArrDataDecimal {

    @Column(nullable = false)
    private BigDecimal value;

    @Field(name = "valueDecimal", store = Store.YES)
    @NumericField
    @Override
    public BigDecimal getValue() {
        return value;
    }

    @Override
    public void setValue(final BigDecimal value) {
        this.value = value;
    }

    @Override
    public String getFulltextValue() {
        return value == null ? null : value.toPlainString();
    }
}
