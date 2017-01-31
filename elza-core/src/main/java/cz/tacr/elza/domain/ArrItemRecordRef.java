package cz.tacr.elza.domain;

import java.util.Objects;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrItemRecordRef extends ArrItemData {

    private RegRecord record;

    private Integer recordId;

    public RegRecord getRecord() {
        return record;
    }

    public void setRecord(final RegRecord record) {
        this.record = record;
        this.recordId = record == null ? null : record.getRecordId();
    }

    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(final Integer recordId) {
        this.recordId = recordId;
    }

    @Override
    public String toString() {
        return (record != null ) ? record.getRecord() : null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrItemRecordRef that = (ArrItemRecordRef) o;
        return Objects.equals(record, that.record);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), record);
    }
}
