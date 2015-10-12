package cz.tacr.elza.domain;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * @author Martin Šlapa
 * @since 12.10.2015
 */
@Entity(name = "arr_data_decimal")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ArrDataDecimal extends ArrData implements cz.tacr.elza.api.ArrDataDecimal {

    @Column(nullable = false)
    private BigDecimal value;

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(final BigDecimal value) {
        this.value = value;
    }

}
