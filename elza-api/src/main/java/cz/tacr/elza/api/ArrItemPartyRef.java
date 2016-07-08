package cz.tacr.elza.api;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrItemPartyRef<P extends ParParty> extends ArrItemData {

    P getParty();


    void setParty(P party);
}
