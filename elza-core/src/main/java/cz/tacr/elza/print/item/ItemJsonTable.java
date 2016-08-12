package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.Output;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemJsonTable extends AbstractItem<ElzaTable> {

    public ItemJsonTable(ArrItem arrItem, Output output, NodeId nodeId, ElzaTable value) {
        super(arrItem, output, nodeId);
        setValue(value);
    }

    @Override
    public String serializeValue() {
        return getValue().toString();
    }

}
