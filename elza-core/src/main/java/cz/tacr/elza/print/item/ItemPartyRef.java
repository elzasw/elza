package cz.tacr.elza.print.item;

import cz.tacr.elza.print.Record;
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
	
    @Override
    public <T> T getValue(final Class<T> type) {
    	// allow to get directly record
    	if(type == Record.class) {
    		return type.cast(party.getRecord());
    	}
        return type.cast(getValue());
    }	
}
