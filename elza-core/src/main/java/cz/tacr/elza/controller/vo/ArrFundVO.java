package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.Column;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * VO pro archivní pomůcku.
 *
 * @since 7. 1. 2016
 */
public class ArrFundVO extends ArrFundBaseVO {

    private Date createDate;

    private String internalCode;

    private Integer institutionId;

    private List<ArrFundVersionVO> versions = new LinkedList<>();

    private List<ApScopeVO> apScopes;

    private List<ArrOutputVO> validNamedOutputs;

    private Integer fundNumber;

    private String unitdate;

    private String mark;

    public ArrFundVO() {

    }

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

    public List<ArrOutputVO> getValidNamedOutputs() {
        return validNamedOutputs;
    }

    public void setValidNamedOutputs(final List<ArrOutputVO> validNamedOutputs) {
        this.validNamedOutputs = validNamedOutputs;
    }

    public Integer getFundNumber() {
        return fundNumber;
    }

    public void setFundNumber(final Integer fundNumber) {
        this.fundNumber = fundNumber;
    }

    public String getUnitdate() {
        return unitdate;
    }

    public void setUnitdate(final String unitdate) {
        this.unitdate = unitdate;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(final String mark) {
        this.mark = mark;
    }
}
