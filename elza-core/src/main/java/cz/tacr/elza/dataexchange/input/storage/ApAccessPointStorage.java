package cz.tacr.elza.dataexchange.input.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.Validate;
import org.hibernate.Session;

import cz.tacr.elza.dataexchange.input.ApChangeHolder;
import cz.tacr.elza.dataexchange.input.DEImportException;
import cz.tacr.elza.dataexchange.input.aps.context.AccessPointWrapper;
import cz.tacr.elza.dataexchange.input.context.ImportInitHelper;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.projection.ApAccessPointInfo;
import cz.tacr.elza.domain.projection.ApExternalIdInfo;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApDescriptionRepository;
import cz.tacr.elza.repository.ApExternalIdRepository;
import cz.tacr.elza.repository.ApNameRepository;

public class ApAccessPointStorage extends EntityStorage<AccessPointWrapper> {

    private final ApAccessPointRepository apRepository;

    private final ApNameRepository apNameRepository;

    private final ApDescriptionRepository apDescRepository;

    private final ApExternalIdRepository apEidRepository;

    private final ApChangeHolder changeHolder;

    public ApAccessPointStorage(Session session, StoredEntityCallback persistEntityListener,
            ApChangeHolder changeHolder,
            ImportInitHelper initHelper) {
        super(session, persistEntityListener);
        this.apRepository = initHelper.getApRepository();
        this.apNameRepository = initHelper.getApNameRepository();
        this.apDescRepository = initHelper.getApDescRepository();
        this.apEidRepository = initHelper.getApEidRepository();
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
            apIds.add(Validate.notNull(apId));
        }
        ApChange change = changeHolder.getChange();
        apNameRepository.deleteByAccessPointIdIn(apIds, change);
        apDescRepository.deleteByAccessPointIdIn(apIds, change);
        apEidRepository.deleteByAccessPointIdIn(apIds, change);
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
        List<ApAccessPointInfo> currentAps = apRepository.findInfoByUuidIn(uuidMap.keySet());
        for (ApAccessPointInfo info : currentAps) {
            AccessPointWrapper ew = uuidMap.get(info.getUuid());
            ew.changeToUpdated(info);
        }
    }

    private void pairAccessPointsByEid(Collection<AccessPointWrapper> apws) {
        Map<String, EidTypeGroup> typeGroupedMap = new HashMap<>();
        // create external id type groups
        for (AccessPointWrapper apw : apws) {
            if (!apw.getSaveMethod().equals(SaveMethod.CREATE)) {
                continue; // ignore paired by UUID
            }
            MultiValuedMap<String, String> typeValueMap = apw.getEidTypeValueMap();
            if (typeValueMap == null) {
                continue; // no external ids
            }
            MapIterator<String, String> typeValueIt = typeValueMap.mapIterator();
            while (typeValueIt.hasNext()) {
                String type = typeValueIt.getKey();
                EidTypeGroup typeGroup = typeGroupedMap.get(type);
                if (typeGroup == null) {
                    typeGroupedMap.put(type, typeGroup = new EidTypeGroup(type));
                }
                typeGroup.addEid(typeValueIt.getValue(), apw);
            }
        }
        // find pairs by external ids
        typeGroupedMap.forEach((type, group) -> {
            List<ApExternalIdInfo> currentEids = apEidRepository.findInfoByExternalIdTypeCodeAndValuesIn(type,
                                                                                                         group.getValues());
            for (ApExternalIdInfo info : currentEids) {
                AccessPointWrapper apw = group.getWrapper(info.getValue());
                apw.changeToUpdated(info.getAccessPoint());
            }
        });
    }

    private static class EidTypeGroup {

        private final Map<String, AccessPointWrapper> valueWrapperMap = new HashMap<>();

        private final List<String> values = new ArrayList<>();

        private final String type;

        public EidTypeGroup(String type) {
            this.type = type;
        }

        public Collection<String> getValues() {
            return values;
        }

        public AccessPointWrapper getWrapper(String value) {
            return valueWrapperMap.get(value);
        }

        public void addEid(String value, AccessPointWrapper wrapper) {
            if (valueWrapperMap.put(value, wrapper) != null) {
                throw new DEImportException("Duplicate AP external id, type=" + type + ", value=" + value);
            }
            values.add(value);
        }
    }
}
