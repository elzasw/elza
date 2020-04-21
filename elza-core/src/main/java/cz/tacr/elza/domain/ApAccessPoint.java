package cz.tacr.elza.domain;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.annotations.Type;

import cz.tacr.elza.api.interfaces.IApAccessPoint;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.interfaces.Versionable;

/**
 * Rejstříkové heslo.
 */
@Entity(name = "ap_access_point")
public class ApAccessPoint extends AbstractVersionableEntity implements Versionable, IApAccessPoint {

    public static final String FIELD_ACCESS_POINT_ID = "accessPointId";
    public static final String FIELD_UUID = "uuid";
    public static final String FIELD_NAMES = "names";
    public static final String FIELD_DESCRIPTIONS = "descriptions";
    public static final String STATE = "state";
    public static final String RULE_SYSTEM_ID = "ruleSystemId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer accessPointId;

    @Column(length = StringLength.LENGTH_36, nullable = false, unique = true)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApRuleSystem.class)
    @JoinColumn(name = RULE_SYSTEM_ID)
    private ApRuleSystem ruleSystem;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM)
    private ApStateEnum state;

    @Column
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String errorDescription;

    // without getter/setter only for JPA query
    @OneToMany(mappedBy = "accessPoint")
    private List<ApName> names;

    // without getter/setter only for JPA query
    @OneToMany(mappedBy = "accessPoint")
    private List<ApDescription> descriptions;

    @Column
    private LocalDateTime lastUpdate;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApPart.class)
    @JoinColumn(name = "preferred_part_id")
    private ApPart preferredPart;

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

    public ApRuleSystem getRuleSystem() {
        return ruleSystem;
    }

    public void setRuleSystem(final ApRuleSystem ruleSystem) {
        this.ruleSystem = ruleSystem;
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

    public ApPart getPreferredPart() {
        return preferredPart;
    }

    public void setPreferredPart(ApPart preferredPart) {
        this.preferredPart = preferredPart;
    }

    @Override
    public String toString() {
        return "ApAccessPoint pk=" + accessPointId;
    }
}
