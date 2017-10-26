package cz.tacr.elza.dataexchange.common;

import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.schema.v2.PacketState;

/**
 * Packet state converter for data-exchange.
 */
public class PacketStateConvertor {

    private PacketStateConvertor() {
    }

    public static ArrPacket.State convert(PacketState value) {
        if (value == null) {
            return ArrPacket.State.OPEN; // default
        }
        switch (value) {
            case O:
                return ArrPacket.State.OPEN;
            case C:
                return ArrPacket.State.CLOSED;
            case D:
                return ArrPacket.State.CANCELED;
            default:
                throw new IllegalArgumentException("Uknown packet state:" + value);
        }
    }

    public static PacketState convert(ArrPacket.State value) {
        switch (value) {
            case OPEN:
                return null; // default
            case CLOSED:
                return PacketState.C;
            case CANCELED:
                return PacketState.D;
            default:
                throw new IllegalArgumentException("Uknown packet state:" + value);
        }
    }
}
