package cz.tacr.elza.service.cache;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUriRef;
import cz.tacr.elza.repository.*;
import org.apache.commons.collections4.CollectionUtils;
import org.castor.core.util.Assert;
import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Lists;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.table.ElzaRow;
import cz.tacr.elza.domain.table.ElzaTable;
import cz.tacr.elza.exception.ObjectNotFoundException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;

/**
 * Service for caching node related entities.
 *
 * sestavuje jednotný objekt {@link CachedNode}, který se při ukládání do DB
 * serializuje do JSON pro určení, co se má serializovat se využívá interface
 * {@link NodeCacheSerializable} + základní primitivní typy při spuštění
 * synchronizace {@link #syncCache()} je zamknuta cache pro čtení
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

    /*
    @Autowired
    private PartyNameComplementRepository partyNameComplementRepository;

    @Autowired
    private PartyNameRepository partyNameRepository;*/

    @Autowired
    private ApAccessPointRepository accessPointRepository;

    @Autowired
    private FundFileRepository fundFileRepository;

    @Autowired
    private DaoRepository daoRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private CachedNodeRepository cachedNodeRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
	private StaticDataService staticDataService;

    @Autowired
    private DataUriRefRepository dataUriRefRepository;

    @Autowired
    private ArrRefTemplateRepository refTemplateRepository;

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
            logger.debug(">syncNodes(nodeIds:" + nodeIds + ")");
            syncNodesInternal(nodeIds);
            logger.debug("<syncNodes(nodeIds:" + nodeIds + ")");
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
     * Získání sestavené cachované JP.
     *
     * @param nodeId identifikátor JP
     * @return JP
     */
	@Transactional(value = TxType.MANDATORY)
	public RestoredNode getNode(final Integer nodeId) {
        readLock.lock();
        try {
            logger.debug(">getNode(nodeId:" + nodeId + ")");
			RestoredNode nodeInternal = getNodeInternal(nodeId);
            logger.debug("<getNode(nodeId:" + nodeId + ")");
            return nodeInternal;
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
            logger.debug(">getNodes(nodeIds:" + nodeIds + ")");
			Map<Integer, RestoredNode> nodesInternal = getNodesInternal(nodeIds);
            logger.debug("<getNodes(nodeIds:" + nodeIds + ")");
            return nodesInternal;
        } finally {
            readLock.unlock();
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
            logger.debug(">saveNodes(" + cachedNodes + ")");
            saveNodesInternal(cachedNodes, flush);
            logger.debug("<saveNodes(" + cachedNodes + ")");
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
            logger.debug(">saveNode(" + cachedNode + ")");
            saveNodesInternal(Collections.singletonList(cachedNode), flush);
            logger.debug("<saveNode(" + cachedNode + ")");
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
            logger.debug(">deleteNodes(" + nodeIds + ")");
            deleteNodesInternal(nodeIds);
            logger.debug("<deleteNodes(" + nodeIds + ")");
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

        List<ArrCachedNode> cachedNodes = cachedNodeRepository.findByNodeIdIn(nodeIds);

        logger.debug("Synchronizace požadovaných JP: " + cachedNodes.size());
        int i = 0;
        for (List<ArrCachedNode> partCachedNodes : Lists.partition(cachedNodes, SYNC_BATCH_NODE_SIZE)) {
            i++;
            logger.debug("Sestavuji JP " + ((i - 1) * SYNC_BATCH_NODE_SIZE + 1) + "-" + ((i * SYNC_BATCH_NODE_SIZE) < nodeIds.size() ? (i * SYNC_BATCH_NODE_SIZE) : nodeIds.size()));
			List<ArrCachedNode> updatedNodes = updateCachedNodes(partCachedNodes);
			cachedNodeRepository.saveAll(updatedNodes);
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
                    ArrData data = descItem.getData();

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
                cachedNode.setData(serialize(result, false)); // není třeba validovat, protože použe mažeme odkaz na JP
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
        cachedNodeRepository.deleteByNodeIdIn(nodeIds);
        cachedNodeRepository.flush();
    }

    /**
     * Synchronizace záznamů v databázi.
     */
    private void syncCacheInternal() {
		ScrollableResults uncachedNodes = nodeRepository.findUncachedNodes();

		List<Integer> partNodeIds = new ArrayList<>(SYNC_BATCH_NODE_SIZE);
		int count = 0;
		while (uncachedNodes.next()) {
			Object obj = uncachedNodes.get(0);

			partNodeIds.add((Integer) obj);
			count++;
			if (count % SYNC_BATCH_NODE_SIZE == 0) {
				logger.info("Sestavuji JP " + (count - SYNC_BATCH_NODE_SIZE + 1) + "-" + count);

				processNewNodes(partNodeIds);
				partNodeIds.clear();

			}
		}
		// process remaining nodes
		if (partNodeIds.size() > 0) {
			logger.info("Sestavuji JP " + ((count / SYNC_BATCH_NODE_SIZE) * SYNC_BATCH_NODE_SIZE + 1) + "-" + count);
			processNewNodes(partNodeIds);
		}

		logger.info("Všechny JP jsou synchronizovány");
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
        Map<Integer, List<ArrDescItem>> nodeIdItems = createNodeDescItemMap(nodeIds);
        Map<Integer, List<ArrDaoLink>> nodeIdDaoLinks = createNodeDaoLinkMap(nodeIds);

        for (ArrNode node : nodes) {
            Integer nodeId = node.getNodeId();

			// serialize node data
            CachedNode cn = new CachedNode(nodeId, node.getUuid());
            cn.setDescItems(nodeIdItems.get(nodeId));
            cn.setDaoLinks(nodeIdDaoLinks.get(nodeId));
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
     *
     * @param cachedNodes
     * @return
     */
    private List<ArrCachedNode> updateCachedNodes(final List<ArrCachedNode> cachedNodes) {
        Map<Integer, ArrCachedNode> nodeCachedNodes = new HashMap<>();
        for (ArrCachedNode cachedNode : cachedNodes) {
            nodeCachedNodes.put(cachedNode.getNodeId(), cachedNode);
        }

        Set<Integer> nodeIds = nodeCachedNodes.keySet();
        List<ArrNode> nodes = nodeRepository.findAllById(nodeIds);
        Map<Integer, List<ArrDescItem>> nodeIdItems = createNodeDescItemMap(nodeIds);
        Map<Integer, List<ArrDaoLink>> nodeIdDaoLinks = createNodeDaoLinkMap(nodeIds);

        for (ArrNode node : nodes) {
            Integer nodeId = node.getNodeId();

            CachedNode cn = new CachedNode(nodeId, node.getUuid());
            cn.setDescItems(nodeIdItems.get(nodeId));
            cn.setDaoLinks(nodeIdDaoLinks.get(nodeId));

            String nodeData = serialize(cn);
            ArrCachedNode cachedNode = nodeCachedNodes.get(nodeId);
            cachedNode.setData(nodeData);
        }

        return cachedNodes;
    }

    private Map<Integer, List<ArrDaoLink>> createNodeDaoLinkMap(final Collection<Integer> nodeIds) {
        List<ArrDaoLink> daoLinks = daoLinkRepository.findByNodeIdInAndDeleteChangeIsNull(nodeIds);

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
        List<ArrDescItem> descItems = descItemRepository.findByNodeIdsAndDeleteChangeIsNull(nodeIds);

        Map<Integer, List<ArrDescItem>> nodeIdItems = new HashMap<>();
        for (ArrDescItem descItem : descItems) {
            descItem = HibernateUtils.unproxy(descItem);
            List<ArrDescItem> items = nodeIdItems.get(descItem.getNodeId());
            if (items == null) {
                items = new ArrayList<>();
                nodeIdItems.put(descItem.getNodeId(), items);
            }
            items.add(descItem);
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
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");
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
     * Metoda projde předané JP a provede donačtené návazných entit.
     *
     * @param cachedNodes seznam JP, kterým se doplňují návazné entity
     */
	private void reloadCachedNodes(final Collection<RestoredNode> cachedNodes) {

		StaticDataProvider sdp = staticDataService.getData();
        RestoreAction ra = new RestoreAction(sdp, entityManager, structureDataRepository,
                accessPointRepository,
                fundFileRepository,
                daoRepository,
                nodeRepository,
                dataUriRefRepository,
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
	 * @param cachedNode serializovaný objekt
	 * @return sestavený objekt
	 */
	private RestoredNode deserialize(final ArrCachedNode cachedNode) {
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
	 * Create empty node in cache
	 *
	 * Node is created without any data
	 *
	 * @param node
	 */
	@Transactional(value = TxType.MANDATORY)
	public void createEmptyNode(ArrNode node) {
		readLock.lock();
		try {
			logger.debug(">createNodes({})", node.getNodeId());
			createEmptyNodes(Collections.singletonList(node));
			logger.debug("<createNodes({})", node.getNodeId());
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Založení nových záznamů v cache pro JP.
	 *
	 * @param nodes seznam zakládaných objektů
	 */
	private void createEmptyNodes(final Collection<ArrNode> nodes) {
		List<ArrCachedNode> records = new ArrayList<>(nodes.size());

		for (ArrNode node : nodes) {
            CachedNode cachedNode = new CachedNode(node.getNodeId(), node.getUuid());
			String data = serialize(cachedNode);

			ArrCachedNode record = new ArrCachedNode();
			record.setNode(node);
			record.setData(data);
			records.add(record);
		}
		cachedNodeRepository.saveAll(records);
	}

    /**
     * Flush repository
     */
    public void flushChanges() {
        cachedNodeRepository.flush();
    }
}
