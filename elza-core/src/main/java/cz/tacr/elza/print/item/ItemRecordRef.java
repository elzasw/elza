package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.print.Record;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemRecordRef extends AbstractItem<Record> {


    public ItemRecordRef(ArrItem arrItem, Output output, Node node, Record value) {
        super(arrItem, output, node);
        setValue(value);
        value.setItem(this);
    }

    @Override
    public String serializeValue() {
        return getValue().serialize();
    }

}
