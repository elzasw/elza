package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.print.Packet;

// TODO - JavaDoc - Lebeda
/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemPacketRef extends AbstractItem<Packet> {


    public ItemPacketRef(ArrItem arrItem, Output output, Node node, Packet value) {
        super(arrItem, output, node);
        setValue(value);
    }

    @Override
    public String serializeValue() {
        return getValue().serialize();
    }
}
