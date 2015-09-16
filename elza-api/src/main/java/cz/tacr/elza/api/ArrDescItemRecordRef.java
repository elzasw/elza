package cz.tacr.elza.api;

/**
 * TODO: dospat komentář
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemRecordRef<FC extends ArrChange, RT extends RulDescItemType, RS extends RulDescItemSpec, N extends ArrNode, R extends RegRecord>
        extends ArrDescItem<FC, RT, RS, N> {

    R getRecord();


    void setRecord(R record);
}
