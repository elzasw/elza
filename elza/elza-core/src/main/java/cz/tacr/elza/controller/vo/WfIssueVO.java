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
     * Referenční označení jednotky popisu od kořene k uzlu - při zakládání není vyplněno
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String[] referenceMark;

    /**
     * Příznak, zda byla odkazovaná úroveň vymazána/zneplatněna
     */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private Boolean levelDeleted = false;

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
     * Uživatel, který připomínku založil - doplní systém dle přihlášeného uživatele - při zakládání není vyplněno
     */
    private UsrUserVO userCreate;

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

    public String[] getReferenceMark() {
        return referenceMark;
    }

    public void setReferenceMark(String[] referenceMark) {
        this.referenceMark = referenceMark;
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

    public Boolean getLevelDeleted() {
        return levelDeleted;
    }

    public void setLevelDeleted(Boolean levelDeleted) {
        this.levelDeleted = levelDeleted;
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

    public UsrUserVO getUserCreate() {
        return userCreate;
    }

    public void setUserCreate(UsrUserVO userCreate) {
        this.userCreate = userCreate;
    }

    public LocalDateTime getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(LocalDateTime timeCreated) {
        this.timeCreated = timeCreated;
    }
}
