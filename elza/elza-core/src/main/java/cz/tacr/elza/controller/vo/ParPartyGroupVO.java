package cz.tacr.elza.controller.vo;

import java.util.List;


/**
 * Organizace nebo skupina osob, která se označuje konkrétním jménem a která vystupuje nebo může vystupovat jako entita.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.12.2015
 */
public class ParPartyGroupVO extends ParPartyVO {
    private String scope;

    private String foundingNorm;

    private String scopeNorm;

    private String organization;

    private List<ParPartyGroupIdentifierVO> partyGroupIdentifiers;

    public String getScope() {
        return scope;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }

    public String getFoundingNorm() {
        return foundingNorm;
    }

    public void setFoundingNorm(final String foundingNorm) {
        this.foundingNorm = foundingNorm;
    }

    public String getScopeNorm() {
        return scopeNorm;
    }

    public void setScopeNorm(final String scopeNorm) {
        this.scopeNorm = scopeNorm;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(final String organization) {
        this.organization = organization;
    }

    public List<ParPartyGroupIdentifierVO> getPartyGroupIdentifiers() {
        return partyGroupIdentifiers;
    }

    public void setPartyGroupIdentifiers(final List<ParPartyGroupIdentifierVO> partyGroupIdentifiers) {
        this.partyGroupIdentifiers = partyGroupIdentifiers;
    }
}
