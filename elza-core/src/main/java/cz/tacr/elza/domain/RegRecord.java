package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;


/**
 * Záznamy v rejstříku.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "reg_record")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RegRecord extends AbstractVersionableEntity
        implements cz.tacr.elza.api.RegRecord<RegRegisterType, RegExternalSource, RegVariantRecord, RegRecord> {

    @Id
    @GeneratedValue
    private Integer recordId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRegisterType.class)
    @JoinColumn(name = "registerTypeId", nullable = false)
    private RegRegisterType registerType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRecord.class)
    @JoinColumn(name = "parentRecordId")
    private RegRecord parentRecord;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegExternalSource.class)
    @JoinColumn(name = "externalSourceId")
    private RegExternalSource externalSource;

    @RestResource(exported = false)
    @OneToMany(mappedBy = "regRecord")
    private List<RegVariantRecord> variantRecordList = new ArrayList<>(0);

    @Column(length = 1000, nullable = false)
    private String record;

    @Column
    private String characteristics;

    @Column()
    private String note;

    @Column(nullable = false)
    private Boolean local;

    @Column(name = "externalId", length = 250)
    private String externalId;

    /* Konstanty pro vazby a fieldy. */
    public static final String VARIANT_RECORD_LIST = "variantRecordList";
    public static final String REGISTER_TYPE = "registerType";
    public static final String PARENT_RECORD = "parentRecord";
    public static final String RECORD = "record";
    public static final String CHARACTERISTICS = "characteristics";
    public static final String NOTE = "note";


    @Override
    public Integer getRecordId() {
        return recordId;
    }

    @Override
    public void setRecordId(final Integer recordId) {
        this.recordId = recordId;
    }

    @Override
    public RegRegisterType getRegisterType() {
        return registerType;
    }

    @Override
    public void setRegisterType(final RegRegisterType registerType) {
        this.registerType = registerType;
    }

    @Override
    public RegRecord getParentRecord() {
        return parentRecord;
    }

    @Override
    public void setParentRecord(final RegRecord parentRecord) {
        this.parentRecord = parentRecord;
    }

    @Override
    public RegExternalSource getExternalSource() {
        return externalSource;
    }

    @Override
    public void setExternalSource(final RegExternalSource externalSource) {
        this.externalSource = externalSource;
    }

    @Override
    public String getRecord() {
        return record;
    }

    @Override
    public void setRecord(final String record) {
        this.record = record;
    }

    @Override
    public String getCharacteristics() {
        return characteristics;
    }

    @Override
    public void setCharacteristics(final String characteristics) {
        this.characteristics = characteristics;
    }

    @Override
    public String getNote() {
        return note;
    }

    @Override
    public void setNote(final String note) {
        this.note = note;
    }

    @Override
    public Boolean getLocal() {
        return local;
    }

    @Override
    public void setLocal(final Boolean local) {
        this.local = local;
    }

    @Override
    public String getExternalId() {
        return externalId;
    }

    @Override
    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    @Override
    public void setVariantRecordList(final List<RegVariantRecord> variantRecordList) {
        this.variantRecordList = variantRecordList;
    }

    @Override
    public List<RegVariantRecord> getVariantRecordList() {
        return variantRecordList;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.api.RegRecord)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        RegRecord other = (RegRecord) obj;

        return new EqualsBuilder().append(recordId, other.getRecordId()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(recordId).toHashCode();
    }

    @Override
    public String toString() {
        return "RegRecord pk=" + recordId;
    }
}
