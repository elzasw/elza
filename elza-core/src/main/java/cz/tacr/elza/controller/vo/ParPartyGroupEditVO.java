package cz.tacr.elza.controller.vo;

/**
 * Organizace nebo skupina osob, která se označuje konkrétním jménem a která vystupuje nebo může vystupovat jako entita.
 */
public class ParPartyGroupEditVO extends ParPartyEditVO {

    private String scope;

    private String foundingNorm;

    private String scopeNorm;

    private String organization;


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

}
