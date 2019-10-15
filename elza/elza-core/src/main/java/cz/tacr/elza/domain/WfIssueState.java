package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;

@Entity(name = "wf_issue_state")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class WfIssueState {

    // --- fields ---

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer issueStateId;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean startState;

    @Column(nullable = false)
    private boolean finalState;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private RulPackage rulPackage;

    // --- getters/setters ---

    public Integer getIssueStateId() {
        return issueStateId;
    }

    public void setIssueStateId(Integer issueStateId) {
        this.issueStateId = issueStateId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isStartState() {
        return startState;
    }

    public void setStartState(boolean startState) {
        this.startState = startState;
    }

    public boolean isFinalState() {
        return finalState;
    }

    public void setFinalState(boolean finalState) {
        this.finalState = finalState;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    // --- methods ---

    @Override
    public String toString() {
        return "WfIssueState pk=" + issueStateId;
    }
}
