package cz.tacr.elza.dataexchange.input.storage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import cz.tacr.elza.domain.ApExternalSystem;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointWrapper;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.dataexchange.input.context.PersistMethod;
import cz.tacr.elza.domain.ApRecord;
import cz.tacr.elza.domain.projection.ApRecordInfo;
import cz.tacr.elza.domain.projection.ApRecordInfoExternal;
import cz.tacr.elza.repository.ApRecordRepository;
import cz.tacr.elza.repository.ApVariantRecordRepository;
import cz.tacr.elza.service.ArrangementService;

/**
 * Specialization of wrapper storage witch pairs imported records with database state.
 */
class ApRecordStorage extends EntityStorage<AccessPointWrapper> {

    private final ApRecordRepository recordRepository;

    private final ArrangementService arrangementService;

    private final ApVariantRecordRepository variantRecordRepository;

    private final LocalDateTime updateDateTime;

    public ApRecordStorage(StorageListener storageListener,
                           LocalDateTime updateDateTime,
                           Session session,
                           ImportInitHelper initHelper) {
        super(session, storageListener);
        this.updateDateTime = updateDateTime;
        this.recordRepository = initHelper.getRecordRepository();
        this.arrangementService = initHelper.getArrangementService();
        this.variantRecordRepository = initHelper.getVariantRecordRepository();
    }

    @Override
    public void save(Collection<AccessPointWrapper> items) {
        pairCurrentRecordsByUuid(items);
        pairCurrentRecordsByEid(items);
        super.save(items);
    }

    @Override
    protected void update(Collection<AccessPointWrapper> items) {
        prepareCollectionForUpdate(items);
        super.update(items);
    }

    @Override
    protected void create(AccessPointWrapper item, Session session) {
        prepareItemForCreate(item);
        super.create(item, session);
    }

    /**
     * Deletes current sub-entities for each access point.
     */
    private void prepareCollectionForUpdate(Collection<AccessPointWrapper> items) {
        List<ApRecord> records = new ArrayList<>(items.size());
        for (AccessPointWrapper rw : items) {
            ApRecord record = rw.getEntity();
            Validate.notNull(record.getRecordId());
            records.add(record);
        }
        variantRecordRepository.deleteByApRecordIn(records);
    }

    /**
     * Sets last update dateTime and unique UUID for new entity.
     */
    private void prepareItemForCreate(AccessPointWrapper item) {
        ApRecord record = item.getEntity();
        if (record.getLastUpdate() == null) {
            record.setLastUpdate(updateDateTime);
        }
        record.setUuid(arrangementService.generateUuid());
    }

    /**
     * Searches for record pairs by uuid, paired records will be prepared for update.
     */
    private void pairCurrentRecordsByUuid(Collection<AccessPointWrapper> rws) {
        // create record map and check uuid duplicate
        Map<String, AccessPointWrapper> uuidMap = new HashMap<>();
        for (AccessPointWrapper rw : rws) {
            String uuid = rw.getEntity().getUuid();
            if (uuid != null) {
                if (uuidMap.put(uuid, rw) != null) {
                    throw new DEImportException("Duplicate record uuid, uuid:" + uuid);
                }
            }
        }
        // find pairs by uuid
        List<ApRecordInfo> pairs = recordRepository.findByUuidIn(uuidMap.keySet());
        for (ApRecordInfo pair : pairs) {
            AccessPointWrapper rw = uuidMap.get(pair.getUuid());
            rw.setPair(pair);
        }
    }

    /**
     * Searches for record pairs by external id, paired records will be prepared for update.
     */
    private void pairCurrentRecordsByEid(Collection<AccessPointWrapper> rws) {
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
