package cz.tacr.elza.domain;

import static cz.tacr.elza.domain.enumeration.StringLength.LENGTH_ENUM;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Basic;
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
 * Řídící pravidla archivního popisu, které definuje dané rozšíření.
 *
 * @since 17.10.2017
 */
@Entity(name = "rul_extension_rule")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulExtensionRule {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer extensionRuleId;

    @Enumerated(EnumType.STRING)
    @Column(length = LENGTH_ENUM, nullable = false)
    private RuleType ruleType;

    @Column(nullable = false)
    private Integer priority;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulComponent.class)
    @JoinColumn(name = "componentId", nullable = false)
    private RulComponent component;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulArrangementExtension.class)
    @JoinColumn(name = "arrangementExtensionId", nullable = false)
    private RulArrangementExtension arrangementExtension;

    @Column(name = "arrangementExtensionId", insertable = false, updatable = false)
    private Integer arrangementExtensionId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Basic
    @Column(name = "compatibility_rul_package")
    private Integer compatibilityRulPackage;

    @Column(length = StringLength.LENGTH_250)
    private String condition;

    /**
     * @return identifikátor entity
     */
    public Integer getExtensionRuleId() {
        return extensionRuleId;
    }

    /**
     * @param extensionRuleId identifikátor entity
     */
    public void setExtensionRuleId(final Integer extensionRuleId) {
        this.extensionRuleId = extensionRuleId;
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

    public RulComponent getComponent() {
        return component;
    }

    public void setComponent(final RulComponent component) {
        this.component = component;
    }

    public RulArrangementExtension getArrangementExtension() {
        return arrangementExtension;
    }

    public Integer getArrangementExtensionId() {
        return arrangementExtensionId;
    }

    public void setArrangementExtension(final RulArrangementExtension arrangementExtension) {
        this.arrangementExtension = arrangementExtension;
        this.arrangementExtensionId = arrangementExtension != null ? arrangementExtension.getArrangementExtensionId()
                : null;
    }

    public Integer getCompatibilityRulPackage() {
        return compatibilityRulPackage;
    }

    public void setCompatibilityRulPackage(Integer compatibilityRulPackage) {
        this.compatibilityRulPackage = compatibilityRulPackage;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
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
