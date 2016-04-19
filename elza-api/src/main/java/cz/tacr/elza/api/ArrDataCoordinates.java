package cz.tacr.elza.api;

import com.vividsolutions.jts.geom.Geometry;

import java.io.Serializable;


/**
 * Hodnota atributu archivního popisu typu Coordinates.
 * @author Martin Šlapa
 * @since 1.9.2015
 */
public interface ArrDataCoordinates extends Serializable{


    Geometry getValue();


    void setValue(final Geometry value);
}
