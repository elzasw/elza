package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.print.NodeId;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemJsonTable extends AbstractItem {
	
	ElzaTable value;

    public ItemJsonTable(final NodeId nodeId, final ElzaTable value) {
        super(nodeId);
        this.value = value;
    }

    @Override
    public String serializeValue() {
        return value.toString();
    }

	@Override
	public Object getValue() {
		return value;
	}
}
