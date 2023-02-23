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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;

@Entity(name = "wf_issue_type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class WfIssueType {

    // --- fields ---

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer issueTypeId;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer viewOrder;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private RulPackage rulPackage;

    // --- getters/setters ---

    public Integer getIssueTypeId() {
        return issueTypeId;
    }

    public void setIssueTypeId(Integer issueTypeId) {
        this.issueTypeId = issueTypeId;
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

    public Integer getViewOrder() {
        return viewOrder;
    }

    public void setViewOrder(Integer viewOrder) {
        this.viewOrder = viewOrder;
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
        return "WfIssueType pk=" + issueTypeId;
    }
}
