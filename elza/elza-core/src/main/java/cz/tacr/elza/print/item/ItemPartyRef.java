package cz.tacr.elza.print.item;

import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.party.Party;

/**
 * Party reference
 */
public class ItemPartyRef extends AbstractItem {

    private final Party party;

    public ItemPartyRef(final Party party) {
        this.party = party;
    }

    @Override
    public String getSerializedValue() {
        return party.getName().getFullName();
    }

    @Override
    protected Party getValue() {
        return party;
    }
    
    public Party getParty() {
    	return party;
    }

    @Override
    public <T> T getValue(final Class<T> type) {
        // allow to get directly AP
        if (type == Record.class) {
            return type.cast(party.getRecord());
        }
        return type.cast(getValue());
    }
}
