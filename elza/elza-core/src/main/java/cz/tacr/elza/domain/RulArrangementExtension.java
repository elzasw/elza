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
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;


/**
 * Definice rozšíření pro řídící pravidla popisu, které se přiřazují k JP.
 *
 * @since 17.10.2017
 */
@Entity(name = "rul_arrangement_extension")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulArrangementExtension {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer arrangementExtensionId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

    @Column(name = "ruleSetId", insertable = false, updatable = false)
    private Integer ruleSetId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @Column(length = StringLength.LENGTH_50, nullable = false, unique = true)
    private String code;

    /**
     * @return identifikátor entity
     */
    public Integer getArrangementExtensionId() {
        return arrangementExtensionId;
    }

    /**
     * @param arrangementExtensionId identifikátor entity
     */
    public void setArrangementExtensionId(final Integer arrangementExtensionId) {
        this.arrangementExtensionId = arrangementExtensionId;
    }

    /**
     * @return pravidla
     */
    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    /**
     * @param ruleSet pravidla
     */
    public void setRuleSet(final RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
        this.ruleSetId = (ruleSet != null) ? ruleSet.getRuleSetId() : null;
    }

    /**
     * @return název
     */
    public String getName() {
        return name;
    }

    /**
     * @param name název souboru
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return balíček
     */
    public RulPackage getRulPackage() {
        return rulPackage;
    }

    /**
     * @param rulPackage balíček
     */
    public void setRulPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    /**
     * @return kód entity
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code kód entity
     */
    public void setCode(final String code) {
        this.code = code;
    }
}
