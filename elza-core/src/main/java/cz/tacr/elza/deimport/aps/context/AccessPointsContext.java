package cz.tacr.elza.deimport.aps.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.context.ImportContext;
import cz.tacr.elza.deimport.context.ImportPhase;
import cz.tacr.elza.deimport.context.ImportPhaseChangeListener;
import cz.tacr.elza.deimport.context.ObservableImport;
import cz.tacr.elza.deimport.storage.StorageManager;
import cz.tacr.elza.domain.RegCoordinates;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegScope;
import cz.tacr.elza.domain.RegVariantRecord;
import cz.tacr.elza.repository.RegExternalSystemRepository;

/**
 * Context for data exchange access points.
 */
public class AccessPointsContext {

    private final Map<String, RecordImportInfo> accessPointEntryIdMap = new HashMap<>();

    private final StorageManager storageManager;

    private final int batchSize;

    private final RegScope importScope;

    private final Map<String, RegExternalSystem> externalSystemCodeMap;

    private final List<AccessPointWrapper> accessPointQueue = new ArrayList<>();

    private final List<APVariantNameWrapper> variantNameQueue = new ArrayList<>();

    private final List<APGeoLocationWrapper> geoLocationQueue = new ArrayList<>();

    public AccessPointsContext(StorageManager storageManager,
                               int batchSize,
                               RegScope importScope,
                               RegExternalSystemRepository externalSystemRepository) {
        this.storageManager = storageManager;
        this.batchSize = batchSize;
        this.importScope = importScope;
        this.externalSystemCodeMap = loadExternalSystemCodeMap(externalSystemRepository);
    }

    public void init(ObservableImport observableImport) {
        observableImport.registerPhaseChangeListener(new AccessPointsPhaseEndListener());
    }

    public RegScope getImportScope() {
        return importScope;
    }

    public RegExternalSystem getExternalSystemByCode(String code) {
        return externalSystemCodeMap.get(code);
    }

    public RecordImportInfo getRecordInfo(String apeId) {
        return accessPointEntryIdMap.get(apeId);
    }

    /**
     * Add access point for storage
     * @param record access point to be saved
     * @param apeId Import ID of the access point
     * @param parentRecordInfo Parent information
     * @return Return record info
     */
    public RecordImportInfo addAccessPoint(RegRecord record, String apeId, RecordImportInfo parentRecordInfo) {
    	// check if parent record is stored
    	if(parentRecordInfo!=null) {
    		RecordImportInfo parentInfo = accessPointEntryIdMap.get(parentRecordInfo.getApEntryId());
    		if(parentInfo==null) {
    			throw new DEImportException("Cannot find parent info for access point, apeId:" + apeId + ", parent apeId: " + parentRecordInfo.getApEntryId());
    		}
    		// check if has ID
    		Integer parentId = parentInfo.getId();
    		if(parentId == null) {
    			// store to DB -> ID should be set
    			storeAccessPoints();
    			// check result
    			Validate.notNull(parentInfo.getId(), "Cannot get parentId, access point: {}", apeId);
    		}
    	}
    	
    	// append access point info
        RecordImportInfo info = new RecordImportInfo(apeId, record.getRegisterType());
        if (accessPointEntryIdMap.putIfAbsent(apeId, info) != null) {
            throw new DEImportException("Access point has duplicate id, apeId:" + apeId);
        }
        accessPointQueue.add(new AccessPointWrapper(record, info, parentRecordInfo));
        if (accessPointQueue.size() >= batchSize) {
            storeAccessPoints();
        }
        return info;
    }

    public void addVariantName(RegVariantRecord variantRecord, RecordImportInfo recordInfo) {
        variantNameQueue.add(new APVariantNameWrapper(variantRecord, recordInfo));
        if (variantNameQueue.size() >= batchSize) {
            storeVariantNames();
        }
    }

    public void addGeoLocation(RegCoordinates coordinates, RecordImportInfo recordInfo) {
        geoLocationQueue.add(new APGeoLocationWrapper(coordinates, recordInfo));
        if (geoLocationQueue.size() >= batchSize) {
            storeGeoLocations();
        }
    }

    /**
     * Store all queued entities.
     */
    public void storeAll() {
        storeAccessPoints();
        storeVariantNames();
        storeGeoLocations();
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

    private void storeGeoLocations() {
        if (geoLocationQueue.isEmpty()) {
            return;
        }
        storeAccessPoints();
        storageManager.saveAPGeoLocations(geoLocationQueue);
        geoLocationQueue.clear();
    }

    private static Map<String, RegExternalSystem> loadExternalSystemCodeMap(RegExternalSystemRepository externalSystemRepository) {
        List<RegExternalSystem> externalSystems = externalSystemRepository.findAll();
        Map<String, RegExternalSystem> externalSystemCodeMap = new HashMap<>(externalSystems.size());
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
