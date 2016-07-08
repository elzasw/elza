package cz.tacr.elza.domain;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrItemRecordRef extends ArrItemData implements cz.tacr.elza.api.ArrItemRecordRef<RegRecord> {

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
        return (record != null ) ? record.getRecord() : null;
    }
}
