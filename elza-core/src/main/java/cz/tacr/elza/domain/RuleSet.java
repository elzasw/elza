package cz.tacr.elza.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Entity(name = "RUL_RULE_SET")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RuleSet extends EntityBase {

    @Id
    @GeneratedValue
    private Integer ruleSetId;

    @Column(length = 50, nullable = false)
    private String name;

    public Integer getRuleSetId() {
      return ruleSetId;
    }

    public void setRuleSetId(Integer ruleSetId) {
      this.ruleSetId = ruleSetId;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
        return "RuleSet pk=" + ruleSetId;
    }
}
