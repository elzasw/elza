package cz.tacr.elza.service.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.common.db.QueryResults;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApCachedAccessPointRepository;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.search.SearchIndexSupport;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

@Service
public class AccessPointCacheService implements SearchIndexSupport<ApCachedAccessPoint> {

    private static final Logger logger = LoggerFactory.getLogger(NodeCacheService.class);

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwl.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwl.writeLock();

    private final ObjectMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    @Autowired
    private ApStateRepository stateRepository;

    @Autowired
    private ApPartRepository partRepository;

    @Autowired
    private ApItemRepository itemRepository;

    @Autowired
    private ApIndexRepository indexRepository;

    @Autowired
    private ApCachedAccessPointRepository cachedAccessPointRepository;

    @Autowired
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private ApBindingItemRepository bindingItemRepository;

    /**
     * Maximální počet AP, které se mají dávkově zpracovávat pro synchronizaci.
     */
    @Value("${elza.ap.cache.batchsize:800}")
    private int syncApBatchSize = 800;

    @Value("${elza.ap.cache.transsize:800}")
    private int syncApTransSize = 800;

    @Autowired
    @Qualifier("transactionManager")
    protected PlatformTransactionManager txManager;

    public AccessPointCacheService() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setVisibility(new ApVisibilityChecker(AccessPointCacheSerializable.class,
                String.class, Number.class, Boolean.class, Iterable.class,
                LocalDate.class, LocalDateTime.class));
    }

    /**
     * Synchronizace záznamů v databázi.
     *
     * Synchronní metoda volaná z transakce.
     */
    public void syncCache() {
        writeLock.lock();
        try {
            logger.info("Spuštění synchronizace cache pro AP");
            int off = 0;
            Integer numProcessed;
            do {
                TransactionTemplate tt = new TransactionTemplate(txManager);
                final int off2 = off;
                numProcessed = tt.execute(t -> syncCacheInternal(off2));
                off += numProcessed;
            } while (numProcessed > 0);

            logger.info("Všechny AP jsou synchronizovány");
            logger.info("Ukončení synchronizace cache pro AP");
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Synchronizace záznamů v databázi.
     * 
     * @param offset
     * @return Number of processed items
     */
    private int syncCacheInternal(int offset) {
        ScrollableResults uncachedAPs = accessPointRepository.findUncachedAccessPoints();

        List<Integer> apIds = new ArrayList<>(syncApBatchSize);
        int count = 0;
        while (uncachedAPs.next()) {
            Object obj = uncachedAPs.get(0);

            apIds.add((Integer) obj);
            count++;
            if (count % syncApBatchSize == 0) {
                logger.info("Sestavuji AP " + (count - syncApBatchSize + 1 + offset) + "-" + (count + offset));

                processNewAPs(apIds);
                apIds.clear();
                // check transaction size
                if (count >= syncApTransSize) {
                    break;
                }
            }
        }
        // process remaining APs
        if (apIds.size() > 0) {
            logger.info("Sestavuji AP " + ((count / syncApBatchSize) * syncApBatchSize + 1 + offset) + "-" + (count
                    + offset));
            processNewAPs(apIds);
        }
        return count;
    }

    private void processNewAPs(List<Integer> accessPointIds) {
        List<ApCachedAccessPoint> cachedAccessPoints = createCachedAccessPoints(accessPointIds);
        cachedAccessPointRepository.saveAll(cachedAccessPoints);
        //flush a batch of updates and release memory:
        entityManager.flush();
        entityManager.clear();
    }

    private List<ApCachedAccessPoint> createCachedAccessPoints(List<Integer> accessPointIds) {
        List<ApAccessPoint> accessPointList = accessPointRepository.findAllById(accessPointIds);
        Validate.isTrue(accessPointIds.size() == accessPointList.size(), "Found unexpected number of accesspoints");

        // Add all aps
        Map<Integer, CachedAccessPoint> apMap = accessPointList.stream()
                .collect(Collectors.toMap(ApAccessPoint::getAccessPointId, ap -> createCachedAccessPoint(ap)));

        // set ap state
        List<ApState> apStates = stateRepository.findLastByAccessPointIds(accessPointIds);
        Validate.isTrue(apStates.size() == accessPointIds.size(), "Found unexpected number of ap states");
        for (ApState apState : apStates) {
            apState = HibernateUtils.unproxy(apState);
            CachedAccessPoint cap = apMap.get(apState.getAccessPointId());
            cap.setApState(apState);
        }

        createCachedPartMap(accessPointList, apMap);
        createCachedBindingMap(accessPointList, apMap);

        List<ApCachedAccessPoint> apCachedAccessPoints = new ArrayList<>();

        for (ApAccessPoint accessPoint : accessPointList) {

            CachedAccessPoint cachedAccessPoint = apMap.get(accessPoint.getAccessPointId());
            String data = serialize(cachedAccessPoint);

            ApCachedAccessPoint apCachedAccessPoint = new ApCachedAccessPoint();
            apCachedAccessPoint.setAccessPoint(accessPoint);
            apCachedAccessPoint.setData(data);
            apCachedAccessPoints.add(apCachedAccessPoint);
        }
        return apCachedAccessPoints;
    }

    @Transactional
    public void createApCachedAccessPoint(Integer accessPointId) {
        writeLock.lock();
        try {
            ApCachedAccessPoint oldApCachedAccessPoint = cachedAccessPointRepository.findByAccessPointId(accessPointId);
            if (oldApCachedAccessPoint != null) {
                cachedAccessPointRepository.delete(oldApCachedAccessPoint);
            }
            processNewAPs(Collections.singletonList(accessPointId));
        } finally {
            writeLock.unlock();
        }
    }

    private CachedAccessPoint createCachedAccessPoint(ApAccessPoint accessPoint) {
        CachedAccessPoint cachedAccessPoint = new CachedAccessPoint();
        cachedAccessPoint.setAccessPointId(accessPoint.getAccessPointId());
        cachedAccessPoint.setErrorDescription(accessPoint.getErrorDescription());
        cachedAccessPoint.setLastUpdate(accessPoint.getLastUpdate());
        cachedAccessPoint.setPreferredPartId(accessPoint.getPreferredPartId());
        cachedAccessPoint.setState(accessPoint.getState());
        cachedAccessPoint.setUuid(accessPoint.getUuid());
        return cachedAccessPoint;
    }

    private CachedPart createCachedPart(ApPart part) {
        CachedPart cachedPart = new CachedPart();
        cachedPart.setPartId(part.getPartId());
        cachedPart.setCreateChangeId(part.getCreateChangeId());
        cachedPart.setDeleteChangeId(part.getDeleteChangeId());
        cachedPart.setErrorDescription(part.getErrorDescription());
        cachedPart.setState(part.getState());
        cachedPart.setPartTypeCode(part.getPartType().getCode());
        cachedPart.setKeyValue(HibernateUtils.unproxy(part.getKeyValue()));
        cachedPart.setParentPartId(part.getParentPartId());

        return cachedPart;
    }

    private void createCachedPartMap(List<ApAccessPoint> accessPointList,
                                     Map<Integer, CachedAccessPoint> apMap) {

        Map<Integer, CachedPart> partMap = fillCachedPartMap(accessPointList, apMap);

        addItemsToCachedPartMap(accessPointList, partMap);

        addIndexesToCachedPartMap(accessPointList, partMap);
    }

    private Map<Integer, CachedPart> fillCachedPartMap(List<ApAccessPoint> accessPointList, Map<Integer, CachedAccessPoint> apMap) {
        Map<Integer, CachedPart> partMap = new HashMap<>();

        List<ApPart> parts = partRepository.findValidPartByAccessPoints(accessPointList);
        for(ApPart part: parts) {
            part = HibernateUtils.unproxy(part);
            CachedPart cachedPart = createCachedPart(part);
            partMap.put(cachedPart.getPartId(), cachedPart);

            CachedAccessPoint cap = apMap.get(part.getAccessPointId());
            Validate.notNull(cap, "AP not in the result");
            cap.addPart(cachedPart);
        }
        return partMap;
    }

    private void addItemsToCachedPartMap(List<ApAccessPoint> accessPointList, Map<Integer, CachedPart> partMap) {
        List<ApItem> apItems = itemRepository.findValidItemsByAccessPoints(accessPointList);
        if (CollectionUtils.isNotEmpty(apItems)) {
            for (ApItem item : apItems) {
                item = HibernateUtils.unproxy(item);
                CachedPart part = partMap.get(item.getPartId());
                if (part == null) {
                    Validate.notNull(part, "Missing part, partId: %s", item.getPartId());
                }
                part.addItem(item);
            }
        }
    }

    private void addIndexesToCachedPartMap(List<ApAccessPoint> accessPointList, Map<Integer, CachedPart> partMap) {
        List<ApIndex> apIndices = indexRepository.findIndicesByAccessPoints(accessPointList);
        if (CollectionUtils.isNotEmpty(apIndices)) {
            for (ApIndex index : apIndices) {
                index = HibernateUtils.unproxy(index);
                CachedPart part = partMap.get(index.getPartId());
                if (part == null) {
                    Validate.notNull(part, "Missing part, partId: %s", index.getPartId());
                }
                part.addIndex(index);
            }
        }        
    }

    private void createCachedBindingMap(List<ApAccessPoint> accessPointList,
                                        Map<Integer, CachedAccessPoint> apMap) {

        List<ApBindingState> bindingStates = bindingStateRepository.findByAccessPoints(accessPointList);
        if (CollectionUtils.isEmpty(bindingStates)) {
            return;
        }
        List<ApBinding> bindings = new ArrayList<>();
        Map<Integer, CachedBinding> bindingMap = new HashMap<>();
        for (ApBindingState bindingState : bindingStates) {
            bindingState = HibernateUtils.unproxy(bindingState);
            ApBinding binding = HibernateUtils.unproxy(bindingState.getBinding());
            bindings.add(binding);

            CachedAccessPoint cap = apMap.get(bindingState.getAccessPointId());
            Validate.notNull(cap, "AP not found, accessPointId: %s", bindingState.getAccessPointId());

            CachedBinding cb = createCachedBinding(binding, bindingState);
            bindingMap.put(binding.getBindingId(), cb);

            cap.addBinding(cb);
        }

        List<ApBindingItem> bindingItems = bindingItemRepository.findByBindings(bindings);
        for (ApBindingItem bindingItem : bindingItems) {
            bindingItem = HibernateUtils.unproxy(bindingItem);
            Integer bindingId = bindingItem.getBindingId();
            CachedBinding b = bindingMap.get(bindingId);

            Validate.notNull(b, "Cached binding not found, bindingId: %s", bindingId);
            b.addBindingItem(bindingItem);
        }

    }

    private CachedBinding createCachedBinding(ApBinding binding, ApBindingState bindingState) {
        CachedBinding cachedBinding = new CachedBinding();
        cachedBinding.setId(binding.getBindingId());
        cachedBinding.setExternalSystemCode(binding.getApExternalSystem().getCode());
        cachedBinding.setValue(binding.getValue());
        cachedBinding.setBindingState(bindingState);
        return cachedBinding;

    }

    @Transactional
    public CachedAccessPoint findCachedAccessPoint(Integer accessPointId) {
        readLock.lock();
        try {
            ApCachedAccessPoint apCachedAccessPoint = cachedAccessPointRepository.findByAccessPointId(accessPointId);
            CachedAccessPoint cachedAccessPoint = null;
            if (apCachedAccessPoint != null) {
                cachedAccessPoint = deserialize(apCachedAccessPoint.getData());
            }
            return cachedAccessPoint;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Deserializace entity
     * 
     * Must be called inside transaction
     * 
     * @param data
     * @return
     */
    @Transactional(value = TxType.MANDATORY)
    public CachedAccessPoint deserialize(String data) {
        try {
            CachedAccessPoint cap = mapper.readValue(data, CachedAccessPoint.class);
            restoreLinks(cap);
            return cap;
        } catch (IOException e) {
            logger.error("Failed to deserialize object, data: " +
                    data);
            throw new SystemException("Nastal problém při deserializaci objektu", e);
        }
    }

    private void restoreLinks(CachedAccessPoint cap) {
        List<CachedBinding> cachedBindings = cap.getBindings();
        if(CollectionUtils.isEmpty(cachedBindings)) {
            // nothing to link
            return;
        }
        ApAccessPoint ap = entityManager.getReference(ApAccessPoint.class, cap.getAccessPointId());
        for (CachedBinding cachedBinding : cachedBindings) {
            ApBinding b = entityManager.getReference(ApBinding.class, cachedBinding.getId());
            ApBindingState bs = cachedBinding.getBindingState();
            bs.setAccessPoint(ap);
            bs.setBinding(b);
            bs.setCreateChange(entityManager.getReference(ApChange.class, bs.getCreateChangeId()));
            if (bs.getDeleteChangeId() != null) {
                bs.setDeleteChange(entityManager.getReference(ApChange.class, bs.getDeleteChangeId()));
            }
            if (bs.getSyncChangeId() != null) {
                bs.setSyncChange(entityManager.getReference(ApChange.class, bs.getSyncChangeId()));
            }

            // set items
            List<ApBindingItem> bil = cachedBinding.getBindingItemList();
            if (bil != null) {
                restoreLinks(b, bil);
            }
        }
        /*
        ApAccessPoint ap = new ApAccessPoint();
        ap.setAccessPointId(cap.getAccessPointId());
        ap.setErrorDescription(cap.getErrorDescription());
        ap.setLastUpdate(cap.getLastUpdate());
        //ap.setPreferredPart(null);
        ap.setState(cap.getState());
        ap.setUuid(cap.getUuid());
        //ap.setVersion(cap.get);
        List<CachedPart> cachedParts = cap.getParts();
        for(CachedPart cachedPart: cachedParts) {
            ApPart part = new ApPart();
            part.setAccessPoint(ap);
            ApChange createChange = entityManager.getReference(ApChange.class, part.getCreateChangeId());
            part.setCreateChange(createChange);
            if(part.getDeleteChangeId()!=null) {
                ApChange deleteChange = entityManager.getReference(ApChange.class, part.getDeleteChangeId());
                part.setDeleteChange(deleteChange);
                
            }
            part.setErrorDescription(cachedPart.getErrorDescription());
            part.setKeyValue(cachedPart.getKeyValue());
            //part.setParentPart(part);
            //part.setPartType(cachedPart.getPartTypeCode());
            part.setState(cachedPart.getState());
            
        }*/
    }

    private void restoreLinks(ApBinding b, List<ApBindingItem> bil) {
        for (ApBindingItem bi : bil) {
            bi.setBinding(b);
            bi.setCreateChange(entityManager.getReference(ApChange.class, bi.getCreateChangeId()));
            if (bi.getDeleteChangeId() != null) {
                bi.setDeleteChange(entityManager.getReference(ApChange.class, bi.getDeleteChangeId()));
            }
            if (bi.getItemId() != null) {
                bi.setItem(entityManager.getReference(ApItem.class, bi.getItemId()));
            }
            if (bi.getPartId() != null) {
                bi.setPart(entityManager.getReference(ApPart.class, bi.getPartId()));
            }
        }

    }

    private String serialize(CachedAccessPoint cachedAccessPoint) {
        try {
            return mapper.writeValueAsString(cachedAccessPoint);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize object, accessPointId: " +
                    cachedAccessPoint.getAccessPointId(), e);
            throw new SystemException("Nastal problém při serializaci objektu, accessPointId: " +
                    cachedAccessPoint.getAccessPointId(), e)
                            .set("accessPointId", cachedAccessPoint.getAccessPointId());
        }
    }

    public void deleteCachedAccessPoint(ApAccessPoint accessPoint) {
        ApCachedAccessPoint oldApCachedAccessPoint = cachedAccessPointRepository.findByAccessPointId(accessPoint.getAccessPointId());
        if (oldApCachedAccessPoint != null) {
            cachedAccessPointRepository.delete(oldApCachedAccessPoint);
        }
    }

    @Transactional(value = TxType.MANDATORY)
    public QueryResults<CachedAccessPoint> search(SearchFilterVO searchFilter,
                                                  Collection<Integer> apTypeIds,
                                                  Collection<Integer> scopeIds,
                                                  ApState.StateApproval state,
                                                  Integer from,
                                                  Integer count, StaticDataProvider sdp) {
        String searchText = (searchFilter != null) ? searchFilter.getSearch() : null;

        QueryResults<ApCachedAccessPoint> r = cachedAccessPointRepository
                .findApCachedAccessPointisByQuery(searchText,
                                                  searchFilter,
                                                  apTypeIds,
                                                  scopeIds,
                                                  state,
                                                  from, count,
                                                  sdp);
        if (CollectionUtils.isEmpty(r.getRecords())) {
            return QueryResults.emptyResult(r.getRecordCount());
        }

        List<CachedAccessPoint> capList = new ArrayList<>(r.getRecords().size());
        for (ApCachedAccessPoint capd : r.getRecords()) {
            CachedAccessPoint cap = deserialize(capd.getData());
            capList.add(cap);
        }
        QueryResults<CachedAccessPoint> result = new QueryResults<>(r.getRecordCount(), capList);
        return result;
    }

    @Override
    public Map<Integer, ApCachedAccessPoint> findToIndex(Collection<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        // moznost optimalizovat nacteni vcene zavislosti
        return cachedAccessPointRepository.findAllById(ids).stream().collect(Collectors.toMap(o -> o.getAccessPointId(), o -> o));
    }
}
