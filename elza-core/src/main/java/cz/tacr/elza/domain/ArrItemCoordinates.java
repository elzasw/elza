package cz.tacr.elza.domain;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

import java.util.Objects;

/**
 * Implementace třídy {@link cz.tacr.elza.api.ArrItemCoordinates}
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrItemCoordinates extends ArrItemData implements cz.tacr.elza.api.ArrItemCoordinates {

    private Geometry value;

    @Override
    public Geometry getValue() {
        return value;
    }

    @Override
    public void setValue(Geometry value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return new WKTWriter().writeFormatted(value);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrItemCoordinates that = (ArrItemCoordinates) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), value);
    }
}
