package cz.tacr.elza.drools.model.item;

import org.locationtech.jts.geom.Geometry;

import cz.tacr.elza.common.GeometryConvertor;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.domain.RulItemSpec;

public class CoordinatesItem extends Item {

    private final CoordType coordType;

    public CoordinatesItem(Integer id, ItemType itemType, RulItemSpec itemSpec, Geometry geometry) {
        super(id, itemType, itemSpec, GeometryConvertor.convert(geometry));

        // Convert coordinates type
        switch (geometry.getGeometryType()) {
        case Geometry.TYPENAME_POINT:
            coordType = CoordType.POINT;
            break;
        case Geometry.TYPENAME_LINESTRING:
            coordType = CoordType.LINESTRING;
            break;
        case Geometry.TYPENAME_LINEARRING:
        case Geometry.TYPENAME_POLYGON:
            coordType = CoordType.POLYGON;
            break;
        case Geometry.TYPENAME_MULTIPOINT:
            coordType = CoordType.MULTIPOINT;
            break;
        case Geometry.TYPENAME_MULTILINESTRING:
            coordType = CoordType.MULTILINESTRING;
            break;
        case Geometry.TYPENAME_MULTIPOLYGON:
            coordType = CoordType.MULTIPOLYGON;
            break;
        case Geometry.TYPENAME_GEOMETRYCOLLECTION:
        default:
            coordType = CoordType.GEOMETRYCOLLECTION;
        }
    }

    public CoordType getCoordType() {
        return coordType;
    }

}
