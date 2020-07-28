package cz.tacr.elza.domain.vo;

import org.locationtech.jts.geom.Geometry;


/**
 * Popisek souřadnicové hodnoty atributu uzlu.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 20.04.2016
 */
public class CoordinatesTitleValue extends TitleValue {

    /**
     * Typ objektu (B-bod, L-cesta, P-polygon, N-ostatní)
     */
    private String geomType;

    public CoordinatesTitleValue() {
    }

    public CoordinatesTitleValue(final Geometry geometry) {
        init(geometry);
    }

    private void init(final Geometry geometry) {
        String value = "";
        switch (geometry.getDimension()) {
            case 0:
                geomType = "B";
                value = geometry.getCoordinate().x + ", " + geometry.getCoordinate().y;
                break;
            case 1:
                geomType = "L";
                value = geometry.getCoordinates().length + "";
                break;
            case 2:
                geomType = "P";
                value = geometry.getCoordinates().length + "";
                break;
            default:
                geomType = "N";
                value = geometry.getCoordinates().length + "";
        }

        setValue(value);
    }

    public String getGeomType() {
        return geomType;
    }

    public void setGeomType(final String geomType) {
        this.geomType = geomType;
    }
}
