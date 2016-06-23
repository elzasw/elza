package cz.tacr.elza.api;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrItemRecordRef<R extends RegRecord> extends ArrItemData {

    R getRecord();


    void setRecord(R record);
}
