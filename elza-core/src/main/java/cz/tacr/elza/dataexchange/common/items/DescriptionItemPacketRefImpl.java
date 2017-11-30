package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.sections.context.PacketInfo;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.schema.v2.DescriptionItemPacketRef;

public class DescriptionItemPacketRefImpl extends DescriptionItemPacketRef {

    @Override
    public ImportableItemData createData(ImportContext context, DataType dataType) {
        if (dataType != DataType.PACKET_REF) {
            throw new DEImportException("Unsupported data type:" + dataType);
        }
        PacketInfo packetInfo = context.getSections().getCurrentSection().getPacketInfo(getPcid());
        if (packetInfo == null) {
            throw new DEImportException("Referenced packet not found, packetId:" + getPcid());
        }
        ArrDataPacketRef data = new ArrDataPacketRef();
        data.setPacket(packetInfo.getEntityReference(context.getSession()));

        String fulltext = ArrPacket.createFulltext(packetInfo.getStorageNumber(), packetInfo.getPacketType());
        return new ImportableItemData(data, fulltext);
    }
}
