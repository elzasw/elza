package cz.tacr.elza.api;


import java.io.Serializable;


/**
 * hodnota atributu archivního popisu typu RecordRef.
 * @author Martin Šlapa
 * @since 1.9.2015
 */
public interface ArrDataRecordRef<R extends RegRecord> extends Serializable{


    R getRecord();


    void setRecord(final R recordId);
}
