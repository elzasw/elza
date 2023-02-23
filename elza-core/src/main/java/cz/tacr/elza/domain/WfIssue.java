package cz.tacr.elza.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;

import org.hibernate.annotations.Type;

import cz.tacr.elza.api.interfaces.IArrFund;
import cz.tacr.elza.api.interfaces.IWfIssueList;

/**
 * Jednotlivý problém
 */
@Entity(name = "wf_issue")
public class WfIssue implements IArrFund, IWfIssueList {

    public static final String FIELD_NODE_ID = "nodeId";

    // --- fields ---

    /**
     * Indentifikátor připomínky
     */
    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer issueId;

    /**
     * Protokol
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = WfIssueList.class)
    @JoinColumn(name = "issue_list_id", nullable = false)
    private WfIssueList issueList;

    /**
     * Číslo připomínky v rámci protokolu - generované systémem
     */
    @Column(nullable = false)
    private Integer number;

    /**
     * Uzel
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrNode.class, optional = true)
    @JoinColumn(name = FIELD_NODE_ID, nullable = true)
    private ArrNode node;

    @Column(name = FIELD_NODE_ID, nullable = true, updatable = false, insertable = false)
    protected Integer nodeId;

    /**
     * Druh připomínky
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = WfIssueType.class, optional = false)
    @JoinColumn(name = "issue_type_id", nullable = false)
    private WfIssueType issueType;

    /**
     * Stav připomínky
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = WfIssueState.class, optional = false)
    @JoinColumn(name = "issue_state_id", nullable = false)
    private WfIssueState issueState;

    /**
     * Text připomínky
     */
    @Column(nullable = false)
    @Lob
    //@Type(type = "org.hibernate.type.TextType") TODO pasek
    private String description;

    /**
     * Indentifikátor uživatele, který připomínku založil
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = UsrUser.class, optional = false)
    @JoinColumn(name = "user_create_id", nullable = false)
    private UsrUser userCreate;

    /**
     * Datum a čas založení této připomínky
     */
    @Column(nullable = false)
    private LocalDateTime timeCreated;

    // --- getters/setters ---

    public Integer getIssueId() {
        return issueId;
    }

    public void setIssueId(Integer issueId) {
        this.issueId = issueId;
    }

    public WfIssueList getIssueList() {
        return issueList;
    }

    public void setIssueList(WfIssueList issueList) {
        this.issueList = issueList;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public ArrNode getNode() {
        return node;
    }

    public void setNode(ArrNode node) {
        this.node = node;
        this.nodeId = (node != null) ? node.getNodeId() : null;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public WfIssueType getIssueType() {
        return issueType;
    }

    public void setIssueType(WfIssueType issueType) {
        this.issueType = issueType;
    }

    public WfIssueState getIssueState() {
        return issueState;
    }

    public void setIssueState(WfIssueState issueState) {
        this.issueState = issueState;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UsrUser getUserCreate() {
        return userCreate;
    }

    public void setUserCreate(UsrUser userCreate) {
        this.userCreate = userCreate;
    }

    public LocalDateTime getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(LocalDateTime timeCreated) {
        this.timeCreated = timeCreated;
    }

    // --- methods ---

    @Override
    public ArrFund getFund() {
        return issueList != null ? issueList.getFund() : null;
    }

    @Override
    public Integer getIssueListId() {
        return issueList != null ? issueList.getIssueListId() : null;
    }

    @Override
    public String toString() {
        return "WfIssue pk=" + issueId;
    }
}
