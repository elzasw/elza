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
@XmlSeeAlso(value = {Person.class, Dynasty.class, PartyGroup.class, TemporaryCorporation.class, TemporaryEvent.class})
public abstract class AbstractParty {

    /** Pro vazbu z hodnoty party_ref. */
    @XmlID
    @XmlAttribute(name = "party-id", required = true)
    private String partyId;

    /** Vazba na rejstřík. */
    @XmlIDREF
    @XmlAttribute(name = "record-id", required = true)
    private Record record;

    /** Kód typu osoby. */
    @XmlAttribute(name = "party-type-code", required = true)
    private String partyTypeCode;

    /** Preferované jméno osoby. */
    @XmlElement(name = "preferred-name", required = true)
    private PartyName preferredName;

    /** Ostatní jména. */
    @XmlElement(name = "variant-name")
    @XmlElementWrapper(name = "variant-name-list")
    private List<PartyName> variantNames;

    /** Dějiny. */
    @XmlElement(name = "history")
    private String history;

    /** Zdroje informací. */
    @XmlElement(name = "source-informations")
    private String sourceInformations;

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

    public PartyName getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(PartyName prefferedName) {
        this.preferredName = prefferedName;
    }

    public String getHistory() {
        return history;
    }

    public void setHistory(String history) {
        this.history = history;
    }

    public String getSourceInformations() {
        return sourceInformations;
    }

    public void setSourceInformations(String sourceInformations) {
        this.sourceInformations = sourceInformations;
    }

    public List<PartyName> getVariantNames() {
        return variantNames;
    }

    public void setVariantNames(List<PartyName> variantNames) {
        this.variantNames = variantNames;
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
