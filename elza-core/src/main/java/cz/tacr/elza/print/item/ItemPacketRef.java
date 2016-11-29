package cz.tacr.elza.print.item;

import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.Packet;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemPacketRef extends AbstractItem {
	
	Packet packet;

    public ItemPacketRef(final NodeId nodeId, final Packet packet) {
        super(nodeId);
        this.packet = packet;
    }

    @Override
    public String serializeValue() {
        return packet.serialize();
    }
    
    @Override
    public Object getValue() {
    	return packet;
    }
    
    public Packet getPacket() {
    	return packet;
    }

    public Packet getPacket() {
        return getValue(Packet.class);
    }
}
