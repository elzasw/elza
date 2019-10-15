package cz.tacr.elza.domain;

import cz.tacr.elza.domain.enumeration.StringLength;

import javax.persistence.*;

/**
 * Pravidla popisu přístupového bodu.
 * Vlastní pravidla jsou uložena v externím souboru {@link RulComponent}.
 *
 * @since 17.07.2018
 */
@Entity
@Table(name = "ap_rule_system")
public class ApRuleSystem {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY)
    private Integer ruleSystemId;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    public Integer getRuleSystemId() {
        return ruleSystemId;
    }

    public void setRuleSystemId(final Integer ruleSystemId) {
        this.ruleSystemId = ruleSystemId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public RulPackage getRulPackage() {
        return rulPackage;
    }

    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }
}
