package cz.tacr.elza.api;

import java.math.BigDecimal;


/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 12.10.2015
 */
public interface ArrDescItemDecimal<N extends ArrNode> extends ArrDescItem<N> {

    BigDecimal getValue();


    void setValue(BigDecimal value);
}
