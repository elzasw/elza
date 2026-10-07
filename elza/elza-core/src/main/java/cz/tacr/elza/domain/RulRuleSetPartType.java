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
 * Part type offered by an entity rule set ({@code rul_rule_set/<RS>/rul_part_type.xml}), in the order
 * of the file. A rule set without members offers all part types.
 */
@Entity(name = "rul_rule_set_part_type")
@Table
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RulRuleSetPartType {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer ruleSetPartTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer ruleSetId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPartType.class)
    @JoinColumn(name = "partTypeId", nullable = false)
    private RulPartType partType;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer partTypeId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer packageId;

    /** Order within the members the package declares for the rule set. */
    @Column(nullable = false)
    private Integer position;

    public Integer getRuleSetPartTypeId() {
        return ruleSetPartTypeId;
    }

    public void setRuleSetPartTypeId(final Integer ruleSetPartTypeId) {
        this.ruleSetPartTypeId = ruleSetPartTypeId;
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

    public RulPartType getPartType() {
        return partType;
    }

    public Integer getPartTypeId() {
        return partTypeId;
    }

    public void setPartType(final RulPartType partType) {
        this.partType = partType;
        this.partTypeId = partType != null ? partType.getPartTypeId() : null;
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

    public Integer getPosition() {
        return position;
    }

    public void setPosition(final Integer position) {
        this.position = position;
    }
}
