package cz.tacr.elza.domain;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.search.IndexArrDataWhenHasDescItemInterceptor;


/**
 * Hodnota atributu archivního popisu typu desetinného čísla.
 *
 * @author Martin Šlapa
 * @since 12.10.2015
 */
@Indexed(interceptor = IndexArrDataWhenHasDescItemInterceptor.class)
@Entity(name = "arr_data_decimal")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataDecimal extends ArrData {

    @Column(nullable = false)
    private BigDecimal value;

    public BigDecimal getValue() {
        return value;
    }

    @Field(name = "valueDecimal", store = Store.YES)
    public Double getValueDouble() {
        return value.doubleValue();
    }

    public void setValue(final BigDecimal value) {
        this.value = value;
    }

    @Override
    public String getFulltextValue() {
        return value.toPlainString();
    }
}
