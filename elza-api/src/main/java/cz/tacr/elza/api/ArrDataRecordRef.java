package cz.tacr.elza.api;


import java.io.Serializable;


/**
 * hodnota atributu archivního popisu typu RecordRef.
 * @author Martin Šlapa
 * @since 1.9.2015
 */
public interface ArrDataRecordRef extends Serializable{


    Integer getRecordId();


    void setRecordId(final Integer recordIdId);
}
