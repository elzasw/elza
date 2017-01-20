package cz.tacr.elza.xmlimport.v1.vo.party;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.util.Assert;

import cz.tacr.elza.utils.PartyType;
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
@XmlSeeAlso(value = {Person.class, Dynasty.class, PartyGroup.class, Event.class})
public abstract class AbstractParty {

    /** Pro vazbu z hodnoty party_ref. */
    @XmlAttribute(name = "party-id", required = true)
    private String partyId;

    /** Vazba na rejstřík. */
    @XmlAttribute(name = "record-id", required = true)
    private String recordId;

    @XmlTransient
    private Record record;

    /** Kód typu osoby. */
    @XmlTransient
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
    private List<String> creatorIds;

    @XmlTransient
    private List<AbstractParty> creators;

    /** Vztahy. */
    @XmlElement(name = "relation")
    @XmlElementWrapper(name = "relation-list")
    private List<Relation> events;

    /** Stručný popis osoby. Délka 1000. */
    @XmlElement(name = "characteristics")
    private String characteristics;

    /** Instituce. */
    @XmlElement(name = "institution")
    private Institution institution;

    public AbstractParty() {
    }

    public AbstractParty(final PartyType partyType) {
        Assert.notNull(partyType);

        this.partyTypeCode = partyType.getCode();
    }

    public String getPartyId() {
        return partyId;
    }

    public void setPartyId(final String partyId) {
        this.partyId = partyId;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(final Record record) {
        this.record = record;
        if (record != null) {
            recordId = record.getRecordId();
        }
    }

    public String getPartyTypeCode() {
        return partyTypeCode;
    }
    public PartyName getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(final PartyName prefferedName) {
        this.preferredName = prefferedName;
    }

    public String getHistory() {
        return history;
    }

    public void setHistory(final String history) {
        this.history = history;
    }

    public String getSourceInformations() {
        return sourceInformations;
    }

    public void setSourceInformations(final String sourceInformations) {
        this.sourceInformations = sourceInformations;
    }

    public List<PartyName> getVariantNames() {
        return variantNames;
    }

    public void setVariantNames(final List<PartyName> variantNames) {
        this.variantNames = variantNames;
    }

    public List<AbstractParty> getCreators() {
        return creators;
    }

    public void setCreators(final List<AbstractParty> creators) {
        this.creators = creators;
    }

    public List<Relation> getEvents() {
        return events;
    }

    public void setEvents(final List<Relation> events) {
        this.events = events;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(final String characteristics) {
        this.characteristics = characteristics;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(final Institution institution) {
        this.institution = institution;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(final String recordId) {
        this.recordId = recordId;
    }

    public List<String> getCreatorIds() {
        return creatorIds;
    }

    public void setCreatorIds(final List<String> creatorIds) {
        this.creatorIds = creatorIds;
    }
}
