package cz.tacr.elza.domain;

/**
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrDescItemPartyRef extends ArrDescItem  implements cz.tacr.elza.api.ArrDescItemPartyRef<ArrChange, RulDescItemType, RulDescItemSpec, ArrNode, ParParty> {

    private ParParty party;

    @Override
    public ParParty getParty() {
        return party;
    }

    @Override
    public void setParty(ParParty party) {
        this.party = party;
    }

    @Override
    public String toString() {
        return party.getRecord().getRecord();
    }
}
