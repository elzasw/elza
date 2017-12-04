package cz.tacr.elza.print.item;

import cz.tacr.elza.print.Record;

/**
 * Record reference
 *
 */
public class ItemRecordRef extends AbstractItem {

    private final Record record;

    public ItemRecordRef(final Record record) {
        this.record = record;
    }

    @Override
    public String getSerializedValue() {
        return record.getRecord();
    }

    @Override
    protected Record getValue() {
        return record;
    }
}
