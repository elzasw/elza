package cz.tacr.elza.domain;

import java.util.Objects;

/**
 * Rozšiřuje atribut archivního popisu o jeho hodnotu.
 *
 * @author Martin Šlapa
 * @since 15.9.15
 */
@Deprecated
public class ArrItemPartyRef extends ArrItemData {

    private ParParty party;

    private Integer partyId;

    public ParParty getParty() {
        return party;
    }

    public void setParty(final ParParty party) {
        this.party = party;
        this.partyId = party == null ? null : party.getPartyId();
    }

    public Integer getPartyId() {
        return partyId;
    }

    public void setPartyId(final Integer partyId) {
        this.partyId = partyId;
    }

    @Override
    public String toString() {
        // getAccessPoint nahrazeno za toString aby slo prelozit
        return (party != null && party.getAccessPoint() != null) ? party.getAccessPoint().toString() : null;
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
