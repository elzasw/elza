package cz.tacr.elza.print.party;

import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.domain.ParPartyGroup;

/**
 * PartyGroup
 */
public class PartyGroup extends Party {

    private final String scope;

    private final String foundingNorm;

    private final String scopeNorm;

    private final String organization;

    public PartyGroup(ParPartyGroup parPartyGroup, PartyInitHelper initHelper) {
        super(parPartyGroup, initHelper);
        this.scope = parPartyGroup.getScope();
        this.foundingNorm = parPartyGroup.getFoundingNorm();
        this.scopeNorm = parPartyGroup.getScopeNorm();
        this.organization = parPartyGroup.getOrganization();
    }

    public String getFoundingNorm() {
        return foundingNorm;
    }

    public String getOrganization() {
        return organization;
    }

    public String getScope() {
        return scope;
    }

    public String getScopeNorm() {
        return scopeNorm;
    }

    @Override
    public PartyType getPartyType() {
        return PartyType.GROUP_PARTY;
    }
}
