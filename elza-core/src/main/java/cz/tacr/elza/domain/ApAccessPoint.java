package cz.tacr.elza.domain;

import java.util.List;

import javax.persistence.*;

import cz.tacr.elza.api.interfaces.IApScope;
import cz.tacr.elza.domain.enumeration.StringLength;
import org.hibernate.annotations.Type;

/**
 * Rejstříkové heslo.
 */
@Entity(name = "ap_access_point")
public class ApAccessPoint implements IApScope {

    public static final String FIELD_ACCESS_POINT_ID = "accessPointId";
    public static final String FIELD_UUID = "uuid";
    public static final String FIELD_AP_TYPE = "apType";
    public static final String FIELD_AP_TYPE_ID = "apTypeId";
    public static final String FIELD_SCOPE = "scope";
    public static final String FIELD_SCOPE_ID = "scopeId";
    public static final String FIELD_NAMES = "names";
    public static final String FIELD_DESCRIPTIONS = "descriptions";
    public static final String FIELD_DELETE_CHANGE_ID = "deleteChangeId";
    public static final String STATE = "state";
    public static final String RULE_SYSTEM_ID = "ruleSystemId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer accessPointId;

    @Column(length = StringLength.LENGTH_36, nullable = false, unique = true)
    private String uuid;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApType.class)
    @JoinColumn(name = FIELD_AP_TYPE_ID, nullable = false)
    private ApType apType;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer apTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApScope.class)
    @JoinColumn(name = FIELD_SCOPE_ID, nullable = false)
    private ApScope scope;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer scopeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "createChangeId", nullable = false)
    private ApChange createChange;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = FIELD_DELETE_CHANGE_ID, nullable = true)
    private ApChange deleteChange;

    @Column(nullable = true, updatable = false, insertable = false)
    private Integer deleteChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApRuleSystem.class)
    @JoinColumn(name = RULE_SYSTEM_ID)
    private ApRuleSystem ruleSystem;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM)
    private ApState state;

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
     * @param apType
     *            typ rejstříku
     */
    public void setApType(final ApType apType) {
        this.apType = apType;
        this.apTypeId = apType != null ? apType.getApTypeId() : null;
    }

    public Integer getApTypeId() {
        return apTypeId;
    }

    /**
     * @return třída rejstříku
     */
    public ApScope getScope() {
        return scope;
    }

    /**
     * @param scope
     *            třída rejstříku
     */
    public void setScope(final ApScope scope) {
        this.scope = scope;
        this.scopeId = scope != null ? scope.getScopeId() : null;
    }

    @Override
    public Integer getScopeId() {
        return scopeId;
    }

    public ApChange getCreateChange() {
        return createChange;
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public void setCreateChange(ApChange createChange) {
        this.createChange = createChange;
    }

    public ApChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(ApChange deleteChange) {
        this.deleteChange = deleteChange;
    }

    public ApRuleSystem getRuleSystem() {
        return ruleSystem;
    }

    public void setRuleSystem(final ApRuleSystem ruleSystem) {
        this.ruleSystem = ruleSystem;
    }

    public ApState getState() {
        return state;
    }

    public void setState(final ApState state) {
        this.state = state;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(final String errorDescription) {
        this.errorDescription = errorDescription;
    }

    @Override
    public String toString() {
        return "ApAccessPoint pk=" + accessPointId;
    }
}
