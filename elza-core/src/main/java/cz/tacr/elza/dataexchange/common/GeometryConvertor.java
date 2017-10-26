package cz.tacr.elza.dataexchange.common;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

/**
 * Geometry converter for data-exchange.
 */
public class GeometryConvertor {

    private GeometryConvertor() {
    }

    public static String convert(Geometry value) {
        if (value == null) {
            return null;
        }
        // TODO: why not use geometry.toText() like GeometryConverterFactory ?
        // ArrDataCoordinates.toString ?
        String type = value.getGeometryType().toUpperCase();
        if (type.equals("POINT")) {
            return new WKTWriter().writeFormatted(value);
        } else {
            return type + "( " + value.getCoordinates().length + " )";
        }
    }

    public static Geometry convert(String value) throws ParseException {
        // TODO: why not use WTKReader2 like GeometryConverterFactory ?
        return new WKTReader().read(value);
    }
}
