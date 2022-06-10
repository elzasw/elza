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
import cz.tacr.elza.dataexchange.output.sections.LevelInfoImpl;
import cz.tacr.elza.dataexchange.output.sections.StructObjectInfoLoader;
import cz.tacr.elza.dataexchange.output.writer.LevelInfo;
import cz.tacr.elza.dataexchange.output.writer.StructObjectInfo;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.RulItemSpec;

public class AccessRestrictFilter implements ExportFilter {

    Set<Integer> restrictedNodeIds = new HashSet<>();
    //private StaticDataProvider sdp;
    private ItemType restrAccessShared;
    private ItemType restrAccessInline;
    //private StructType structTypeInline;
    //private StructType structTypeShared;

    /**
     * Map of restriction definition objects
     */
    private Map<Integer, StructObjectInfo> structRestrDefsMap = new HashMap<>();

    /**
     * IDs of filtered Sois for export
     */
    private Set<Integer> filteredSois = new HashSet<>();
    private ItemType restrType;
    private RulItemSpec restArchDescSpec;

    private StructObjectInfoLoader soiLoader;

    public AccessRestrictFilter(final EntityManager em, final StaticDataProvider sdp) {
        //this.sdp = sdp;

        this.restrAccessInline = sdp.getItemTypeByCode("ZP2015_RESTRICTION_ACCESS_INLINE");
        //this.structTypeInline = sdp.getStructuredTypeByCode("ZP2015_ACCESS_COND");

        this.restrAccessShared = sdp.getItemTypeByCode("ZP2015_RESTRICTION_ACCESS_SHARED");
        //this.structTypeShared = sdp.getStructuredTypeByCode("ZP2015_ACCESS_COND_TYPE");

        this.restrType = sdp.getItemTypeByCode("ZP2015_RESTRICTED_ACCESS_TYPE");
        this.restArchDescSpec = sdp.getItemSpecByCode("ZP2015_RESTRICTION_ARCHDESC");

        this.soiLoader = new StructObjectInfoLoader(em, 1, sdp);
    }

    @Override
    public LevelInfo processLevel(LevelInfoImpl levelInfo) {
        if (restrictedNodeIds.contains(levelInfo.getParentNodeId())) {
            restrictedNodeIds.add(levelInfo.getNodeId());
            return null;
        }
        // check if contains anon sobj
        for (ArrItem item : levelInfo.getItems()) {
            // skip items without data
            if (item.getData() == null) {
                continue;
            }

            if (item.getItemTypeId().equals(restrAccessShared.getItemTypeId())) {
                // found shared restr
                ArrDataStructureRef dsr = (ArrDataStructureRef) item.getData();
                StructObjectInfo soi = readSoiFromDB(dsr.getStructuredObjectId());

                Optional<ArrItem> restrItem = getItem(soi.getItems(), restrType);
                if(restrItem==null) {
                    // missing restriction type, maybe throw exception
                    continue;
                }
                if (restArchDescSpec != null &&
                        restArchDescSpec.getItemSpecId().equals(restrItem.get().getItemSpecId())) {
                    restrictedNodeIds.add(levelInfo.getNodeId());
                    filteredSois.add(soi.getId());
                    return null;
                }
            }
            if (item.getItemTypeId().equals(restrAccessInline.getItemTypeId())) {
                // found inline restr
                ArrDataStructureRef dsr = (ArrDataStructureRef) item.getData();
                StructObjectInfo soi = readSoiFromDB(dsr.getStructuredObjectId());

                Optional<ArrItem> restrItem = getItem(soi.getItems(), restrType);
                if(restrItem==null) {
                    // missing restriction type, maybe throw exception
                    continue;
                }
                if (restArchDescSpec != null &&
                        restArchDescSpec.getItemSpecId().equals(restrItem.get().getItemSpecId())) {
                    restrictedNodeIds.add(levelInfo.getNodeId());
                    filteredSois.add(soi.getId());
                    return null;
                }
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
