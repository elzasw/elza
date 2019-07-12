package cz.tacr.elza.domain;

import java.time.LocalDateTime;
import java.util.List;

import javax.persistence.*;

import cz.tacr.elza.api.interfaces.IApAccessPoint;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.interfaces.Versionable;
import org.hibernate.annotations.Type;

/**
 * Rejstříkové heslo.
 */
@Entity(name = "ap_access_point")
public class ApAccessPoint extends AbstractVersionableEntity implements Versionable, IApAccessPoint {

    // todo[ap_state]: odtranit konstanty pro fieldy apType, scope, deleteChange, createChange
    public static final String FIELD_ACCESS_POINT_ID = "accessPointId";
    public static final String FIELD_UUID = "uuid";
    public static final String FIELD_AP_TYPE = "apType";
    public static final String FIELD_AP_TYPE_ID = "apTypeId";
    public static final String FIELD_SCOPE = "scope";
    public static final String FIELD_SCOPE_ID = "scopeId";
    public static final String FIELD_NAMES = "names";
    public static final String FIELD_DESCRIPTIONS = "descriptions";
    public static final String FIELD_DELETE_CHANGE_ID = "deleteChangeId";
    public static final String FIELD_CREATE_CHANGE_ID = "createChangeId";
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

    @Override
    public String toString() {
        return "ApAccessPoint pk=" + accessPointId;
    }
}
