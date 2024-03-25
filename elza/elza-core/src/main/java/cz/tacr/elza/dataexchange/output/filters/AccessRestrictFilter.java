package cz.tacr.elza.dataexchange.output.filters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import cz.tacr.elza.common.db.HibernateUtils;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.sections.LevelInfoImpl;
import cz.tacr.elza.dataexchange.output.sections.StructObjectInfoLoader;
import cz.tacr.elza.dataexchange.output.writer.LevelInfo;
import cz.tacr.elza.dataexchange.output.writer.StructObjectInfo;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrItem;

public class AccessRestrictFilter implements ExportFilter {

    final Set<Integer> restrictedNodeIds = new HashSet<>();
    final private StaticDataProvider sdp;
    final private ElzaLocale elzaLocale;

    /**
     * Map of restriction definition objects
     */
    final private Map<Integer, StructObjectInfo> structRestrDefsMap = new HashMap<>();

    /**
     * Map of restriction levels <levelId, List<ArrItem>>
     */
    private Map<Integer, List<ArrItem>> levelRestrMap = new HashMap<>();

    /**
     * IDs of filtered Sois for export
     */
    final private Set<Integer> filteredSois = new HashSet<>();

    final private StructObjectInfoLoader soiLoader;

    final private AccessRestrictConfig efc;

    final private FilterRules filterRules;

    public AccessRestrictFilter(final EntityManager em, final StaticDataProvider sdp, final AccessRestrictConfig efc,
                                final ElzaLocale elzaLocale) {
        this.sdp = sdp;
        this.efc = efc;
        this.soiLoader = new StructObjectInfoLoader(em, 1, sdp);
        this.filterRules = new FilterRules(efc, sdp);
        this.elzaLocale = elzaLocale;
    }

    @Override
    public LevelInfo processLevel(LevelInfoImpl levelInfo) {
        if (restrictedNodeIds.contains(levelInfo.getParentNodeId())) {
            restrictedNodeIds.add(levelInfo.getNodeId());
            return null;
        }
        ApplyFilter filter = new ApplyFilter();

        Map<ItemType, List<ArrItem>> itemsByType = levelInfo.getItems().stream()
                .collect(Collectors.groupingBy(item -> sdp.getItemTypeById(item.getItemTypeId())));

        // to get list of restriction ids by restrictions item types
        List<ArrItem> restrictionItems = new ArrayList<>();
        for (ItemType itemType : filterRules.getRestrictionTypes()) {
            List<ArrItem> restrictionList = itemsByType.get(itemType);
            if (restrictionList != null) {
                for (ArrItem item : restrictionList) {
                    // skip items without data
                    if (item.getData() == null) {
                        continue;
                    }
                    // found restr
                    restrictionItems.add(item);
                }
            }
        }

        // expand restriction list to include parent(s) restriction list
        if (levelInfo.getParentNodeId() != null) {
            List<ArrItem> restParentItems = levelRestrMap.get(levelInfo.getParentNodeId());
            if (restParentItems != null) {
                restrictionItems.addAll(restParentItems);
            }
        }

        // if we have a list - we have to filter
        if (!restrictionItems.isEmpty()) {
            levelRestrMap.put(levelInfo.getNodeId(), restrictionItems);

            for (ArrItem restrictionItem : restrictionItems) {
                ItemType itemType = sdp.getItemTypeById(restrictionItem.getItemTypeId());
                Integer restrStructId = null;
                Collection<ArrItem> restrItems;
                if (itemType.getDataType().equals(DataType.STRUCTURED)) {
                    // Load real structured level
                    ArrData data = HibernateUtils.unproxy(restrictionItem.getData());
                    ArrDataStructureRef sdr = (ArrDataStructureRef) data;
                    Integer restrictionId = sdr.getStructuredObjectId();
                    StructObjectInfo soi = readSoiFromDB(restrictionId);
                    restrStructId = soi.getId();
                    restrItems = soi.getItems();
                } else {
                    // Create fake SOI from current level
                    restrItems = levelInfo.getItems();
                }

                FilterRuleContext frCtx = new FilterRuleContext(restrItems);
                for (FilterRule rule : filterRules.getFilterRules()) {
                    FilterRuleResultType result = processRule(rule, frCtx, levelInfo, itemsByType, restrStructId,
                                                              filter);
                    if (result == FilterRuleResultType.RESULT_BREAK) {
                        break;
                }
            }
        }
        }

        return filter.apply(levelInfo);
    }

