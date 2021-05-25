package cz.tacr.elza.domain;

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
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.api.interfaces.IApScope;
import cz.tacr.elza.domain.enumeration.StringLength;
import cz.tacr.elza.domain.interfaces.Versionable;
import cz.tacr.elza.service.cache.AccessPointCacheSerializable;

@Entity(name = "ap_state")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class ApState extends AbstractVersionableEntity implements IApScope, Versionable, AccessPointCacheSerializable {

    public static final String FIELD_ACCESS_POINT = "accessPoint";
    public static final String FIELD_ACCESS_POINT_ID = "accessPointId";
    public static final String FIELD_AP_TYPE_ID = "apTypeId";
    public static final String FIELD_SCOPE_ID = "scopeId";
    public static final String FIELD_STATE_APPROVAL = "stateApproval";
    public static final String FIELD_CREATE_CHANGE_ID = "createChangeId";
    public static final String FIELD_DELETE_CHANGE_ID = "deleteChangeId";
    public static final String FIELD_REPLACED_BY = "replacedBy";
    public static final String FIELD_CREATE_CHANGE = "createChange";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer stateId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = FIELD_ACCESS_POINT_ID, nullable = false)
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
    @JoinColumn(name = FIELD_CREATE_CHANGE_ID, nullable = false)
    private ApChange createChange;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = FIELD_DELETE_CHANGE_ID)
    private ApChange deleteChange;

    @Column(updatable = false, insertable = false)
    private Integer deleteChangeId;

    @Enumerated(EnumType.STRING)
    @Column(name = FIELD_STATE_APPROVAL, length = StringLength.LENGTH_ENUM, nullable = false)
    private ApState.StateApproval stateApproval;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApAccessPoint.class)
    @JoinColumn(name = FIELD_REPLACED_BY, nullable = true)
    private ApAccessPoint replacedBy;

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
        this.accessPointId = accessPoint != null ? accessPoint.getAccessPointId() : null;
    }

    public Integer getAccessPointId() {
        return accessPointId;
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }

    public ApState.StateApproval getStateApproval() {
        return stateApproval;
    }

    public void setStateApproval(ApState.StateApproval stateApproval) {
        this.stateApproval = stateApproval;
    }

    public ApAccessPoint getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(ApAccessPoint replacedBy) {
        this.replacedBy = replacedBy;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * Stav přístupového bodu.
     */
    public enum StateApproval implements AccessPointCacheSerializable {

        /**
         * Nový přístupový bod.
         */
        NEW,

        /**
         * Připraven ke schválení.
         */
        TO_APPROVE,

        /**
         * Schválený.
         */
        APPROVED,

        /**
         * K doplnění.
         */
        TO_AMEND,

        /**
         * Revize v přípravě. 
         */
        REV_NEW,

        /**
         * Revize ke schválení.
         */
        REV_PREPARED,

        /**
         * Revize k doplnění.
         */
        REV_AMEND;

    }
}
