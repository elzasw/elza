package cz.tacr.elza.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.api.Versionable;
import cz.tacr.elza.api.interfaces.IRegScope;
import cz.tacr.elza.domain.enumeration.StringLength;


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
        implements cz.tacr.elza.api.RegRecord<RegRegisterType, RegVariantRecord, RegRecord,
        RegScope>, Versionable, Serializable, IRegScope {

    @Id
    @GeneratedValue
    private Integer recordId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRegisterType.class)
    @JoinColumn(name = "registerTypeId", nullable = false)
    @JsonIgnore
    private RegRegisterType registerType;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegRecord.class)
    @JoinColumn(name = "parentRecordId")
    @JsonIgnore
    private RegRecord parentRecord;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegExternalSystem.class)
    @JoinColumn(name = "externalSystemId")
    @JsonIgnore
    private RegExternalSystem externalSystem;

    @RestResource(exported = false)
    @OneToMany(mappedBy = "regRecord")
    @JsonIgnore
    private List<RegVariantRecord> variantRecordList = new ArrayList<>(0);

    @RestResource(exported = false)
    @OneToMany(mappedBy = "record", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ParRelationEntity> relationEntities = new ArrayList<>();

    @Column(length = StringLength.LENGTH_1000, nullable = false)
    @JsonIgnore
    private String record;

    @Column
    @JsonIgnore
    private String characteristics;

    @Column()
    @JsonIgnore
    private String note;

    @Column(name = "externalId", length = StringLength.LENGTH_250)
    @JsonIgnore
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RegScope.class)
    @JoinColumn(name = "scopeId", nullable = false)
    @JsonIgnore
    private RegScope scope;

    @Column(length = StringLength.LENGTH_36, nullable = false, unique = true)
    private String uuid;

    @Column(nullable = false)
    private LocalDateTime lastUpdate;

    /* Konstanty pro vazby a fieldy. */
    public static final String VARIANT_RECORD_LIST = "variantRecordList";
    public static final String REGISTER_TYPE = "registerType";
    public static final String PARENT_RECORD = "parentRecord";
    public static final String RECORD = "record";
    public static final String CHARACTERISTICS = "characteristics";
    public static final String NOTE = "note";
    public static final String LOCAL = "local";
    public static final String RECORD_ID = "recordId";
    public static final String SCOPE = "scope";
    public static final String UUID = "uuid";
    public static final String LAST_UPDATE = "lastUpdate";


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

    public RegExternalSystem getExternalSystem() {
        return externalSystem;
    }

    public void setExternalSystem(final RegExternalSystem externalSystem) {
        this.externalSystem = externalSystem;
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
    public RegScope getScope() {
        return scope;
    }

    @Override
    public void setScope(final RegScope scope) {
        this.scope = scope;
    }

    @Override
    @JsonIgnore
    public RegScope getRegScope() {
        return scope;
    }

    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    @Override
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    @Override
    public void setLastUpdate(final LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
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
