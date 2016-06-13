package cz.tacr.elza.domain;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrDescItemPacketRef extends ArrDescItem implements cz.tacr.elza.api.ArrDescItemPacketRef<ArrChange, RulItemType, RulItemSpec, ArrNode, ArrPacket> {

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
