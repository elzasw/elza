package cz.tacr.elza.service.cache;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.common.db.QueryResults;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApBindingItem;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApKeyValue;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApScope;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.SyncState;
import cz.tacr.elza.drools.model.PartType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ApBindingItemRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApCachedAccessPointRepository;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApPartRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.search.SearchIndexSupport;

@Service
public class AccessPointCacheService implements SearchIndexSupport<ApCachedAccessPoint> {

    private static final Logger logger = LoggerFactory.getLogger(AccessPointCacheService.class);

    private final ObjectMapper mapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    @Qualifier("threadPoolTaskExecutorAP")
    private ThreadPoolTaskExecutor executor;

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
    
    @Autowired
    private StaticDataService staticDataService;

    /**
     * Maximální počet AP, které se mají dávkově zpracovávat pro
     * synchronizaci.
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
                String.class, Number.class, Boolean.class, Iterable.class, SyncState.class,
                LocalDate.class, LocalDateTime.class));
    }

    /**
     * Synchronizace záznamů v databázi.
     *
     * Synchronní metoda volaná z transakce.
     */
    synchronized public void syncCache() {
		logger.info("Spuštění - synchronizace cache pro AP");
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
    }

    /**
     * Synchronizace záznamů v databázi.
     *
     * Synchronní metoda volaná z transakce.
     */
    public void syncCacheParallel() {
        logger.info("Spuštění - synchronizace cache pro AP");

        AtomicInteger atomCounter = new AtomicInteger(0);
        AtomicInteger errorCounter = new AtomicInteger(0);

        synchronized (this) {

            TransactionTemplate tt = new TransactionTemplate(txManager);
            Integer cnt = tt.execute(t -> {
                ScrollableResults uncachedAPs = accessPointRepository.findUncachedAccessPoints();

                int count = 0;
                List<Integer> apIds = new ArrayList<>(syncApBatchSize);
                while (uncachedAPs.next()) {
                    Object obj = uncachedAPs.get(0);

                    apIds.add((Integer) obj);
                    count++;
                    if (count % syncApBatchSize == 0) {
                        atomCounter.incrementAndGet();
                        addParallelSync(atomCounter, errorCounter, apIds, count - apIds.size());
                        apIds.clear();
                    }
                }
                if (apIds.size() > 0) {
                    atomCounter.incrementAndGet();
                    addParallelSync(atomCounter, errorCounter, apIds, count - apIds.size());
                }
                return count;
            });

            logger.info("Počet AP k synchronizaci: {}", cnt);
        }

        synchronized (atomCounter) {
            while (atomCounter.get() > 0) {
                try {
                    atomCounter.wait(100);
                } catch (InterruptedException e) {
                    logger.error("AP synchronization interrupted");
                    throw new SystemException("AP synchronization interrupted");
                }
            }
        }

        if (errorCounter.get() > 0) {
            logger.error("AP synchronization failed");
            throw new SystemException("AP synchronization failed");
        }

        logger.info("Všechny AP jsou synchronizovány");
        logger.info("Ukončení synchronizace cache pro AP");
    }

    private void addParallelSync(final AtomicInteger atomCounter,
                                 final AtomicInteger errorCounter,
                                 final List<Integer> apIds,
                                 final int offset) {
        // IDS to own list
        final List<Integer> ids = new ArrayList<>(apIds);
        this.executor.execute(() -> parallelSync(atomCounter, errorCounter, ids, offset));
    }

