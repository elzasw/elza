package cz.tacr.elza.dataexchange.input.aps.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.context.ImportContext;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.context.ImportPhase;
import cz.tacr.elza.dataexchange.input.context.ImportPhaseChangeListener;
import cz.tacr.elza.dataexchange.input.context.ObservableImport;
import cz.tacr.elza.dataexchange.input.storage.StorageManager;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.ApExternalIdType;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApNameType;
import cz.tacr.elza.domain.ApRecord;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApVariantRecord;
import cz.tacr.elza.repository.ApExternalSystemRepository;

/**
 * Context for data exchange access points.
 */
public class AccessPointsContext {

	private final Map<String, AccessPointInfo> apEntryIdMap = new HashMap<>();

	private final StorageManager storageManager;

	private final int batchSize;

	private final ApScope scope;

	private final ApChange createChange;

	private final StaticDataProvider staticData;

	private final List<AccessPointWrapper> accessPointQueue = new ArrayList<>();

	private final List<APVariantNameWrapper> variantNameQueue = new ArrayList<>();

	public AccessPointsContext(StorageManager storageManager, int batchSize, ApScope scope, ApChange createChange,
			StaticDataProvider staticData) {
		this.storageManager = storageManager;
		this.batchSize = batchSize;
		this.scope = scope;
		this.createChange = createChange;
		this.staticData = staticData;
	}

	public void init(ObservableImport observableImport) {
		observableImport.registerPhaseChangeListener(new AccessPointsPhaseEndListener());
	}

	public ApScope getScope() {
		return scope;
	}

	public ApChange getCreateChange() {
		return createChange;
	}

	public AccessPointInfo getAccessPointInfo(String entryId) {
		return apEntryIdMap.get(entryId);
	}

	public ApExternalIdType getApEidTypeByCode(String code) {
		return staticData.getApEidTypeByCode(code);
	}

	/**
	 * Add access point for storage
	 *
	 * @param ap
	 *            access point to be saved
	 * @param entryId
	 *            import id of the access point
	 * @param parentAPInfo
	 *            Parent information
	 * @return Return access point import info
	 */
	public AccessPointInfo addAccessPoint(ApAccessPoint entity, String entryId) {
		// append access point info
		AccessPointInfo info = new AccessPointInfo(entryId, ap.getApType());
		if (apEntryIdMap.putIfAbsent(entryId, info) != null) {
			throw new DEImportException("Access point has duplicate id, apeId:" + entryId);
		}
		accessPointQueue.add(new AccessPointWrapper(ap, info, parentAPInfo));
		if (accessPointQueue.size() >= batchSize) {
			storeAccessPoints();
		}
		return info;
	}

	public void addVariantName(ApVariantRecord variantName, AccessPointInfo apInfo) {
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

	private static Map<String, ApExternalSystem> loadExternalSystemCodeMap(
			ApExternalSystemRepository externalSystemRepository) {
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

	public void addExternalId(ApExternalId apEid, AccessPointInfo info) {
		// TODO Auto-generated method stub
		xx
	}

	public void addDescription(ApDescription apDesc, AccessPointInfo info) {
		// TODO Auto-generated method stub
		xx
	}

	/**
	 * @return True when value is valid language defined as 3 letters ISO code in
	 *         SysLanguage table. Empty language is also valid.
	 */
	public boolean isValidLanguage(String lang) {
		// TODO Auto-generated method stub
		return false;sss
	}

	public ApNameType getNameTypeByCode(String t) {
		// TODO Auto-generated method stub
		return null;xx
	}

	public void addName(ApName apName, AccessPointInfo info) {
		// TODO Auto-generated method stub
		cc
	}
}
