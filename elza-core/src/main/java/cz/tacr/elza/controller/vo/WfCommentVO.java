package cz.tacr.elza.controller.vo;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

/**
 * @author <a href="mailto:stepan.marek@marbes.cz">Stepan Marek</a>
 */
public class WfCommentVO {

    // --- fields ---

    private Integer id;
    @NotNull
    private Integer issueId;
    @NotBlank
    private String comment;
    private Integer userId;
    private Integer prevStateId;
    private Integer nextStateId;
    private LocalDateTime timeCreated;

    // --- getters/setters ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIssueId() {
        return issueId;
    }

    public void setIssueId(Integer issueId) {
        this.issueId = issueId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getPrevStateId() {
        return prevStateId;
    }

    public void setPrevStateId(Integer prevStateId) {
        this.prevStateId = prevStateId;
    }

    public Integer getNextStateId() {
        return nextStateId;
    }

    public void setNextStateId(Integer nextStateId) {
        this.nextStateId = nextStateId;
    }

    public LocalDateTime getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(LocalDateTime timeCreated) {
        this.timeCreated = timeCreated;
    }
}
