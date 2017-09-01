package cz.tacr.elza.print.item;

import cz.tacr.elza.print.Record;

/**
 * Record reference
 * 
 */
public class ItemRecordRef extends AbstractItem {
	
	Record record;

    public ItemRecordRef(final Record record) {
        super();
        
        this.record = record;
    }

    @Override
    public String serializeValue() {
        return record.getRecord();
    }

	@Override
	public Object getValue() {
		return record;
	}
	
	Record getRecord() {
		return record;
	}
}
