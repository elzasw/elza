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

    /**
     * Extensions of the rule set by arrangement extension id
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

    /**
     * Member classes in display order (ids)
     */
    private List<Integer> apTypeOrder = List.of();

    /**
     * Part types offered by the rule set in display order (ids); empty when the rule set does not
     * list its part types
     */
    private List<Integer> partTypeOrder = List.of();

    /**
     * Read-only of classes as the package of the rule set declares them (class id to read-only)
     */
    private Map<Integer, Boolean> declaredReadOnly = Map.of();

    /**
     * Specifications the rule set sees, by item type id; only item types with at least one hidden
     * specification are listed, see {@link #getItemSpecs(ItemType)}
     */
    private Map<Integer, List<CachedItemSpec>> itemSpecsByTypeId = Map.of();

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
        this.ruleSetExtensionsById = exts.stream()
                .map(ruleExt -> new RuleSetExtension(ruleExt, extRulesByExtId.get(ruleExt.getArrangementExtensionId())))
                .collect(Collectors.toMap(rex -> rex.getEntity().getArrangementExtensionId(), p -> p));
        this.entityRulesByKind = entityRules.stream()
                .sorted(Comparator.comparing(RulEntityRule::getPriority)
                        .thenComparing(RulEntityRule::getEntityRuleId))
                .collect(Collectors.groupingBy(RulEntityRule::getKind, HashMap::new, Collectors.toList()));
    }
    
    /**
     * @param apTypeMembers
     *            class id to assignable, iterated in display order
     */
    void setApTypeMembers(final Map<Integer, Boolean> apTypeMembers) {
        this.apTypeMembers = Map.copyOf(apTypeMembers);
        this.apTypeOrder = List.copyOf(apTypeMembers.keySet());
    }

    void setPartTypeOrder(final List<Integer> partTypeOrder) {
        this.partTypeOrder = List.copyOf(partTypeOrder);
    }

    /**
     * @param itemSpecsByTypeId
     *            item type id to the specifications the rule set sees, for item types with at least
     *            one hidden specification
     */
    void setItemSpecs(final Map<Integer, List<CachedItemSpec>> itemSpecsByTypeId) {
        this.itemSpecsByTypeId = Map.copyOf(itemSpecsByTypeId);
    }

    /**
     * Specifications of an item type the rule set sees: those assigned to the item type by a
     * package related to the package of the rule set - the package itself, the packages it depends
     * on and the packages depending on it. A specification assigned only by unrelated packages is
     * not offered under this rule set even when a rule opens every specification of the item type.
     * An assignment without a declaration (data older than the declarations) is seen everywhere.
     *
     * @return specifications in the order of the item type
     */
    public List<CachedItemSpec> getItemSpecs(final ItemType itemType) {
        List<CachedItemSpec> visible = itemSpecsByTypeId.get(itemType.getItemTypeId());
        return visible != null ? visible : itemType.getItemSpecs();
    }

    /**
     * Member classes in display order: the members of the owner of the rule set in the order of its
     * file, then members contributed by other packages (in dependency order, then by package code).
     * Empty when the rule set declares no members.
     */
    public List<Integer> getApTypeOrder() {
        return apTypeOrder;
    }

    /**
     * Part types offered by the rule set in display order, composed as {@link #getApTypeOrder()}.
     * Empty when the rule set does not list its part types.
     */
    public List<Integer> getPartTypeOrder() {
        return partTypeOrder;
    }

    void setDeclaredReadOnly(final Map<Integer, Boolean> declaredReadOnly) {
        this.declaredReadOnly = Map.copyOf(declaredReadOnly);
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
     * or - without members - a class that is not read-only as the package of the rule set declares it
     * (or as the class is, when the package does not declare it).
     */
    public boolean isApTypeAssignable(final ApType apType) {
        if (apTypeMembers.isEmpty()) {
            Boolean readOnly = declaredReadOnly.get(apType.getApTypeId());
            return !(readOnly != null ? readOnly : apType.isReadOnly());
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
