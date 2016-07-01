package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.Output;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemEnum extends AbstractItem<String> {

    public ItemEnum(ArrItem arrItem, Output output, Node node, String value) {
        super(arrItem, output, node);
        setValue(value);
    }

    @Override
    public String serializeValue() {
        return getSpecification().getName();
    }

    @Override
    public String serialize() {
        return serializeValue();
    }

}
