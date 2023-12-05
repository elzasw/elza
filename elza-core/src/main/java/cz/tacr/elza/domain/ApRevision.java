package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

@Entity(name = "ap_revision")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "preferredPart", "lastUpdate"})
public class ApRevision {

    public static final String FIELD_STATE = "state";
    public static final String FIELD_DELETE_CHANGE_ID = "deleteChangeId";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer revisionId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApState.class)
    @JoinColumn(name = "state_id", nullable = false)
    private ApState state;

    @Column(name = "state_id", nullable = false, updatable = false, insertable = false)
    private Integer stateId;

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

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApState.class)
    @JoinColumn(name = "merge_state_id", nullable = false)
    private ApState mergeState;

    @Column(name = "merge_state_id", nullable = false, updatable = false, insertable = false)
    private Integer mergeStateId;

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

    public Integer getStateId() {
        return stateId;
    }

    public Integer getCreateChangeId() {
        return createChangeId;
    }

    public Integer getDeleteChangeId() {
        return deleteChangeId;
    }

    public ApState getMergeState() {
        return mergeState;
    }

    public void setMergeState(ApState mergeState) {
        this.mergeState = mergeState;
        this.mergeStateId = mergeState != null ? mergeState.getStateId() : null;
    }

    public Integer getMergeStateId() {
        return mergeStateId;
    }

}
