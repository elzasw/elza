package cz.tacr.elza.domain;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrItemPartyRef extends ArrItemData implements cz.tacr.elza.api.ArrItemPartyRef<ParParty> {

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
        return (party != null && party.getRecord() != null) ? party.getRecord().getRecord() : null;
    }
}
