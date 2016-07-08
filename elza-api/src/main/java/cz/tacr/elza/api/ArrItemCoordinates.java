package cz.tacr.elza.api;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrItemCoordinates extends ArrItemData {

    Geometry getValue();


    void setValue(Geometry value);
}
