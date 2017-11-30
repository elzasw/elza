package cz.tacr.elza.controller.vo;

import java.util.List;

/**
 * Objekt obsahující informace pro založení nového AS.
 * @author Pavel Stánek [pavel.stanek@marbes.cz]
 * @since 10.10.2017
 */
public class CreateFundVO {
    private String name;
    private Integer ruleSetId;
    private String internalCode;
    private Integer institutionId;
    private String dateRange;
    private List<UsrUserVO> adminUsers;
    private List<UsrGroupVO> adminGroups;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    public String getInternalCode() {
        return internalCode;
    }

    public void setInternalCode(String internalCode) {
        this.internalCode = internalCode;
    }

    public Integer getInstitutionId() {
        return institutionId;
    }

    public void setInstitutionId(Integer institutionId) {
        this.institutionId = institutionId;
    }

    public String getDateRange() {
        return dateRange;
    }

    public void setDateRange(String dateRange) {
        this.dateRange = dateRange;
    }

    public List<UsrUserVO> getAdminUsers() {
        return adminUsers;
    }

    public void setAdminUsers(List<UsrUserVO> adminUsers) {
        this.adminUsers = adminUsers;
    }

    public List<UsrGroupVO> getAdminGroups() {
        return adminGroups;
    }

    public void setAdminGroups(List<UsrGroupVO> adminGroups) {
        this.adminGroups = adminGroups;
    }
}
