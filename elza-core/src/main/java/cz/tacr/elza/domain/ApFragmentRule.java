package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.*;

/**
 * @since 18.07.2018
 */
@Entity
@Table(name = "ap_fragment_rule")
public class ApFragmentRule {

    public enum RuleType {

        /**
         * Dostupné prvky popisu, soubor DRL.
         */
        FRAGMENT_ITEMS,

        /**
         * Skript pro generování textových hodnot těla a jednotlivých jmen, soubor GROOVY.
         */
        TEXT_GENERATOR,

    }

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer fragmentRuleId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulComponent.class)
    @JoinColumn(name = "componentId", nullable = false)
    private RulComponent component;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = ApFragmentType.class)
    @JoinColumn(name = "fragmentTypeId", nullable = false)
    private ApFragmentType fragmentType;

    @Column(updatable = false, insertable = false, nullable = false)
    private Integer fragmentTypeId;

    @Enumerated(EnumType.STRING)
    @Column(length = StringLength.LENGTH_ENUM, nullable = false)
    private ApFragmentRule.RuleType ruleType;

    public Integer getFragmentRuleId() {
        return fragmentRuleId;
    }

    public void setFragmentRuleId(final Integer fragmentRuleId) {
        this.fragmentRuleId = fragmentRuleId;
    }

    public RulComponent getComponent() {
        return component;
    }

    public void setComponent(final RulComponent component) {
        this.component = component;
    }

    public ApFragmentType getFragmentType() {
        return fragmentType;
    }

    public void setFragmentType(final ApFragmentType fragmentType) {
        this.fragmentType = fragmentType;
        this.fragmentTypeId = fragmentType == null ? null : fragmentType.getFragmentTypeId();
    }

    public Integer getFragmentTypeId() {
        return fragmentTypeId;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(final RuleType ruleType) {
        this.ruleType = ruleType;
    }
}
