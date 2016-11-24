package cz.tacr.elza.print.item;

import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.Record;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemRecordRef extends AbstractItem {

    public ItemRecordRef(final NodeId nodeId, final Record value) {
        super(nodeId, value);
    }

    @Override
    public String serializeValue() {
        return getValue(Record.class).serialize();
    }
}
