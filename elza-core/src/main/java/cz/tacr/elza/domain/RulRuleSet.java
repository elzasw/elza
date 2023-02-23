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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import cz.tacr.elza.domain.enumeration.StringLength;

/**
 * Pravidla tvorby AP. Primárními pravidly jsou Základní pravidla. Je možné však připravit jiná
 * pravidla tvorby, případně stávající pravidla dále rozpracovat a modifikovat. Je realizována pouze
 * entita obalující základní pravidla, nikoli reálná základní pravidla.
 *
 * @since 22.7.15
 */
@Entity(name = "rul_rule_set")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class RulRuleSet {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer ruleSetId;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", length = StringLength.LENGTH_ENUM, nullable = false)
    private RuleType ruleType;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulComponent.class)
    @JoinColumn(name = "itemTypeComponentId")
    private RulComponent itemTypeComponent;

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(final Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public RulPackage getPackage() {
        return rulPackage;
    }

    public void setPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public RulComponent getItemTypeComponent() {
        return itemTypeComponent;
    }

    public void setItemTypeComponent(RulComponent itemTypeComponent) {
        this.itemTypeComponent = itemTypeComponent;
    }

    @Override
    public String toString() {
        return "RulRuleSet pk=" + ruleSetId;
    }

    public enum RuleType {

        ENTITY,

        ARRANGEMENT;

        public String value() {
            return name();
        }

        public static RuleType fromValue(String v) {
            return valueOf(v);
        }
    }
}
