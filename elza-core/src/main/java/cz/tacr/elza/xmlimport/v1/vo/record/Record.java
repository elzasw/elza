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
    @XmlAttribute(required = true)
    private String recordId;

    /** Kód podtypu rejstříku. */
    @XmlAttribute(required = true)
    private String registerTypeCode;

    /** Kód externího zdroje. */
    @XmlAttribute
    private String externalSourceCode;

    /** Externí identifikátor rejstříkového hesla v externím zdroji záznamů, například interpi. */
    @XmlAttribute(required = true)
    private String externalId;

    /** Rejstříkové heslo. */
    @XmlElement(required = true)
    private String record;

    /** Podrobná charakteristika rejstříkového hesla. */
    @XmlElement(required = true)
    private String characteristics;

    /** Poznámka k heslu v rejstříku. */
    @XmlElement
    private String comment;

    /**
     * Příznak, zda se jedná o lokální nebo globální rejstříkové heslo. Lokální heslo je přiřazené pouze konkrétnímu
     * archivnímu popisu/pomůcce.
     */
    @XmlAttribute(required = true)
    private boolean local;

    /** Seznam podřízených rejstříků. */
    @XmlElement(name = "record")
    @XmlElementWrapper(name = "sub-record-list")
    private List<Record> records;

    /** Seznam variantních rejstříků. */
    @XmlElement(name = "variant-record")
    @XmlElementWrapper(name = "variant-record-list")
    private List<VariantRecord> variantRecords;

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

    public String getRecord() {
        return record;
    }

    public void setRecord(String record) {
        this.record = record;
    }

    public String getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(String characteristics) {
        this.characteristics = characteristics;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
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

    public List<VariantRecord> getVariantRecords() {
        return variantRecords;
    }

    public void setVariantRecords(List<VariantRecord> variantRecords) {
        this.variantRecords = variantRecords;
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
