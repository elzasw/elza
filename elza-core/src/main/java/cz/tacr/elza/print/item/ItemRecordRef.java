package cz.tacr.elza.print.item;

import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.Record;

/**
 * @author Martin Lebeda
 * @author Petr Pytelka
 * 
 */
public class ItemRecordRef extends AbstractItem {
	
	Record record;

    public ItemRecordRef(final NodeId nodeId, final Record record) {
        super(nodeId);
        
        this.record = record;
    }

    @Override
    public String serializeValue() {
        return record.serialize();
    }

	@Override
	public Object getValue() {
		return record;
	}
	
	Record getRecord() {
		return record;
	}
}
