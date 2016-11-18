package cz.tacr.elza.print.item;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;

import cz.tacr.elza.print.NodeId;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemCoordinates extends AbstractItem {

    public ItemCoordinates(final NodeId nodeId, final Geometry value) {
        super(nodeId, value);
    }

    @Override
    public String serializeValue() {
        return new WKTWriter().writeFormatted(getValue(Geometry.class));
    }
}
