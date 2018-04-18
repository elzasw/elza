package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.api.interfaces.IApScope;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.interfaces.Versionable;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.rest.core.annotation.RestResource;

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
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * Rejstříkové heslo.
 *
 * @author Martin Kužel [<a href="mailto:martin.kuzel@marbes.cz">martin.kuzel@marbes.cz</a>]
 * @since 21.8.2015
 */
@Entity(name = "ap_access_point")
@Table
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ApAccessPoint implements Serializable, IApScope {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer accessPointId;

    @RestResource(exported = false)
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApType.class)
    @JoinColumn(name = "apTypeId", nullable = false)
    @JsonIgnore
    private ApType apType;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer apTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApScope.class)
    @JoinColumn(name = "scopeId", nullable = false)
    @JsonIgnore
    private ApScope scope;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer scopeId;

    @RestResource(exported = false)
    @OneToMany(mappedBy = "accessPoint")
    @JsonIgnore
    private List<ApName> nameList = new ArrayList<>(0);

    @RestResource(exported = false)
    @OneToMany(mappedBy = "accessPoint", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ParRelationEntity> relationEntities = new ArrayList<>();

    @RestResource(exported = false)
    @OneToMany(mappedBy = "accessPoint")
    @JsonIgnore
    private List<ApDescription> descriptionList = new ArrayList<>(0);


    @Column(length = StringLength.LENGTH_36, nullable = false, unique = true)
    private String uuid;

    @Column(nullable = false)
    private boolean invalid;

    /* Konstanty pro vazby a fieldy. */
    public static final String AP_TYPE = "apType";
    public static final String ACCESS_POINT_ID = "accessPointId";
    public static final String SCOPE = "scope";
    public static final String UUID = "uuid";
    public static final String INVALID = "invalid";
    public static final String NAME_LIST = "nameList";
    public static final String DESCRIPTION_LIST = "descriptionList";

    /**
     * ID hesla.
     *
     * @return id hesla
     */
    public Integer getAccessPointId() {
        return accessPointId;
    }

    /**
     * ID hesla.
     *
     * @param accessPointId id hesla
     */
    public void setAccessPointId(final Integer accessPointId) {
        this.accessPointId = accessPointId;
    }

    /**
     * Typ rejstříku.
     *
     * @return typ rejstříku
     */
    public ApType getApType() {
        return apType;
    }

    /**
     * Typ rejstříku.
     *
     * @param apType typ rejstříku
     */
    public void setApType(final ApType apType) {
        this.apTypeId = apType == null ? null : apType.getApTypeId();
        this.apType = apType;
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

    /**
     * @return UUID
     */
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

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof cz.tacr.elza.domain.ApAccessPoint)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        cz.tacr.elza.domain.ApAccessPoint other = (cz.tacr.elza.domain.ApAccessPoint) obj;

        return new EqualsBuilder().append(accessPointId, other.getAccessPointId()).isEquals();
    }

    public Integer getApTypeId() {
        return apTypeId;
    }

    public void setApTypeId(final Integer apTypeId) {
        this.apTypeId = apTypeId;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(accessPointId).toHashCode();
    }

    @Override
    public String toString() {
        return "ApAccessPoint pk=" + accessPointId;
    }

    public List<ApName> getNameList() {
        return nameList;
    }

    public void setNameList(List<ApName> nameList) {
        this.nameList = nameList;
    }

    //todo [fric] odebrat az bude jasno co a jak s tim
    public void setLastUpdate(LocalDateTime now) {}
    public LocalDateTime getLastUpdate() {return null;}
}
