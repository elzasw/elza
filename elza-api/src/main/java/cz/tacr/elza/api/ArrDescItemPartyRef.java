package cz.tacr.elza.api;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemPartyRef<N extends ArrNode, P extends ParParty>
        extends ArrDescItem<N> {

    P getParty();


    void setParty(P party);
}
