package cz.tacr.elza.print.item;

import cz.tacr.elza.print.party.Party;

/**
 * Party reference
 *         
 */
public class ItemPartyRef extends AbstractItem {
	
	Party party;

    public ItemPartyRef(final Party party) {
        super();
        
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
