package cz.tacr.elza.xmlimport.v1.vo.party;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;
import cz.tacr.elza.xmlimport.v1.vo.date.ComplexDate;

/**
 * Identifikátor korporace.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 12. 11. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "party-group-id", namespace = NamespaceInfo.NAMESPACE)
public class PartyGroupId {

    /** Zdroj kódu. */
    @XmlElement(name = "source")
    private String source;

    /** Identifikátor. */
    @XmlAttribute(name = "id", required = true)
    private String id;

    /** Poznámka. */
    @XmlElement(name = "note")
    private String note;

    /** Datum od. */
    @XmlElement(name = "valid-from")
    private ComplexDate validFrom;

    /** Datum do. */
    @XmlElement(name = "valid-to")
    private ComplexDate validTo;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public ComplexDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(ComplexDate validFrom) {
        this.validFrom = validFrom;
    }

    public ComplexDate getValidTo() {
        return validTo;
    }

    public void setValidTo(ComplexDate validTo) {
        this.validTo = validTo;
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
