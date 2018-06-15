package cz.tacr.elza.dataexchange.input.storage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApExternalSystem;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointWrapper;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.dataexchange.input.storage.EntityWrapper.PersistType;
import cz.tacr.elza.domain.ApRecord;
import cz.tacr.elza.domain.projection.ApAccessPointInfo;
import cz.tacr.elza.domain.projection.ApRecordInfo;
import cz.tacr.elza.domain.projection.ApRecordInfoExternal;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApDescriptionRepository;
import cz.tacr.elza.repository.ApExternalIdRepository;
import cz.tacr.elza.repository.ApNameRepository;
import cz.tacr.elza.repository.ApRecordRepository;
import cz.tacr.elza.repository.ApVariantRecordRepository;
import cz.tacr.elza.service.ArrangementService;

/**
 * Specialization of wrapper storage witch pairs imported records with database
 * state.
 */
class ApRecordStorage extends EntityStorage<AccessPointWrapper> {

    private final ApAccessPointRepository apRepository;

    private final ApNameRepository apNameRepository;

    private final ApDescriptionRepository apDescRepository;

    private final ApExternalIdRepository apEidRepository;

    private final ArrangementService arrangementService;

    private final ApChange deleteChange;

    public ApRecordStorage(Session session, MemoryManager memoryManager, ApChange deleteChange,
            ImportInitHelper initHelper) {
        super(session, memoryManager);
        this.apRepository = initHelper.getApRepository();
        this.apNameRepository = initHelper.getApNameRepository();
        this.apDescRepository = initHelper.getApDescRepository();
        this.apEidRepository = initHelper.getApEidRepository();
        this.arrangementService = initHelper.getArrangementService();
        this.deleteChange = deleteChange;
    }

    @Override
    public void save(Collection<AccessPointWrapper> items) {
        pairApsByUuid(items);
        pairApsByEid(items);
        super.save(items);
    }

    @Override
    protected void update(Collection<AccessPointWrapper> items) {
        prepareItemsForUpdate(items);
        // actual AP update is not needed
    }

    @Override
    protected void create(AccessPointWrapper item) {
        // set UUID for new AP
        item.getEntity().setUuid(arrangementService.generateUuid());
        // create item
        super.create(item);
    }

    /**
     * Invalidates current sub-entities for each access point.
     */
    private void prepareItemsForUpdate(Collection<AccessPointWrapper> items) {
        List<Integer> apIds = new ArrayList<>(items.size());
        for (AccessPointWrapper item : items) {
            Integer apId = item.getEntity().getAccessPointId();
            apIds.add(Validate.notNull(apId));
        }
        apNameRepository.deleteByAccessPointIdIn(apIds, deleteChange);
        apDescRepository.deleteByAccessPointIdIn(apIds, deleteChange);
        apEidRepository.deleteByAccessPointIdIn(apIds, deleteChange);
    }

    private void pairApsByUuid(Collection<AccessPointWrapper> items) {
        // init UUID -> AP map
        Map<String, AccessPointWrapper> uuidMap = new HashMap<>();
        for (AccessPointWrapper item : items) {
            String uuid = item.getEntity().getUuid();
            if (uuid == null) {
                continue; // UUID not imported
            }
            if (uuidMap.put(uuid, item) != null) {
                throw new DEImportException("Duplicate AP uuid, value=" + uuid);
            }
        }
        // find current AP by UUID
        List<ApAccessPointInfo> currentAps = apRepository.findByUuidIn(uuidMap.keySet());
        for (ApAccessPoint info : currentAps) {
            AccessPointWrapper wrapper = uuidMap.get(info.getUuid());
            if (wrapper.changeToUpdated(info)) {
                memoryManager.onEntityPersist(wrapper, info);
            }
        }
    }

    private void pairApsByEid(Collection<AccessPointWrapper> items) {
        Map<String, ExternalIdTypeGroup> typeGroupMap = new HashMap<>();

        // create external id type groups
        for (AccessPointWrapper item : items) {
            if (!item.getPersistType().equals(PersistType.CREATE)) {
                continue; // ignore paired by UUID
            }
            MultiValuedMap<String, String> typeValueMap = item.getEidTypeValueMap();
            if (typeValueMap == null) {
                continue; // no external ids
            }
            MapIterator<String, String> typeValueIt = typeValueMap.mapIterator();
            while (typeValueIt.hasNext()) {
                String type = typeValueIt.getKey();
                ExternalIdTypeGroup group = typeGroupMap.get(type);
                if (group == null) {
                    typeGroupMap.put(type, group = new ExternalIdTypeGroup());
                }
                group.addAp(typeValueIt.getValue(), item);
            }
        }

        // find pairs by external ids
        typeGroupMap.forEach((type, group) -> {
            List<ApAccessPoint> currentAps = apRepository.findByEidTypeCodeAndEidValuesIn(type, group.getValues());
            for (ApAccessPoint ap : currentAps) {
                AccessPointWrapper rw = group.getAp(ap.getExternalId());
                rw.setPair(pair);
            }
        });
    }

    private static class ExternalIdTypeGroup {

        private final Map<String, AccessPointWrapper> externalIdMap = new HashMap<>();

        public AccessPointWrapper getAp(String externalId) {
            return externalIdMap.get(externalId);
        }

        public void addAp(String value, AccessPointWrapper item) {
            // TODO Auto-generated method stub

        }

        public Collection<String> getValues() {
            return externalIdMap.keySet();
        }

        private void addRecord(AccessPointWrapper record) {
            String externalId = record.getEntity().getExternalId();
            Validate.notEmpty(externalId);
            if (eIdMap.putIfAbsent(externalId, record) != null) {
                throw new DEImportException("Access point has duplicate external id, externalSystemCode:"
                        + record.getEntity().getExternalSystem().getCode() + ", externalId:" + externalId);
            }
        }
    }
}
