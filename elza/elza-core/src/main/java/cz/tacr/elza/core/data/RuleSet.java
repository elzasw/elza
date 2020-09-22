package cz.tacr.elza.core.data;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.domain.RulRuleSet;

public class RuleSet {
    final RulRuleSet entity;
    final List<RuleSetExtension> extensions;
    final Map<String, RuleSetExtension> extensionsByCode;

    RuleSet(final RulRuleSet entity,
            final List<RulArrangementExtension> exts,
            final Map<Integer, List<RulExtensionRule>> extRulesByExtId) {
        this.entity = entity;
        this.extensions = exts.stream()
                .map(ruleExt -> new RuleSetExtension(ruleExt, extRulesByExtId.get(ruleExt.getArrangementExtensionId())))
                .collect(Collectors.toList());
        this.extensionsByCode = extensions.stream().collect(Collectors.toMap(RuleSetExtension::getCode, Function
                .identity()));
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

    public RuleSetExtension getExtByCode(String extCode) {
        return extensionsByCode.get(extCode);
    }
}
