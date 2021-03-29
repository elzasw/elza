package cz.tacr.elza.service.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApCachedAccessPointRepository;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.search.SearchIndexSupport;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME;

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
    private ApBindingRepository bindingRepository;

    @Autowired
    private ApBindingStateRepository bindingStateRepository;

    @Autowired
    private ApBindingItemRepository bindingItemRepository;

    /**
     * Maximální počet AP, které se mají dávkově zpracovávat pro synchronizaci.
     */
    @Value("${elza.ap.cache.batchsize:800}")
    private static final int SYNC_BATCH_AP_SIZE = 800;

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
    @Transactional(TxType.MANDATORY)
    public void syncCache() {
        writeLock.lock();
        try {
            logger.info("Spuštění synchronizace cache pro AP");
            syncCacheInternal();
            logger.info("Ukončení synchronizace cache pro AP");
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Synchronizace záznamů v databázi.
     */
    private void syncCacheInternal() {
        ScrollableResults uncachedAPs = accessPointRepository.findUncachedAccessPoints();

        List<Integer> apIds = new ArrayList<>(SYNC_BATCH_AP_SIZE);
        int count = 0;
        while (uncachedAPs.next()) {
            Object obj = uncachedAPs.get(0);

            apIds.add((Integer) obj);
            count++;
            if (count % SYNC_BATCH_AP_SIZE == 0) {
                logger.info("Sestavuji AP " + (count - SYNC_BATCH_AP_SIZE + 1) + "-" + count);

                processNewAPs(apIds);
                apIds.clear();

            }
        }
        // process remaining APs
        if (apIds.size() > 0) {
            logger.info("Sestavuji AP " + ((count / SYNC_BATCH_AP_SIZE) * SYNC_BATCH_AP_SIZE + 1) + "-" + count);
            processNewAPs(apIds);
        }

        logger.info("Všechny AP jsou synchronizovány");
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
        Map<Integer, List<ApState>> stateMap = stateRepository.findLastByAccessPointIds(accessPointIds).stream()
                .collect(Collectors.groupingBy(i -> i.getAccessPointId()));
        Map<Integer, List<CachedPart>> partMap = createCachedPartMap(accessPointIds, accessPointList);
        Map<Integer, List<CachedBinding>> bindingMap = createCachedBindingMap(accessPointList);

        List<ApCachedAccessPoint> apCachedAccessPoints = new ArrayList<>();

        for (ApAccessPoint accessPoint : accessPointList) {
            ApState state = stateMap.get(accessPoint.getAccessPointId()).get(0);
            List<CachedPart> parts = partMap.get(accessPoint.getAccessPointId());
            List<CachedBinding> bindings = bindingMap.get(accessPoint.getAccessPointId());

            CachedAccessPoint cachedAccessPoint = createCachedAccessPoint(accessPoint, state, parts, bindings);
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
            ApCachedAccessPoint apCachedAccessPoint = createCachedAccessPoint(accessPointId);
            cachedAccessPointRepository.save(apCachedAccessPoint);
        } finally {
            writeLock.unlock();
        }
    }

    private ApCachedAccessPoint createCachedAccessPoint(Integer accessPointId) {
        ApAccessPoint accessPoint = accessPointRepository.findById(accessPointId)
                .orElseThrow(() -> new ObjectNotFoundException("Přístupový bod neexistuje", BaseCode.ID_NOT_EXIST).setId(accessPointId));
        ApState state = stateRepository.findLastByAccessPoint(accessPoint);
        List<CachedPart> parts = createCachedParts(accessPoint);
        List<CachedBinding> bindings = createCachedBindings(accessPoint);

        CachedAccessPoint cachedAccessPoint = createCachedAccessPoint(accessPoint, state, parts, bindings);
        String data = serialize(cachedAccessPoint);

        ApCachedAccessPoint apCachedAccessPoint = new ApCachedAccessPoint();
        apCachedAccessPoint.setAccessPoint(accessPoint);
        apCachedAccessPoint.setData(data);
        return apCachedAccessPoint;
    }

    private CachedAccessPoint createCachedAccessPoint(ApAccessPoint accessPoint, ApState state, List<CachedPart> parts, List<CachedBinding> bindings) {
        CachedAccessPoint cachedAccessPoint = new CachedAccessPoint();
        cachedAccessPoint.setAccessPointId(accessPoint.getAccessPointId());
        cachedAccessPoint.setApState(state);
        cachedAccessPoint.setErrorDescription(accessPoint.getErrorDescription());
        cachedAccessPoint.setLastUpdate(accessPoint.getLastUpdate());
        cachedAccessPoint.setPreferredPartId(accessPoint.getPreferredPartId());
        cachedAccessPoint.setState(accessPoint.getState());
        cachedAccessPoint.setUuid(accessPoint.getUuid());
        cachedAccessPoint.setParts(parts);
        cachedAccessPoint.setBindings(bindings);
        return cachedAccessPoint;
    }

    private List<CachedPart> createCachedParts(ApAccessPoint accessPoint) {
        List<ApPart> parts = partRepository.findValidPartByAccessPoint(accessPoint);
        Map<Integer, List<ApItem>> itemMap = itemRepository.findValidItemsByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(i -> i.getPartId()));
        Map<Integer, List<ApIndex>> indexMap = indexRepository.findIndicesByAccessPoint(accessPoint.getAccessPointId()).stream()
                .collect(Collectors.groupingBy(i -> i.getPart().getPartId()));

        List<CachedPart> cachedParts = new ArrayList<>();

        for (ApPart part : parts) {
            List<ApItem> items = itemMap.get(part.getPartId());
            List<ApIndex> indices = indexMap.get(part.getPartId());
            cachedParts.add(createCachedPart(part, items, indices));
        }

        return cachedParts;
    }

    private CachedPart createCachedPart(ApPart part, List<ApItem> items, List<ApIndex> indices) {
        CachedPart cachedPart = new CachedPart();
        cachedPart.setPartId(part.getPartId());
        cachedPart.setCreateChangeId(part.getCreateChangeId());
        cachedPart.setDeleteChangeId(part.getDeleteChangeId());
        cachedPart.setErrorDescription(part.getErrorDescription());
        cachedPart.setState(part.getState());
        cachedPart.setPartTypeCode(part.getPartType().getCode());
        cachedPart.setKeyValue(part.getKeyValue());
        cachedPart.setParentPartId(part.getParentPart() != null ? part.getParentPart().getPartId() : null);
        cachedPart.setItems(items);
        cachedPart.setIndices(indices);

        return cachedPart;
    }

    private Map<Integer, List<CachedPart>> createCachedPartMap(List<Integer> accessPointIds, List<ApAccessPoint> accessPointList) {
        Map<Integer, List<ApPart>> partMap = partRepository.findValidPartByAccessPoints(accessPointList).stream()
                .collect(Collectors.groupingBy(i -> i.getAccessPointId()));
        Map<Integer, Map<Integer, List<ApItem>>> apItemMap = new HashMap<>();
        Map<Integer, Map<Integer, List<ApIndex>>> apIndexMap = new HashMap<>();
        List<ApItem> apItems = itemRepository.findValidItemsByAccessPoints(accessPointList);
        if (CollectionUtils.isNotEmpty(apItems)) {
            for (ApItem item : apItems) {
                apItemMap.computeIfAbsent(item.getPart().getAccessPointId(), k -> new HashMap<>()).computeIfAbsent(item.getPartId(), l -> new ArrayList<>()).add(item);
            }
        }

        List<ApIndex> apIndices = indexRepository.findIndicesByAccessPoints(accessPointIds);
        if (CollectionUtils.isNotEmpty(apIndices)) {
            for (ApIndex index : apIndices) {
                apIndexMap.computeIfAbsent(index.getPart().getAccessPointId(), k -> new HashMap<>()).computeIfAbsent(index.getPart().getPartId(), l -> new ArrayList<>()).add(index);
            }
        }

        Map<Integer, List<CachedPart>> cachedPartMap = new HashMap<>();

        for (ApAccessPoint accessPoint : accessPointList) {
            List<ApPart> parts = partMap.get(accessPoint.getAccessPointId());
            Map<Integer, List<ApItem>> itemMap = apItemMap.get(accessPoint.getAccessPointId());
            Map<Integer, List<ApIndex>> indexMap = apIndexMap.get(accessPoint.getAccessPointId());

            if (CollectionUtils.isNotEmpty(parts)) {
                List<CachedPart> cachedParts = new ArrayList<>();
                for (ApPart part : parts) {
                    List<ApItem> items = null;
                    if (itemMap != null) {
                        items = itemMap.get(part.getPartId());
                    }
                    List<ApIndex> indices = null;
                    if (indexMap != null) {
                        indices = indexMap.get(part.getPartId());
                    }
                    cachedParts.add(createCachedPart(part, items, indices));
                }
                cachedPartMap.put(accessPoint.getAccessPointId(), cachedParts);
            }
        }

        return cachedPartMap;
    }

    private List<CachedBinding> createCachedBindings(ApAccessPoint accessPoint) {
        Map<Integer, List<ApBindingState>> bindingStateMap = bindingStateRepository.findByAccessPoint(accessPoint).stream()
                .collect(Collectors.groupingBy(i -> i.getBinding().getBindingId()));
        List<ApBinding> bindingList = new ArrayList<>();
        Map<Integer, List<ApBindingItem>> bindingItemMap;

        if (MapUtils.isNotEmpty(bindingStateMap)) {
            bindingList = bindingRepository.findAllById(bindingStateMap.keySet());
        }

        List<CachedBinding> cachedBindingList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(bindingList)) {
            bindingItemMap = bindingItemRepository.findByBindings(bindingList).stream()
                    .collect(Collectors.groupingBy(i -> i.getBinding().getBindingId()));

            for (ApBinding binding : bindingList) {
                ApBindingState bindingState = bindingStateMap.get(binding.getBindingId()).get(0);
                List<ApBindingItem> bindingItemList = bindingItemMap.get(binding.getBindingId());
                cachedBindingList.add(createCachedBinding(binding, bindingState, bindingItemList));
            }
        }

        return cachedBindingList;
    }

    private Map<Integer, List<CachedBinding>> createCachedBindingMap(List<ApAccessPoint> accessPointList) {
        Map<Integer, Map<ApBinding, List<ApBindingState>>> apBindingStateMap = new HashMap<>();
        List<ApBindingState> bindingStates = bindingStateRepository.findByAccessPoints(accessPointList);
        if (CollectionUtils.isNotEmpty(bindingStates)) {
            for (ApBindingState bindingState : bindingStates) {
                apBindingStateMap.computeIfAbsent(bindingState.getAccessPointId(), k -> new HashMap<>()).computeIfAbsent(bindingState.getBinding(), l -> new ArrayList<>()).add(bindingState);
            }
        }

        Map<Integer, Collection<ApBinding>> bindingMap = createBindingMap(apBindingStateMap);
        List<ApBinding> bindingList = createBindingList(bindingMap);
        Map<ApBinding, List<ApBindingItem>> bindingItemMap;

        Map<Integer, List<CachedBinding>> cachedBindingMap = new HashMap<>();

        if (CollectionUtils.isNotEmpty(bindingList)) {
            bindingItemMap = bindingItemRepository.findByBindings(bindingList).stream()
                    .collect(Collectors.groupingBy(ApBindingItem::getBinding));

            for (Map.Entry<Integer, Collection<ApBinding>> entry : bindingMap.entrySet()) {
                Integer accessPointId = entry.getKey();
                List<CachedBinding> cachedBindingList = new ArrayList<>();
                Map<ApBinding, List<ApBindingState>> bindingStateMap = apBindingStateMap.get(accessPointId);

                if (CollectionUtils.isNotEmpty(entry.getValue())) {
                    for (ApBinding binding : entry.getValue()) {
                        ApBindingState bindingState = bindingStateMap.get(binding).get(0);
                        List<ApBindingItem> bindingItemList = bindingItemMap.get(binding);
                        cachedBindingList.add(createCachedBinding(binding, bindingState, bindingItemList));
                    }
                }

                cachedBindingMap.put(accessPointId, cachedBindingList);
            }
        }

        return cachedBindingMap;
    }

    private List<ApBinding> createBindingList(Map<Integer, Collection<ApBinding>> bindingMap) {
        List<ApBinding> bindingList = new ArrayList<>();

        if (MapUtils.isNotEmpty(bindingMap)) {
            for (Map.Entry<Integer, Collection<ApBinding>> entry : bindingMap.entrySet()) {
                bindingList.addAll(entry.getValue());
            }
        }

        return bindingList;
    }

    private Map<Integer, Collection<ApBinding>> createBindingMap(Map<Integer, Map<ApBinding, List<ApBindingState>>> bindingStateMap) {
        Map<Integer, Collection<ApBinding>> bindingMap = new HashMap<>();

        if (MapUtils.isNotEmpty(bindingStateMap)) {
            for (Map.Entry<Integer, Map<ApBinding, List<ApBindingState>>> entry : bindingStateMap.entrySet()) {
                Integer accessPointId = entry.getKey();
                Map<ApBinding, List<ApBindingState>> map = entry.getValue();

                bindingMap.put(accessPointId, map.keySet());
            }
        }

        return bindingMap;
    }

    private CachedBinding createCachedBinding(ApBinding binding, ApBindingState bindingState, List<ApBindingItem> bindingItemList) {
        CachedBinding cachedBinding = new CachedBinding();
        cachedBinding.setId(binding.getBindingId());
        cachedBinding.setExternalSystemCode(binding.getApExternalSystem().getCode());
        cachedBinding.setValue(binding.getValue());
        cachedBinding.setBindingState(bindingState);
        cachedBinding.setBindingItemList(bindingItemList);
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

    public CachedAccessPoint deserialize(String data) {
        try {
            return mapper.readValue(data, CachedAccessPoint.class);
        } catch (IOException e) {
            logger.error("Failed to deserialize object, data: " +
                    data);
            throw new SystemException("Nastal problém při deserializaci objektu", e);
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

    @Override
    public Map<Integer, ApCachedAccessPoint> findToIndex(Collection<Integer> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        // moznost optimalizovat nacteni vcene zavislosti
        return cachedAccessPointRepository.findAllById(ids).stream().collect(Collectors.toMap(o -> o.getAccessPointId(), o -> o));
    }
}
