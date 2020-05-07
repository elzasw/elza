package cz.tacr.elza.dataexchange.input.aps.context;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.ApChangeHolder;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.ObjectIdHolder;
import cz.tacr.elza.dataexchange.input.context.*;
import cz.tacr.elza.dataexchange.input.parts.context.ParentPartWrapper;
import cz.tacr.elza.dataexchange.input.parts.context.PartInfo;
import cz.tacr.elza.dataexchange.input.parts.context.PartWrapper;
import cz.tacr.elza.dataexchange.input.parts.context.PrefferedPartWrapper;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.service.AccessPointItemService;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.ItemService;

import java.util.*;

/**
 * Context for data exchange access points.
 */
public class AccessPointsContext {

    private final Map<String, AccessPointInfo> entryIdApInfoMap = new HashMap<>();

    private final Map<String, AccessPointInfo> pgIdApInfoMap = new HashMap<>();

    private final StorageManager storageManager;

    private final int batchSize;

    private final ApScope scope;

    private final ApChangeHolder changeHolder;

    private final ObjectIdHolder objectIdHolder;

    private final StaticDataProvider staticData;

    private final ArrangementService arrangementService;

    private final AccessPointService accessPointService;

    private final AccessPointItemService accessPointItemService;

    private final List<AccessPointWrapper> apQueue = new ArrayList<>();

    private final List<ApExternalIdWrapper> eidQueue = new ArrayList<>();

    private final List<PartWrapper> partQueue = new ArrayList<>();

    private final List<ParentPartWrapper> parentPartQueue = new ArrayList<>();

    private Map<PartWrapper, String> parentPartIdMap = new HashMap<>();

    private final List<PrefferedPartWrapper> prefferedPartQueue = new ArrayList<>();

    private Set<AccessPointInfo> prefferedPartApInfos = new HashSet<>();

    public AccessPointsContext(StorageManager storageManager, int batchSize, ApScope scope, ApChangeHolder changeHolder,
                               StaticDataProvider staticData, ImportInitHelper initHelper, ObjectIdHolder objectIdHolder) {
        this.storageManager = storageManager;
        this.batchSize = batchSize;
        this.scope = scope;
        this.changeHolder = changeHolder;
        this.staticData = staticData;
        this.arrangementService = initHelper.getArrangementService();
        this.accessPointService = initHelper.getAccessPointService();
        this.accessPointItemService = initHelper.getAccessPointItemService();
        this.objectIdHolder = objectIdHolder;
    }

    public void init(ObservableImport observableImport) {
        observableImport.registerPhaseChangeListener(new AccessPointsPhaseEndListener());
    }

    public ApScope getScope() {
        return scope;
    }

    public ApChange getCreateChange() {
        return changeHolder.getChange();
    }

    public AccessPointInfo getApInfo(String entryId) {
        return entryIdApInfoMap.get(entryId);
    }

    public AccessPointInfo getPgIdApInfo(String importId) {
        return pgIdApInfoMap.get(importId);
    }

    public void addToPgIdApInfoMap(String importId, AccessPointInfo apInfo) {
        pgIdApInfoMap.putIfAbsent(importId, apInfo);
    }


    public Collection<AccessPointInfo> getAllAccessPointInfo() {
        return Collections.unmodifiableCollection(entryIdApInfoMap.values());
    }

    public ApExternalIdType getEidType(String code) {
        return staticData.getApEidTypeByCode(code);
    }

    public RulPartType getRulPartType(String code) {
        return staticData.getPartTypeByCode(code);
    }

    public SysLanguage getSysLanguageByCode(String code) {
        return staticData.getSysLanguageByCode(code);
    }

    public void addToParentPartIdMap(PartWrapper partWrapper, String parentFragmentId) {
        parentPartIdMap.putIfAbsent(partWrapper, parentFragmentId);
    }

    /**
     * Add access point for storage
     *
     * @param entity  access point to be saved
     * @param entryId import id of the access point
     * @param eids    AP external ids, can be null
     * @return Return access point import info
     */
    public AccessPointInfo addAccessPoint(ApAccessPoint entity, String entryId, ApState apState, Collection<ApExternalId> eids, Collection<PartWrapper> partWrappers) {
        AccessPointInfo info = new AccessPointInfo(apState);
        if (entryIdApInfoMap.putIfAbsent(entryId, info) != null) {
            throw new DEImportException("Access point has duplicate id, apeId:" + entryId);
        }
        // add to queue
        AccessPointWrapper apWrapper = new AccessPointWrapper(entity, info, eids, arrangementService);
        apQueue.add(apWrapper);
        info.onEntityQueued();
        if (apQueue.size() >= batchSize) {
            storeAccessPoints();
        }
        // add all external ids to queue
        if (eids != null) {
            eids.forEach(eid -> addExternalId(eid, info));
        }

        if (partWrappers != null) {
            partWrappers.forEach(partWrapper -> addPart(partWrapper, apWrapper));
        }



        return info;
    }

