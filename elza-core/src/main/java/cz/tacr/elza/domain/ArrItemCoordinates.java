package cz.tacr.elza.domain;

import java.util.Objects;

import org.locationtech.jts.geom.Geometry;

import cz.tacr.elza.common.GeometryConvertor;

/**
 * Implementace třídy {@link cz.tacr.elza.api.ArrItemCoordinates}
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
@Deprecated
public class ArrItemCoordinates extends ArrItemData {

    private Geometry value;

    public Geometry getValue() {
        return value;
    }

    public void setValue(final Geometry value) {
        this.value = value;
    }

    @Override
    public String toString() {
        String str = GeometryConvertor.convert(value);
        return str;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ArrItemCoordinates that = (ArrItemCoordinates) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }
}
