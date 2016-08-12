package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.print.Packet;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemPacketRef extends AbstractItem<Packet> {


    public ItemPacketRef(ArrItem arrItem, Output output, NodeId nodeId, Packet value) {
        super(arrItem, output, nodeId);
        setValue(value);
    }

    @Override
    public String serializeValue() {
        return getValue().serialize();
    }
}
