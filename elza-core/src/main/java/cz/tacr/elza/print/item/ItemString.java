package cz.tacr.elza.print.item;

import org.apache.commons.lang.StringUtils;

import cz.tacr.elza.print.NodeId;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemString extends AbstractItem<String> {

    public ItemString(final NodeId nodeId, final String value) {
        super(nodeId, value);
    }

    @Override
    public String serializeValue() {
        return StringUtils.trim(getValue());
    }
}
