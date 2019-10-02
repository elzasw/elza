package cz.tacr.elza.print.party;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.domain.ParDynasty;

/**
 * Dynasty
 */
public class Dynasty extends Party {

    private final String genealogy;

    public Dynasty(ParDynasty parDynasty, PartyInitHelper initHelper) {
        super(parDynasty, initHelper);
        this.genealogy = parDynasty.getGenealogy();
    }

    public String getGenealogy() {
        return genealogy;
    }

    @Override
    public PartyType getPartyType() {
        return PartyType.DYNASTY;
    }
}
