package cz.tacr.elza.service.cache;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.time.LocalDate;
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
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Lists;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrInhibitedItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeExtension;
import cz.tacr.elza.domain.factory.DescItemFactory;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.ApAccessPointRepository;
import cz.tacr.elza.repository.ArrRefTemplateRepository;
import cz.tacr.elza.repository.CachedNodeRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.InhibitedItemRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeExtensionRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.service.DescriptionItemService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

/**
 * Cache of serialized description-unit (JP) data. Used for fast form rendering,
 * bulk actions, and fulltext search via Hibernate Search.
 *
 * <h3>Cache contract</h3>
 * <ul>
 *   <li><b>Current state only.</b> The cache reflects what is visible in the
 *       currently open (unlocked) fund version. All loaders filter
 *       {@code deleteChange IS NULL} — soft-deleted desc items, inhibited
 *       items, node extensions, and dao links never enter the cached JSON.</li>
 *   <li><b>Not for historical reads.</b> When reading against a locked
 *       {@link cz.tacr.elza.domain.ArrFundVersion} (non-null {@code lockChange}),
 *       callers must bypass the cache and read directly from DB — see
 *       {@link cz.tacr.elza.service.ArrangementFormService} and
 *       {@link cz.tacr.elza.drools.service.DescItemReader} for the pattern.</li>
 *   <li><b>Row-existence invariant:</b> {@code arr_cached_node} exists for a
 *       node <i>iff</i> that node has at least one {@code arr_level} with
 *       {@code deleteChange IS NULL}. Invalid nodes (all levels soft-deleted)
 *       are removed from the cache and thus from the Lucene index.</li>
 * </ul>
 *
 * <h3>Lifecycle</h3>
 * <ul>
 *   <li>{@link #addNodeToCache}/{@link #addNodesToCache} — add a node to the cache
 *       as a new, empty entry when it first joins the tree.</li>
 *   <li>{@link #syncNodes} — rebuild the serialized JSON from current DB state
 *       after item-level changes. Automatically creates missing rows for
 *       active nodes, so callers like
 *       {@link cz.tacr.elza.service.RevertingChangesService} that may
 *       resurrect soft-deleted levels do not need special handling.</li>
 *   <li>{@link #saveNodes}/{@link #saveNode} — persist an in-memory
 *       {@link CachedNode} without refetching from DB. Used by interactive
 *       editing paths that mutate the deserialized object directly.</li>
 *   <li>{@link #deleteNodes} — drop the cache row. Invoked when the
 *       {@code arr_node} is physically removed (orphan cleanup, full
 *       revert).</li>
 *   <li>{@link #deleteNodesNewTx} — drop the cache row in a fresh transaction.
 *       Used by after-commit hooks in {@link cz.tacr.elza.service.FundLevelService}
 *       when a node's last active {@code arr_level} is soft-deleted; running
 *       the delete in the enclosing transaction would trigger a session-wide
 *       Hibernate auto-flush that collides with unrelated pending writes in
 *       cascading flows (e.g. {@code deleteDaoPackageWithCascade}).</li>
 * </ul>
 *
 * <h3>Hibernate Search integration</h3>
 * {@link ArrCachedNode} is {@code @Indexed}; the serialized {@code data} column
 * feeds the Lucene index used by
 * {@link cz.tacr.elza.service.NodeSearchService}. Changes to the underlying
 * entities (desc items, inhibited items, dao links, node extensions) do not
 * reindex automatically — the index only refreshes when the {@code data}
 * column changes via {@code syncNodes}/{@code saveNodes}, or when the row is
 * deleted via {@code deleteNodes}.
 *
 * <h3>Concurrency</h3>
 * A {@link java.util.concurrent.locks.ReentrantReadWriteLock} guards the
 * cache. {@link #syncCache()} takes the write lock (excludes all other
 * access); ordinary reads and writes share the read lock.
 *
 * <h3>Serialization</h3>
 * {@link CachedNode} is serialized to JSON via Jackson; which getters are
 * serialized is controlled by the {@link NodeCacheSerializable} marker
 * interface plus basic primitive types.
 */
@Service
public class NodeCacheService {

	private static final Logger logger = LoggerFactory.getLogger(NodeCacheService.class);

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwl.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwl.writeLock();

    private final ObjectMapper mapper;

	@PersistenceContext
	private EntityManager entityManager;

    /**
     * Maximální počet JP, které se mají dávkově zpracovávat pro synchronizaci.
     */
    private static final int SYNC_BATCH_NODE_SIZE = 800;

    @Autowired
    private DaoLinkRepository daoLinkRepository;

    @Autowired
    private StructuredObjectRepository structureDataRepository;

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    @Autowired
    private FundFileRepository fundFileRepository;

    @Autowired
    private NodeExtensionRepository nodeExtensionRepository;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private CachedNodeRepository cachedNodeRepository;
    
    @Autowired
    private InhibitedItemRepository inhibitedItemRepository;

    @Autowired
	private StaticDataService staticDataService;

    @Autowired
    private ArrRefTemplateRepository refTemplateRepository;

    @Autowired
    private DescriptionItemService descItemService;

    @Autowired
    @Qualifier("transactionManager")
    protected PlatformTransactionManager txManager;

    @Autowired
    @Qualifier("threadPoolTaskExecutorAR")
    private ThreadPoolTaskExecutor executor;

