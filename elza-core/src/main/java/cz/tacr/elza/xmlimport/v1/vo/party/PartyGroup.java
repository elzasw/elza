package cz.tacr.elza.xmlimport.v1.vo.party;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Korporace.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "party-group", namespace = NamespaceInfo.NAMESPACE)
public class PartyGroup extends AbstractParty {

    /** Funkce korporace. */
    @XmlElement(name = "scope")
    private String scope;

    /** Normy konstitutivní. */
    @XmlElement(name = "founding-norm")
    private String foundingNorm;

    /** Normy působnosti. */
    @XmlElement(name = "scope-norm")
    private String scopeNorm;

    /** Vnitřní struktury. */
    @XmlElement(name = "organization")
    private String organization;

    /** Identifikátory organizace. */
    @XmlElement(name = "party-group-id")
    @XmlElementWrapper(name = "party-group-id-list")
    private List<PartyGroupId> partyGroupIds;

    public PartyGroup() {
        super(PartyType.PARTY_GROUP);
    }

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

    public List<PartyGroupId> getPartyGroupIds() {
        return partyGroupIds;
    }

    public void setPartyGroupIds(final List<PartyGroupId> partyGroupIds) {
        this.partyGroupIds = partyGroupIds;
    }
}
