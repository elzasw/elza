package cz.tacr.elza.print.party;

import cz.tacr.elza.domain.ParPartyGroup;

/**
 * PartyGroup
 */
public class PartyGroup extends Party {

    private final String scope;
    private final String foundingNorm;
    private final String scopeNorm;
    private final String organization;
    
    private PartyGroup(ParPartyGroup parPartyGroup, PartyInitHelper initHelper)
    {
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

	public static PartyGroup newInstance(ParPartyGroup parPartyGroup, PartyInitHelper initHelper) {
		PartyGroup partyGroup = new PartyGroup(parPartyGroup, initHelper);
		return partyGroup;
	}
}
