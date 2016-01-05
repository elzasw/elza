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
public class RulRule implements cz.tacr.elza.api.RulRule<RulPackage, RulRuleSet> {

    @Id
    @GeneratedValue
    private Integer ruleId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
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


    @Override
    public Integer getRuleId() {
        return ruleId;
    }

    @Override
    public void setRuleId(final Integer ruleId) {
        this.ruleId = ruleId;
    }

    @Override
    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    @Override
    public void setRuleSet(final RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    @Override
    public RulPackage getPackage() {
        return rulPackage;
    }

    @Override
    public void setPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void setFilename(final String filename) {
        this.filename = filename;
    }

    @Override
    public RuleType getRuleType() {
        return ruleType;
    }

    @Override
    public void setRuleType(final RuleType ruleType) {
        this.ruleType = ruleType;
    }

    @Override
    public Integer getPriority() {
        return priority;
    }

    @Override
    public void setPriority(final Integer priority) {
        this.priority = priority;
    }
}