    private void parallelSync(AtomicInteger atomCounter, AtomicInteger errorCounter, List<Integer> apIds, int offset) {
        try {
            logger.info("Sestavuji AP {}-{}", 1 + offset, apIds.size() + offset);

            TransactionTemplate tt = new TransactionTemplate(txManager);
            tt.executeWithoutResult(t -> {
                processNewAPs(apIds);
            });
        } catch (Exception e) {
            logger.error("Failed to create AP cache", e);
            errorCounter.incrementAndGet();
        }
        synchronized (atomCounter) {
            int v = atomCounter.decrementAndGet();
            if (v == 0) {
                atomCounter.notify();
            }
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
                logger.info("Sestavuji AP {}-{}", count - syncApBatchSize + 1 + offset, count + offset);

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
        Set<Integer> requestedIds = new HashSet<>(accessPointIds);
        if(requestedIds.size()!=accessPointIds.size()) {
            logger.error("Some ID is multiple times in the query");
        }

        List<ApAccessPoint> accessPointList = accessPointRepository.findAllById(requestedIds);        
        // Prepare map
        final Map<Integer, CachedAccessPoint> apMap = accessPointList.stream()
                .collect(Collectors.toMap(ApAccessPoint::getAccessPointId, ap -> createCachedAccessPoint(ap)));

        // check result
        if (requestedIds.size() != accessPointList.size()) {
            List<Integer> missingIds = requestedIds.stream().filter((reqId) -> apMap.get(reqId)==null)
                    .collect(Collectors.toList());
            logger.error("Some access points not found: {}", missingIds);
            throw new SystemException("Some access points not found", BaseCode.DB_INTEGRITY_PROBLEM)
                    .set("missingIds", missingIds);
        }

        // set ap state
        List<ApState> apStates = stateRepository.findLastByAccessPointIds(accessPointIds);
        if (apStates.size() != accessPointIds.size()) {
        	Map<Integer, ApState> apStatesMap = new HashMap<>();
        	for(ApState apState: apStates) {
        		ApState otherState = apStatesMap.put(apState.getAccessPointId(), apState);
        		if(otherState!=null) {
        			logger.error("Multiple states for same accessPoint, accessPointId: {}, apStateIds: {}, {}",
        					apState.getAccessPointId(), otherState.getStateId(), apState.getStateId());
        			throw new SystemException("Multiple states for same accessPoint", BaseCode.DB_INTEGRITY_PROBLEM)
        				.set("accessPointId", apState.getAccessPointId())
        				.set("apStateId", apState.getStateId())
        				.set("apStateId", otherState.getStateId());
        		}
        	}
        	// Check that we have all states
        	Set<Integer> ids = new HashSet<>();
        	for(ApAccessPoint ap: accessPointList) {
        		ApState apState = apStatesMap.get(ap.getAccessPointId());
        		if (apState == null) {
        			logger.error("Missing state for accesspoint, accessPointId: {}", ap.getAccessPointId());
        			throw new SystemException("Missing state for accesspoint.", BaseCode.DB_INTEGRITY_PROBLEM)
    					.set("accessPointId", ap.getAccessPointId());
        		}
        		if (!ids.add(ap.getAccessPointId())) {
        			logger.error("AccessPoint was already processed, accessPointId: {}", ap.getAccessPointId());
        			throw new SystemException("AccessPoint was already processed.", BaseCode.DB_INTEGRITY_PROBLEM)
    					.set("accessPointId", ap.getAccessPointId());
        		}
        	}
        	logger.error("Different number of apStates and accesspoints.");
			throw new SystemException("Different number of apStates and accesspoints.", BaseCode.DB_INTEGRITY_PROBLEM);
        }
        for (ApState apState : apStates) {
            apState = HibernateUtils.unproxy(apState);
            CachedAccessPoint cap = apMap.get(apState.getAccessPointId());
            cap.setApState(apState);
        }

        createCachedPartMap(accessPointList, apMap);
        createCachedBindingMap(accessPointList, apMap);
        createCachedReplacedMap(accessPointIds, apMap);

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

    /**
     * Create AP in cache
     * 
     * Method will flush entityManager and clear ALL objectcs from entityManager (including HibernateProxyObj)
     * 
     * @param accessPointId
     */
    @Transactional
    public void createApCachedAccessPoint(Integer accessPointId) {
    	
        // flush a batch of updates and release memory:
        this.entityManager.flush();
        this.entityManager.clear();
    
        synchronized (this){
			ApCachedAccessPoint oldApCachedAccessPoint = cachedAccessPointRepository.findByAccessPointId(accessPointId);
			if (oldApCachedAccessPoint != null) {
				cachedAccessPointRepository.delete(oldApCachedAccessPoint);
                this.entityManager.flush();
			}
			processNewAPs(Collections.singletonList(accessPointId));
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
        cachedPart.setLastChangeId(part.getLastChangeId());
        cachedPart.setDeleteChangeId(part.getDeleteChangeId());
        cachedPart.setErrorDescription(part.getErrorDescription());
        cachedPart.setState(part.getState());
        cachedPart.setPartTypeCode(part.getPartType().getCode());
        cachedPart.setKeyValue(HibernateUtils.unproxy(part.getKeyValue()));
        cachedPart.setParentPartId(part.getParentPartId());
        return cachedPart;
    }

    private void createCachedReplacedMap(List<Integer> accessPointIds, Map<Integer, CachedAccessPoint> apMap) {

        List<ApState> states = stateRepository.findLastByReplacedByIds(accessPointIds);

        states.forEach(s -> apMap.get(s.getReplacedById()).addReplacedId(s.getAccessPointId()));
    }

    private void createCachedPartMap(List<ApAccessPoint> accessPointList, Map<Integer, CachedAccessPoint> apMap) {

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

    private void addItemsToCachedPartMap(List<ApAccessPoint> accessPointList,
    		Map<Integer, CachedPart> partMap) {
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
            Validate.isTrue(bindingItem.getItemId() != null || bindingItem.getPartId() != null,
                    "ItemId and PartId should not be NULL together, bindingId: %s", bindingItem.getBindingId());
            bindingItem = HibernateUtils.unproxy(bindingItem);
            Integer bindingId = bindingItem.getBindingId();
            CachedBinding b = bindingMap.get(bindingId);

            Validate.notNull(b, "Cached binding not found, bindingId: %s", bindingId);
            b.addBindingItem(bindingItem);
        }
        logger.debug("Update AccessPointCache, count: {}, bindingId: {}", bindingItems.size(), bindingItems.isEmpty()? null : bindingItems.get(0).getBindingId());
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
		ApCachedAccessPoint apCachedAccessPoint = cachedAccessPointRepository.findByAccessPointId(accessPointId);
		CachedAccessPoint cachedAccessPoint = null;
		if (apCachedAccessPoint != null) {
			cachedAccessPoint = deserialize(apCachedAccessPoint.getData());
		}
		return cachedAccessPoint;
    }

    @Transactional
    public List<CachedAccessPoint> findCachedAccessPoints(Collection<Integer> accessPointIds) {
        List<CachedAccessPoint> cachedAccessPoints = new ArrayList<>(accessPointIds.size()); 
        List<ApCachedAccessPoint> apCachedAccessPoints = cachedAccessPointRepository.findByAccessPointIds(accessPointIds);
        for (ApCachedAccessPoint apCachedAccessPoint : apCachedAccessPoints) {
            CachedAccessPoint cachedAccessPoint = null;
            if (apCachedAccessPoint != null) {
                cachedAccessPoint = deserialize(apCachedAccessPoint.getData());
                cachedAccessPoints.add(cachedAccessPoint);
            }
        }
        return cachedAccessPoints;
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
            throw new SystemException("Nastal problĂ©m pĹ™i deserializaci objektu", e);
        }
    }

    private void restoreLinks(CachedAccessPoint cap) {
        ApAccessPoint ap = entityManager.getReference(ApAccessPoint.class, cap.getAccessPointId());
        ApState state = cap.getApState();
        if (state != null) {
            state.setAccessPoint(ap);

            ApScope scope = entityManager.getReference(ApScope.class, state.getScopeId());
            state.setScope(scope);

            state.setCreateChange(entityManager.getReference(ApChange.class, state.getCreateChangeId()));
            if (state.getDeleteChangeId() != null) {
                state.setDeleteChange(entityManager.getReference(ApChange.class, state.getDeleteChangeId()));
            }
            if (state.getReplacedById() != null) {
                state.setReplacedBy(entityManager.getReference(ApAccessPoint.class, state.getReplacedById()));
            }
        }
        
        StaticDataProvider sdp = staticDataService.getData();

        Map<Integer, ApPart> partMap =  new HashMap<>();
        Map<Integer, ApItem> itemMap =  new HashMap<>();
        
        if (cap.getParts() != null) {
            // restore parts
            List<ApPart> apParts = new ArrayList<>(cap.getParts().size());
        	
            for (CachedPart part : cap.getParts()) {
                // ApPart apPart = entityManager.getReference(ApPart.class, part.getPartId());
                ApPart apPart = new ApPart();
                apPart.setAccessPoint(ap);
                apPart.setCreateChange(entityManager.getReference(ApChange.class, part.getCreateChangeId()));
                if (part.getDeleteChangeId() != null) {
                    apPart.setDeleteChange(entityManager.getReference(ApChange.class, part.getDeleteChangeId()));
                }
                apPart.setErrorDescription(part.getErrorDescription());

                ApKeyValue keyValue = part.getKeyValue();
                if (keyValue != null) {
                    ApScope scope = entityManager.getReference(ApScope.class, keyValue.getScopeId());
                    keyValue.setScope(scope);
                }
                apPart.setKeyValue(keyValue);
                apPart.setLastChange(entityManager.getReference(ApChange.class, part.getLastChangeId()));
                apPart.setPartId(part.getPartId());
                apPart.setPartType(sdp.getPartTypeByCode(part.getPartTypeCode()));
                apPart.setState(part.getState());

                apParts.add(apPart);
                partMap.put(part.getPartId(), apPart);

                if (part.getItems() != null) {
                	// Copy items to new list and sort
                	List<ApItem> items = new ArrayList<>(part.getItems().size());
                	
                    for (ApItem item : part.getItems()) {
                    	itemMap.put(item.getItemId(), item);
                    	
                        item.setPart(apPart);
                        item.setCreateChange(entityManager.getReference(ApChange.class, item.getCreateChangeId()));
                        if (item.getDeleteChangeId() != null) {
                            item.setDeleteChange(entityManager.getReference(ApChange.class, item.getDeleteChangeId()));
                        }
                        ItemType itemType = sdp.getItemTypeById(item.getItemTypeId());
                        Validate.notNull(itemType, "Item type not found, itemTypeId: %s", item.getItemTypeId());
                       	item.setItemType(itemType.getEntity());

                        if(item.getItemSpecId()!=null) {
                        	RulItemSpec itemSpec = sdp.getItemSpecById(item.getItemSpecId());
                        	Validate.notNull(itemSpec, "Item specification not found, itemSpecId: %s", item.getItemSpecId());
                       		item.setItemSpec(itemSpec);
                        }
                        items.add(item);
                    }
                    // sort items
                    items.sort((a,b) -> {
                    	Integer aViewOrder = a.getItemType().getViewOrder();
                    	Integer bViewOrder = b.getItemType().getViewOrder();
                    	int ret = aViewOrder.compareTo(bViewOrder);
                    	if(ret==0) {
                    		aViewOrder = a.getPosition();
                    		bViewOrder = b.getPosition();
                    		ret = aViewOrder.compareTo(bViewOrder);
                    	}
                    	return ret;
                    });
                    part.setItems(items);
                }
            }
            // restore parent part
            for (CachedPart part : cap.getParts()) {
                if (part.getParentPartId() != null) {
                    ApPart apPart = partMap.get(part.getPartId());
                    ApPart apParentPart = partMap.get(part.getParentPartId());
                    apPart.setParentPart(apParentPart);
                }
            }

            cap.setApParts(apParts);
        } else {
            cap.setApParts(Collections.emptyList());
        }

        List<CachedBinding> cachedBindings = cap.getBindings();
        if(CollectionUtils.isEmpty(cachedBindings)) {
            // nothing to link
            return;
        }
        for (CachedBinding cachedBinding : cachedBindings) {
            ApBinding b = entityManager.getReference(ApBinding.class, cachedBinding.getId());
            ApBindingState bs = cachedBinding.getBindingState();
            bs.setAccessPoint(ap);
            bs.setBinding(b);
            bs.setCreateChange(entityManager.getReference(ApChange.class, bs.getCreateChangeId()));
            if (bs.getDeleteChangeId() != null) {
            	// TODO: Vymazane bindingState nemaji byt v cache
                bs.setDeleteChange(entityManager.getReference(ApChange.class, bs.getDeleteChangeId()));
            }
            if (bs.getSyncChangeId() != null) {
                bs.setSyncChange(entityManager.getReference(ApChange.class, bs.getSyncChangeId()));
            }
            if (bs.getApTypeId() != null) {
                bs.setApType(sdp.getApTypeById(bs.getApTypeId()));
            }
            if (bs.getPreferredPartId() != null) {
                ApPart prefPart = partMap.get(bs.getPreferredPartId());
                if (prefPart == null) {
                    prefPart = entityManager.getReference(ApPart.class, bs.getPreferredPartId());
                }
                bs.setPreferredPart(prefPart);
            }

            // set items
            List<ApBindingItem> bil = cachedBinding.getBindingItemList();
            if (bil != null) {
                restoreLinks(b, bil, partMap, itemMap);
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

    private void restoreLinks(ApBinding b, List<ApBindingItem> bil, 
    		Map<Integer, ApPart> partMap, Map<Integer, ApItem> itemMap) {
        for (ApBindingItem bi : bil) {
            bi.setBinding(b);
            bi.setCreateChange(entityManager.getReference(ApChange.class, bi.getCreateChangeId()));
            if (bi.getDeleteChangeId() != null) {
            	// TODO: Vymazane bindingItem nemaji byt v cache
                bi.setDeleteChange(entityManager.getReference(ApChange.class, bi.getDeleteChangeId()));
            }
            if (bi.getItemId() != null) {
            	ApItem item = itemMap.get(bi.getItemId());
            	Validate.notNull(item, "Referenced item not found, itemId: %s", bi.getItemId());
            	
                bi.setItem(item);
            }
            if (bi.getPartId() != null) {
            	ApPart part = partMap.get(bi.getPartId());
            	Validate.notNull(part, "Referenced part not found, partId: %s", bi.getPartId());
                bi.setPart(part);
            }
        }

    }

    private String serialize(CachedAccessPoint cachedAccessPoint) {
        validate(cachedAccessPoint);
        try {
            return mapper.writeValueAsString(cachedAccessPoint);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize object, accessPointId: " +
                    cachedAccessPoint.getAccessPointId(), e);
            throw new SystemException("Nastal problĂ©m pĹ™i serializaci objektu, accessPointId: " +
                    cachedAccessPoint.getAccessPointId(), e)
                            .set("accessPointId", cachedAccessPoint.getAccessPointId());
        }
    }

    private void validate(CachedAccessPoint cachedAccessPoint) {
        // validate only active APs
        if (cachedAccessPoint.getApState() == null ||
                cachedAccessPoint.getApState().getDeleteChangeId() != null) {
            return;
        }
        // validate before writing
        if (cachedAccessPoint.getPreferredPartId() == null) {
            Validate.notNull(cachedAccessPoint.getPreferredPartId(),
                             "Missing preferrdPartId, accessPointId=%s",
                             cachedAccessPoint.getAccessPointId());
        }
        if (cachedAccessPoint.getParts() == null) {
            Validate.notNull(cachedAccessPoint.getParts(),
                             "List of parts is empty, accessPointId=%s",
                             cachedAccessPoint.getAccessPointId());
        }
        // validate parts
        CachedPart prefPart = null;
        Set<Integer> partIds = new HashSet<>();
        Set<Integer> itemIds = new HashSet<>();
        for (CachedPart cachedPart : cachedAccessPoint.getParts()) {
            if (cachedPart.getDeleteChangeId() != null) {
                Validate.isTrue(cachedPart.getDeleteChangeId() == null,
                                "Deleted part cannot be cached, accessPointId=%s",
                                cachedAccessPoint.getAccessPointId());
            }
            if (Objects.equals(cachedAccessPoint.getPreferredPartId(), cachedPart.getPartId())) {
                prefPart = cachedPart;
            }
            if (!partIds.add(cachedPart.getPartId())) {
                Validate.isTrue(false,
                                "Duplicated part in cache, accessPointId=%s, partId=%s",
                                cachedAccessPoint.getAccessPointId(),
                                cachedPart.getPartId());
            }
            // check empty part
            if (CollectionUtils.isEmpty(cachedPart.getItems())) {
                Validate.isTrue(false,
                                "Část popisu entity nemůže být prázdná: Empty part in cache, accessPointId=%s, partId=%s",
                                cachedAccessPoint.getAccessPointId(),
                                cachedPart.getPartId());
            }
            for (ApItem item: cachedPart.getItems()) {
            	if (item.getDeleteChangeId() != null || item.getDeleteChange() != null) {
                    Validate.isTrue(false,
                            "Deleted item cannot be cached, accessPointId=%s",
                            cachedAccessPoint.getAccessPointId());            		
            	}
            	if (!itemIds.add(item.getItemId())) {
                    Validate.isTrue(false,
                            "Duplicated item in cache, accessPointId=%s, itemId=%s",
                            cachedAccessPoint.getAccessPointId(),
                            item.getItemId());
            	}
                ArrData data = item.getData();
                if (data != null) {
                    data.validate();
                }
            }
        }
        // validate preferred name
        if (prefPart == null) {
            Validate.notNull(prefPart, "Missing preferred parts, accessPointId=%s",
                             cachedAccessPoint.getAccessPointId());
        }
        // check type of pref part
        if (!Objects.equals(prefPart.getPartTypeCode(), PartType.PT_NAME.value())) {
            Validate.isTrue(false,
                            "Invalid prefName type, accessPointId=%s, partId=%s",
                            cachedAccessPoint.getAccessPointId(),
                            prefPart.getPartId());
        }
        
        // Validate bindings
        List<CachedBinding> bindings = cachedAccessPoint.getBindings();
        if(bindings!=null) {
        	for(CachedBinding binding: bindings) {
        		ApBindingState bs = binding.getBindingState();
        		if(bs==null) {
                    Validate.notNull(bs, "Binding without BindingState, accessPointId=%s",
                            cachedAccessPoint.getAccessPointId());        			
        		}
        		if(bs.getDeleteChangeId()!=null||bs.getDeleteChange()!=null) {
                    Validate.isTrue(false,
                            "Deleted bindingState cannot be cached, accessPointId=%s",
                            cachedAccessPoint.getAccessPointId());        			
        		}
        		// check items and parts
        		List<ApBindingItem> bindingItems = binding.getBindingItemList();
        		if(bindingItems!=null) {
        			for(ApBindingItem bi: bindingItems) {
        				if(bi.getDeleteChange()!=null||bi.getDeleteChangeId()!=null) {
                            Validate.isTrue(false,
                                    "Deleted bindingItem cannot be cached, accessPointId=%s",
                                    cachedAccessPoint.getAccessPointId());
        				}
        				// check existence of part or item
        				if(bi.getItemId()!=null) {
        					if(!itemIds.contains(bi.getItemId())) {
                                Validate.isTrue(false,
                                                "BindigItem is referencing non existing item, accessPointId=%s, binding.itemId: %s",
                                                bi.getItemId(),
                                                cachedAccessPoint.getAccessPointId());
        					} 
        				} else {
        					if(bi.getItem()!=null) {
        						Validate.isTrue(false, "BindigItem is referencing item without id, accessPointId=%s",
        								cachedAccessPoint.getAccessPointId());        							
        					}        					
        				}
        				if(bi.getPartId()!=null) {
        					if(!partIds.contains(bi.getPartId())) {
        						Validate.isTrue(false, "BindigItem is referencing non existing part, accessPointId=%s",
        								cachedAccessPoint.getAccessPointId());
        					}
        				} else {
        					if(bi.getPart()!=null) {
        						Validate.isTrue(false, "BindigItem is referencing part without id, accessPointId=%s",
        								cachedAccessPoint.getAccessPointId());        							
        					}        					
        				}
        			}
        		}
        	}
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
