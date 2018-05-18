package cz.tacr.elza.domain;

import java.util.Objects;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
@Deprecated
public class ArrItemStructureRef extends ArrItemData {

    private ArrStructuredObject structuredObject;

    private Integer structuredObjectId;

    public ArrStructuredObject getStructuredObject() {
        return structuredObject;
    }

    public void setStructuredObject(final ArrStructuredObject structuredObject) {
        this.structuredObject = structuredObject;
        this.structuredObjectId = structuredObject == null ? null : structuredObject.getStructuredObjectId();
    }

    public Integer getStructuredObjectId() {
        return structuredObjectId;
    }

    public void setStructuredObjectId(final Integer structuredObjectId) {
        this.structuredObjectId = structuredObjectId;
    }

    @Override
    public String toString() {
        return (structuredObject != null ) ? structuredObject.getValue() : null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrItemStructureRef that = (ArrItemStructureRef) o;
        return Objects.equals(structuredObject, that.structuredObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), structuredObject);
    }
}
