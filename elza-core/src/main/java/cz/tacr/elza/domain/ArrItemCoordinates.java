package cz.tacr.elza.domain;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

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
}
