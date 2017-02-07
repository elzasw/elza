package cz.tacr.elza.domain;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Lob;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.hibernate.annotations.Type;


/**
 * Organizace nebo skupina osob, která se označuje konkrétním jménem a která vystupuje nebo může vystupovat jako entita.
 */
@Entity(name = "par_party_group")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParPartyGroup extends ParParty implements cz.tacr.elza.api.ParPartyGroup {

    @Column
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @JsonIgnore
    private String scope;

    @Column
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @JsonIgnore
    private String foundingNorm;

    @Column
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @JsonIgnore
    private String scopeNorm;

    @Column
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    @JsonIgnore
    private String organization;

    @OneToMany(mappedBy = "partyGroup", fetch = FetchType.LAZY)
    @JsonIgnore
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
