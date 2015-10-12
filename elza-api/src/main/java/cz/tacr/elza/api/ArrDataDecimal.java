package cz.tacr.elza.api;


import java.io.Serializable;
import java.math.BigDecimal;


/**
 * hodnota atributu archivního popisu typu desetinneho cisla.
 *
 * @author Martin Šlapa
 * @since 12.10.2015
 */
public interface ArrDataDecimal extends Serializable {

    BigDecimal getValue();


    void setValue(final BigDecimal value);
}
