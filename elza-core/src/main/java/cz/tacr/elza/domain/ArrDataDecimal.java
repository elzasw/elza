package cz.tacr.elza.domain;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import cz.tacr.elza.filter.condition.LuceneDescItemCondition;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.annotations.Store;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Hodnota atributu archivního popisu typu desetinného čísla.
 *
 * @author Martin Šlapa
 * @since 12.10.2015
 */
@Entity(name = "arr_data_decimal")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataDecimal extends ArrData {

    @Column(nullable = false)
    private BigDecimal value;

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(final BigDecimal value) {
        this.value = value;
    }

    @Override
    public String getFulltextValue() {
        return value.toPlainString();
    }

    @Override
    public Double getValueDouble() {
        return value.doubleValue();
    }
}
