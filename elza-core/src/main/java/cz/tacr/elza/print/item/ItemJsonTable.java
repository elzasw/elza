package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.print.NodeId;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemJsonTable extends AbstractItem {

    public ItemJsonTable(final NodeId nodeId, final ElzaTable value) {
        super(nodeId, value);
    }

    @Override
    public String serializeValue() {
        return getValue(ElzaTable.class).toString();
    }

    public ElzaTable getElzaTable() {
        return getValue(ElzaTable.class);
    }
}
