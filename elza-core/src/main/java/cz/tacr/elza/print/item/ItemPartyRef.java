package cz.tacr.elza.print.item;

import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.Output;
import cz.tacr.elza.print.party.Party;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class ItemPartyRef extends AbstractItem<Party> {

    public ItemPartyRef(ArrItem arrItem, Output output, NodeId nodeId, Party value) {
        super(arrItem, output, nodeId);
        setValue(value);
    }

    @Override
    public String serializeValue() {
        return getValue().serialize();
    }

}
