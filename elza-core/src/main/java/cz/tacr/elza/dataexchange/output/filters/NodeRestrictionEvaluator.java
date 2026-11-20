package cz.tacr.elza.dataexchange.output.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.sections.StructObjectInfoLoader;
import cz.tacr.elza.dataexchange.output.writer.StructObjectInfo;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.service.cache.AccessPointCacheProvider;
import jakarta.annotation.Nullable;

/**
 * Evaluates the rules of an output filter for the nodes of a tree.
 *
 * The restriction items of a node are its own restriction items plus all restriction items of
 * its ancestors, so a restriction set on a parent is evaluated again for every descendant. A node
 * with a hidden ancestor is hidden as well.
 *
 * The evaluator keeps state of the evaluated nodes, so a parent has to be evaluated before its
 * children. One instance serves one tree walk and is not thread safe.
 */
public class NodeRestrictionEvaluator {

    private final FilterRules filterRules;

    private final StaticDataProvider sdp;

    private final AccessPointCacheProvider apCacheProvider;

    private final Locale locale;

    private final StructObjectInfoLoader soiLoader;

    /**
     * Restriction definitions (structured objects) already read from DB, by structured object id
     */
    private final Map<Integer, StructObjectInfo> restrictionDefsById = new HashMap<>();

    /**
     * Restriction items, including the inherited ones, of every evaluated node which has some
     */
    private final Map<Integer, List<ArrItem>> restrictionItemsByNodeId = new HashMap<>();

    /**
     * Nodes hidden by the filter, including nodes hidden because of a hidden ancestor
     */
    private final Set<Integer> hiddenNodeIds = new HashSet<>();

    public NodeRestrictionEvaluator(final FilterRules filterRules,
                                    final StaticDataProvider sdp,
                                    final AccessPointCacheProvider apCacheProvider,
                                    final Locale locale,
                                    final StructObjectInfoLoader soiLoader) {
        this.filterRules = Objects.requireNonNull(filterRules);
        this.sdp = Objects.requireNonNull(sdp);
        this.apCacheProvider = Objects.requireNonNull(apCacheProvider);
        this.locale = Objects.requireNonNull(locale);
        this.soiLoader = Objects.requireNonNull(soiLoader);
    }

    /**
     * Evaluates the filter rules for one node. The parent node has to be evaluated before.
     *
     * @param nodeId
     *            evaluated node
     * @param parentNodeId
     *            parent of the evaluated node, null for the root node
     * @param nodeItems
     *            description items of the node
     * @return changes of the node required by the filter; when
     *         {@link ApplyFilter#isLevelHidden()} the node and its whole subtree are hidden
     */
    public ApplyFilter evaluate(Integer nodeId, @Nullable Integer parentNodeId,
                                Collection<? extends ArrItem> nodeItems) {
        ApplyFilter filter = new ApplyFilter();

        List<ArrItem> restrictionItems = getRestrictionItems(nodeItems);
        if (parentNodeId != null) {
            List<ArrItem> parentRestrictionItems = restrictionItemsByNodeId.get(parentNodeId);
            if (parentRestrictionItems != null) {
                restrictionItems.addAll(parentRestrictionItems);
            }
        }
        // a hidden parent always has some restriction item, so an unrestricted node
        // never has a hidden parent
        if (restrictionItems.isEmpty()) {
            return filter;
        }
        restrictionItemsByNodeId.put(nodeId, restrictionItems);

        if (parentNodeId != null && hiddenNodeIds.contains(parentNodeId)) {
            hiddenNodeIds.add(nodeId);
            filter.hideLevel();
            return filter;
        }

        Map<ItemType, List<ArrItem>> itemsByType = groupByItemType(nodeItems);

        // non-structured restriction items all share the same context (the node's own items),
        // so that context is evaluated only once regardless of how many such items are present
        boolean ownItemsProcessed = false;
        for (ArrItem restrictionItem : restrictionItems) {
            ItemType itemType = sdp.getItemTypeById(restrictionItem.getItemTypeId());

            Collection<? extends ArrItem> soiItems;
            if (itemType.getDataType().equals(DataType.STRUCTURED)) {
                ArrData data = HibernateUtils.unproxy(restrictionItem.getData());
                if (data == null) {
                    continue;
                }
                ArrDataStructureRef dsr = (ArrDataStructureRef) data;
                StructObjectInfo soi = readRestrictionDef(dsr.getStructuredObjectId());
                soiItems = soi.getItems();
            } else {
                if (ownItemsProcessed) {
                    continue;
                }
                ownItemsProcessed = true;
                soiItems = nodeItems;
            }

            FilterRuleContext filterRuleContext = new FilterRuleContext(soiItems, apCacheProvider, sdp);
            for (FilterRule rule : filterRules.getFilterRules()) {
                FilterRuleResultType result = processRule(rule, filterRuleContext, itemsByType, filter);
                if (result == FilterRuleResultType.RESULT_BREAK) {
                    break;
                }
            }
        }

        if (filter.isLevelHidden()) {
            hiddenNodeIds.add(nodeId);
        }
        return filter;
    }

