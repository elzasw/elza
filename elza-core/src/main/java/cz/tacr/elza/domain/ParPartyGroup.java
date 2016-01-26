package cz.tacr.elza.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;


/**
 * Organizace nebo skupina osob, která se označuje konkrétním jménem a která vystupuje nebo může vystupovat jako entita.
 */
@Entity(name = "par_party_group")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class ParPartyGroup extends ParParty implements cz.tacr.elza.api.ParPartyGroup {

    @Column(length = 1000, nullable = false)
    private String scope;

    @Column(length = 50)
    private String foundingNorm;

    @Column(length = 250)
    private String scopeNorm;

    @Column(length = 1000)
    private String organization;

    @OneToMany(mappedBy = "partyGroup", fetch = FetchType.LAZY)
    private List<ParPartyGroupIdentifier> partyGroupIdentifiers;


    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public void setScope(final String scope) {
        this.scope = scope;
    }

    @Override
    public String getFoundingNorm() {
        return foundingNorm;
    }

    @Override
    public void setFoundingNorm(final String foundingNorm) {
        this.foundingNorm = foundingNorm;
    }

    @Override
    public String getScopeNorm() {
        return scopeNorm;
    }

    @Override
    public void setScopeNorm(final String scopeNorm) {
        this.scopeNorm = scopeNorm;
    }

    @Override
    public String getOrganization() {
        return organization;
    }

    @Override
    public void setOrganization(final String organization) {
        this.organization = organization;
    }

    public List<ParPartyGroupIdentifier> getPartyGroupIdentifiers() {
        return partyGroupIdentifiers;
    }

    public void setPartyGroupIdentifiers(final List<ParPartyGroupIdentifier> partyGroupIdentifiers) {
        this.partyGroupIdentifiers = partyGroupIdentifiers;
    }
}
