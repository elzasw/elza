package cz.tacr.elza.service.importnodes.vo.descitems;

import org.locationtech.jts.geom.Geometry;

/**
 * Rozhraní pro reprezentaci atributu.
 *
 * @since 19.07.2017
 */
public interface ItemCoordinates extends Item {

    Geometry getGeometry();

}
