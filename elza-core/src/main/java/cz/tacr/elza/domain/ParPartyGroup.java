package cz.tacr.elza.domain;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Organizace nebo skupina osob, která se označuje konkrétním jménem a která vystupuje nebo může vystupovat jako entita.
 */
@Entity(name = "par_party_group")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParPartyGroup extends ParParty {

    @Column(length = StringLength.LENGTH_1000)
    @JsonIgnore
    private String scope;

    @Column(length = StringLength.LENGTH_50)
    @JsonIgnore
    private String foundingNorm;

    @Column(length = StringLength.LENGTH_250)
    @JsonIgnore
    private String scopeNorm;

    @Column(length = StringLength.LENGTH_1000)
    @JsonIgnore
    private String organization;

    @OneToMany(mappedBy = "partyGroup", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ParPartyGroupIdentifier> partyGroupIdentifiers;

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

    public List<ParPartyGroupIdentifier> getPartyGroupIdentifiers() {
        return partyGroupIdentifiers;
    }

    public void setPartyGroupIdentifiers(final List<ParPartyGroupIdentifier> partyGroupIdentifiers) {
        this.partyGroupIdentifiers = partyGroupIdentifiers;
    }
}
