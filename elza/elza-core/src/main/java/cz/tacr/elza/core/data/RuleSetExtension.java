package cz.tacr.elza.core.data;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.domain.RulExtensionRule.RuleType;

public class RuleSetExtension {

    final RulArrangementExtension entity;

    final List<RulExtensionRule> extRules;

    final Map<RuleType, List<RulExtensionRule>> extRulesByType;

    RuleSetExtension(final RulArrangementExtension ruleExt,
                     final List<RulExtensionRule> extRules) {
        this.entity = ruleExt;
        this.extRules = extRules != null ? extRules : Collections.emptyList();
        this.extRulesByType = extRules.stream().collect(Collectors.groupingBy(RulExtensionRule::getRuleType));
    }

    public RulArrangementExtension getEntity() {
        return entity;
    }

    public List<RulExtensionRule> getRulesByType(RuleType attributeTypes) {
        return extRulesByType.getOrDefault(attributeTypes, Collections.emptyList());
    }

}
