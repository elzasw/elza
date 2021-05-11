package cz.tacr.elza.controller.vo;

import cz.tacr.elza.domain.RulOutputFilter;

public class RulOutputFilterVO {

    private Integer id;

    private String name;

    private String code;

    private String filename;

    private Integer packageId;

    private Integer ruleSetId;

    public RulOutputFilterVO() {
    }

    public RulOutputFilterVO(RulOutputFilter outputFilter) {
        this.id = outputFilter.getOutputFilterId();
        this.name = outputFilter.getName();
        this.code = outputFilter.getCode();
        this.filename = outputFilter.getFilename();
        this.packageId = outputFilter.getPackageId();
        this.ruleSetId = outputFilter.getRuleSetId();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Integer getPackageId() {
        return packageId;
    }

    public void setPackageId(Integer packageId) {
        this.packageId = packageId;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }
}
