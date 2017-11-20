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

    private ArrStructureData structureData;

    private Integer structureDataId;

    public ArrStructureData getStructureData() {
        return structureData;
    }

    public void setStructureData(final ArrStructureData structureData) {
        this.structureData = structureData;
        this.structureDataId = structureData == null ? null : structureData.getStructureDataId();
    }

    public Integer getStructureDataId() {
        return structureDataId;
    }

    public void setStructureDataId(final Integer structureDataId) {
        this.structureDataId = structureDataId;
    }

    @Override
    public String toString() {
        return (structureData != null ) ? structureData.getValue() : null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrItemStructureRef that = (ArrItemStructureRef) o;
        return Objects.equals(structureData, that.structureData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), structureData);
    }
}
