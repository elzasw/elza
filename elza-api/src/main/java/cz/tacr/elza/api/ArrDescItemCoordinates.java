package cz.tacr.elza.api;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public interface ArrDescItemCoordinates<FC extends ArrChange, RT extends RulDescItemType, RS extends RulDescItemSpec, N extends ArrNode> extends ArrDescItem<FC, RT, RS, N> {

    Geometry getValue();


    void setValue(Geometry value);
}
