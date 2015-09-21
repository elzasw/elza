package cz.tacr.elza.api;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemRecordRef<FC extends ArrChange, RT extends RulDescItemType, RS extends RulDescItemSpec, N extends ArrNode, R extends RegRecord>
        extends ArrDescItem<FC, RT, RS, N> {

    R getRecord();


    void setRecord(R record);
}
