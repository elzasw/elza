package cz.tacr.elza.service.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApCachedAccessPointRepository;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.search.SearchIndexSupport;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    /**
     * Maximální počet AP, které se mají dávkově zpracovávat pro synchronizaci.
     */
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

        List<ApCachedAccessPoint> apCachedAccessPoints = new ArrayList<>();

        for (ApAccessPoint accessPoint : accessPointList) {
            ApState state = stateMap.get(accessPoint.getAccessPointId()).get(0);
            List<CachedPart> parts = partMap.get(accessPoint.getAccessPointId());

            CachedAccessPoint cachedAccessPoint = createCachedAccessPoint(accessPoint, state, parts);
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
        ApCachedAccessPoint oldApCachedAccessPoint = cachedAccessPointRepository.findByAccessPointId(accessPointId);
        if (oldApCachedAccessPoint != null) {
            cachedAccessPointRepository.delete(oldApCachedAccessPoint);
        }
        ApCachedAccessPoint apCachedAccessPoint = createCachedAccessPoint(accessPointId);
        cachedAccessPointRepository.save(apCachedAccessPoint);
    }

    private ApCachedAccessPoint createCachedAccessPoint(Integer accessPointId) {
        ApAccessPoint accessPoint = accessPointRepository.findById(accessPointId)
                .orElseThrow(() -> new ObjectNotFoundException("Přístupový bod neexistuje", BaseCode.ID_NOT_EXIST).setId(accessPointId));
        ApState state = stateRepository.findLastByAccessPoint(accessPoint);
        List<CachedPart> parts = createCachedParts(accessPoint);


        CachedAccessPoint cachedAccessPoint = createCachedAccessPoint(accessPoint, state, parts);
        String data = serialize(cachedAccessPoint);

        ApCachedAccessPoint apCachedAccessPoint = new ApCachedAccessPoint();
        apCachedAccessPoint.setAccessPoint(accessPoint);
        apCachedAccessPoint.setData(data);
        return apCachedAccessPoint;
    }

    private CachedAccessPoint createCachedAccessPoint(ApAccessPoint accessPoint, ApState state, List<CachedPart> parts) {
        CachedAccessPoint cachedAccessPoint = new CachedAccessPoint();
        cachedAccessPoint.setAccessPointId(accessPoint.getAccessPointId());
        cachedAccessPoint.setApState(state);
        cachedAccessPoint.setErrorDescription(accessPoint.getErrorDescription());
        cachedAccessPoint.setLastUpdate(accessPoint.getLastUpdate());
        cachedAccessPoint.setPreferredPartId(accessPoint.getPreferredPartId());
        cachedAccessPoint.setState(accessPoint.getState());
        cachedAccessPoint.setUuid(accessPoint.getUuid());
        cachedAccessPoint.setParts(parts);
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
        cachedPart.setCreateChange(part.getCreateChange());
        cachedPart.setDeleteChange(part.getDeleteChange());
        cachedPart.setErrorDescription(part.getErrorDescription());
        cachedPart.setState(part.getState());
        cachedPart.setPartType(part.getPartType());
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

    @Transactional
    public CachedAccessPoint findCachedAccessPoint(Integer accessPointId) {
        ApCachedAccessPoint apCachedAccessPoint = cachedAccessPointRepository.findByAccessPointId(accessPointId);
        CachedAccessPoint cachedAccessPoint = null;
        if (apCachedAccessPoint != null) {
            cachedAccessPoint = deserialize(apCachedAccessPoint.getData());
        }
        return cachedAccessPoint;
    }

    public CachedAccessPoint deserialize(String data) {
        try {
            return mapper.readValue(data, CachedAccessPoint.class);
        } catch (IOException e) {
            throw new SystemException("Nastal problém při deserializaci objektu", e);
        }
    }

    private String serialize(CachedAccessPoint cachedAccessPoint) {
        try {
            return mapper.writeValueAsString(cachedAccessPoint);
        } catch (JsonProcessingException e) {
            throw new SystemException("Nastal problém při serializaci objektu", e);
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
