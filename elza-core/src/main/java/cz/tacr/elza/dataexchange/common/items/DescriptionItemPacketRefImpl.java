package cz.tacr.elza.dataexchange.common.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.sections.context.PacketInfo;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrDataPacketRef.ArrDataPacketRefIndexProvider;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.schema.v2.DescriptionItemPacketRef;

public class DescriptionItemPacketRefImpl extends DescriptionItemPacketRef {

    @Override
    public ArrData createData(ImportContext context, DataType dataType) {
        if (dataType != DataType.PACKET_REF) {
            throw new DEImportException("Unsupported data type:" + dataType);
        }
        PacketInfo packetInfo = context.getSections().getCurrentSection().getPacketInfo(getPcid());
        if (packetInfo == null) {
            throw new DEImportException("Referenced packet not found, packetId:" + getPcid());
        }
        ArrDataPacketRef data = new ArrDataPacketRef(new ArrDataPacketRefIndexProvider() {
            @Override
            public String getStorageNumber() {
                return packetInfo.getStorageNumber();
            }

            @Override
            public RulPacketType getPacketType() {
                return packetInfo.getPacketType();
            }
        });
        data.setPacket(packetInfo.getEntityReference(context.getSession()));
        return data;
    }
}
