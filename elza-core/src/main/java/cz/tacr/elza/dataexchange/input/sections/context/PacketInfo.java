package cz.tacr.elza.dataexchange.input.sections.context;

import cz.tacr.elza.dataexchange.input.context.EntityIdHolder;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulPacketType;

public class PacketInfo extends EntityIdHolder<ArrPacket> {

    private final RulPacketType packetType;

    private final String storageNumber;

    public PacketInfo(RulPacketType packetType, String storageNumber) {
        super(ArrPacket.class);
        this.packetType = packetType;
        this.storageNumber = storageNumber;
    }

    public RulPacketType getPacketType() {
        return packetType;
    }

    public String getStorageNumber() {
        return storageNumber;
    }
}
