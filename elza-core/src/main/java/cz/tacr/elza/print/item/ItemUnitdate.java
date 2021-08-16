package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.print.UnitDate;
import cz.tacr.elza.print.item.convertors.UnitDatePrintConvertor;

/**
 * Unit date
 *
 */
public class ItemUnitdate extends AbstractItem {

    private final UnitDate value;

    public ItemUnitdate(final UnitDate value) {
        this.value = value;
    }

    @Override
    public String getSerializedValue() {
        return UnitDatePrintConvertor.convertToPrint(value);
    }

    @Override
    protected UnitDate getValue() {
        return value;
    }

    public UnitDate getUnitDate() {
        return value;
    }

    public String getValueFrom() {
        return UnitDateConvertor.beginToString(value, true);
    }

    public String getValueTo() {
        return UnitDateConvertor.endToString(value, true);
    }
}
