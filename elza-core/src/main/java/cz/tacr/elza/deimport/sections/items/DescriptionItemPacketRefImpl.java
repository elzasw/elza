package cz.tacr.elza.deimport.sections.items;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.sections.context.PacketImportInfo;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrDataPacketRef.ArrDataPacketRefIndexProvider;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.schema.v2.DescriptionItemPacketRef;

public class DescriptionItemPacketRefImpl extends DescriptionItemPacketRef {

    @Override
    protected boolean isDataTypeSupported(DataType dataType) {
        return dataType == DataType.PACKET_REF;
    }

    @Override
    protected ArrData createData(ImportContext context, RuleSystemItemType itemType) {
        PacketImportInfo packetInfo = context.getSections().getCurrentSection().getPacketInfo(getPcid());
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
        data.setPacket(packetInfo.getEntityRef(context.getSession(), ArrPacket.class));
        return data;
    }
}
