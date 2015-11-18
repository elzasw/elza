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

    /** Pořadí. */
    @XmlAttribute(required = true)
    private Integer position;

    /** Kód typu doplňku jména. */
    @XmlAttribute(required = true)
    private String partyNameComplementTypeName;

    /** Doplněk*/
    @XmlElement(required = true)
    private String complement;

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public String getPartyNameComplementTypeName() {
        return partyNameComplementTypeName;
    }

    public void setPartyNameComplementTypeName(String partyNameComplementTypeName) {
        this.partyNameComplementTypeName = partyNameComplementTypeName;
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
