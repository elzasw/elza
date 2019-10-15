package cz.tacr.elza.domain;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Implementace RulArrangementRule. Základní pravidla pro archivní popis.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@Entity(name = "rul_arrangement_rule")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulArrangementRule {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer arrangementRuleId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer ruleSetId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Column(nullable = false, insertable = false, updatable = false)
    private Integer packageId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulComponent.class)
    @JoinColumn(name = "componentId", nullable = false)
    private RulComponent component;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private RuleType ruleType;

    @Column(nullable = false)
    private Integer priority;

    /**
     * @return identifikátor entity
     */
    public Integer getArrangementRuleId() {
        return arrangementRuleId;
    }

    /**
     * @param arrangementRuleId identifikátor entity
     */
    public void setArrangementRuleId(final Integer arrangementRuleId) {
        this.arrangementRuleId = arrangementRuleId;
    }

    /**
     * @return pravidla
     */
    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    /**
     * @param ruleSet pravidla
     */
    public void setRuleSet(final RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
        this.ruleSetId = ruleSet != null ? ruleSet.getRuleSetId() : null;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    /**
     * @return balíček
     */
    public RulPackage getPackage() {
        return rulPackage;
    }

    /**
     * @param rulPackage balíček
     */
    public void setPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
        this.packageId = rulPackage != null ? rulPackage.getPackageId() : null;
    }

    public Integer getPackageId() {
        return packageId;
    }

    public RulComponent getComponent() {
        return component;
    }

    public void setComponent(final RulComponent component) {
        this.component = component;
    }

    /**
     * @return typ pravidel
     */
    public RuleType getRuleType() {
        return ruleType;
    }

    /**
     * @param ruleType typ pravidel
     */
    public void setRuleType(final RuleType ruleType) {
        this.ruleType = ruleType;
    }

    /**
     * @return priorita vykonávání
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * @param priority priorita vykonávání
     */
    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    /**
     * Typy pravidel.
     */
    public enum RuleType {
        CONFORMITY_INFO,
        CONFORMITY_IMPACT,
        ATTRIBUTE_TYPES,
        NEW_LEVEL
    }
}
