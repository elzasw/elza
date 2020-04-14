package cz.tacr.elza.dataexchange.input.aps.context;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.ApChangeHolder;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.*;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.service.AccessPointService;
import cz.tacr.elza.service.ArrangementService;

import java.util.*;

/**
 * Context for data exchange access points.
 */
public class AccessPointsContext {

    private final Map<String, AccessPointInfo> entryIdApInfoMap = new HashMap<>();

    private final StorageManager storageManager;

    private final int batchSize;

    private final ApScope scope;

    private final ApChangeHolder changeHolder;

    private final StaticDataProvider staticData;

    private final ArrangementService arrangementService;

    private final AccessPointService accessPointService;

    private final List<AccessPointWrapper> apQueue = new ArrayList<>();

    private final List<ApExternalIdWrapper> eidQueue = new ArrayList<>();

    public AccessPointsContext(StorageManager storageManager, int batchSize, ApScope scope, ApChangeHolder changeHolder,
                               StaticDataProvider staticData, ImportInitHelper initHelper) {
        this.storageManager = storageManager;
        this.batchSize = batchSize;
        this.scope = scope;
        this.changeHolder = changeHolder;
        this.staticData = staticData;
        this.arrangementService = initHelper.getArrangementService();
        this.accessPointService = initHelper.getAccessPointService();
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

    public ApExternalIdType getEidType(String code) {
        return staticData.getApEidTypeByCode(code);
    }

    public SysLanguage getSysLanguageByCode(String code) {
        return staticData.getSysLanguageByCode(code);
    }

    /**
     * Add access point for storage
     *
     * @param entity
     *            access point to be saved
     * @param entryId
     *            import id of the access point
     * @param eids
     *            AP external ids, can be null
     * @return Return access point import info
     */
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

    /**
     * Store all queued entities.
     */
    public void storeAll() {
        storeAccessPoints();
        storeExternalIds(false);
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

    public int nextNameObjectId() {
        return accessPointService.nextNameObjectId();
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