    /**
     *
     * @param rule
     * @param levelInfo
     * @param itemsByType
     * @param soiRestrId
     *            Optional ID of source structured ID
     *            If ID is set and level is ignored, whole structured object is
     *            marked as ignored
     * @param restrItems
     * @param soi
     * @param filter
     */
    private FilterRuleResultType processRule(FilterRule rule,
                             FilterRuleContext filterRuleContext,
                             LevelInfoImpl levelInfo,
                            Map<ItemType, List<ArrItem>> itemsByType,
                             @Nullable Integer soiRestrId,
                            ApplyFilter filter) {

        if (!rule.canApply(filterRuleContext)) {
            // rule does not apply for this soi
            return FilterRuleResultType.RESULT_CONTINUE;
        }

        if (rule.isBreakEval()) {
            return FilterRuleResultType.RESULT_BREAK;
        }

        // if we need to hide level
        if (rule.isHiddenLevel()) {
            restrictedNodeIds.add(levelInfo.getNodeId());
            // TODO: This should be reworked
            //       SOI should be automatically removed when not referenced and
            //       based on this criteria
            if (soiRestrId != null) {
                filteredSois.add(soiRestrId);
            }
            filter.hideLevel();

            return FilterRuleResultType.RESULT_BREAK;
        }

        boolean changed = false;
        // hidden Dao
        if (rule.isHiddenDao()) {
            filter.hideDao();
            changed = true;
        }

        // check hidden items
        if (rule.getHiddenTypes() != null) {
            for (ItemType hiddenType : rule.getHiddenTypes()) {
                List<ArrItem> hiddenItems = itemsByType.get(hiddenType);
                if (CollectionUtils.isNotEmpty(hiddenItems)) {
                    hiddenItems.forEach(hi -> filter.addHideItem(hi));
                    changed = true;
                }
            }
        }

        // replace itemType(s)
        if (rule.getReplaceItems() != null) {
            for (ReplaceItem replaceDef : rule.getReplaceItems()) {
                List<ArrItem> replaceItems = itemsByType.get(replaceDef.getSource());
                if (CollectionUtils.isNotEmpty(replaceItems)) {
                    // source found -> store as new target
                    for (ArrItem replaceItem : replaceItems) {
                        List<ArrItem> replacedItems = itemsByType.get(replaceDef.getTarget());
                        // if exists ArrItem(s) with Target type
                        if (CollectionUtils.isNotEmpty(replacedItems)) {
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
        }

        // add itemsOnChange if changed
        rule.addItems(itemsByType, filter, changed, filterRuleContext, elzaLocale.getLocale());

        return FilterRuleResultType.RESULT_CONTINUE;
        }

    private StructObjectInfo readSoiFromDB(Integer structuredObjectId) {
        StructObjectInfo soi = structRestrDefsMap.get(structuredObjectId);
        if (soi != null) {
            return soi;
        }
        // read from DB
        SoiLoadDispatcher soiLoadDisp = new SoiLoadDispatcher();
        soiLoader.addRequest(structuredObjectId, soiLoadDisp);
        soi = soiLoadDisp.getResult();
        Validate.notNull(soi);

        structRestrDefsMap.put(structuredObjectId, soi);
        return soi;
    }

    @Override
    public StructObjectInfo processStructObj(StructObjectInfo structObjectInfo) {
        if (this.filteredSois.contains(structObjectInfo.getId())) {
            return null;
        }
        return structObjectInfo;
    }
}
