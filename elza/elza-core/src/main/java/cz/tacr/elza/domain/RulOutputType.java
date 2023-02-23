package cz.tacr.elza.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.domain.enumeration.StringLength;
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


/**
 * Domain object for output type
 */
@Entity(name = "rul_output_type")
@Table
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RulOutputType {

    @Id
    @GeneratedValue
    @Access(AccessType.PROPERTY) // required to read id without fetch from db
    private Integer outputTypeId;

    @Column(length = StringLength.LENGTH_50, nullable = false)
    private String code;

    @Column(length = StringLength.LENGTH_250, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulRuleSet.class)
    @JoinColumn(name = "ruleSetId", nullable = false)
    private RulRuleSet ruleSet;

	@Column(nullable = true, updatable = false, insertable = false)
	private Integer ruleSetId;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulComponent.class)
    @JoinColumn(name = "componentId")
    private RulComponent component;

    public Integer getOutputTypeId() {
        return outputTypeId;
    }

    public void setOutputTypeId(final Integer outputTypeId) {
        this.outputTypeId = outputTypeId;
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

    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

	public Integer getRuleSetId() {
		return ruleSetId;
	}

    public void setRuleSet(final RulRuleSet ruleSet) {
        this.ruleSet = ruleSet;
		this.ruleSetId = ruleSet != null ? ruleSet.getRuleSetId() : null;
    }

    public RulComponent getComponent() {
        return component;
    }

    public void setComponent(final RulComponent component) {
        this.component = component;
    }
}
