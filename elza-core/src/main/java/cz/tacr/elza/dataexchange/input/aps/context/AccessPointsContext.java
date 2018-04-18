package cz.tacr.elza.dataexchange.input.aps.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.context.ImportPhaseChangeListener;
import cz.tacr.elza.dataexchange.input.context.ObservableImport;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.repository.ApExternalSystemRepository;
import cz.tacr.elza.service.vo.ApAccessPointData;

/**
 * Context for data exchange access points.
 */
public class AccessPointsContext {

    private final Map<String, AccessPointInfo> apEntryIdMap = new HashMap<>();

    private final StorageManager storageManager;

    private final int batchSize;

    private final ApScope importScope;

    private final Map<String, ApExternalSystem> externalSystemCodeMap;

    private final List<AccessPointWrapper> accessPointQueue = new ArrayList<>();

    private final List<APVariantNameWrapper> variantNameQueue = new ArrayList<>();

    public AccessPointsContext(StorageManager storageManager, int batchSize, ApScope importScope, ImportInitHelper initHelper) {
        this.storageManager = storageManager;
        this.batchSize = batchSize;
        this.importScope = importScope;
        this.externalSystemCodeMap = loadExternalSystemCodeMap(initHelper.getExternalSystemRepository());
    }

    public void init(ObservableImport observableImport) {
        observableImport.registerPhaseChangeListener(new AccessPointsPhaseEndListener());
    }

    public ApScope getImportScope() {
        return importScope;
    }

    public ApExternalSystem getExternalSystemByCode(String code) {
        return externalSystemCodeMap.get(code);
    }

    public AccessPointInfo getAccessPointInfo(String entryId) {
        return apEntryIdMap.get(entryId);
    }

    /**
     * Add access point for storage
     *
     * @param ap access point to be saved
     * @param entryId import id of the access point
     * @return Return access point import info
     */
    public AccessPointInfo addAccessPoint(ApAccessPointData ap, String entryId) {
        // append access point info
        AccessPointInfo info = new AccessPointInfo(entryId, ap.getAccessPoint().getApType());
        if (apEntryIdMap.putIfAbsent(entryId, info) != null) {
            throw new DEImportException("Access point has duplicate id, apeId:" + entryId);
        }
        accessPointQueue.add(new AccessPointWrapper(ap, info));
        if (accessPointQueue.size() >= batchSize) {
            storeAccessPoints();
        }
        return info;
    }

    public void addVariantName(ApName variantName, AccessPointInfo apInfo) {
        variantNameQueue.add(new APVariantNameWrapper(variantName, apInfo));
        if (variantNameQueue.size() >= batchSize) {
            storeVariantNames();
        }
    }

    /**
     * Store all queued entities.
     */
    public void storeAll() {
        storeAccessPoints();
        storeVariantNames();
    }

    public void storeAccessPoints() {
        if (accessPointQueue.isEmpty()) {
            return;
        }
        storageManager.saveAccessPoints(accessPointQueue);
        accessPointQueue.clear();
    }

    private void storeVariantNames() {
        if (variantNameQueue.isEmpty()) {
            return;
        }
        storeAccessPoints();
        storageManager.saveAPVariantNames(variantNameQueue);
        variantNameQueue.clear();
    }

    private static Map<String, ApExternalSystem> loadExternalSystemCodeMap(ApExternalSystemRepository externalSystemRepository) {
        List<ApExternalSystem> externalSystems = externalSystemRepository.findAll();
        Map<String, ApExternalSystem> externalSystemCodeMap = new HashMap<>(externalSystems.size());
        externalSystems.forEach(es -> externalSystemCodeMap.put(es.getCode(), es));
        return externalSystemCodeMap;
    }

    /**
     * Listens for end of access points phase and stores all remaining entities.
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
