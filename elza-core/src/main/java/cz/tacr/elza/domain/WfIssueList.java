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

import cz.tacr.elza.api.interfaces.IArrFund;
import cz.tacr.elza.api.interfaces.IWfIssueList;
import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Seznam problémů
 */
@Entity(name = "wf_issue_list")
public class WfIssueList implements IArrFund, IWfIssueList {

    // --- fields ---

    /**
     * Indentifikátor protokolu
     */
    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer issueListId;

    /**
     * Archivní soubor
     */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ArrFund.class, optional = false)
    @JoinColumn(name = "fund_id", nullable = false)
    private ArrFund fund;

    @Column(name = "fund_id", updatable = false, insertable = false)
    private Integer fundId;

    /**
     * Stav protokolu (otevřený/uzavřený)
     */
    @Column(nullable = false)
    private Boolean open;

    /**
     * Název protokolu
     */
    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    // --- getters/setters ---

    @Override
    public Integer getIssueListId() {
        return issueListId;
    }

    public void setIssueListId(Integer issueListId) {
        this.issueListId = issueListId;
    }

    public ArrFund getFund() {
        return fund;
    }

    public Integer getFundId() {
        return fundId;
    }

    public void setFund(ArrFund fund) {
        this.fund = fund;
        this.fundId = fund == null ? null : fund.getFundId();
    }

    public Boolean getOpen() {
        return open;
    }

    public void setOpen(Boolean open) {
        this.open = open;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // --- methods ---

    @Override
    public String toString() {
        return "WfIssueList pk=" + issueListId;
    }
}
