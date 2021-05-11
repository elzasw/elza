package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity(name = "rul_export_filter")
public class RulExportFilter {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer exportFilterId;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String name;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String filename;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer packageId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer ruleSetId;

    public Integer getExportFilterId() {
        return exportFilterId;
    }

    public void setExportFilterId(Integer exportFilterId) {
        this.exportFilterId = exportFilterId;
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

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(RulPackage rulPackage) {
        this.rulPackage = rulPackage;
        this.packageId = rulPackage != null ? rulPackage.getPackageId() : null;
    }

    public Integer getPackageId() {
        return packageId;
    }

    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    public void setRuleSet(RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
        this.ruleSetId = ruleSet != null ? ruleSet.getRuleSetId() : null;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }
}
