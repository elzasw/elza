package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.Output;

import java.math.BigDecimal;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemDecimal extends AbstractItem<BigDecimal> {

    public ItemDecimal(ArrItem arrItem, Output output, Node node, BigDecimal value) {
        super(arrItem, output, node);
        setValue(value);
    }

    @Override
    public String serializeValue() {
        return getValue().toString();
    }

}
