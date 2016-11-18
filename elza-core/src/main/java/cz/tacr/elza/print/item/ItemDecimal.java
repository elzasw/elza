package cz.tacr.elza.print.item;

import java.math.BigDecimal;

import cz.tacr.elza.print.NodeId;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemDecimal extends AbstractItem {

    public ItemDecimal(final NodeId nodeId, final BigDecimal value) {
        super(nodeId, value);
    }

    @Override
    public String serializeValue() {
        return getValue(BigDecimal.class).toString();
    }
}
