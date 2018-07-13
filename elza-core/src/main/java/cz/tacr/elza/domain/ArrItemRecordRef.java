package cz.tacr.elza.domain;

import java.util.Objects;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
@Deprecated
public class ArrItemRecordRef extends ArrItemData {

    private ApAccessPoint accessPoint;

    private Integer recordId;

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(final ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
        this.recordId = accessPoint == null ? null : accessPoint.getAccessPointId();
    }

    public Integer getRecordId() {
        return recordId;
    }

    public void setRecordId(final Integer recordId) {
        this.recordId = recordId;
    }

    @Override
    public String toString() {
        //nahrazeno za toString aby bylo mozne prelozit program
        return (accessPoint != null ) ? accessPoint.toString() : null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrItemRecordRef that = (ArrItemRecordRef) o;
        return Objects.equals(accessPoint, that.accessPoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), accessPoint);
    }
}
