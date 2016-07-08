package cz.tacr.elza.print.party;

/**
 * @author <a href="mailto:martin.lebeda@marbes.cz">Martin Lebeda</a>
 *         Date: 22.6.16
 */
public class PartyGroup extends Party {
    private String scope;
    private String foundingNorm;
    private String scopeNorm;
    private String organization;

    public String getFoundingNorm() {
        return foundingNorm;
    }

    public void setFoundingNorm(String foundingNorm) {
        this.foundingNorm = foundingNorm;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getScopeNorm() {
        return scopeNorm;
    }

    public void setScopeNorm(String scopeNorm) {
        this.scopeNorm = scopeNorm;
    }
}
