package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import cz.tacr.elza.ax.IdObject;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "rul_rule_set")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "id"})
public class RuleSet extends EntityBase implements IdObject<Integer> {

    @Id
    @GeneratedValue
    private Integer ruleSetId;

    @Column(length = 5, nullable = false)
    private String code;

    @Column(length = 50, nullable = false)
    private String name;

    public Integer getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(final Integer ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "RuleSet pk=" + ruleSetId;
    }

    @Override
    public Integer getId() {
        return ruleSetId;
    }
}
