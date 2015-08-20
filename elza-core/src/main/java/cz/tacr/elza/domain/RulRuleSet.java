package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.req.ax.IdObject;

import java.io.Serializable;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "rul_rule_set")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class RulRuleSet implements IdObject<Integer>, cz.tacr.elza.api.RulRuleSet, Serializable {

    @Id
    @GeneratedValue
    private Integer ruleSetId;

    @Column(length = 5, nullable = false)
    private String code;

    @Column(length = 50, nullable = false)
    private String name;

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
    public String toString() {
        return "RulRuleSet pk=" + ruleSetId;
    }

    @Override
    @JsonIgnore
    public Integer getId() {
        return ruleSetId;
    }
}
