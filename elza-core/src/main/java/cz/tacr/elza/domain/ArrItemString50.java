package cz.tacr.elza.domain;

import cz.tacr.elza.domain.interfaces.IArrItemStringValue;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;

import java.util.Objects;

public class ArrItemString50 extends ArrItemData implements IArrItemStringValue {

    private String value;

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(final String value) {
        if(value.length() > 50) {
            throw new SystemException("Délka řetězce přesahuje limit: 50 znaků");
        }
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrItemString50 that = (ArrItemString50) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }

}
