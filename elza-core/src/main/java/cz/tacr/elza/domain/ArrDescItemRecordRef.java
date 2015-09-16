package cz.tacr.elza.domain;

/**
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrDescItemRecordRef extends ArrDescItem implements cz.tacr.elza.api.ArrDescItemRecordRef<ArrChange, RulDescItemType, RulDescItemSpec, ArrNode, RegRecord> {

    private RegRecord record;

    @Override
    public RegRecord getRecord() {
        return record;
    }

    @Override
    public void setRecord(RegRecord record) {
        this.record = record;
    }

    @Override
    public String toString() {
        return record.getRecord();
    }
}
