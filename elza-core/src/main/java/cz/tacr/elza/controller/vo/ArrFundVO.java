package cz.tacr.elza.controller.vo;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * VO pro archivní pomůcku.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 7. 1. 2016
 */
public class ArrFundVO extends ArrFundBaseVO {

    private Date createDate;

    private String internalCode;

    private Integer institutionId;

    private List<ArrFundVersionVO> versions = new LinkedList<>();

    private List<ApScopeVO> apScopes;

    private List<ArrOutputDefinitionVO> validNamedOutputs;

    private List<ArrOutputDefinitionVO> historicalNamedOutputs;

    /**
     * Seznam otevřených připomínek.
     */
    private List<WfSimpleIssueVO> issues;

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public List<ArrFundVersionVO> getVersions() {
        return versions;
    }

    public void setVersions(List<ArrFundVersionVO> versions) {
        this.versions = versions;
    }

    public List<ApScopeVO> getApScopes() {
        return apScopes;
    }

    public void setApScopes(List<ApScopeVO> apScopes) {
        this.apScopes = apScopes;
    }

    public String getInternalCode() {
        return internalCode;
    }

    public void setInternalCode(final String internalCode) {
        this.internalCode = internalCode;
    }

    public Integer getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(final Integer institutionId) {
        this.institutionId = institutionId;
    }

    public List<ArrOutputDefinitionVO> getValidNamedOutputs() {
        return validNamedOutputs;
    }

    public void setValidNamedOutputs(final List<ArrOutputDefinitionVO> validNamedOutputs) {
        this.validNamedOutputs = validNamedOutputs;
    }

    public List<ArrOutputDefinitionVO> getHistoricalNamedOutputs() {
        return historicalNamedOutputs;
    }

    public void setHistoricalNamedOutputs(final List<ArrOutputDefinitionVO> historicalNamedOutputs) {
        this.historicalNamedOutputs = historicalNamedOutputs;
    }

    public List<WfSimpleIssueVO> getIssues() {
        return issues;
    }

    public void setIssues(List<WfSimpleIssueVO> issues) {
        this.issues = issues;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
