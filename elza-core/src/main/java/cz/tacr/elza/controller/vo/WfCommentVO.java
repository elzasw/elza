package cz.tacr.elza.controller.vo;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

/**
 * Komentář k připomínce
 */
public class WfCommentVO {

    // --- fields ---

    /**
     * Indentifikátor komentáře - při zakládání není vyplněno
     */
    private Integer id;

    /**
     * Indentifikátor připomínky - při zakládání povinné
     */
    @NotNull
    private Integer issueId;

    /**
     * Text komentáře - při zakládání povinné
     */
    @NotBlank
    private String comment;

    /**
     * Indentifikátor uživatele, který komentář založil - doplní systém dle přihlášeného uživatele - při zakládání není vyplněno
     */
    @NotNull
    private Integer userId;

    /**
     * Indentifikátor stavu připomínky před založením tohoto komentáře - při zakládání není vyplněno
     */
    private Integer prevStateId;

    /**
     * Indentifikátor stavu připomínky po založení tohoto komentáře - při zakládání volitelné
     */
    private Integer nextStateId;

    /**
     * Datum a čas založení tohoto komentáře - při zakládání není vyplněno
     */
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
