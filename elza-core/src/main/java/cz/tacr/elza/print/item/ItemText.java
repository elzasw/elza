package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.Output;
import org.apache.commons.lang.StringUtils;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemText extends AbstractItem<String> {

    public ItemText(ArrItem arrItem, Output output, NodeId nodeId, String value) {
        super(arrItem, output, nodeId);
        setValue(value);
    }

    @Override
    public String serializeValue() {
        return StringUtils.trim(getValue());
    }

}
