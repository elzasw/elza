package cz.tacr.elza.suzap.v1.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Abstraktní osoba.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 10. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "party", namespace = NamespaceInfo.NAMESPACE)
public class Party {

    /** Pro vazbu z hodnoty party_ref. */
    @XmlID
    @XmlElement(required = true)
    private String partyId;

    /** Vazba na rejstřík. */
    @XmlIDREF
    @XmlElement(required = true)
    private Record record;

    /** Kód typu osoby. */
    @XmlElement(required = true)
    private String partyTypeCode;

    /** Preferované jméno osoby. */
    @XmlElement
    private PartyName prefferedName;

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(String partyId) {
        this.partyId = partyId;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }

    public String getPartyTypeCode() {
        return partyTypeCode;
    }

    public void setPartyTypeCode(String partyTypeCode) {
        this.partyTypeCode = partyTypeCode;
    }

    public PartyName getPrefferedName() {
        return prefferedName;
    }

    public void setPrefferedName(PartyName prefferedName) {
        this.prefferedName = prefferedName;
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
