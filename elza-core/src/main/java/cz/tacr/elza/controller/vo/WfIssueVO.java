package cz.tacr.elza.controller.vo;

import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Připomínka
 */
public class WfIssueVO {

    // --- fields ---

    /**
     * Indentifikátor připomínky - při zakládání není vyplněno
     */
    private Integer id;

    /**
     * Indentifikátor protokolu - při zakládání povinné
     */
    @NotNull
    private Integer issueListId;

    /**
     * Číslo připomínky v rámci protokolu - generované systémem - při zakládání není vyplněno
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer number;

    /**
     * Indentifikátor uzlu - při zakládání volitelné
     */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private Integer nodeId;

    /**
     * Indentifikátor druhu připomínky - při zakládání povinné
     */
    @NotNull
    private Integer issueTypeId;

    /**
     * Indentifikátor stavu připomínky - při zakládání není vyplněno
     */
    private Integer issueStateId;

    /**
     * Text připomínky - při zakládání povinné
     */
    @NotBlank
    private String description;

    /**
     * Indentifikátor uživatele, který připomínku založil - doplní systém dle přihlášeného uživatele - při zakládání není vyplněno
     */
    private Integer userCreateId;

    /**
     * Datum a čas založení této připomínky
     */
    private LocalDateTime timeCreated;

    // --- getters/setters ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIssueListId() {
        return issueListId;
    }

    public void setIssueListId(Integer issueListId) {
        this.issueListId = issueListId;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public Integer getIssueTypeId() {
        return issueTypeId;
    }

    public void setIssueTypeId(Integer issueTypeId) {
        this.issueTypeId = issueTypeId;
    }

    public Integer getIssueStateId() {
        return issueStateId;
    }

    public void setIssueStateId(Integer issueStateId) {
        this.issueStateId = issueStateId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getUserCreateId() {
        return userCreateId;
    }

    public void setUserCreateId(Integer userCreateId) {
        this.userCreateId = userCreateId;
    }

    public LocalDateTime getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(LocalDateTime timeCreated) {
        this.timeCreated = timeCreated;
    }
}
