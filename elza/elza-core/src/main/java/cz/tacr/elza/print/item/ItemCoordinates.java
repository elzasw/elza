package cz.tacr.elza.print.item;

import java.util.Base64;

import org.locationtech.jts.geom.Geometry;

import cz.tacr.elza.common.GeometryConvertor;

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
        String str = GeometryConvertor.convert(value);
        return str;
    }

    public String getBase64Value() {
        byte data[] = GeometryConvertor.convertToWkb(value);
        return Base64.getEncoder().withoutPadding().encodeToString(data);
    }

    @Override
    protected Geometry getValue() {
        return value;
    }
}
