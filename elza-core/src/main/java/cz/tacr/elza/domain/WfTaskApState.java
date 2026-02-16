package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity(name = "wf_task_ap_state")
public class WfTaskApState {

    public static final String FIELD_STATE = "state";
    public static final String FIELD_TASK = "task";

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer taskApStateId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = WfTask.class)
    @JoinColumn(name = "taskId", nullable = false)
    private WfTask task;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer taskId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApState.class)
    @JoinColumn(name = "stateId", nullable = false)
    private ApState state;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer stateId;

	public Integer getTaskApStateId() {
		return taskApStateId;
	}

	public void setTaskApStateId(Integer taskApStateId) {
		this.taskApStateId = taskApStateId;
	}

	public Integer getTaskId() {
		return taskId;
	}

	public WfTask getTask() {
		return task;
	}

	public void setTask(WfTask task) {
		this.task = task;
		this.taskId = task != null ? task.getTaskId() : null;
	}

	public Integer getStateId() {
		return stateId;
	}

	public ApState getState() {
		return state;
	}

	public void setState(ApState state) {
		this.state = state;
		this.stateId = state != null ? state.getStateId() : null;
	}
}
