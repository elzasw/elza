package cz.tacr.elza.domain;

import java.time.OffsetDateTime;

import org.hibernate.Length;

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

@Entity(name = "wf_task")
public class WfTask {

    public static final String FIELD_TIME_CLOSED = "timeClosed";
	public static final String FIELD_ASSIGNEE_ID = "assigneeId";

    public enum Status {
    	NEW, CANCELLED, FINISHED
    }

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer taskId;

    @Column(nullable = false)
    private OffsetDateTime timeCreated;

    @Column(nullable = true)
    private OffsetDateTime timeClosed;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "assigneeId", nullable = false)
    private UsrUser assignee;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer assigneeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "creatorId", nullable = true)
    private UsrUser creator;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer creatorId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class)
    @JoinColumn(name = "closedById", nullable = true)
    private UsrUser closedBy;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer closedById;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_10, nullable = false)
    private WfTask.Status status;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = WfTaskType.class)
    @JoinColumn(name = "taskTypeId", nullable = false)
    private WfTaskType taskType;

    @Column(nullable = false, updatable = false, insertable = false)
    private Integer taskTypeId;

    @Column(length = Length.LONG, nullable = true)
    private String description;

	public Integer getTaskId() {
		return taskId;
	}

	public void setTaskId(Integer taskId) {
		this.taskId = taskId;
	}

	public OffsetDateTime getTimeCreated() {
		return timeCreated;
	}

	public void setTimeCreated(OffsetDateTime timeCreated) {
		this.timeCreated = timeCreated;
	}

	public OffsetDateTime getTimeClosed() {
		return timeClosed;
	}

	public void setTimeClosed(OffsetDateTime timeClosed) {
		this.timeClosed = timeClosed;
	}

	public Integer getAssigneeId() {
		return assigneeId;
	}

	public UsrUser getAssignee() {
		return assignee;
	}

	public void setAssignee(UsrUser assignee) {
		this.assignee = assignee;
		this.assigneeId = assignee != null ? assignee.getUserId() : null;
	}

	public Integer getCreatorId() {
		return creatorId;
	}

	public UsrUser getCreator() {
		return creator;
	}

	public void setCreator(UsrUser creator) {
		this.creator = creator;
		this.creatorId = creator != null ? creator.getUserId() : null;
	}

	public Integer getClosedById() {
		return closedById;
	}

	public UsrUser getClosedBy() {
		return closedBy;
	}

	public void setClosedBy(UsrUser closedBy) {
		this.closedBy = closedBy;
		this.closedById = closedBy != null ? closedBy.getUserId() : null;
	}

	public WfTask.Status getStatus() {
		return status;
	}

	public void setStatus(WfTask.Status status) {
		this.status = status;
	}

	public Integer getTaskTypeId() {
		return taskTypeId;
	}

	public WfTaskType getTaskType() {
		return taskType;
	}

	public void setTaskType(WfTaskType taskType) {
		this.taskType = taskType;
		this.taskTypeId = taskType != null ? taskType.getTaskTypeId() : null;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