    public NodeCacheService() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setVisibility(new InterfaceVisibilityChecker(NodeCacheSerializable.class,
                String.class, Number.class, Boolean.class, Iterable.class,
                LocalDate.class, ElzaTable.class, ElzaRow.class,
                // used in ElzaRow
                Map.class));
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
            logger.info("Spuštění synchronizace cache pro JP");
            syncCacheInternal();
            logger.info("Ukončení synchronizace cache pro JP");
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Synchronizace požadovaných JP.
     *
     * @param nodeIds seznam požadovaných JP k synchronizaci
     */
    @Transactional
    public void syncNodes(final Collection<Integer> nodeIds) {
        writeLock.lock();
        try {
            logger.trace("syncNodes(nodeIds: {})", nodeIds);
            syncNodesInternal(nodeIds);
            logger.trace("end of syncNodes(nodeIds: {})", nodeIds);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Odstranění u JP okazující JP.
     *
     * @param nodeIds odebírané JP
     * @param referralNodeIds zdrojové-odkazující JP ve který je třeba odebrat JP
     */
    @Transactional
    public void removeReferralNodeIds(final Set<Integer> nodeIds, final Set<Integer> referralNodeIds) {
        writeLock.lock();
        try {
            logger.debug(">removeReferralNodeIds(nodeIds:{}, referralNodeIds:{})", nodeIds, referralNodeIds);
            removeReferralNodeIdsInternal(nodeIds, referralNodeIds);
            logger.debug("<removeReferralNodeIds(nodeIds:{}, referralNodeIds:{})", nodeIds, referralNodeIds);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Obnovení u JP okazující JP.
     *
     * @param nodeIds     restorované JP
     * @param uuidNodeMap mapa JP podle UUID
     */
    @Transactional
    public void restoreReferralNodeIds(final Set<Integer> nodeIds, final Map<String, ArrNode> uuidNodeMap) {
        writeLock.lock();
        try {
            logger.debug(">restoreReferralNodeIds(nodeIds:{}, uuidNodeMap:{})", nodeIds, uuidNodeMap);
            restoreReferralNodeIdsInternal(nodeIds, uuidNodeMap);
            logger.debug("<restoreReferralNodeIds(nodeIds:{}, uuidNodeMap:{})", nodeIds, uuidNodeMap);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Získání sestavené cachované JP.
     *
     * @param nodeId identifikátor JP
     * @return JP
     */
	@Transactional(value = TxType.MANDATORY)
	public RestoredNode getNode(final Integer nodeId) {
        readLock.lock();
        try {
            logger.trace("getNode(nodeId: {})", nodeId);
			RestoredNode nodeInternal = getNodeInternal(nodeId);
            return nodeInternal;
        } catch (Exception e) {
            logger.error("Failed to read nodeId: {}", nodeId, e);
            throw e;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Získání sestavených cachovaných JP.
     *
     * @param nodeIds identifikátory JP
     * @return seznam JP
     */
	@Transactional(value = TxType.MANDATORY)
	public Map<Integer, RestoredNode> getNodes(final Collection<Integer> nodeIds) {
        readLock.lock();
        try {
            logger.trace("getNodes(nodeId: {})", nodeIds);
			Map<Integer, RestoredNode> nodesInternal = getNodesInternal(nodeIds);
            return nodesInternal;
        } catch (Exception e) {
            logger.error("Failed to read nodes, ids: {}", nodeIds, e);
            throw e;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Získání sestavených cachovaných JP.
     *
     * @param nodes
     * @return seznam JP
     */
    @Transactional(value = TxType.MANDATORY)
    public Collection<RestoredNode> getRestoredNodes(final Collection<ArrNode> nodes) {
        readLock.lock();
        try {
            logger.trace("getNodes(nodes: {})", nodes);
            Collection<RestoredNode> nodesInternal = getRestoredNodesInternal(nodes);
            return nodesInternal;
        } catch (Exception e) {
            logger.error("Failed to read nodes: {}", nodes, e);
            throw e;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Deserializace CachedNode.
     *
     * @param data
     * @return CachedNode
     */
    public CachedNode deserialize(final String data) {
        try {
        	return mapper.readValue(data, CachedNode.class);
        } catch (IOException e) {
            logger.error("Failed to deserialize object, data: " + data);
            throw new SystemException("Při deserializaci objektu se objevil problém", e);
        }
    }

    /**
     * Uložení záznamů.
     *
     * @param cachedNodes
     *            seznam ukládaných objektů
     * @param flush
     *            Priznak, zda se ma provest flush tabulky
     */
    @Transactional
    public void saveNodes(final Collection<? extends CachedNode> cachedNodes, boolean flush) {
        readLock.lock();
        try {
            logger.trace("saveNodes({})", cachedNodes);
            saveNodesInternal(cachedNodes, flush);
            logger.trace("end of saveNodes({})", cachedNodes);
        } finally {
            readLock.unlock();
        }
    }


    /**
     * Uložení záznamu.
     *
     * @param cachedNode
     *            ukládaný objekt
     * @param flush
     *            Priznak, zda se ma provest flush tabulky
     */
    @Transactional
    public void saveNode(final CachedNode cachedNode, boolean flush) {
        readLock.lock();
        try {
            logger.trace("saveNode({})", cachedNode);
            saveNodesInternal(Collections.singletonList(cachedNode), flush);
            logger.trace("end of saveNode({})", cachedNode);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Odstranění JP.
     *
     * @param nodeIds seznam identifikátorů mazaných JP
     */
    @Transactional
    public void deleteNodes(final Collection<Integer> nodeIds) {
        writeLock.lock();
        try {
            logger.trace("deleteNodes({})", nodeIds);
            deleteNodesInternal(nodeIds);
            logger.trace("end of deleteNodes({})", nodeIds);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Drop cache rows in a fresh transaction, independent of any enclosing one.
     *
     * Used by after-commit hooks (e.g. from {@code FundLevelService}) that need
     * to remove cache rows for nodes whose last active level was just
     * soft-deleted. Running the delete in the enclosing transaction triggers a
     * session-wide Hibernate auto-flush that collides with unrelated pending
     * writes in cascading flows; a fresh transaction has a clean session.
     */
    @Transactional(value = TxType.REQUIRES_NEW)
    public void deleteNodesNewTx(final Collection<Integer> nodeIds) {
        writeLock.lock();
        try {
            logger.trace("deleteNodesNewTx({})", nodeIds);
            deleteNodesInternal(nodeIds);
            logger.trace("end of deleteNodesNewTx({})", nodeIds);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Drop cache rows for nodes that violate the row-existence invariant —
     * i.e. nodes whose every {@code arr_level} has {@code deleteChange IS NOT NULL}.
     *
     * Intended for startup cleanup to repair pre-existing stale entries that
     * would otherwise leak into the Lucene index.
     */
    @Transactional(TxType.MANDATORY)
    public void clearInvalidCachedNodes() {
        writeLock.lock();
        try {
            List<Integer> invalidIds = cachedNodeRepository.findInvalidCachedNodeIds();
            if (invalidIds.isEmpty()) {
                logger.debug("No invalid cached nodes found");
                return;
            }
            logger.info("Removing {} cached node(s) without any active level", invalidIds.size());
            deleteNodesInternal(invalidIds);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Synchronizace požadovaných JP.
     *
     * @param nodeIds seznam požadovaných JP k synchronizaci
     */
    private void syncNodesInternal(final Collection<Integer> nodeIds) {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return;
        }

        // Create empty cache rows for active nodes that are not yet cached.
        // Needed after RevertingChangesService resurrects soft-deleted levels,
        // where a node transitions from "all levels deleted" back to "has
        // active level" and its cache row was previously dropped.
        List<Integer> uncachedActive = nodeRepository.findUncachedActiveNodeIds(nodeIds);
        if (!uncachedActive.isEmpty()) {
            logger.debug("Re-creating cache rows for {} active node(s) without cache", uncachedActive.size());
            List<ArrNode> nodesToCreate = nodeRepository.findAllById(uncachedActive);
            addNodesToCache(nodesToCreate);
        }

        List<ArrCachedNode> cachedNodes = cachedNodeRepository.findByNodeIdIn(nodeIds);
        if (cachedNodes.isEmpty()) {
            return;
        }

        logger.debug("Synchronizace požadovaných JP: {}", cachedNodes.size());
        int i = 0;
        for (List<ArrCachedNode> partCachedNodes : Lists.partition(cachedNodes, SYNC_BATCH_NODE_SIZE)) {
            i++;
            logger.debug("Sestavuji JP " + ((i - 1) * SYNC_BATCH_NODE_SIZE + 1) + "-" + ((i * SYNC_BATCH_NODE_SIZE) < nodeIds.size() ? (i * SYNC_BATCH_NODE_SIZE) : nodeIds.size()));
            cachedNodeRepository.saveAll(updateCachedNodes(partCachedNodes));
        }
    }

    /**
     * Odstranění u JP okazující JP.
     *
     * @param nodeIds odebírané JP
     * @param referralNodeIds zdrojové-odkazující JP ve který je třeba odebrat JP
     */
    private void removeReferralNodeIdsInternal(final Set<Integer> nodeIds, final Set<Integer> referralNodeIds) {
        List<ArrCachedNode> cachedNodes = ObjectListIterator.findIterable(referralNodeIds, cachedNodeRepository::findByNodeIdIn);
        List<ArrCachedNode> cachedNodesUpdated = new ArrayList<>();
        for (ArrCachedNode cachedNode : cachedNodes) {
            RestoredNode result = deserialize(cachedNode);
            boolean change = false;

            List<ArrDescItem> descItems = result.getDescItems();
            if (CollectionUtils.isNotEmpty(descItems)) {
                for (ArrDescItem descItem : descItems) {
                    ArrData data = HibernateUtils.unproxy(descItem.getData());

                    if (data instanceof ArrDataUriRef) {
                        ArrDataUriRef dataRef = (ArrDataUriRef) data;
                        Integer nodeId = dataRef.getNodeId();
                        if (nodeId != null && nodeIds.contains(nodeId)) {
                            change = true;
                            dataRef.setArrNode(null);
                        }
                    }
                }
            }

            if (change) {
                cachedNode.setData(serialize(result, false)); // není třeba validovat, protože pouze mažeme odkaz na JP
                cachedNodesUpdated.add(cachedNode);
            }
        }
        if (cachedNodesUpdated.size() > 0) {
            cachedNodeRepository.saveAll(cachedNodesUpdated);
        }
    }

    /**
     * Obnovení u JP okazující JP.
     *
     * @param nodeIds     restorované JP
     * @param uuidNodeMap mapa JP podle UUID
     */
    private void restoreReferralNodeIdsInternal(final Set<Integer> nodeIds, final Map<String, ArrNode> uuidNodeMap) {
        List<ArrCachedNode> cachedNodes = ObjectListIterator.findIterable(nodeIds, cachedNodeRepository::findByNodeIdIn);
        List<ArrCachedNode> cachedNodesUpdated = new ArrayList<>();
        for (ArrCachedNode cachedNode : cachedNodes) {
            RestoredNode result = deserialize(cachedNode);
            boolean change = false;

            List<ArrDescItem> descItems = result.getDescItems();
            if (CollectionUtils.isNotEmpty(descItems)) {
                for (ArrDescItem descItem : descItems) {
                    ArrData data = HibernateUtils.unproxy(descItem.getData());

                    if (data instanceof ArrDataUriRef) {
                        ArrDataUriRef dataRef = (ArrDataUriRef) data;
                        Integer nodeId = dataRef.getNodeId();
                        URI tempUri = URI.create(dataRef.getUriRefValue()).normalize();
                        if (nodeId == null && DescItemFactory.ELZA_NODE.equalsIgnoreCase(tempUri.getScheme())) {
                            String nodeUuid = tempUri.getAuthority();
                            ArrNode node = uuidNodeMap.get(nodeUuid);
                            if (node != null) { // může to být odkaz z jiného AS, takže teoreticky může se stát, že se nenajde JP
                                change = true;
                                dataRef.setArrNode(node);
                            }
                        }
                    }
                }
            }

            if (change) {
                cachedNode.setData(serialize(result, false)); // není třeba validovat, protože pouze připojujeme odkaz na JP
                cachedNodesUpdated.add(cachedNode);
            }
        }
        if (cachedNodesUpdated.size() > 0) {
            cachedNodeRepository.saveAll(cachedNodesUpdated);
        }
    }

    /**
     * Odstranění JP.
     *
     * @param nodeIds seznam identifikátorů mazaných JP
     */
    private void deleteNodesInternal(final Collection<Integer> nodeIds) {
        // Fetch and delete managed entities instead of issuing a bulk JPQL
        // DELETE. A bulk DELETE triggers Hibernate's auto-flush across the
        // entire session, which can expose unrelated pending writes from the
        // surrounding transaction (e.g. transient cache rows still queued for
        // insert via saveAll during cascade cleanup in deleteDaoPackageWithCascade).
        // deleteAll(entities) queues em.remove calls; the actual SQL is emitted
        // at the next flush/commit, without forcing a session-wide flush now.
        //
        // Load in batches.
        List<ArrCachedNode> entities = ObjectListIterator.findIterable(nodeIds, cachedNodeRepository::findByNodeIdsInNoFetch);
        if (!entities.isEmpty()) {
            cachedNodeRepository.deleteAll(entities);
        }
    }

    /**
     * Synchronizace cache pro JP.
     *
     * Synchronní metoda volaná z transakce.
     */
    @Transactional(value=TxType.NEVER)
    public void syncCacheParallel() {
        logger.info("Node cache synchronization started.");

        AtomicInteger atomCounter = new AtomicInteger(0);
        AtomicInteger errorCounter = new AtomicInteger(0);
        
        int totalQueueCapacity = this.executor.getQueueCapacity();

        synchronized (this) {
            TransactionTemplate tt = new TransactionTemplate(txManager);
            Integer cnt = tt.execute(t -> {
        		ScrollableResults<Integer> uncachedNodes = nodeRepository.findUncachedNodes();
        		
        		List<Integer> batchNodeIds = new ArrayList<>(SYNC_BATCH_NODE_SIZE);
                int count = 0;
        		while (uncachedNodes.next()) {
        			Integer nodeId = uncachedNodes.get();

        			batchNodeIds.add(nodeId);
        			count++;
        			if (count % SYNC_BATCH_NODE_SIZE == 0) {
        				logger.debug("Adding nodes to queue: {}-{}", count - SYNC_BATCH_NODE_SIZE + 1, count);

        				int numWaiting = atomCounter.incrementAndGet();
                        // check if executor has free slots
						while (numWaiting + 5 >= totalQueueCapacity) {
							try {
								logger.debug("Waiting to add nodes to queue: {}-{}", count - SYNC_BATCH_NODE_SIZE + 1, count);
								atomCounter.wait(1000);
								numWaiting = atomCounter.get();
							} catch (InterruptedException e) {
								logger.error("Cache node synchronization interrupted");
								throw new SystemException("Cache node synchronization interrupted");
							}							
						}
        				
        				// check if executor has free slots
                        addParallelSync(atomCounter, errorCounter, batchNodeIds, count - batchNodeIds.size());
        				batchNodeIds.clear();
        			}
        		}
        		// process remaining nodes
                if (batchNodeIds.size() > 0) {
                    atomCounter.incrementAndGet();
                    addParallelSync(atomCounter, errorCounter, batchNodeIds, count - batchNodeIds.size());
                }
                return count;
            });

            logger.info("Number of nodes requiring synchronization: {}", cnt);
        }

        synchronized (atomCounter) {
            while (atomCounter.get() > 0) {
                try {
                    atomCounter.wait(100);
                } catch (InterruptedException e) {
                    logger.error("JP synchronization interrupted");
                    throw new SystemException("JP synchronization interrupted");
                }
            }
        }

        if (errorCounter.get() > 0) {
            logger.error("JP synchronization failed");
            throw new SystemException("JP synchronization failed");
        }

        logger.info("Node cache synchronization finished.");
    }

    private void addParallelSync(final AtomicInteger atomCounter, 
    							 final AtomicInteger errorCounter, 
    							 final List<Integer> nodeIds, 
    							 final int offset) {
    	// IDS to own list
    	final List<Integer> ids = new ArrayList<>(nodeIds);
    	this.executor.execute(() -> parallelSync(atomCounter, errorCounter, ids, offset));
    }

    private void parallelSync(AtomicInteger atomCounter, AtomicInteger errorCounter, List<Integer> nodeIds, int offset) {
        try {
            logger.info("Creating cache for nodes {}-{}", offset + 1, nodeIds.size() + offset);

            TransactionTemplate tt = new TransactionTemplate(txManager);
            tt.executeWithoutResult(t -> {
            	processNewNodes(nodeIds);
            });
        } catch (Exception e) {
            logger.error("Failed to create node cache, ids: {}", nodeIds, e);
            errorCounter.incrementAndGet();
        }
        logger.debug("Finished cache for nodes {}-{}", offset + 1, nodeIds.size() + offset);
        synchronized (atomCounter) {
            int v = atomCounter.decrementAndGet();
            if (v == 0) {
                atomCounter.notify();
            }
        }
    }

    /**
     * Synchronizace záznamů v databázi.
     */
    private void syncCacheInternal() {
		ScrollableResults<Integer> uncachedNodes = nodeRepository.findUncachedNodes();

		List<Integer> batchNodeIds = new ArrayList<>(SYNC_BATCH_NODE_SIZE);
		int count = 0;
		while (uncachedNodes.next()) {
			Integer nodeId = uncachedNodes.get();

			batchNodeIds.add(nodeId);
			count++;
			if (count % SYNC_BATCH_NODE_SIZE == 0) {
				logger.info("Sestavuji JP " + (count - SYNC_BATCH_NODE_SIZE + 1) + "-" + count);

				processNewNodes(batchNodeIds);
				batchNodeIds.clear();
			}
		}
		// process remaining nodes
		if (batchNodeIds.size() > 0) {
			logger.info("Sestavuji JP " + ((count / SYNC_BATCH_NODE_SIZE) * SYNC_BATCH_NODE_SIZE + 1) + "-" + count);
			processNewNodes(batchNodeIds);
		}

		logger.info("Všechny JP jsou synchronizovány");
    }

	/**
	 * Safety guard for the cache row-existence invariant: every node written to
	 * the cache must have at least one active {@code arr_level}
	 * ({@code deleteChange IS NULL}).
	 *
	 * <p>
	 * Fails fast if any requested node has no active level, so a call path that
	 * wrongly tries to cache an invalid node is surfaced immediately rather than
	 * silently leaking the node into the Lucene index. The offending node IDs are
	 * logged together with the originating {@code context} to make the violating
	 * path easy to identify.
	 *
	 * @param context short identifier of the calling write path (for diagnostics)
	 * @param nodeIds nodes about to be written to the cache
	 */
	private void assertNodesHaveActiveLevel(final String context, final Collection<Integer> nodeIds) {
		if (nodeIds.isEmpty()) {
			return;
		}
		assertNodesHaveActiveLevel(context, nodeIds, levelRepository.findNodeIdsWithActiveLevel(nodeIds));
	}

	/**
	 * Variant reusing an already-fetched set of active node IDs, avoiding a second
	 * query when the caller has computed it (see {@link #createCachedNodes}).
	 *
	 * @param context       short identifier of the calling write path (for diagnostics)
	 * @param nodeIds       nodes about to be written to the cache
	 * @param activeNodeIds subset of {@code nodeIds} that have an active level
	 */
	private void assertNodesHaveActiveLevel(final String context,
	                                        final Collection<Integer> nodeIds,
	                                        final Set<Integer> activeNodeIds) {
		if (nodeIds.isEmpty() || activeNodeIds.containsAll(nodeIds)) {
			return;
		}
		List<Integer> invalidNodeIds = nodeIds.stream()
				.filter(id -> !activeNodeIds.contains(id))
				.distinct()
				.collect(Collectors.toList());
		logger.error("[{}] Refusing to cache {} node(s) without any active arr_level "
				+ "(deleteChange IS NULL). The cache row-existence invariant requires every "
				+ "cached node to have at least one active level (see NodeCacheService class "
				+ "documentation). Invalid nodeIds: {}", context, invalidNodeIds.size(), invalidNodeIds);
		throw new SystemException("Cannot cache node(s) without an active level", BaseCode.INVALID_STATE)
				.set("context", context)
				.set("invalidNodeIds", invalidNodeIds);
	}

	private void processNewNodes(List<Integer> nodeIds) {
		List<ArrCachedNode> cachedNodes = createCachedNodes(nodeIds);
		cachedNodeRepository.saveAll(cachedNodes);
		//flush a batch of updates and release memory:
		entityManager.flush();
		entityManager.clear();
	}

    /**
     * Vytvoření nových záznamů podle identifikátorů JP v aktuální podobně.
     *
     * @param nodeIds seznam identifikátorů JP
     * @return seznam cache databázových objektů
     */
    private List<ArrCachedNode> createCachedNodes(final List<Integer> nodeIds) {
        List<ArrCachedNode> result = new ArrayList<>(nodeIds.size());

        List<ArrNode> nodes = nodeRepository.findAllById(nodeIds);
        // Enforce the cache row-existence invariant: a cache row must never be built
        // for a node without an active arr_level, otherwise it would leak into the
        // Lucene index. Fails fast to surface any call path that violates this.
        Set<Integer> activeNodeIds = levelRepository.findNodeIdsWithActiveLevel(nodeIds);
        assertNodesHaveActiveLevel("createCachedNodes", nodeIds, activeNodeIds);
        Map<Integer, List<ArrDescItem>> nodeIdItems = createNodeDescItemMap(activeNodeIds);
        Map<Integer, List<ArrInhibitedItem>> nodeIdInhibitedItems = createNodeInhibitedItemMap(activeNodeIds);
        Map<Integer, List<ArrDaoLink>> nodeIdDaoLinks = createNodeDaoLinkMap(activeNodeIds);
        Map<Integer, List<ArrNodeExtension>> nodeIdNodeExtension = createNodeExtensionMap(activeNodeIds);

        for (ArrNode node : nodes) {
            Integer nodeId = node.getNodeId();

			// serialize node data
            CachedNode cn = new CachedNode(nodeId, node.getUuid(), node.getFundId());
            cn.setDescItems(nodeIdItems.get(nodeId));
            cn.setInhibitedItems(nodeIdInhibitedItems.get(nodeId));
            cn.setDaoLinks(nodeIdDaoLinks.get(nodeId));
            cn.setNodeExtensions(nodeIdNodeExtension.get(nodeId));
			String nodeData = serialize(cn);

			// prepare final object
			ArrCachedNode cachedNode = new ArrCachedNode();
			cachedNode.setNode(node);
			cachedNode.setData(nodeData);
            result.add(cachedNode);
        }

        return result;
    }

    /**
     * Refreshes the serialized content of the given cache rows from the current DB state.
     *
     * Only rows whose node still has an active level are refreshed and returned. A node
     * that lost its last active level keeps its (now stale) row until the caller deletes
     * it (e.g. FundLevelService's after-commit delete), so it is skipped here rather than
     * refreshed — refreshing it would leak the node into the Lucene index.
     *
     * @return the subset of {@code cachedNodes} that was refreshed (nodes with an active level)
     */
    private List<ArrCachedNode> updateCachedNodes(final List<ArrCachedNode> cachedNodes) {
        Map<Integer, ArrCachedNode> nodeCachedNodes = new HashMap<>();
        for (ArrCachedNode cachedNode : cachedNodes) {
            nodeCachedNodes.put(cachedNode.getNodeId(), cachedNode);
        }

        Set<Integer> activeNodeIds = levelRepository.findNodeIdsWithActiveLevel(nodeCachedNodes.keySet());

        List<ArrNode> nodes = nodeRepository.findAllById(activeNodeIds);
        if (activeNodeIds.size() != nodes.size()) {
            logger.error("Number of active nodes for update does not match the number of found nodes in DB! activeNodeIds: {}, found nodes: {}", activeNodeIds.size(), nodes.size());
            throw new SystemException("Number of nodes for update does not match the number of found nodes in DB!")
                    .set("activeNodeIdsSize", activeNodeIds.size())
                    .set("foundNodesSize", nodes.size());
        }
        Map<Integer, List<ArrDescItem>> nodeIdItems = createNodeDescItemMap(activeNodeIds);
        Map<Integer, List<ArrInhibitedItem>> nodeIdInhibitedItems = createNodeInhibitedItemMap(activeNodeIds);
        Map<Integer, List<ArrDaoLink>> nodeIdDaoLinks = createNodeDaoLinkMap(activeNodeIds);
        Map<Integer, List<ArrNodeExtension>> nodeIdNodeExtension = createNodeExtensionMap(activeNodeIds);

        List<ArrCachedNode> refreshed = new ArrayList<>(nodes.size());
        for (ArrNode node : nodes) {
            Integer nodeId = node.getNodeId();

            CachedNode cn = new CachedNode(nodeId, node.getUuid(), node.getFundId());
            cn.setDescItems(nodeIdItems.get(nodeId));
            cn.setInhibitedItems(nodeIdInhibitedItems.get(nodeId));
            cn.setDaoLinks(nodeIdDaoLinks.get(nodeId));
            cn.setNodeExtensions(nodeIdNodeExtension.get(nodeId));

            ArrCachedNode cachedNode = nodeCachedNodes.get(nodeId);
            cachedNode.setData(serialize(cn));
            refreshed.add(cachedNode);
        }

        return refreshed;
    }

    private Map<Integer, List<ArrNodeExtension>> createNodeExtensionMap(final Collection<Integer> nodeIds) {
        if (nodeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ArrNodeExtension> nodeExtensions = nodeExtensionRepository.findByNodeIdInAndDeleteChangeIsNull(nodeIds);

        Map<Integer, List<ArrNodeExtension>> nodeIdNodeExtension = new HashMap<>();
        for (ArrNodeExtension nodeExtension : nodeExtensions) {
            nodeExtension = HibernateUtils.unproxy(nodeExtension);
            List<ArrNodeExtension> links = nodeIdNodeExtension.get(nodeExtension.getNodeId());
            if (links == null) {
                links = new ArrayList<>();
                nodeIdNodeExtension.put(nodeExtension.getNodeId(), links);
            }
            links.add(nodeExtension);
        }
        return nodeIdNodeExtension;
    }

    private Map<Integer, List<ArrDaoLink>> createNodeDaoLinkMap(final Collection<Integer> nodeIds) {
        if (nodeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ArrDaoLink> daoLinks = daoLinkRepository.findByNodeIdsAndFetchDao(nodeIds);

        Map<Integer, List<ArrDaoLink>> nodeIdDaoLinks = new HashMap<>();
        for (ArrDaoLink daoLink : daoLinks) {
            daoLink = HibernateUtils.unproxy(daoLink);
            List<ArrDaoLink> links = nodeIdDaoLinks.get(daoLink.getNodeId());
            if (links == null) {
                links = new ArrayList<>();
                nodeIdDaoLinks.put(daoLink.getNodeId(), links);
            }
            links.add(daoLink);
        }
        return nodeIdDaoLinks;
    }

    private Map<Integer, List<ArrDescItem>> createNodeDescItemMap(final Collection<Integer> nodeIds) {
        if (nodeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ArrDescItem> descItems = descItemService.findByNodeIdsAndDeleteChangeIsNull(nodeIds);

        Map<Integer, List<ArrDescItem>> nodeIdItems = new HashMap<>();
        for (ArrDescItem descItem : descItems) {
            descItem = HibernateUtils.unproxy(descItem);
            descItem.setData(HibernateUtils.unproxy(descItem.getData()));
            List<ArrDescItem> items = nodeIdItems.get(descItem.getNodeId());
            if (items == null) {
                items = new ArrayList<>();
                nodeIdItems.put(descItem.getNodeId(), items);
            }
            items.add(descItem);
        }
        return nodeIdItems;
    }

    private Map<Integer, List<ArrInhibitedItem>> createNodeInhibitedItemMap(final Collection<Integer> nodeIds) {
        if (nodeIds.isEmpty()) {
            return Collections.emptyMap();
        }
    	List<ArrInhibitedItem> inhibitedItems = inhibitedItemRepository.findByNodeIdsAndDeleteChangeIsNull(nodeIds);
    	
    	Map<Integer, List<ArrInhibitedItem>> nodeIdItems = new HashMap<>();
    	for (ArrInhibitedItem inhibitedItem : inhibitedItems) {
    		inhibitedItem = HibernateUtils.unproxy(inhibitedItem);
            List<ArrInhibitedItem> items = nodeIdItems.get(inhibitedItem.getNodeId());
            if (items == null) {
                items = new ArrayList<>();
                nodeIdItems.put(inhibitedItem.getNodeId(), items);
            }
            items.add(inhibitedItem);
    	}
    	return nodeIdItems;
    }

    /**
     * Získání sestavené cachované JP.
     *
     * @param nodeId identifikátor JP
     * @return JP
     */
	private RestoredNode getNodeInternal(final Integer nodeId) {
        Validate.notNull(nodeId, "Identifikátor JP musí být vyplněn");
		ArrCachedNode cachedNode = cachedNodeRepository.findByNodeId(nodeId);
        if (cachedNode == null) {
            throw new ObjectNotFoundException("Node not found in cache", ArrangementCode.NODE_NOT_FOUND)
                    .set("id", nodeId);
        }
		RestoredNode result = deserialize(cachedNode);
        reloadCachedNodes(Collections.singletonList(result));
        return result;
    }

    /**
     * Získání sestavených cachovaných JP.
     *
     * @param nodeIds identifikátory JP
     * @return seznam JP
     */
	private Map<Integer, RestoredNode> getNodesInternal(final Collection<Integer> nodeIds) {
        List<ArrCachedNode> cachedNodes = cachedNodeRepository.findByNodeIdIn(nodeIds);
		Map<Integer, RestoredNode> result = new HashMap<>(cachedNodes.size());
        for (ArrCachedNode cachedNode : cachedNodes) {
			RestoredNode restoredNode = deserialize(cachedNode);
			result.put(cachedNode.getNodeId(), restoredNode);
        }
        reloadCachedNodes(result.values());
        return result;
    }

    /**
     * Získání sestavených cachovaných JP.
     *
     * @param nodeIds identifikátory JP
     * @return seznam JP
     */
    private Collection<RestoredNode> getRestoredNodesInternal(final Collection<ArrNode> nodes) {
        List<ArrCachedNode> cachedNodes = cachedNodeRepository.findByNodeIn(nodes);
        if (cachedNodes.size() != nodes.size()) {
            Collection<Integer> cachedNodeIds = cachedNodes.stream().map(i -> i.getNodeId()).collect(Collectors.toList());
            Collection<Integer> missingNodeIds = new ArrayList<>();
            nodes.forEach(i -> {
                if (!cachedNodeIds.contains(i.getNodeId())) {
                    missingNodeIds.add(i.getNodeId());
                }
            });
            throw new SystemException("Missing nodes data in cache").set("missingNodeIds", missingNodeIds);
        }
        Collection<RestoredNode> result = new ArrayList<>(cachedNodes.size());
        cachedNodes.forEach(i -> result.add(deserialize(i)));
        reloadCachedNodes(result);
        return result;
    }

    /**
     * Metoda projde předané JP a provede donačtené návazných entit.
     *
     * @param cachedNodes seznam JP, kterým se doplňují návazné entity
     */
	public void reloadCachedNodes(final Collection<RestoredNode> cachedNodes) {

		StaticDataProvider sdp = staticDataService.getData();
        RestoreAction ra = new RestoreAction(sdp, entityManager, structureDataRepository,
                accessPointRepository,
                fundFileRepository,
                daoRepository,
                nodeRepository,
                refTemplateRepository);
        ra.restore(cachedNodes);
    }

    /**
     * Uložení záznamů.
     *
     * @param cachedNodes
     *            seznam ukládaných objektů
     * @param flush
     *            Priznak, zda se ma provest flush tabulky
     */
    private void saveNodesInternal(final Collection<? extends CachedNode> cachedNodes, boolean flush) {
        Map<Integer, CachedNode> cachedNodeMap = new HashMap<>(cachedNodes.size());
        for (CachedNode cachedNode : cachedNodes) {
            cachedNodeMap.put(cachedNode.getNodeId(), cachedNode);
        }

        List<ArrCachedNode> records = cachedNodeRepository.findByNodeIdIn(cachedNodeMap.keySet());

        if (records.size() != cachedNodes.size()) {
            throw new SystemException("Počet ukládaných JP neodpovídá počtu nalezených v cache!")
            .set("saveCount", cachedNodes.size())
            .set("foundCount", records.size());
        }

        for (ArrCachedNode record : records) {
			String data = serialize(cachedNodeMap.get(record.getNodeId()));
			record.setData(data);
			cachedNodeRepository.save(record);
        }
        if (flush) {
            cachedNodeRepository.flush();
        }
    }

    /**
     * Serializace objektu.
     *
     * @param cachedNode serializovaný objekt
     * @return výsledek serializace
     */
    private String serialize(final CachedNode cachedNode) {
        return serialize(cachedNode, true);
    }

    /**
     * Serializace objektu.
     *
     * @param cachedNode serializovaný objekt
     * @return výsledek serializace
     */
    private String serialize(final CachedNode cachedNode, final boolean validate) {
        if (validate) {
            // Validate that node contains all required data
            cachedNode.validate();
        }
        try {
            return mapper.writeValueAsString(cachedNode);
        } catch (JsonProcessingException e) {
            throw new SystemException("Nastal problém při serializaci objektu", e);
        }
    }

    /**
	 * Deserializace objektu.
	 * 
	 * Metoda vrací přímo deserializovaný objekt bez obnovených propojení na související 
	 * objekty.
	 *
	 * @param cachedNode serializovaný objekt
	 * @return sestavený objekt
	 */
	public RestoredNode deserialize(final ArrCachedNode cachedNode) {
        try {
			RestoredNode restoredNode = mapper.readValue(cachedNode.getData(), RestoredNode.class);

			// restore node ref
            ArrNode node = cachedNode.getNode();
            List<ArrDaoLink> daoLinks = restoredNode.getDaoLinks();
            if (daoLinks != null) {
                daoLinks.forEach(daoLink -> {
                    daoLink.setNode(node);
                });
            }
			restoredNode.setNodeId(cachedNode.getNodeId());
            restoredNode.setNode(node);
			return restoredNode;
        } catch (IOException e) {
            throw new SystemException("Nastal problém při deserializaci objektu", e);
        }
    }

    /**
     * Třída pro serializaci potřebných objektů.
     */
    private static class InterfaceVisibilityChecker extends VisibilityChecker.Std {

        /**
         * Seznam tříd, které se můžou serializovat.
         */
        private final Set<Class> classes;

        public InterfaceVisibilityChecker(final Class<?>... clazzes) {
            super(JsonAutoDetect.Visibility.PUBLIC_ONLY);
            classes = new HashSet<>();
            Collections.addAll(classes, clazzes);
        }

        @Override
        public boolean isGetterVisible(Method m) {
            for (Class<?> aClass1 : classes) {
                if (aClass1.isAssignableFrom(m.getReturnType())) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean isGetterVisible(AnnotatedMethod m) {
            return isGetterVisible(m.getAnnotated());
        }

    }

	/**
	 * Registers a node in the cache as a new, empty entry.
	 *
	 * See {@link #addNodesToCache(Collection)} for the precondition and contract.
	 *
	 * @param node existing node (with an active level) to register in the cache
	 */
	@Transactional(value = TxType.MANDATORY)
	public void addNodeToCache(ArrNode node) {
        addNodesToCache(Collections.singletonList(node));
	}

	/**
	 * Registers nodes in the cache as new, empty entries.
	 *
	 * <p>
	 * Precondition: each node must already have an active {@code arr_level}
	 * ({@code deleteChange IS NULL}). This is enforced by
	 * {@link #assertNodesHaveActiveLevel} — callers must register a node only
	 * after its level has been created. The entries carry no content; items are
	 * filled in later via {@link #syncNodes}.
	 *
	 * @param nodes existing nodes (each with an active level) to register in the cache
	 * @return the created cache records
	 */
    @Transactional(value = TxType.MANDATORY)
    public List<ArrCachedNode> addNodesToCache(final Collection<ArrNode> nodes) {
        readLock.lock();
        try {
            List<Integer> nodeIds = nodes.stream().map(ArrNode::getNodeId).collect(Collectors.toList());
            // All callers register a node in the cache only after its level exists,
            // so the row-existence invariant must hold here.
            assertNodesHaveActiveLevel("addNodesToCache", nodeIds);

            List<ArrCachedNode> records = new ArrayList<>(nodes.size());

            for (ArrNode node : nodes) {
                // Node has to have valid nodeId
            	Objects.requireNonNull(node.getNodeId());

                CachedNode cachedNode = new CachedNode(node.getNodeId(), node.getUuid(), node.getFundId());
                String data = serialize(cachedNode, false);

                ArrCachedNode record = new ArrCachedNode();
                record.setNode(node);
                record.setData(data);
                records.add(record);
            }
            List<ArrCachedNode> result = cachedNodeRepository.saveAll(records);

            if (logger.isDebugEnabled()) {
                logger.debug("created nodes in cache - empty, ids: {}", nodeIds);
            }
            return result;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Flush repository
     */
    public void flushChanges() {
        cachedNodeRepository.flush();
    }
}
