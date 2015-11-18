package cz.tacr.elza.xmlimport.v1.vo.party;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;
import cz.tacr.elza.xmlimport.v1.vo.record.Record;

/**
 * Abstraktní osoba.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 10. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "abstract-party", namespace = NamespaceInfo.NAMESPACE)
@XmlSeeAlso(value = {Person.class, Dynasty.class, Corporation.class, TemporaryCorporation.class, TemporaryEvent.class})
public abstract class AbstractParty {

    /** Pro vazbu z hodnoty party_ref. */
    @XmlID
    @XmlAttribute(required = true)
    private String partyId;

    /** Vazba na rejstřík. */
    @XmlIDREF
    @XmlAttribute(required = true)
    private Record record;

    /** Kód typu osoby. */
    @XmlAttribute(required = true)
    private String partyTypeCode;

    /** Preferované jméno osoby. */
    @XmlElement(required = true)
    private PartyName prefferedName;

    /** Ostatní jména. */
    @XmlElement(name = "other-name")
    @XmlElementWrapper(name = "other-name-list")
    private List<PartyName> otherNames;

    /** Dějiny. */
    @XmlElement
    private String history;

    /** Zdroj informací. */
    @XmlAttribute
    private String externalSystemCode;

    /** Autoři. */
    @XmlElement(name = "creator")
    @XmlElementWrapper(name = "creator-name-list")
    private List<Person> creators;

    /** Události. */
    @XmlElement(name = "event")
    @XmlElementWrapper(name = "event-list")
    private List<Event> events;

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

    public String getHistory() {
        return history;
    }

    public void setHistory(String history) {
        this.history = history;
    }

    public String getExternalSystemCode() {
        return externalSystemCode;
    }

    public void setExternalSystemCode(String externalSystemCode) {
        this.externalSystemCode = externalSystemCode;
    }

    public List<PartyName> getOtherNames() {
        return otherNames;
    }

    public void setOtherNames(List<PartyName> otherNames) {
        this.otherNames = otherNames;
    }

    public List<Person> getCreators() {
        return creators;
    }

    public void setCreators(List<Person> creators) {
        this.creators = creators;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
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
