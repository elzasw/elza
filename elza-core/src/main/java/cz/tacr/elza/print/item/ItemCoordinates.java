package cz.tacr.elza.print.item;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

/**
 * Coordinates
 */
public class ItemCoordinates extends AbstractItem {

    private final Geometry value;

    public ItemCoordinates(final Geometry geometry) {
        this.value = geometry;
    }

    @Override
    public String getSerializedValue() {
        return new WKTWriter().writeFormatted(value);
    }

    @Override
    protected Geometry getValue() {
        return value;
    }
}
