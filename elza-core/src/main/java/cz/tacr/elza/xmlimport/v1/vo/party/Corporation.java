package cz.tacr.elza.xmlimport.v1.vo.party;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Korporace.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "corporation", namespace = NamespaceInfo.NAMESPACE)
public class Corporation extends AbstractParty {

    /** Funkce korporace. */
    @XmlElement
    private String scope;

    /** Normy konstitutivní. */
    @XmlElement
    private String foundingNorm;

    /** Normy působnosti. */
    @XmlElement
    private String scopeNorm;

    /** Vnitřní struktury. */
    @XmlElement
    private String organization;

    /** Identifikátory organizace. */
    @XmlElement(name = "corporation-id")
    @XmlElementWrapper(name = "corporation-id-list")
    private List<CorporationId> corporationIds;

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getFoundingNorm() {
        return foundingNorm;
    }

    public void setFoundingNorm(String foundingNorm) {
        this.foundingNorm = foundingNorm;
    }

    public String getScopeNorm() {
        return scopeNorm;
    }

    public void setScopeNorm(String scopeNorm) {
        this.scopeNorm = scopeNorm;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public List<CorporationId> getCorporationIds() {
        return corporationIds;
    }

    public void setCorporationIds(List<CorporationId> corporationIds) {
        this.corporationIds = corporationIds;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
