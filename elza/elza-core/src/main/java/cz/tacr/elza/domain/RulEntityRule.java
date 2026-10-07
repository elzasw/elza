package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Rule (DRL) of an entity rule set: which items are available in a part of an entity, or how an
 * entity is validated.
 *
 * <p>A rule belongs to the rule set it was imported into ({@code rul_rule_set/<RS>/rul_entity_rule.xml})
 * and runs only for entities whose scope uses that rule set. The entity class and the part type
 * narrow it down; without them it applies to all classes or all parts.
 */
@Entity(name = "rul_entity_rule")
@Table
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class RulEntityRule {

    /**
     * What the rule decides.
     */
    public enum Kind {
        /** Item types available in a part of an entity. */
        AVAILABLE_ITEMS,
        /** Validation of the whole entity. */
        VALIDATION,
        /**
         * Groovy script building the name and indexes of a part (display name, sort name, key values);
         * the most specific rule applies.
         */
        INDEX
    }

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer entityRuleId;

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
    private Kind kind;

    /** Entity class, with its subclasses; null for all classes. */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApType.class)
    @JoinColumn(name = "apTypeId")
    private ApType apType;

    @Column(insertable = false, updatable = false)
    private Integer apTypeId;

    /** Part type; null for all parts. */
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPartType.class)
    @JoinColumn(name = "partTypeId")
    private RulPartType partType;

    @Column(insertable = false, updatable = false)
    private Integer partTypeId;

    @Column(nullable = false)
    private Integer priority;

    @Column(name = "compatibility_rul_package")
    private Integer compatibilityRulPackage;

    public Integer getEntityRuleId() {
        return entityRuleId;
    }

    public void setEntityRuleId(final Integer entityRuleId) {
        this.entityRuleId = entityRuleId;
    }

    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    public void setRuleSet(final RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
        this.ruleSetId = ruleSet != null ? ruleSet.getRuleSetId() : null;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
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

    public Kind getKind() {
        return kind;
    }

    public void setKind(final Kind kind) {
        this.kind = kind;
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

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(final Integer priority) {
        this.priority = priority;
    }

    public Integer getCompatibilityRulPackage() {
        return compatibilityRulPackage;
    }

    public void setCompatibilityRulPackage(final Integer compatibilityRulPackage) {
        this.compatibilityRulPackage = compatibilityRulPackage;
    }
}
