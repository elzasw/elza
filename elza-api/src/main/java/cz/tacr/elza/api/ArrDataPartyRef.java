package cz.tacr.elza.api;


import java.io.Serializable;


/**
 * hodnota atributu archivního popisu typu PartyRef.
 * @author Martin Šlapa
 * @since 1.9.2015
 */
public interface ArrDataPartyRef<P extends ParParty> extends Serializable{


    Integer getPosition();


    void setPosition(final Integer position);


    P getParty();


    void setParty(final P party);
}
