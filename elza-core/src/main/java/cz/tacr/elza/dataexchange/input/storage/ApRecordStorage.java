package cz.tacr.elza.dataexchange.input.storage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApDescription;
import cz.tacr.elza.domain.ApExternalSystem;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointWrapper;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
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

    public ApRecordStorage(StorageListener storageListener, LocalDateTime updateDateTime, Session session,
            ImportInitHelper initHelper) {
        super(session, storageListener);
        this.updateDateTime = updateDateTime;
        this.recordRepository = initHelper.getRecordRepository();
        this.arrangementService = initHelper.getArrangementService();
        this.variantRecordRepository = initHelper.getVariantRecordRepository();
    }

    @Override
    public void save(Collection<AccessPointWrapper> items) {
        pairCurrentApsByUuid(items);
        pairCurrentApsByEid(items);
        super.save(items);
    }

    @Override
    protected void update(Collection<AccessPointWrapper> items) {
        prepareItemsForUpdate(items);
        // AP update is not needed
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
        apNameRepository.deleteAllByAccessPointIdIn(apIds, deleteChange);
        apDescRepository.deleteAllByAccessPointIdIn(apIds, deleteChange);
        apEidRepository.deleteAllByAccessPointIdIn(apIds, deleteChange);
    }

    /**
     * Searches for current AP with same UUID, paired AP will be prepared for
     * update.
     */
    private void pairCurrentApsByUuid(Collection<AccessPointWrapper> items) {
        // init mapping: UUID -> AP wrapper
        Map<String, AccessPointWrapper> uuidMap = new HashMap<>();
        for (AccessPointWrapper item : items) {
            String uuid = item.getEntity().getUuid();
            if (uuid == null) {
                continue;
            }
            if (uuidMap.put(uuid, item) != null) {
                throw new DEImportException(
                        "Duplicate AP uuid, apId=" + item.getEntity().getAccessPointId() + ", uuid=" + uuid);
            }
        }
        // find all current AP by UUID
        List<ApAccessPointInfo> currentAps = apRepository.findByUuidIn(uuidMap.keySet());
        for (ApAccessPointInfo info : currentAps) {
            AccessPointWrapper wrapper = uuidMap.get(info.getUuid());
            wrapper.prepareUpdate(info);
        }
    }

    /**
     * Searches for AP with same external id, paired AP will be prepared for update.
     */
    private void pairCurrentApsByEid(Collection<AccessPointWrapper> rws) {
        // create external system groups by code
        ExternalSystemAggregator aggregator = new ExternalSystemAggregator();
        for (AccessPointWrapper rw : rws) {
            // check for eid existence and if not already paired (uuid priority)
            if (rw.getEntity().getExternalId() != null && rw.getPersistMethod().equals(PersistMethod.CREATE)) {
                aggregator.addRecord(rw);
            }
        }
        // find pairs by eid
        aggregator.forEach((code, group) -> {
            List<ApRecordInfoExternal> pairs = recordRepository.findByExternalSystemCodeAndExternalIdIn(code,
                    group.getExternalIds());
            for (ApRecordInfoExternal pair : pairs) {
                AccessPointWrapper rw = group.getRecord(pair.getExternalId());
                rw.setPair(pair);
            }
        });
    }

    private static class ExternalSystemAggregator {

        private final Map<String, ExternalSystemGroup> codeMap = new HashMap<>();

        public void addRecord(AccessPointWrapper record) {
            ApExternalSystem system = record.getEntity().getExternalSystem();
            Validate.notNull(system);
            ExternalSystemGroup group = codeMap.computeIfAbsent(system.getCode(), k -> new ExternalSystemGroup());
            group.addRecord(record);
        }

        public void forEach(BiConsumer<String, ExternalSystemGroup> action) {
            codeMap.entrySet().forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
        }

        public static class ExternalSystemGroup {

            private final Map<String, AccessPointWrapper> eIdMap = new HashMap<>();

            public AccessPointWrapper getRecord(String externalId) {
                return eIdMap.get(externalId);
            }

            public Collection<String> getExternalIds() {
                return Collections.unmodifiableCollection(eIdMap.keySet());
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
}
