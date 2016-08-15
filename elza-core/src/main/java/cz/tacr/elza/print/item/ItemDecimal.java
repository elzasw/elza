package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.Output;

import java.math.BigDecimal;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemDecimal extends AbstractItem<BigDecimal> {

    public ItemDecimal(ArrItem arrItem, Output output, NodeId nodeId, BigDecimal value) {
        super(arrItem, output, nodeId);
        setValue(value);
    }

    @Override
    public String serializeValue() {
        return getValue().toString();
    }

}
