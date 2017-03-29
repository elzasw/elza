package cz.tacr.elza.print.item;

import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.Packet;

/**
 * @sinceDate: 22.6.16
 */
public class ItemPacketRef extends AbstractItem {
	
	Packet packet;

    public ItemPacketRef(final NodeId nodeId, final Packet packet) {
        super(nodeId);
        this.packet = packet;
    }

    @Override
    public String serializeValue() {
        return packet.formatAsString(Packet.FormatType.NUMBER_WITH_TYPE);
    }
    
    @Override
    public Object getValue() {
    	return packet;
    }
    
    public Packet getPacket() {
    	return packet;
    }
}
