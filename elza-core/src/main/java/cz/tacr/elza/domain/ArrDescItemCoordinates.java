package cz.tacr.elza.domain;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrDescItemCoordinates extends ArrDescItem implements cz.tacr.elza.api.ArrDescItemCoordinates<ArrChange, RulItemType, RulItemSpec, ArrNode> {

    private Geometry value;

    @Override
    public Geometry getValue() {
        return value;
    }

    @Override
    public void setValue(Geometry value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return new WKTWriter().writeFormatted(value);
    }
}
