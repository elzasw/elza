package cz.tacr.elza.controller.vo;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import cz.tacr.elza.domain.ArrFund;

/**
 * VO pro archivní pomůcku.
 *
 * @since 7. 1. 2016
 */
public class ArrFundVO extends ArrFundBaseVO {

    private Date createDate;

    private Integer institutionId;

    private List<ArrFundVersionVO> versions = new LinkedList<>();

    private List<ApScopeVO> apScopes;

    private List<ArrOutputVO> validNamedOutputs;

    private String unitdate;

    public ArrFundVO() {

    }

    public ArrFundVO(final ArrFund fund) {
    	super(fund);
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

    public String getUnitdate() {
        return unitdate;
    }

    public void setUnitdate(final String unitdate) {
        this.unitdate = unitdate;
    }

    public static ArrFundVO newInstance(final ArrFund fund) {
        return new ArrFundVO(fund);
    }
}
