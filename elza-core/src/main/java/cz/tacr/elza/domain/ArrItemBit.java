package cz.tacr.elza.domain;

import java.util.Objects;

public class ArrItemBit extends ArrItemData {

    private Boolean value;

    public Boolean isValue() {
        return value;
    }

    public void setValue(Boolean value) {
        this.value = value;
    }

    @Override
    public String toString() { return value == null ? null : Boolean.toString(value);}

    @Override
    public boolean equals(final Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrItemBit that = (ArrItemBit) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }
}
