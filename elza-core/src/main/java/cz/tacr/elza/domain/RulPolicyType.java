package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;

@Entity(name = "rul_policy_type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class RulPolicyType implements cz.tacr.elza.api.RulPolicyType<RulPackage, RulRuleSet> {

    @Id
    @GeneratedValue
    private Integer policyTypeId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

    @Override
    public Integer getPolicyTypeId() {
        return policyTypeId;
    }

    @Override
    public void setPolicyTypeId(final Integer policyTypeId) {
        this.policyTypeId = policyTypeId;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(final String code) {
        this.code = code;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public RulPackage getRulPackage() {
        return rulPackage;
    }

    @Override
    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    @Override
    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    @Override
    public void setRuleSet(final RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }
}
