package cz.tacr.elza.api;

import java.io.Serializable;

public interface RulRuleSet extends Serializable {

    Integer getRuleSetId();

    void setRuleSetId(Integer ruleSetId);

    String getCode();

    void setCode(String code);

    String getName();

    void setName(String name);
}
