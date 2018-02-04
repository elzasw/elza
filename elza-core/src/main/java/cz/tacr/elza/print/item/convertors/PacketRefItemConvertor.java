package cz.tacr.elza.print.item.convertors;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.Packet;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.ItemPacketRef;
import cz.tacr.elza.print.item.ItemType;

public class PacketRefItemConvertor extends AbstractItemConvertor {

    @Override
    protected AbstractItem convert(ArrItem item, ItemType itemType) {
        if (itemType.getDataType() != DataType.PACKET_REF) {
            return null;
        }
        ArrDataPacketRef data = (ArrDataPacketRef) item.getData();
        Packet packet = context.getPacket(data.getPacket());

        return new ItemPacketRef(packet);
    }
}
