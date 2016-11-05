package cz.tacr.elza.domain;

import java.util.Objects;

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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrItemPacketRef that = (ArrItemPacketRef) o;
        return Objects.equals(packet, that.packet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), packet);
    }
}
