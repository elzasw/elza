package cz.tacr.elza.domain;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.Length;
import org.hibernate.annotations.Type;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.api.interfaces.IApAccessPoint;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.interfaces.Versionable;

/**
 * Rejstříkové heslo.
 */
@Entity(name = "ap_access_point")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "preferredPart", "lastUpdate"})
public class ApAccessPoint extends AbstractVersionableEntity implements Versionable, IApAccessPoint {

    public static final String FIELD_ACCESS_POINT_ID = "accessPointId";
    public static final String FIELD_UUID = "uuid";
    public static final String FIELD_NAMES = "names";
    public static final String FIELD_DESCRIPTIONS = "descriptions";
    public static final String FIELD_LAST_UPDATE = "lastUpdate";
    public static final String STATE = "state";
    public static final String RULE_SYSTEM_ID = "ruleSystemId";
    public static final String FIELD_PREFFERED_PART = "preferredPart";
    public static final String FIELD_PREFFERED_PART_ID = "preferredPartId";
    public static final String FIELD_USER_LIST = "userList";
    public static final String PARTS = "parts";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer accessPointId;

    @Column(length = StringLength.LENGTH_36, nullable = false, unique = true)
    private String uuid;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private ApStateEnum state;

    @Column(length = Length.LONG) // Hibernate long text field
    private String errorDescription;

    @Column
    private LocalDateTime lastUpdate;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApPart.class)
    @JoinColumn(name = "preferred_part_id")
    private ApPart preferredPart;

    @Column(name = "preferred_part_id", updatable = false, insertable = false)
    private Integer preferredPartId;

    @OneToMany(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name= "accessPointId")
    private List<UsrUser> userList;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "accessPoint")
    private List<ApPart> parts;

    /**
     * ID hesla.
     *
     * @return id hesla
     */
    @Override
    public Integer getAccessPointId() {
        return accessPointId;
    }

    /**
     * ID hesla.
     *
     * @param accessPointId
     *            id hesla
     */
    public void setAccessPointId(final Integer accessPointId) {
        this.accessPointId = accessPointId;
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
     * @param uuid
     *            UUID
     */
    public void setUuid(final String uuid) {
        this.uuid = uuid;
    }

    public ApStateEnum getState() {
        return state;
    }

    public void setState(final ApStateEnum state) {
        this.state = state;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(final String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public LocalDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(final LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public Integer getPreferredPartId() {
        return preferredPartId;
    }

    public ApPart getPreferredPart() {
        return preferredPart;
    }

    public void setPreferredPart(ApPart preferredPart) {
        this.preferredPart = preferredPart;
        this.preferredPartId = (preferredPart != null) ? preferredPart.getPartId() : null;
    }

    @Override
    public String toString() {
        return "ApAccessPoint pk=" + accessPointId;
    }
}
