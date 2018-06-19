package cz.tacr.elza.core.data;

import cz.tacr.elza.domain.RulRuleSet;
import org.apache.commons.lang3.Validate;

public class RuleSystemImpl implements RuleSystem {

    private final RulRuleSet ruleSet;

    public RuleSystemImpl(RulRuleSet ruleSet) {
        this.ruleSet = Validate.notNull(ruleSet);
    }

    @Override
    public RulRuleSet getRuleSet() {
        return ruleSet;
    }

}
