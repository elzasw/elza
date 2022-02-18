package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.domain.RulRuleSet;

public class RuleSet {

    final RulRuleSet entity;

    final List<RuleSetExtension> ruleSetExtensions;

    final Map<String, List<RulExtensionRule>> extRuleByCondition;

    final String NULL_CONDITION = "$_NULL";

    RuleSet(final RulRuleSet entity,
            final List<RulArrangementExtension> exts,
            final Map<Integer, List<RulExtensionRule>> extRulesByExtId,
            final List<RulExtensionRule> rulExtensionRules) {
        this.entity = entity;
        this.ruleSetExtensions = exts.stream()
                .map(ruleExt -> new RuleSetExtension(ruleExt, extRulesByExtId.get(ruleExt.getArrangementExtensionId())))
                .collect(Collectors.toList());
        this.extRuleByCondition = rulExtensionRules.stream()
                .collect(Collectors.groupingBy(p -> {
                    return p.getCondition() == null? NULL_CONDITION : p.getCondition();
                }, HashMap::new, Collectors.toCollection(ArrayList::new)));
    }

    public RulRuleSet getEntity() {
        return entity;
    }

    public String getCode() {
        return entity.getCode();
    }

    public Integer getRuleSetId() {
        return entity.getRuleSetId();
    }

    public List<RulExtensionRule> getExtByCondition(String condition) {
        if (condition == null) {
            return extRuleByCondition.get(NULL_CONDITION);
        }
        return extRuleByCondition.get(condition);
    }
}