    /**
     * Items of the node with a restriction definition (types listed in the filter
     * {@code restrictions}).
     */
    private List<ArrItem> getRestrictionItems(Collection<? extends ArrItem> nodeItems) {
        List<ArrItem> restrictionItems = new ArrayList<>();
        for (ArrItem item : nodeItems) {
            if (item.getData() == null) {
                continue;
            }
            for (ItemType restrictionType : filterRules.getRestrictionTypes()) {
                if (restrictionType.getItemTypeId().equals(item.getItemTypeId())) {
                    restrictionItems.add(item);
                }
            }
        }
        return restrictionItems;
    }

    private Map<ItemType, List<ArrItem>> groupByItemType(Collection<? extends ArrItem> nodeItems) {
        Map<ItemType, List<ArrItem>> itemsByType = new HashMap<>();
        for (ArrItem item : nodeItems) {
            ItemType itemType = sdp.getItemTypeById(item.getItemTypeId());
            itemsByType.computeIfAbsent(itemType, t -> new ArrayList<>()).add(item);
        }
        return itemsByType;
    }

    private FilterRuleResultType processRule(FilterRule rule,
                                             FilterRuleContext filterRuleContext,
                                             Map<ItemType, List<ArrItem>> itemsByType,
                                             ApplyFilter filter) {

        if (!rule.canApply(filterRuleContext)) {
            // rule does not apply for this soi
            return FilterRuleResultType.RESULT_CONTINUE;
        }

        if (rule.isBreakEval()) {
            return FilterRuleResultType.RESULT_BREAK;
        }

        if (rule.isHiddenLevel()) {
            filter.hideLevel();
            return FilterRuleResultType.RESULT_BREAK;
        }

        boolean changed = false;

        if (rule.isHiddenDao()) {
            filter.hideDao();
            changed = true;
        }

        if (rule.getHiddenTypes() != null) {
            for (ItemType hiddenType : rule.getHiddenTypes()) {
                List<ArrItem> hiddenItems = itemsByType.get(hiddenType);
                if (CollectionUtils.isNotEmpty(hiddenItems)) {
                    hiddenItems.forEach(filter::addHideItem);
                    changed = true;
                }
            }
        }

        if (rule.getReplaceItems() != null) {
            for (ReplaceItem replaceDef : rule.getReplaceItems()) {
                List<ArrItem> replaceItems = itemsByType.get(replaceDef.getSource());
                if (CollectionUtils.isNotEmpty(replaceItems)) {
                    // source found -> store as new target
                    // hide original Target item(s), they are replaced by Source item(s)
                    List<ArrItem> replacedItems = itemsByType.get(replaceDef.getTarget());
                    if (CollectionUtils.isNotEmpty(replacedItems)) {
                        replacedItems.forEach(filter::addHideItem);
                    }
                    for (ArrItem replaceItem : replaceItems) {
                        // hide Source item
                        filter.addHideItem(replaceItem);
                        // copy from Source item
                        ArrItem copy = replaceItem.makeCopy();
                        // set Target type to copy of Source item
                        copy.setItemType(replaceDef.getTarget().getEntity());
                        filter.addItem(copy);
                        changed = true;
                    }
                }
            }
        }

        // added items may define further restrictions (e.g. end date of the restriction)
        // which are evaluated by the following rules
        rule.addItems(itemsByType, filter, changed, filterRuleContext, locale);

        return FilterRuleResultType.RESULT_CONTINUE;
    }

    private StructObjectInfo readRestrictionDef(Integer structuredObjectId) {
        StructObjectInfo soi = restrictionDefsById.get(structuredObjectId);
        if (soi != null) {
            return soi;
        }
        SoiLoadDispatcher soiLoadDisp = new SoiLoadDispatcher();
        soiLoader.addRequest(structuredObjectId, soiLoadDisp);
        soi = soiLoadDisp.getResult();
        Objects.requireNonNull(soi, "Restriction definition not found, structuredObjectId: " + structuredObjectId);

        restrictionDefsById.put(structuredObjectId, soi);
        return soi;
    }
}
