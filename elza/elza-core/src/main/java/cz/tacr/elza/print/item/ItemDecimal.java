package cz.tacr.elza.print.item;

import java.math.BigDecimal;

/**
 * Decimal number for print
 *
 */
public class ItemDecimal extends AbstractItem {

    private final BigDecimal value;

    public ItemDecimal(final BigDecimal value) {
        this.value = value;
    }

    @Override
    public String getSerializedValue() {
        return value.toString();
    }

    @Override
    protected BigDecimal getValue() {
        return value;
    }
}
