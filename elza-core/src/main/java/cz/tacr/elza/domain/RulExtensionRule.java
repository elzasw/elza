package cz.tacr.elza.domain;

import static cz.tacr.elza.domain.enumeration.StringLength.LENGTH_ENUM;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Basic;
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

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Basic
    @Column(name = "compatibility_rul_package")
    private Integer compatibilityRulPackage;

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

    public void setArrangementExtension(final RulArrangementExtension arrangementExtension) {
        this.arrangementExtension = arrangementExtension;
    }

    public Integer getCompatibilityRulPackage() {
        return compatibilityRulPackage;
    }

    public void setCompatibilityRulPackage(Integer compatibilityRulPackage) {
        this.compatibilityRulPackage = compatibilityRulPackage;
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
