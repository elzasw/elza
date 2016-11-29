package cz.tacr.elza.print.item;

import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.UnitDate;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemUnitdate extends AbstractItem {

    public ItemUnitdate(final NodeId nodeId, final UnitDate value) {
        super(nodeId, value);
    }

    @Override
    public String serializeValue() {
        return getValue(UnitDate.class).serialize();
    }

    public UnitDate getUnitDate() {
        return getValue(UnitDate.class);
    }
}
