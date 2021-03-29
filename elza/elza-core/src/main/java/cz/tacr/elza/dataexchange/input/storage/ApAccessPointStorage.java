package cz.tacr.elza.dataexchange.input.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.repository.*;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.ApChangeHolder;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointWrapper;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.projection.ApAccessPointInfo;
import cz.tacr.elza.domain.projection.ApExternalIdInfo;

public class ApAccessPointStorage extends EntityStorage<AccessPointWrapper> {

    private final ApAccessPointRepository apRepository;

    private final ApBindingRepository bindingRepository;

    private final ApBindingStateRepository bindingStateRepository;

    private final ApChangeHolder changeHolder;

    private final ApStateRepository apStateRepository;

    public ApAccessPointStorage(Session session, StoredEntityCallback persistEntityListener,
            ApChangeHolder changeHolder,
            ImportInitHelper initHelper) {
        super(session, persistEntityListener);
        this.apRepository = initHelper.getApRepository();
        this.bindingRepository = initHelper.getBindingRepository();
        this.bindingStateRepository = initHelper.getBindingStateRepository();
        this.apStateRepository = initHelper.getApStateRepository();
        this.changeHolder = changeHolder;
    }

    @Override
    public void store(Collection<AccessPointWrapper> apws) {
        pairAccessPointsByUuid(apws);
        pairAccessPointsByEid(apws);
        // store all wrappers as persist or merge
        super.store(apws);
    }

    @Override
    protected void mergeEntities(Collection<AccessPointWrapper> apws) {
        invalidateSubEntities(apws);
        // actual AP update is not needed
    }

    /**
     * Invalidates current sub-entities for each access point.
     */
    private void invalidateSubEntities(Collection<AccessPointWrapper> apws) {
        List<Integer> apIds = new ArrayList<>(apws.size());
        for (AccessPointWrapper apw : apws) {
            Integer apId = apw.getEntity().getAccessPointId();
            if (apId == null) {
                Validate.notNull(apId);
            }
            apIds.add(apId);
        }
        ApChange change = changeHolder.getChange();
        if (apIds.size() > 0) {
            // this is probably incorrect
            // why we need to invalidate binding?
            bindingStateRepository.invalidateByAccessPointIdIn(apIds, change);
        }
    }

    private void pairAccessPointsByUuid(Collection<AccessPointWrapper> apws) {
        // init UUID -> AP map
        Map<String, AccessPointWrapper> uuidMap = new HashMap<>();
        for (AccessPointWrapper apw : apws) {
            String uuid = apw.getEntity().getUuid();
            if (uuid == null) {
                continue; // UUID not imported
            }
            if (uuidMap.put(uuid, apw) != null) {
                throw new DEImportException("Duplicate AP uuid, value=" + uuid);
            }
        }
        // find current AP by UUID
        Set<String> uuids = uuidMap.keySet();
        if (!uuids.isEmpty()) {
            List<ApAccessPointInfo> currentAps = apRepository.findActiveInfoByUuids(uuids);
            for (ApAccessPointInfo info : currentAps) {
                AccessPointWrapper ew = uuidMap.get(info.getUuid());
                ew.changeToUpdated(info);
            }
        }
    }

    private void pairAccessPointsByEid(Collection<AccessPointWrapper> apws) {
        Map<Integer, EidLookup> typeIdMap = new HashMap<>();
        // create external id type groups
        for (AccessPointWrapper apw : apws) {
            if (!apw.getSaveMethod().equals(SaveMethod.CREATE)) {
                continue; // ignore paired by UUID
            }
            Collection<ApBindingState> eids = apw.getExternalIds();
            if (eids == null) {
                continue; // no external ids
            }
            for (ApBindingState eid : eids) {
                EidLookup lookup = typeIdMap.get(eid.getBinding().getApExternalSystem().getExternalSystemId());
                if (lookup == null) {
                    lookup = new EidLookup(eid.getBinding().getApExternalSystem().getCode());
                    typeIdMap.put(eid.getBinding().getApExternalSystem().getExternalSystemId(), lookup);
                }
                lookup.addWrapper(eid.getBinding().getValue(), apw);
            }
        }
        // find pairs by external ids
        typeIdMap.forEach((typeId, group) -> {
            List<ApExternalIdInfo> currentEids = bindingStateRepository
                    .findActiveInfoByTypeIdAndValues(typeId, group.getValues());
            for (ApExternalIdInfo info : currentEids) {
                AccessPointWrapper apw = group.getWrapper(info.getValue());
                apw.changeToUpdated(info.getAccessPoint());
            }
        });
    }

    private static class EidLookup {

        private final Map<String, AccessPointWrapper> valueMap = new HashMap<>();

        private final List<String> values = new ArrayList<>();

        private final String typeCode;

        public EidLookup(String typeCode) {
            this.typeCode = typeCode;
        }

        public Collection<String> getValues() {
            return values;
        }

        public AccessPointWrapper getWrapper(String value) {
            return valueMap.get(value);
        }

        public void addWrapper(String value, AccessPointWrapper wrapper) {
            if (valueMap.put(value, wrapper) != null) {
                throw new DEImportException("Duplicate AP external id, typeCode=" + typeCode + ", value=" + value);
            }
            values.add(value);
        }
    }
}
