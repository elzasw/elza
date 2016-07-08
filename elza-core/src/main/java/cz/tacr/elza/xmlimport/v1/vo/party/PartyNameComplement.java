package cz.tacr.elza.xmlimport.v1.vo.party;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Doplněk jména osoby.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "party-name-complement", namespace = NamespaceInfo.NAMESPACE)
public class PartyNameComplement {

    /** Kód typu doplňku jména. */
    @XmlAttribute(name = "party-name-complement-type-code", required = true)
    private String partyNameComplementTypeCode;

    /** Doplněk*/
    @XmlElement(name = "complement", required = true)
    private String complement;

    public String getPartyNameComplementTypeCode() {
        return partyNameComplementTypeCode;
    }

    public void setPartyNameComplementTypeCode(String partyNameComplementTypeCode) {
        this.partyNameComplementTypeCode = partyNameComplementTypeCode;
    }

    public String getComplement() {
        return complement;
    }

    public void setComplement(String complement) {
        this.complement = complement;
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
