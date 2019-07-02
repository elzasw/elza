package cz.tacr.elza.domain;

import cz.tacr.elza.api.interfaces.IApScope;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.interfaces.Versionable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;

@Entity(name = "ap_state")
@Cache(region = "domain", usage = CacheConcurrencyStrategy.READ_WRITE)
public class ApState extends AbstractVersionableEntity implements IApScope, Versionable {

    public static final String FIELD_AP_TYPE_ID = "apTypeId";
    public static final String FIELD_SCOPE_ID = "scopeId";
    public static final String FIELD_DELETE_CHANGE_ID = "deleteChangeId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer stateId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = "accessPointId")
    private ApAccessPoint accessPoint;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer accessPointId;

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
    @JoinColumn(name = FIELD_DELETE_CHANGE_ID)
    private ApChange deleteChange;

    @Column(updatable = false, insertable = false)
    private Integer deleteChangeId;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM)
    private ApStateApproval stateApproval;

    @Column(length = StringLength.LENGTH_2000)
    private String comment;

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
        this.createChangeId = createChange != null ? createChange.getChangeId() : null;
    }

    public ApChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(ApChange deleteChange) {
        this.deleteChange = deleteChange;
        this.deleteChangeId = deleteChange != null ? deleteChange.getChangeId() : null;
    }

    public Integer getStateId() {
        return stateId;
    }

    public void setStateId(Integer stateId) {
        this.stateId = stateId;
    }

    public ApAccessPoint getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(ApAccessPoint accessPoint) {
        this.accessPoint = accessPoint;
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public void setAccessPointId(Integer accessPointId) {
        this.accessPointId = accessPointId;
    }

    public void setApTypeId(Integer apTypeId) {
        this.apTypeId = apTypeId;
    }

    public void setScopeId(Integer scopeId) {
        this.scopeId = scopeId;
    }

    public void setCreateChangeId(Integer createChangeId) {
        this.createChangeId = createChangeId;
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }

    public void setDeleteChangeId(Integer deleteChangeId) {
        this.deleteChangeId = deleteChangeId;
    }

    public ApStateApproval getStateApproval() {
        return stateApproval;
    }

    public void setStateApproval(ApStateApproval stateApproval) {
        this.stateApproval = stateApproval;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
