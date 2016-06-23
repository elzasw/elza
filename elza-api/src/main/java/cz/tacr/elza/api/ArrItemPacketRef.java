package cz.tacr.elza.api;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrItemPacketRef<P extends ArrPacket> extends ArrItemData {

    P getPacket();


    void setPacket(P packet);
}
