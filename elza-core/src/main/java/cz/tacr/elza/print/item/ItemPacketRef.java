package cz.tacr.elza.print.item;

import cz.tacr.elza.print.Packet;

public class ItemPacketRef extends AbstractItem {

    private final Packet packet;

    public ItemPacketRef(final Packet packet) {
        this.packet = packet;
    }

    @Override
    public String getSerializedValue() {
        return packet.formatAsString(Packet.FormatType.NUMBER_WITH_TYPE);
    }

    @Override
    public boolean isValueSerializable() {
        return true;
    }

    @Override
    protected Packet getValue() {
        return packet;
    }
}
