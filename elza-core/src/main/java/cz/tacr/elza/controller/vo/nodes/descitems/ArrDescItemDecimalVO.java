package cz.tacr.elza.controller.vo.nodes.descitems;

import java.math.BigDecimal;


/**
 * VO hodnoty atributu - decimal.
 *
 * @author Martin Šlapa
 * @since 8.1.2016
 */
public class ArrDescItemDecimalVO extends ArrDescItemVO {

    /**
     * desetinné číslo
     */
    private BigDecimal value;

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(final BigDecimal value) {
        this.value = value;
    }
}