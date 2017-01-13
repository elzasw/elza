package cz.tacr.elza.domain;

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
 * Implementace RulRule.
 *
 * @author Martin Šlapa
 * @since 14.12.2015
 */
@Entity(name = "rul_rule")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulRule {

    @Id
    @GeneratedValue
    private Integer ruleId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = true)
    private RulRuleSet ruleSet;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Column(length = 250, nullable = false)
    private String filename;

    @Enumerated(EnumType.STRING)
    @Column(length = 50, nullable = false)
    private RuleType ruleType;

    @Column(nullable = false)
    private Integer priority;

    /**
     * @return identifikátor entity
     */
    public Integer getRuleId() {
        return ruleId;
    }

    /**
     * @param ruleId identifikátor entity
     */
    public void setRuleId(final Integer ruleId) {
        this.ruleId = ruleId;
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
     * @return název souboru
     */
    public String getFilename() {
        return filename;
    }

    /**
     * @param filename název souboru
     */
    public void setFilename(final String filename) {
        this.filename = filename;
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
        OUTPUT_ATTRIBUTE_TYPES,
        NEW_LEVEL
    }
}
