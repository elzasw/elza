package cz.tacr.elza.deimport.storage;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.deimport.DEImportException;
import cz.tacr.elza.deimport.aps.context.AccessPointWrapper;
import cz.tacr.elza.deimport.context.EntityState;
import cz.tacr.elza.domain.RegExternalSystem;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.projection.RegRecordInfo;
import cz.tacr.elza.domain.projection.RegRecordInfoExternal;
import cz.tacr.elza.repository.RegCoordinatesRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.repository.RegVariantRecordRepository;
import cz.tacr.elza.service.ArrangementService;

/**
 * Specialization of wrapper storage witch pairs imported records with database state.
 */
class RegRecordStorage extends EntityStorage<AccessPointWrapper> {

    private final RegRecordRepository recordRepository;

    private final ArrangementService arrangementService;

    private final RegCoordinatesRepository coordinatesRepository;

    private final RegVariantRecordRepository variantRecordRepository;

    private final LocalDateTime updateDateTime;

    public RegRecordStorage(Session session,
                            StorageListener storageListener,
                            LocalDateTime updateDateTime,
                            RegRecordRepository recordRepository,
                            ArrangementService arrangementService,
                            RegCoordinatesRepository coordinatesRepository,
                            RegVariantRecordRepository variantRecordRepository) {
        super(session, storageListener);
        this.updateDateTime = updateDateTime;
        this.recordRepository = recordRepository;
        this.arrangementService = arrangementService;
        this.coordinatesRepository = coordinatesRepository;
        this.variantRecordRepository = variantRecordRepository;
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
        List<RegRecord> records = new ArrayList<>(items.size());
        for (AccessPointWrapper rw : items) {
            RegRecord record = rw.getEntity();
            Validate.notNull(record.getRecordId());
            records.add(record);
        }
        variantRecordRepository.deleteByRegRecordIn(records);
        coordinatesRepository.deleteByRegRecordIn(records);
    }

    /**
     * Sets last update dateTime and unique UUID for new entity.
     */
    private void prepareItemForCreate(AccessPointWrapper item) {
        RegRecord record = item.getEntity();
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
        List<RegRecordInfo> pairs = recordRepository.findByUuidIn(uuidMap.keySet());
        for (RegRecordInfo pair : pairs) {
            AccessPointWrapper rw = uuidMap.get(pair.getUuid());
            preparePairedRecord(rw, pair);
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
            if (rw.getEntity().getExternalId() != null && rw.getState().equals(EntityState.CREATE)) {
                aggregator.addRecord(rw);
            }
        }
        // find pairs by eid
        aggregator.forEach((code, group) -> {
            List<RegRecordInfoExternal> pairs = recordRepository.findByExternalSystemCodeAndExternalIdIn(code,
                    group.getExternalIds());
            for (RegRecordInfoExternal pair : pairs) {
                AccessPointWrapper rw = group.getRecord(pair.getExternalId());
                preparePairedRecord(rw, pair);
            }
        });
    }

    /**
     * Copies record uuid, version, recordId from pair and marks record as paired.
     *
     * @throws DEImportException When scope does not match with pair.
     */
    private void preparePairedRecord(AccessPointWrapper rw, RegRecordInfo pair) {
        RegRecord record = rw.getEntity();
        Integer scopeId = record.getScope().getScopeId();
        if (!scopeId.equals(pair.getScopeId())) {
            throw new DEImportException("Import scope doesn't match with scope of paired record, import scopeId:" + scopeId
                    + ", paired scopeId:" + pair.getScopeId());
        }
        record.setUuid(pair.getUuid());
        record.setVersion(pair.getVersion());
        record.setRecordId(pair.getRecordId());
        if (record.getLastUpdate() == null) {
            record.setLastUpdate(LocalDateTime.now(ZoneOffset.UTC));
            rw.markAsPaired(true);
        } else if (record.getLastUpdate().isAfter(pair.getLastUpdate())) {
            rw.markAsPaired(true);
        } else {
            rw.markAsPaired(false);
        }
    }

    private static class ExternalSystemAggregator {

        private final Map<String, ExternalSystemGroup> codeMap = new HashMap<>();

        public void addRecord(AccessPointWrapper record) {
            RegExternalSystem system = record.getEntity().getExternalSystem();
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
