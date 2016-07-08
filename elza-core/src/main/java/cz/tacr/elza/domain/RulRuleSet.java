package cz.tacr.elza.domain;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.apache.commons.lang.builder.EqualsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * popis {@link cz.tacr.elza.api.RulRuleSet}.
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "rul_rule_set")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class RulRuleSet implements cz.tacr.elza.api.RulRuleSet<RulPackage>, Serializable {

    @Id
    @GeneratedValue
    private Integer ruleSetId;

    @Column(length = 50, nullable = false)
    private String code;

    @Column(length = 250, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = RulPackage.class)
    @JoinColumn(name = "packageId", nullable = false)
    private RulPackage rulPackage;

    @Override
    public Integer getRuleSetId() {
        return ruleSetId;
    }

    @Override
    public void setRuleSetId(final Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public void setCode(String code) {
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
    public RulPackage getPackage() {
        return rulPackage;
    }

    @Override
    public void setPackage(final RulPackage rulPackage) {
        this.rulPackage = rulPackage;
    }

    @Override
    public String toString() {
        return "RulRuleSet pk=" + ruleSetId;
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof RulRuleSet)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        RulRuleSet other = (RulRuleSet) obj;

        return new EqualsBuilder().append(ruleSetId, other.getRuleSetId()).isEquals();
    }
}
