package cz.tacr.elza.dataexchange.output.filters;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.output.filters.AccessRestrictConfig.Def;
import cz.tacr.elza.dataexchange.output.sections.LevelInfoImpl;
import cz.tacr.elza.dataexchange.output.sections.StructObjectInfoLoader;
import cz.tacr.elza.dataexchange.output.writer.LevelInfo;
import cz.tacr.elza.dataexchange.output.writer.StructObjectInfo;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulItemSpec;

public class AccessRestrictFilter implements ExportFilter {

    Set<Integer> restrictedNodeIds = new HashSet<>();
    private StaticDataProvider sdp;

    /**
     * Map of restriction definition objects
     */
    private Map<Integer, StructObjectInfo> structRestrDefsMap = new HashMap<>();

    /**
     * IDs of filtered Sois for export
     */
    private Set<Integer> filteredSois = new HashSet<>();

    private StructObjectInfoLoader soiLoader;

    private AccessRestrictConfig efc;

    public AccessRestrictFilter(final EntityManager em, final StaticDataProvider sdp, final AccessRestrictConfig efc) {
        this.sdp = sdp;
        this.efc = efc;
        this.soiLoader = new StructObjectInfoLoader(em, 1, sdp);
    }

    @Override
    public LevelInfo processLevel(LevelInfoImpl levelInfo) {
        if (restrictedNodeIds.contains(levelInfo.getParentNodeId())) {
            restrictedNodeIds.add(levelInfo.getNodeId());
            return null;
        }
        // TODO: create item type Map<ItemType, List<ArrItem>>  
        for (Def def : efc.getDefs()) {
            levelInfo = processDef(def, levelInfo);
            if (levelInfo == null) {
                return null;
            }
        }

        return levelInfo;
    }

    private LevelInfoImpl processDef(Def def, LevelInfoImpl levelInfo) {
        ItemType structItemType = sdp.getItemTypeByCode(def.getWhen().getStructItemType());
        
        ItemType itemType = sdp.getItemTypeByCode(def.getWhen().getItemType());
        RulItemSpec itemSpec = sdp.getItemSpecByCode(def.getWhen().getItemSpec());
                
        // check if contains anon sobj
        for (ArrItem item : levelInfo.getItems()) {
            // skip items without data
            if (item.getData() == null) {
                continue;
            }
            if (item.getItemTypeId().equals(structItemType.getItemTypeId())) {
                // found restr
                ArrDataStructureRef dsr = (ArrDataStructureRef) item.getData();
                StructObjectInfo soi = readSoiFromDB(dsr.getStructuredObjectId());

                Optional<ArrItem> restrItem = getItem(soi.getItems(), itemType);
                if (restrItem == null) {
                    // missing restriction type, maybe throw exception
                    break;
                }
                if (itemSpec == null ||
                        !itemSpec.getItemSpecId().equals(restrItem.get().getItemSpecId())) {
                    break;
                }

                // apply result
                if (def.getResult().getHiddenItem()) {
                    restrictedNodeIds.add(levelInfo.getNodeId());
                    filteredSois.add(soi.getId());
                    return null;
                }
                // TODO different result
            }
        }
        return levelInfo;
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

    private Optional<ArrItem> getItem(Collection<ArrItem> items, ItemType itemType) {
        return items.stream().filter(i -> i.getItemTypeId().equals(itemType.getItemTypeId())).findFirst();
    }

    @Override
    public StructObjectInfo processStructObj(StructObjectInfo structObjectInfo) {
        if (this.filteredSois.contains(structObjectInfo.getId())) {
            return null;
        }
        return structObjectInfo;
    }
}
