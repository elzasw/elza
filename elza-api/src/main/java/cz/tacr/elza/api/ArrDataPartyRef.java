package cz.tacr.elza.api;


import java.io.Serializable;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
public interface ArrDataPartyRef extends Serializable{


    Integer getPosition();


    void setPosition(final Integer position);


    Integer getAbstractPartyId();


    void setAbstractPartyId(final Integer abstractPartyId);
}
