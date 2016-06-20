package cz.tacr.elza.api;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemRecordRef<N extends ArrNode, R extends RegRecord>
        extends ArrDescItem<N> {

    R getRecord();


    void setRecord(R record);
}
