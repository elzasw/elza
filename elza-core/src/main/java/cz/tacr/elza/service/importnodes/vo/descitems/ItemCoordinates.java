package cz.tacr.elza.service.importnodes.vo.descitems;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Rozhraní pro reprezentaci atributu.
 *
 * @since 19.07.2017
 */
public interface ItemCoordinates extends Item {

    Geometry getGeometry();

}
