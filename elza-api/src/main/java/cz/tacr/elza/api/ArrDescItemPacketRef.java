package cz.tacr.elza.api;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemPacketRef<FC extends ArrChange, RT extends RulDescItemType, RS extends RulDescItemSpec, N extends ArrNode, P extends ArrPacket>
        extends ArrDescItem<FC, RT, RS, N> {

    P getPacket();


    void setPacket(P packet);
}
