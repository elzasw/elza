package cz.tacr.elza.dataexchange.output.filters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.filters.AccessRestrictConfig.Def;
import cz.tacr.elza.dataexchange.output.sections.LevelInfoImpl;
import cz.tacr.elza.dataexchange.output.sections.StructObjectInfoLoader;
import cz.tacr.elza.dataexchange.output.writer.LevelInfo;
import cz.tacr.elza.dataexchange.output.writer.StructObjectInfo;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrItem;

public class AccessRestrictFilter implements ExportFilter {

    Set<Integer> restrictedNodeIds = new HashSet<>();
    private StaticDataProvider sdp;

    /**
     * Map of restriction definition objects
     */
    private Map<Integer, StructObjectInfo> structRestrDefsMap = new HashMap<>();

    /**
     * Map of restriction levels <levelId, List<restrictionId>>
     */
    private Map<Integer, List<Integer>> levelRestrMap = new HashMap<>();

    /**
     * IDs of filtered Sois for export
     */
    private Set<Integer> filteredSois = new HashSet<>();

    private StructObjectInfoLoader soiLoader;

    private AccessRestrictConfig efc;
    
    final private List<ItemType> restrictionTypes;
    final private List<FilterRule> filterRules;

    public AccessRestrictFilter(final EntityManager em, final StaticDataProvider sdp, final AccessRestrictConfig efc) {
        this.sdp = sdp;
        this.efc = efc;
        this.soiLoader = new StructObjectInfoLoader(em, 1, sdp);
        this.restrictionTypes = initRestrictionTypes(efc);
        this.filterRules = initFilterRules(efc);
    }

    private List<ItemType> initRestrictionTypes(final AccessRestrictConfig efc) {
        List<ItemType> restrictionTypes = new ArrayList<>(efc.getRestrictions().size());
        for (String structItemTypeCode : efc.getRestrictions()) {
            ItemType structItemType = sdp.getItemTypeByCode(structItemTypeCode);
            if (structItemType == null || structItemType.getDataType() != DataType.STRUCTURED) {
                throw new IllegalStateException("Struct item type is undefined or not STRUCTURED");
            }
            restrictionTypes.add(structItemType);
        }
        return restrictionTypes;
    }

    private List<FilterRule> initFilterRules(final AccessRestrictConfig efc) {
        List<FilterRule> rules = new ArrayList<>(efc.getDefs().size());
        for (Def def : efc.getDefs()) {
            FilterRule rule = new FilterRule(def, sdp);
            rules.add(rule);
        }
        return rules;
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
        List<Integer> restrictionIds = new ArrayList<>();
        for (ItemType structItemType : restrictionTypes) {
            List<ArrItem> restrictionList = itemsByType.get(structItemType);
            if (restrictionList != null) {
                for (ArrItem item : restrictionList) {
                    // skip items without data
                    if (item.getData() == null) {
                        continue;
                    }
                    // found restr
                    ArrDataStructureRef dsr = (ArrDataStructureRef) item.getData();
                    restrictionIds.add(dsr.getStructuredObjectId());
                }
            }
        }

        // expand restriction list to include parent(s) restriction list
        if (levelInfo.getParentNodeId() != null) {
            List<Integer> restParentIds = levelRestrMap.get(levelInfo.getParentNodeId());
            if (restParentIds != null) {
                restrictionIds.addAll(restParentIds);
            }
        }
        // if we have a list - we can to put to map and to filter
        if (!restrictionIds.isEmpty()) {
            levelRestrMap.put(levelInfo.getNodeId(), restrictionIds);

            for (FilterRule rule : filterRules) {
                processDef(rule, levelInfo, itemsByType, restrictionIds, filter);
            }
        }

        return filter.apply(levelInfo);
    }

    private void processDef(FilterRule rule, LevelInfoImpl levelInfo,
                            Map<ItemType, List<ArrItem>> itemsByType,
                            List<Integer> restrictionIds,
                            ApplyFilter filter) {

        for (Integer restrictionId : restrictionIds) {
            StructObjectInfo soi = readSoiFromDB(restrictionId);

            if (!rule.canApply(soi, levelInfo)) {
                // rule does not apply for this soi
                continue;
            }

            // if we need to hide level
            if (rule.isHiddenLevel()) {
                restrictedNodeIds.add(levelInfo.getNodeId());
                filteredSois.add(soi.getId());
                filter.hideLevel();
                break;
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
                        for (ArrItem replaceItem : replaceItems) {
                            // source found -> store as new target
                            List<ArrItem> replacedItems = itemsByType.get(replaceDef.getTarget());
                            if (CollectionUtils.isNotEmpty(replacedItems)) {
                                for (ArrItem replacedItem : replacedItems) {
                                    // hide target item
                                    filter.addHideItem(replacedItem);
                                    // copy from source item
                                    ArrItem copy = replaceItem.makeCopy();
                                    copy.setItemType(replaceDef.getTarget().getEntity());
                                    filter.addItem(copy);
                                    changed = true;
                                }
                            }
                        }
                    }
                }
            }

            // add itemsOnChange if changed
            if (rule.getAddItemsOnChange() != null && changed) {
                rule.getAddItemsOnChange().forEach(i -> filter.addItem(i));
            }
            if (rule.getAddItems() != null) {
                rule.getAddItems().forEach(i -> filter.addItem(i));
            }
        }
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
