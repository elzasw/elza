package cz.tacr.elza.domain;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrItemPacketRef extends ArrItemData implements cz.tacr.elza.api.ArrItemPacketRef<ArrPacket> {

    private ArrPacket packet;

    @Override
    public ArrPacket getPacket() {
        return packet;
    }

    @Override
    public void setPacket(ArrPacket packet) {
        this.packet = packet;
    }

    @Override
    public String toString() {
        return (packet != null ) ? packet.getStorageNumber() : null;
    }
}
