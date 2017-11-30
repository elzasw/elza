package cz.tacr.elza.domain;

import java.util.Objects;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
@Deprecated
public class ArrItemPacketRef extends ArrItemData {

    private ArrPacket packet;

    private Integer packetId;

    public ArrPacket getPacket() {
        return packet;
    }

    public void setPacket(final ArrPacket packet) {
        this.packet = packet;
        this.packetId = packet == null ? null : packet.getPacketId();
    }

    public Integer getPacketId() {
        return packetId;
    }

    public void setPacketId(final Integer packetId) {
        this.packetId = packetId;
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
