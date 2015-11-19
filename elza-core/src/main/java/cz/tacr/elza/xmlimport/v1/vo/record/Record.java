package cz.tacr.elza.xmlimport.v1.vo.record;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
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
    @XmlID
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
    @XmlElement(name = "characteristics", required = true)
    private String characteristics;

    /** Poznámka k heslu v rejstříku. */
    @XmlElement(name = "note")
    private String note;

    /**
     * Příznak, zda se jedná o lokální nebo globální rejstříkové heslo. Lokální heslo je přiřazené pouze konkrétnímu
     * archivnímu popisu/pomůcce.
     */
    @XmlAttribute(name = "local", required = true)
    private boolean local;

    /** Seznam podřízených rejstříků. */
    @XmlElement(name = "record")
    @XmlElementWrapper(name = "sub-record-list")
    private List<Record> records;

    /** Seznam variantních rejstříků. */
    @XmlElement(name = "variant-name")
    @XmlElementWrapper(name = "variant-name-list")
    private List<VariantRecord> variantNames;

    /** Souřadnice. */
    @XmlElement(name = "record-coordinates")
    @XmlElementWrapper(name = "record-coordinate-list")
    private List<RecordCoordinates> recordCoordinates;

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getRegisterTypeCode() {
        return registerTypeCode;
    }

    public void setRegisterTypeCode(String registerTypeCode) {
        this.registerTypeCode = registerTypeCode;
    }

    public String getExternalSourceCode() {
        return externalSourceCode;
    }

    public void setExternalSourceCode(String externalSourceCode) {
        this.externalSourceCode = externalSourceCode;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getPreferredName() {
        return preferredName;
    }

    public void setPreferredName(String preferredName) {
        this.preferredName = preferredName;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(String characteristics) {
        this.characteristics = characteristics;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isLocal() {
        return local;
    }

    public void setLocal(boolean local) {
        this.local = local;
    }

    public List<Record> getRecords() {
        return records;
    }

    public void setRecords(List<Record> records) {
        this.records = records;
    }

    public List<VariantRecord> getVariantNames() {
        return variantNames;
    }

    public void setVariantNames(List<VariantRecord> variantNames) {
        this.variantNames = variantNames;
    }

    public List<RecordCoordinates> getRecordCoordinates() {
        return recordCoordinates;
    }

    public void setRecordCoordinates(List<RecordCoordinates> recordCoordinates) {
        this.recordCoordinates = recordCoordinates;
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
