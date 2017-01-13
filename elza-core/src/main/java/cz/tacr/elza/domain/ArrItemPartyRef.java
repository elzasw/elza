package cz.tacr.elza.domain;

import java.util.Objects;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
public class ArrItemPartyRef extends ArrItemData {

    private ParParty party;

    public ParParty getParty() {
        return party;
    }

    public void setParty(final ParParty party) {
        this.party = party;
    }

    @Override
    public String toString() {
        return (party != null && party.getRecord() != null) ? party.getRecord().getRecord() : null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrItemPartyRef that = (ArrItemPartyRef) o;
        return Objects.equals(party, that.party);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), party);
    }
}
