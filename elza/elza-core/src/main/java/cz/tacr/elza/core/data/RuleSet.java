package cz.tacr.elza.core.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulArrangementExtension;
import cz.tacr.elza.domain.RulArrangementRule;
import cz.tacr.elza.domain.RulArrangementRule.RuleType;
import cz.tacr.elza.domain.RulEntityRule;
import cz.tacr.elza.domain.RulExtensionRule;
import cz.tacr.elza.domain.RulRuleSet;

public class RuleSet {

    final RulRuleSet entity;

    // TODO: check if this it used
    final List<RuleSetExtension> ruleSetExtensions;
    
    /**
     * Map of rule set extensions by rule set id
     */
    final Map<Integer, RuleSetExtension> ruleSetExtensionsById;

    /**
     * Entity rules of this rule set by kind, sorted by priority
     */
    final Map<RulEntityRule.Kind, List<RulEntityRule>> entityRulesByKind;

    final Map<RuleType, List<RulArrangementRule>> rulesByType;

    /**
     * Member classes (id to assignable); empty when the rule set declares no members and offers all
     * classes
     */
    private Map<Integer, Boolean> apTypeMembers = Map.of();

    RuleSet(final RulRuleSet entity,
            final List<RulArrangementRule> rules,
            final List<RulArrangementExtension> exts,
            final Map<Integer, List<RulExtensionRule>> extRulesByExtId,
            final List<RulEntityRule> entityRules) {
        this.entity = entity;
        this.rulesByType = rules.stream().collect(Collectors.groupingBy(RulArrangementRule::getRuleType,
                                                                        Collectors.toList()));
        // sort rules
        for (List<RulArrangementRule> rulesPerType : rulesByType.values()) {
            rulesPerType.sort((a, b) -> {
                int ret = a.getPriority().compareTo(b.getPriority());
                if (ret == 0) {
                    ret = a.getArrangementRuleId().compareTo(b.getArrangementRuleId());
                }
                return ret;
            });
        }
        this.ruleSetExtensions = exts.stream()
                .map(ruleExt -> new RuleSetExtension(ruleExt, extRulesByExtId.get(ruleExt.getArrangementExtensionId())))
                .collect(Collectors.toList());
        this.ruleSetExtensionsById = this.ruleSetExtensions.stream().collect(
        		Collectors.toMap(rex -> rex.getEntity().getArrangementExtensionId(), p -> p));
        this.entityRulesByKind = entityRules.stream()
                .sorted(Comparator.comparing(RulEntityRule::getPriority)
                        .thenComparing(RulEntityRule::getEntityRuleId))
                .collect(Collectors.groupingBy(RulEntityRule::getKind, HashMap::new, Collectors.toList()));
    }
    
    void setApTypeMembers(final Map<Integer, Boolean> apTypeMembers) {
        this.apTypeMembers = Map.copyOf(apTypeMembers);
    }

    /**
     * @return true when the rule set declares its classes ({@code rul_ap_type.xml}); otherwise it
     *         offers all classes
     */
    public boolean hasApTypeMembers() {
        return !apTypeMembers.isEmpty();
    }

    /**
     * Entities of the class may be in scopes of this rule set.
     */
    public boolean offersApType(final ApType apType) {
        return apTypeMembers.isEmpty() || apTypeMembers.containsKey(apType.getApTypeId());
    }

    /**
     * The class can be chosen for an entity in scopes of this rule set: a member declared assignable,
     * or - without members - a class that is not read-only.
     */
    public boolean isApTypeAssignable(final ApType apType) {
        if (apTypeMembers.isEmpty()) {
            return !apType.isReadOnly();
        }
        return Boolean.TRUE.equals(apTypeMembers.get(apType.getApTypeId()));
    }

    public RuleSetExtension getRuleSetExtension(Integer ruleSetExtensionId) {
		return ruleSetExtensionsById.get(ruleSetExtensionId);
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

    /**
     * Entity rules to run, in the order of execution: rules for all classes first, then for each
     * class from the root of the class hierarchy down to the entity's class; on each level the rules
     * without a part type before the rules of a part type; by priority within a group.
     *
     * @param kind
     *            kind of the rules
     * @param apTypeIds
     *            ids of the entity's class and its parents, from the class up to the root
     * @param partTypeId
     *            part type; null for the rules of all part types
     * @return rules of this rule set only
     */
    public List<RulEntityRule> getEntityRules(final RulEntityRule.Kind kind, final List<Integer> apTypeIds,
                                              final Integer partTypeId) {
        List<RulEntityRule> rules = entityRulesByKind.getOrDefault(kind, Collections.emptyList());
        if (rules.isEmpty()) {
            return rules;
        }
        List<RulEntityRule> result = new ArrayList<>();
        addEntityRules(result, rules, null, partTypeId);
        for (int i = apTypeIds.size() - 1; i >= 0; i--) {
            addEntityRules(result, rules, apTypeIds.get(i), partTypeId);
        }
        return result;
    }

    private static void addEntityRules(final List<RulEntityRule> result, final List<RulEntityRule> rules,
                                       final Integer apTypeId, final Integer partTypeId) {
        for (RulEntityRule rule : rules) {
            if (Objects.equals(apTypeId, rule.getApTypeId()) && rule.getPartTypeId() == null) {
                result.add(rule);
            }
        }
        for (RulEntityRule rule : rules) {
            if (Objects.equals(apTypeId, rule.getApTypeId()) && rule.getPartTypeId() != null
                    && (partTypeId == null || partTypeId.equals(rule.getPartTypeId()))) {
                result.add(rule);
            }
        }
    }

    /**
     * Get list of rules by type
     * 
     * Rules are sorted by priority
     * 
     * @param ruleType
     * @return
     */
    public List<RulArrangementRule> getRulesByType(RuleType ruleType) {
        return rulesByType.getOrDefault(ruleType, Collections.emptyList());
    }
}
