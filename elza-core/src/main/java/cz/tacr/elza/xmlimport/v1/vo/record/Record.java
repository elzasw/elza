package cz.tacr.elza.xmlimport.v1.vo.record;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import cz.tacr.elza.xmlimport.v1.vo.NamespaceInfo;

/**
 * Rejstříkové heslo.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 27. 10. 2015
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "record", namespace = NamespaceInfo.NAMESPACE)
public class Record {

    /** Pro vazbu z osoby a hodnoty record_ref. */
    @XmlAttribute(name = "record-id", required = true)
    private String recordId;

    /** Kód podtypu rejstříku. */
    @XmlAttribute(name = "register-type-code", required = true)
    private String registerTypeCode;

    /** Kód externího zdroje. */
    @XmlAttribute(name = "external-source-code")
    private String externalSourceCode;

    /** Externí identifikátor rejstříkového hesla v externím zdroji záznamů, například interpi. */
    @XmlAttribute(name = "external-id", required = true)
    private String externalId;

    /** Rejstříkové heslo. */
    @XmlElement(name = "preferred-name", required = true)
    private String preferredName;

    /** Podrobná charakteristika rejstříkového hesla. */
    @XmlElement(name = "characteristics")
    private String characteristics;

    /** Poznámka k heslu v rejstříku. */
    @XmlElement(name = "note")
    private String note;

    /** Datum poslední aktualizace. */
    @XmlElement(name = "last-update")
    private Date lastUpdate;

    /** UUID, v exportu bude vždy vyplněno, v importu nemusí být. */
    @XmlAttribute(name = "uuid")
    private String uuid;

    /** Seznam variantních rejstříků. */
    @XmlElement(name = "variant-name")
    @XmlElementWrapper(name = "variant-name-list")
    private List<VariantRecord> variantNames;

    /** Souřadnice. */
    @XmlElement(name = "record-coordinates")
    private RecordCoordinates recordCoordinates;

    /** Seznam podřízených rejstříků. */
    @XmlElement(name = "record")
    @XmlElementWrapper(name = "sub-record-list")
    private List<Record> records;

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(final String recordId) {
        this.recordId = recordId;
    }

    public String getRegisterTypeCode() {
        return registerTypeCode;
    }

    public void setRegisterTypeCode(final String registerTypeCode) {
        this.registerTypeCode = registerTypeCode;
    }

    public String getExternalSourceCode() {
        return externalSourceCode;
    }

    public void setExternalSourceCode(final String externalSourceCode) {
        this.externalSourceCode = externalSourceCode;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    public String getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(final String preferredName) {
        this.preferredName = preferredName;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(final String characteristics) {
        this.characteristics = characteristics;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(final Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public List<VariantRecord> getVariantNames() {
        return variantNames;
    }

    public void setVariantNames(final List<VariantRecord> variantNames) {
        this.variantNames = variantNames;
    }

    public RecordCoordinates getRecordCoordinates() {
        return recordCoordinates;
    }

    public void setRecordCoordinates(final RecordCoordinates recordCoordinates) {
        this.recordCoordinates = recordCoordinates;
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setRecords(final List<Record> records) {
        this.records = records;
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
