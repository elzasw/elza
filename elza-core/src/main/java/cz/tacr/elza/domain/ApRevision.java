package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.enumeration.StringLength;

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

@Entity(name = "ap_revision")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "preferredPart", "lastUpdate"})
public class ApRevision {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer revisionId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApState.class)
    @JoinColumn(name = "state_id", nullable = false)
    private ApState state;

    @Column(name = "state_id", nullable = false, updatable = false, insertable = false)
    private Integer stateId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApType.class)
    @JoinColumn(name = "type_id", nullable = false)
    private ApType type;

    @Column(name = "type_id", nullable = false, updatable = false, insertable = false)
    private Integer typeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApPart.class)
    @JoinColumn(name = "preferred_part_id")
    private ApPart preferredPart;

    @Column(name = "preferred_part_id", updatable = false, insertable = false)
    private Integer preferredPartId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApRevPart.class)
    @JoinColumn(name = "rev_preferred_part_id")
    private ApRevPart revPreferredPart;

    @Column(name = "rev_preferred_part_id", updatable = false, insertable = false)
    private Integer revPreferredPartId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "create_change_id", nullable = false)
    private ApChange createChange;

    @Column(name = "create_change_id", nullable = false, updatable = false, insertable = false)
    private Integer createChangeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApChange.class)
    @JoinColumn(name = "delete_change_id")
    private ApChange deleteChange;

    @Column(name = "delete_change_id", updatable = false, insertable = false)
    private Integer deleteChangeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "state_approval", length = StringLength.LENGTH_ENUM, nullable = false)
    private ApRevision.StateApproval stateApproval;

    public Integer getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(Integer revisionId) {
        this.revisionId = revisionId;
    }

    public ApState getState() {
        return state;
    }

    public void setState(ApState state) {
        this.state = state;
        this.stateId = state != null ? state.getStateId() : null;
    }

    public ApType getType() {
        return type;
    }

    public void setType(ApType type) {
        this.type = type;
        this.typeId = type != null ? type.getApTypeId() : null;
    }

    public ApPart getPreferredPart() {
        return preferredPart;
    }

    public void setPreferredPart(ApPart preferredPart) {
        this.preferredPart = preferredPart;
        this.preferredPartId = preferredPart != null ? preferredPart.getPartId() : null;
    }

    public ApRevPart getRevPreferredPart() {
        return revPreferredPart;
    }

    public void setRevPreferredPart(ApRevPart revPreferredPart) {
        this.revPreferredPart = revPreferredPart;
        this.revPreferredPartId = revPreferredPart != null ? revPreferredPart.getPartId() : null;
    }

    public ApChange getCreateChange() {
        return createChange;
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

    public StateApproval getStateApproval() {
        return stateApproval;
    }

    public void setStateApproval(StateApproval stateApproval) {
        this.stateApproval = stateApproval;
    }

    public Integer getStateId() {
        return stateId;
    }

    public Integer getTypeId() {
        return typeId;
    }

    public Integer getPreferredPartId() {
        return preferredPartId;
    }

    public Integer getRevPreferredPartId() {
        return revPreferredPartId;
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }

    /**
     * Stav revize.
     */
    public enum StateApproval {

        /**
         * Nová revize přístupového bodu.
         */
        ACTIVE,

        /**
         * Připraven ke schválení.
         */
        TO_APPROVE,

        /**
         * K doplnění.
         */
        TO_AMEND;

    }

}
