package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.ArrStructuredObject;

import java.util.Objects;

/**
 * VO pro hodnotu strukt. typu.
 *
 * @since 08.11.2017
 */
public class ArrStructureDataVO {

    public Integer id;
    public String value;
    public ArrStructuredObject.State state;
    public Boolean assignable;
    public String errorDescription;
    public String typeCode;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrStructureDataVO that = (ArrStructureDataVO) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(value, that.value) &&
                state == that.state &&
                Objects.equals(assignable, that.assignable) &&
                Objects.equals(errorDescription, that.errorDescription) &&
                Objects.equals(typeCode, that.typeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, value, state, assignable, errorDescription, typeCode);
    }
}
