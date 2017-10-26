package cz.tacr.elza.dataexchange.output.items;

import org.apache.commons.lang.Validate;

import cz.tacr.elza.dataexchange.common.items.DescriptionItemPacketRefImpl;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataPacketRef;

public class PacketRefConvertor implements ItemDataConvertor {

    @Override
    public DescriptionItemPacketRefImpl convert(ArrData data) {
        Validate.isTrue(data.getClass() == ArrDataPacketRef.class, "Invalid data type, dataId:", data.getDataId());

        ArrDataPacketRef packetRef = (ArrDataPacketRef) data;
        DescriptionItemPacketRefImpl item = new DescriptionItemPacketRefImpl();
        item.setPcid(packetRef.getPacketId().toString());
        return item;
    }
}
