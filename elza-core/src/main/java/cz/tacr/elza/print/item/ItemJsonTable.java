package cz.tacr.elza.print.item;

// TODO - JavaDoc - Lebeda

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.Output;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemJsonTable extends AbstractItem<ElzaTable> {

    public ItemJsonTable(ArrItem arrItem, Output output, Node node, ElzaTable value) {
        super(arrItem, output, node);
        setValue(value);
    }



    @Override
    public String serializeValue() {
        // TODO Lebeda - JsonTable - jak bude reprezentovaná?
        return getValue().toString();
    }

}
