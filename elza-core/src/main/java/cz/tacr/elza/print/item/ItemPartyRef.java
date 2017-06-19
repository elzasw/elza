package cz.tacr.elza.print.item;

import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.party.Party;

/**
 * @author Martin Lebeda
 * @author Petr Pytelka
 *         
 */
public class ItemPartyRef extends AbstractItem {
	
	Party party;

    public ItemPartyRef(final NodeId nodeId, final Party party) {
        super(nodeId);
        
        this.party = party;
    }

    @Override
    public String serializeValue() {
        return party.getName();
    }
    
    public Party getParty() {
    	return party;
    }

	@Override
	public Object getValue() {
		return party;
	}
}
