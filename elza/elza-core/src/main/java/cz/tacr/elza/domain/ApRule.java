package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.*;

/**
 * @since 17.07.2018
 */
@Entity
@Table(name = "ap_rule")
public class ApRule {

    public enum RuleType {

        /**
         * Dostupné prvky popisu, soubor DRL.
         */
        BODY_ITEMS,

        /**
         * Dostupné prvky popisu pro jméno, soubor DRL.
         */
        NAME_ITEMS,

        /**
         * Skript pro generování textových hodnot těla a jednotlivých jmen, soubor GROOVY.
         */
        TEXT_GENERATOR,

        /**
         * Skript pro migraci na strukturovaný typ, soubor GROOVY.
         */
        MIGRATE

    }

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer ruleId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulComponent.class)
    @JoinColumn(name = "componentId", nullable = false)
    private RulComponent component;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApRuleSystem.class)
    @JoinColumn(name = "ruleSystemId", nullable = false)
    private ApRuleSystem ruleSystem;

    @Column(updatable = false, insertable = false, nullable = false)
    private Integer ruleSystemId;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private ApRule.RuleType ruleType;

    public Integer getRuleId() {
        return ruleId;
    }

    public void setRuleId(final Integer ruleId) {
        this.ruleId = ruleId;
    }

    public RulComponent getComponent() {
        return component;
    }

    public void setComponent(final RulComponent component) {
        this.component = component;
    }

    public ApRuleSystem getRuleSystem() {
        return ruleSystem;
    }

    public void setRuleSystem(final ApRuleSystem ruleSystem) {
        this.ruleSystem = ruleSystem;
        this.ruleSystemId = ruleSystem == null ? null : ruleSystem.getRuleSystemId();
    }

    public Integer getRuleSystemId() {
        return ruleSystemId;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(final RuleType ruleType) {
        this.ruleType = ruleType;
    }
}
