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
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Entity class used by an entity rule set ({@code rul_rule_set/<RS>/rul_ap_type.xml}).
 *
 * <p>A rule set with members offers only its members; whether a member can be assigned to an entity
 * is stated here and overrides {@link ApType#isReadOnly()}. The rows belong to the package that
 * declared them - the owner of the rule set or a package contributing to it; when several packages
 * state the same class, the package deeper in dependency order wins.
 */
@Entity(name = "rul_rule_set_ap_type")
@Table
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RulRuleSetApType {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer ruleSetApTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer ruleSetId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApType.class)
    @JoinColumn(name = "apTypeId", nullable = false)
    private ApType apType;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer apTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer packageId;

    @Column(nullable = false)
    private Boolean assignable;

    public Integer getRuleSetApTypeId() {
        return ruleSetApTypeId;
    }

    public void setRuleSetApTypeId(final Integer ruleSetApTypeId) {
        this.ruleSetApTypeId = ruleSetApTypeId;
    }

    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSet(final RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
        this.ruleSetId = ruleSet != null ? ruleSet.getRuleSetId() : null;
    }

    public ApType getApType() {
        return apType;
    }

    public Integer getApTypeId() {
        return apTypeId;
    }

    public void setApType(final ApType apType) {
        this.apType = apType;
        this.apTypeId = apType != null ? apType.getApTypeId() : null;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public Integer getPackageId() {
        return packageId;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
        this.packageId = rulPackage != null ? rulPackage.getPackageId() : null;
    }

    public Boolean getAssignable() {
        return assignable;
    }

    public void setAssignable(final Boolean assignable) {
        this.assignable = assignable;
    }
}
