package cz.tacr.elza.domain;

import java.time.LocalDateTime;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Type;

import cz.tacr.elza.api.interfaces.IWfIssueList;

@Entity(name = "wf_comment")
public class WfComment implements IWfIssueList {

    // --- fields ---

    /**
     * Indentifikátor komentáře
     */
    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer commentId;

    /**
     * Připomínka
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = WfIssue.class, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    private WfIssue issue;

    /**
     * Text komentáře
     */
    @Column(nullable = false)
    @Lob
    @Type(type = "org.hibernate.type.TextType")
    private String comment;

    /**
     * Uživatel, který komentář založil
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UsrUser user;

    /**
     * Stav připomínky před založením tohoto komentáře
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = WfIssueState.class, optional = false)
    @JoinColumn(name = "prev_state_id", nullable = false)
    private WfIssueState prevState;

    /**
     * Stavu připomínky po založení tohoto komentáře
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = WfIssueState.class, optional = false)
    @JoinColumn(name = "next_state_id", nullable = false)
    private WfIssueState nextState;

    /**
     * Datum a čas založení tohoto komentáře
     */
    @Column(nullable = false)
    private LocalDateTime timeCreated;

    // --- getters/setters ---

    public Integer getCommentId() {
        return commentId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
    }

    public WfIssue getIssue() {
        return issue;
    }

    public void setIssue(WfIssue issue) {
        this.issue = issue;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public UsrUser getUser() {
        return user;
    }

    public void setUser(UsrUser user) {
        this.user = user;
    }

    public WfIssueState getPrevState() {
        return prevState;
    }

    public void setPrevState(WfIssueState prevState) {
        this.prevState = prevState;
    }

    public WfIssueState getNextState() {
        return nextState;
    }

    public void setNextState(WfIssueState nextState) {
        this.nextState = nextState;
    }

    public LocalDateTime getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(LocalDateTime timeCreated) {
        this.timeCreated = timeCreated;
    }

    // --- methods ---

    @Override
    public Integer getIssueListId() {
        return issue != null ? issue.getIssueListId() : null;
    }

    @Override
    public String toString() {
        return "WfComment pk=" + commentId;
    }
}
