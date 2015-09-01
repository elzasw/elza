package cz.tacr.elza.api;


import java.io.Serializable;


/**
 * @author Martin Šlapa
 * @since 1.9.2015
 */
public interface ArrDataRecordRef extends Serializable{


    Integer getPosition();


    void setPosition(final Integer position);


    Integer getRecordId();


    void setRecordId(final Integer recordIdId);
}
