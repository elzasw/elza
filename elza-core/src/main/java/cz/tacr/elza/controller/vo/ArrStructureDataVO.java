package cz.tacr.elza.controller.vo;

import java.util.Objects;

import cz.tacr.elza.domain.ArrStructuredObject;

/**
 * VO pro hodnotu strukt. typu.
 *
 * @since 08.11.2017
 */
public class ArrStructureDataVO {

    protected Integer id;
    protected String value;
    protected ArrStructuredObject.State state;
    protected Boolean assignable;
    protected String errorDescription;
    protected String typeCode;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public ArrStructuredObject.State getState() {
        return state;
    }

    public void setState(ArrStructuredObject.State state) {
        this.state = state;
    }

    public Boolean getAssignable() {
        return assignable;
    }

    public void setAssignable(Boolean assignable) {
        this.assignable = assignable;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

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
