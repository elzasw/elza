package cz.tacr.elza.print.item;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

/**
 * Coordinates
 */
public class ItemCoordinates extends AbstractItem {
	
	Geometry value;

    public ItemCoordinates(final Geometry geometry) {
        super();
        this.value = geometry;
    }

    @Override
    public String serializeValue() {
        return new WKTWriter().writeFormatted(value);
    }

    @Override
    public Object getValue() {
    	return value;
    }

    public Geometry getGeometry() {
        return value;
    }

}
