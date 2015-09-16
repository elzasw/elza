package cz.tacr.elza.api;

/**
 * TODO: dospat komentář
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemPartyRef<FC extends ArrChange, RT extends RulDescItemType, RS extends RulDescItemSpec, N extends ArrNode, P extends ParParty>
        extends ArrDescItem<FC, RT, RS, N> {

    P getParty();


    void setParty(P party);
}