    public AccessPointInfo addAccessPoint(ApAccessPoint entity, String entryId, ApState apState, Collection<ApExternalId> eids) {
        AccessPointInfo info = new AccessPointInfo(apState);
        if (entryIdApInfoMap.putIfAbsent(entryId, info) != null) {
            throw new DEImportException("Access point has duplicate id, apeId:" + entryId);
        }
        // add to queue
        apQueue.add(new AccessPointWrapper(entity, info, eids, arrangementService));
        info.onEntityQueued();
        if (apQueue.size() >= batchSize) {
            storeAccessPoints();
        }
        // add all external ids to queue
        if (eids != null) {
            eids.forEach(eid -> addExternalId(eid, info));
        }
        return info;
    }

    private void addExternalId(ApExternalId entity, AccessPointInfo apInfo) {
        eidQueue.add(new ApExternalIdWrapper(entity, apInfo));
        apInfo.onEntityQueued();
        if (eidQueue.size() >= batchSize) {
            storeExternalIds(true);
        }
    }

    private void addPart(PartWrapper partWrapper, AccessPointWrapper apWrapper) {
        AccessPointInfo apInfo = apWrapper.getApInfo();
        partWrapper.getPartInfo().setApInfo(apInfo);

        partQueue.add(partWrapper);
        apInfo.onEntityQueued();
        if (partQueue.size() >= batchSize) {
            storeParts(true);
        }

        if(!prefferedPartApInfos.contains(apInfo) && partWrapper.getPartInfo().getRulPartType().getCode().equals("PT_NAME")) {
            PrefferedPartWrapper prefferedPartWrapper = new PrefferedPartWrapper(apInfo, partWrapper.getPartInfo());
            prefferedPartQueue.add(prefferedPartWrapper);
            prefferedPartApInfos.add(apInfo);
            apInfo.onEntityQueued();
        }

        if (parentPartIdMap.containsValue(partWrapper.getPartInfo().getImportId())) {
            for (Map.Entry<PartWrapper, String> entry : parentPartIdMap.entrySet()) {
                if (partWrapper.getPartInfo().getImportId().equals(entry.getValue())) {
                    ParentPartWrapper parentPartWrapper = new ParentPartWrapper(entry.getKey().getPartInfo(), partWrapper.getPartInfo());
                    parentPartQueue.add(parentPartWrapper);
                    entry.getKey().getPartInfo().onEntityQueued();
                    if (parentPartQueue.size() > batchSize) {
                        storeParentParts(true);
                    }
                }
            }
        }


    }

    /**
     * Store all queued entities.
     */
    public void storeAll() {
        storeAccessPoints();
        storeExternalIds(false);
        storeParts(false);
        storeParentParts(false);
        storePrefferedParts(false);
    }

    public void storeAccessPoints() {
        storageManager.storeAccessPoints(apQueue);
        apQueue.clear();
    }

    private void storeExternalIds(boolean storeReferenced) {
        if (storeReferenced) {
            storeAccessPoints();
        }
        storageManager.storeGeneric(eidQueue);
        eidQueue.clear();
    }

    private void storeParts(boolean storeReferenced) {
        if (storeReferenced) {
            storeAccessPoints();
        }
        //storageManager.storeGeneric(partQueue);
        storageManager.storeParts(partQueue);
        for (PartWrapper wrapper : partQueue) {
            wrapper.getPartInfo().setEntityId(wrapper.getEntity().getPartId());
        }
        partQueue.clear();
    }

    private void storeParentParts(boolean storeReferenced) {
        if (storeReferenced) {
            storeParts(true);
        }
        storageManager.storeRefUpdates(parentPartQueue);
        parentPartQueue.clear();
    }

    private void storePrefferedParts(boolean storeReferenced) {
        if (storeReferenced) {
            storeParts(true);
        }
        storageManager.storeRefUpdates(prefferedPartQueue);
        prefferedPartQueue.clear();
    }

    /*public int nextNameObjectId() {
        return accessPointService.nextNameObjectId();
    }*/

    public int nextItemObjectId() {
        return accessPointItemService.nextItemObjectId();
    }

    /**
     * Listens for end of AP phase and stores all remaining entities.
     */
    private static class AccessPointsPhaseEndListener implements ImportPhaseChangeListener {

        @Override
        public boolean onPhaseChange(ImportPhase previousPhase, ImportPhase nextPhase, ImportContext context) {
            if (previousPhase == ImportPhase.ACCESS_POINTS) {
                context.getAccessPoints().storeAll();
                return false;
            }
            return !ImportPhase.ACCESS_POINTS.isSubsequent(nextPhase);
        }
    }
}
