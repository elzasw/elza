package cz.tacr.elza.print.item;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTWriter;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.Output;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemCoordinates extends AbstractItem<Geometry> {
    public ItemCoordinates(ArrItem arrItem, Output output, NodeId nodeId, Geometry value) {
        super(arrItem, output, nodeId);
        setValue(value);
    }

    @Override
    public String serializeValue() {
        return new WKTWriter().writeFormatted(getValue());
    }

}
