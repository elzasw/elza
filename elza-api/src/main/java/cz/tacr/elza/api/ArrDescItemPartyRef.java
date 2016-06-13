package cz.tacr.elza.api;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemPartyRef<FC extends ArrChange, RT extends RulItemType, RS extends RulItemSpec, N extends ArrNode, P extends ParParty>
        extends ArrDescItem<FC, RT, RS, N> {

    P getParty();


    void setParty(P party);
}
