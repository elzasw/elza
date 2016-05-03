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

    @Override
    public BigDecimal getValue() {
        return value;
    }

    @Field(name = "valueDecimal", store = Store.YES)
    public Double getValueDouble() {
        return value.doubleValue();
    }

    @Override
    public void setValue(final BigDecimal value) {
        this.value = value;
    }

    @Override
    public String getFulltextValue() {
        RulDescItemSpec descItemSpec = getDescItem().getDescItemSpec();
        if (descItemSpec == null) {
            return value.toPlainString();
        }

        return descItemSpec.getName() + ": " + value.toPlainString();
    }
}
