package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulExtensionRule;

import java.util.List;


/**
 * Repository pro {@link RulExtensionRule} - Custom.
 *
 * @since 23.10.2017
 */
public interface ExtensionRuleRepositoryCustom {

    /**
     * Vyhledá pravidla podle definicí a typu pravidla. Výsledek je řazen podle priority a definicí.
     *
     * @param arrangementExtensions definice řídících pravidel
     * @param ruleType typ pravidel
     * @return nalezená pravidla
     */
    List<RulExtensionRule> findExtensionRules(List<RulArrangementExtension> arrangementExtensions, RulExtensionRule.RuleType ruleType);
}
