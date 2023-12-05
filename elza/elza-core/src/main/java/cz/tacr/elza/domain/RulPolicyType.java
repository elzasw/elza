package cz.tacr.elza.domain;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Typy kontrol, validací, archivního popisu. Každá chyba validace má přiřazen právě jeden typ kontroly (validace),
 * který je k chybě přiřazen na výstupu z pravidel (drools).
 *
 * @author Martin Šlapa
 * @since 22.3.2016
 *
 */
@Entity(name = "rul_policy_type")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class RulPolicyType {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
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

    /**
     * @return identifikátor položky
     */
    public Integer getPolicyTypeId() {
        return policyTypeId;
    }

    /**
     * @param policyTypeId identifikátor položky
     */
    public void setPolicyTypeId(final Integer policyTypeId) {
        this.policyTypeId = policyTypeId;
    }

    /**
     * @return kód typu
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code kód typu
     */
    public void setCode(final String code) {
        this.code = code;
    }

    /**
     * @return název typu
     */
    public String getName() {
        return name;
    }

    /**
     * @param name název typu
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return balíček pravidel
     */
    public RulPackage getRulPackage() {
        return rulPackage;
    }

    /**
     * @param rulPackage balíček pravidel
     */
    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
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
}
