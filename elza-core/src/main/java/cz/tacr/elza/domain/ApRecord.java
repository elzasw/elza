package cz.tacr.elza.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
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

import cz.tacr.elza.api.interfaces.IApScope;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.interfaces.Versionable;


/**
 * Rejstříkové heslo.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "ap_record")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ApRecord extends AbstractVersionableEntity implements Versionable, Serializable, IApScope {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer recordId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApType.class)
    @JoinColumn(name = "apTypeId", nullable = false)
    @JsonIgnore
    private ApType apType;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer apTypeId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApExternalSystem.class)
    @JoinColumn(name = "externalSystemId")
    @JsonIgnore
    private ApExternalSystem externalSystem;

    @Column(updatable = false, insertable = false)
    private Integer externalSystemId;

    @RestResource(exported = false)
    @OneToMany(mappedBy = "apRecord")
    @JsonIgnore
    private List<ApVariantRecord> variantRecordList = new ArrayList<>(0);

    @RestResource(exported = false)
    @OneToMany(mappedBy = "record", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ParRelationEntity> relationEntities = new ArrayList<>();

    @Column(length = StringLength.LENGTH_1000, nullable = false)
    @JsonIgnore
    private String record;

    @Column(length = StringLength.LENGTH_1000)
    @JsonIgnore
    private String characteristics;

    @Column(name = "externalId", length = StringLength.LENGTH_250)
    @JsonIgnore
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApScope.class)
    @JoinColumn(name = "scopeId", nullable = false)
    @JsonIgnore
    private ApScope scope;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer scopeId;

    @Column(length = StringLength.LENGTH_36, nullable = false, unique = true)
    private String uuid;

    @Column(nullable = false)
    private LocalDateTime lastUpdate;

    @Column(nullable = false)
    private boolean invalid;

    /* Konstanty pro vazby a fieldy. */
    public static final String VARIANT_RECORD_LIST = "variantRecordList";
    public static final String AP_TYPE = "apType";
    public static final String RECORD = "record";
    public static final String CHARACTERISTICS = "characteristics";
    public static final String LOCAL = "local";
    public static final String RECORD_ID = "recordId";
    public static final String SCOPE = "scope";
    public static final String UUID = "uuid";
    public static final String LAST_UPDATE = "lastUpdate";
    public static final String INVALID = "invalid";

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
    public ApType getApType() {
        return apType;
    }

    /**
     * Typ rejstříku.
     * @param apType typ rejstříku
     */
    public void setApType(final ApType apType) {
        this.apTypeId = apType == null ? null : apType.getApTypeId();
        this.apType = apType;
    }

    public ApExternalSystem getExternalSystem() {
        return externalSystem;
    }

    public void setExternalSystem(final ApExternalSystem externalSystem) {
        this.externalSystem = externalSystem;
        this.externalSystemId = externalSystem != null ? externalSystem.getExternalSystemId() : null;
    }

    public Integer getExternalSystemId() {
        return externalSystemId;
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
    public void setVariantRecordList(final List<ApVariantRecord> variantRecordList) {
        this.variantRecordList = variantRecordList;
    }

    /**
     * Vazba na variantní záznamy.
     *
     * @return množina, může být prázdná.
     */
    public List<ApVariantRecord> getVariantRecordList() {
        return variantRecordList;
    }

    /**
     * @return třída rejstříku
     */
    public ApScope getScope() {
        return scope;
    }

    /**
     * @param scope třída rejstříku
     */
    public void setScope(final ApScope scope) {
        this.scope = scope;
        this.scopeId = scope != null ? scope.getScopeId() : null;
    }

    public Integer getScopeId() {
        return scopeId;
    }

    @Override
    public ApScope getApScope() {
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

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof ApRecord)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        ApRecord other = (ApRecord) obj;

        return new EqualsBuilder().append(recordId, other.getRecordId()).isEquals();
    }

    public Integer getApTypeId() {
        return apTypeId;
    }

    public void setApTypeId(final Integer apTypeId) {
        this.apTypeId = apTypeId;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(recordId).toHashCode();
    }

    @Override
    public String toString() {
        return "ApRecord pk=" + recordId;
    }
}
