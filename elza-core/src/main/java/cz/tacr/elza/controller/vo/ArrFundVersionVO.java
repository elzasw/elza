package cz.tacr.elza.controller.vo;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import javax.annotation.Nullable;

/**
 * VO pro verzi archivní pomůcky.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 7. 1. 2016
 */
public class ArrFundVersionVO {

    private Integer id;

    private Date createDate;

    private Date lockDate;

    private String dateRange;

    private Integer ruleSetId;

    private Boolean strictMode;

    private Integer packageId;

    /**
     * Seznam otevřených připomínek.
     */
    @Nullable
    private List<WfSimpleIssueVO> issues;

    /**
     * Nastavení zobrazení lektorování.
     */
    @Nullable
    private WfConfigVO config;

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(final Date createDate) {
        this.createDate = createDate;
    }

    public Date getLockDate() {
        return lockDate;
    }

    public void setLockDate(final Date lockDate) {
        this.lockDate = lockDate;
    }

    public String getDateRange() {
        return dateRange;
    }

    public void setDateRange(final String dateRange) {
        this.dateRange = dateRange;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(final Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    public Boolean getStrictMode() {
        return strictMode;
    }

    public void setStrictMode(final Boolean strictMode) {
        this.strictMode = strictMode;
    }

    public Integer getPackageId() {
        return packageId;
    }

    public void setPackageId(final Integer packageId) {
        this.packageId = packageId;
    }

    public List<WfSimpleIssueVO> getIssues() {
        return issues;
    }

    public void setIssues(List<WfSimpleIssueVO> issues) {
        this.issues = issues;
    }

    public WfConfigVO getConfig() {
        return config;
    }

    public void setConfig(final WfConfigVO config) {
        this.config = config;
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
