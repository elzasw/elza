package cz.tacr.elza.api;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemCoordinates<N extends ArrNode> extends ArrDescItem<N> {

    Geometry getValue();


    void setValue(Geometry value);
}
