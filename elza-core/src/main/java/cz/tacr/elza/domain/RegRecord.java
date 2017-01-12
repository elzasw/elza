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
 * Rejstříkové heslo.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "reg_record")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RegRecord extends AbstractVersionableEntity implements Versionable, Serializable, IRegScope {

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

    /**
     * ID hesla.
     * @return  id hesla
     */
    public Integer getRecordId() {
        return recordId;
    }

    /**
     * ID hesla.
     * @param recordId  id hesla
     */
    public void setRecordId(final Integer recordId) {
        this.recordId = recordId;
    }

    /**
     * Typ rejstříku.
     * @return  typ rejstříku
     */
    public RegRegisterType getRegisterType() {
        return registerType;
    }

    /**
     * Typ rejstříku.
     * @param registerType typ rejstříku
     */
    public void setRegisterType(final RegRegisterType registerType) {
        this.registerType = registerType;
    }

    /**
     * Nadřazený záznam rejstříku.
     * @return  nadřazený záznam rejstříku
     */
    public RegRecord getParentRecord() {
        return parentRecord;
    }

    /**
     * Nadřazený záznam rejstříku.
     * @param parentRecord nadřazený záznam rejstříku
     */
    public void setParentRecord(final RegRecord parentRecord) {
        this.parentRecord = parentRecord;
    }

    public RegExternalSystem getExternalSystem() {
        return externalSystem;
    }

    public void setExternalSystem(final RegExternalSystem externalSystem) {
        this.externalSystem = externalSystem;
    }

    /**
     * Rejstříkové heslo.
     * @return rejstříkové heslo
     */
    public String getRecord() {
        return record;
    }

    /**
     * Rejstříkové heslo.
     * @param record rejstříkové heslo
     */
    public void setRecord(final String record) {
        this.record = record;
    }

    /**
     * Podrobná charakteristika rejstříkového hesla.
     * @return podrobná charakteristika rejstříkového hesla
     */
    public String getCharacteristics() {
        return characteristics;
    }

    /**
     * Podrobná charakteristika rejstříkového hesla.
     * @param characteristics podrobná charakteristika rejstříkového hesla.
     */
    public void setCharacteristics(final String characteristics) {
        this.characteristics = characteristics;
    }

    /**
     * Poznámka k heslu v rejstříku.
     * @return poznámka k heslu v rejstříku
     */
    public String getNote() {
        return note;
    }

    /**
     * Poznámka k heslu v rejstříku.
     * @param note poznámka k heslu v rejstříku
     */
    public void setNote(final String note) {
        this.note = note;
    }

    /**
     * Externí identifikátor rejstříkového hesla v externím zdroji záznamů, například interpi.
     * @return externí identifikátor rejstříkového hesla v externím zdroji záznamů, například interpi
     */
    public String getExternalId() {
        return externalId;
    }

    /**
     * Externí identifikátor rejstříkového hesla v externím zdroji záznamů, například interpi.
     *
     * @param externalId externí identifikátor rejstříkového hesla v externím zdroji záznamů, například interpi
     */
    public void setExternalId(final String externalId) {
        this.externalId = externalId;
    }

    /**
     * Vazba na variantní záznamy.
     *
     * @param variantRecordList množina záznamů.
     */
    public void setVariantRecordList(final List<RegVariantRecord> variantRecordList) {
        this.variantRecordList = variantRecordList;
    }

    /**
     * Vazba na variantní záznamy.
     *
     * @return množina, může být prázdná.
     */
    public List<RegVariantRecord> getVariantRecordList() {
        return variantRecordList;
    }

    /**
     * @return třída rejstříku
     */
    public RegScope getScope() {
        return scope;
    }

    /**
     * @param scope třída rejstříku
     */
    public void setScope(final RegScope scope) {
        this.scope = scope;
    }

    @Override
    public RegScope getRegScope() {
        return scope;
    }

    /** @return UUID */
    public String getUuid() {
        return uuid;
    }

    /**
     * UUID.
     *
     * @param uuid UUID
     */
    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    /** @return čas poslední aktualizace rejstříku nebo osoby */
    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    /**
     * Čas poslední aktualizace rejstříku nebo osoby.
     *
     * @param lastUpdate as poslední aktualizace rejstříku nebo osoby
     */
    public void setLastUpdate(final LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RegRecord)) {
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
