package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;
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
import jakarta.persistence.ManyToOne;

@Entity(name = "ap_rev_state")
public class ApRevState {

    public static final String FIELD_REVISION = "revision";
    public static final String FIELD_DELETE_CHANGE_ID = "deleteChangeId";
    public static final String FIELD_STATE_APPROVAL = "stateApproval";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer stateId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApRevision.class)
    @JoinColumn(name = "revisionId", nullable = false)
    private ApRevision revision;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer revisionId;

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
    private RevStateApproval stateApproval;

    private String comment;

    public Integer getStateId() {
        return stateId;
    }

    public void setStateId(Integer stateId) {
        this.stateId = stateId;
    }

    public ApRevision getRevision() {
        return revision;
    }

    public void setRevision(ApRevision revision) {
        this.revision = revision;
        this.revisionId = revision != null ? revision.getRevisionId() : null;
    }

    public Integer getRevisionId() {
        return revisionId;
    }

    public ApType getType() {
        return type;
    }

    public void setType(ApType type) {
        this.type = type;
        this.typeId = type != null ? type.getApTypeId() : null;
    }
    
    public Integer getTypeId() {
        return typeId;
    }

    public ApPart getPreferredPart() {
        return preferredPart;
    }

    public void setPreferredPart(ApPart preferredPart) {
        this.preferredPart = preferredPart;
        this.preferredPartId = preferredPart != null ? preferredPart.getPartId() : null;
    }

    public Integer getPreferredPartId() {
        return preferredPartId;
    }

    public ApRevPart getRevPreferredPart() {
        return revPreferredPart;
    }

    public void setRevPreferredPart(ApRevPart revPreferredPart) {
        this.revPreferredPart = revPreferredPart;
        this.revPreferredPartId = revPreferredPart != null ? revPreferredPart.getPartId() : null;
    }

    public Integer getRevPreferredPartId() {
        return revPreferredPartId;
    }

    public ApChange getCreateChange() {
        return createChange;
    }

    public void setCreateChange(ApChange createChange) {
        this.createChange = createChange;
        this.createChangeId = createChange != null ? createChange.getChangeId() : null;
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public ApChange getDeleteChange() {
        return deleteChange;
    }

    public void setDeleteChange(ApChange deleteChange) {
        this.deleteChange = deleteChange;
        this.deleteChangeId = deleteChange != null ? deleteChange.getChangeId() : null;
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }

    public RevStateApproval getStateApproval() {
        return stateApproval;
    }

    public void setStateApproval(RevStateApproval stateApproval) {
        this.stateApproval = stateApproval;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
