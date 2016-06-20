package cz.tacr.elza.api;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemPacketRef<N extends ArrNode, P extends ArrPacket>
        extends ArrDescItem<N> {

    P getPacket();


    void setPacket(P packet);
}
