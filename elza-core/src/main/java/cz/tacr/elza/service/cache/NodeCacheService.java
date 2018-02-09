package cz.tacr.elza.service.cache;

import java.io.IOException;
import java.lang.reflect.Method;
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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.castor.core.util.Assert;
import org.hibernate.ScrollableResults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.google.common.collect.Lists;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.core.data.RuleSystemProvider;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.CachedNodeRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegRecordRepository;

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
    private NodeRegisterRepository nodeRegisterRepository;

    @Autowired
    private DaoLinkRepository daoLinkRepository;

    @Autowired
    private PacketRepository packetRepository;

    @Autowired
    private PartyRepository partyRepository;

    @Autowired
    private RegRecordRepository regRecordRepository;

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

    public NodeCacheService() {
        mapper = new ObjectMapper();
        mapper.setVisibility(new InterfaceVisibilityChecker(NodeCacheSerializable.class,
                String.class, Number.class, Boolean.class, Iterable.class));
    }

    /**
     * Synchronizace záznamů v databázi.
     */
    @Transactional
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
            logger.debug(">syncNodes(nodeIds:" + nodeIds + ")");
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
     * @param cachedNodes seznam ukládaných objektů
     */
    @Transactional
    public void saveNodes(final Collection<CachedNode> cachedNodes) {
        readLock.lock();
        try {
            logger.debug(">saveNodes(" + cachedNodes + ")");
            saveNodesInternal(cachedNodes);
            logger.debug("<saveNodes(" + cachedNodes + ")");
        } finally {
            readLock.unlock();
        }
    }


    /**
     * Uložení záznamu.
     *
     * @param cachedNode ukládaný objekt
     */
    @Transactional
    public void saveNode(final CachedNode cachedNode) {
        readLock.lock();
        try {
            logger.debug(">saveNode(" + cachedNode + ")");
			saveNodesInternal(Collections.singletonList(cachedNode));
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
        List<ArrCachedNode> cachedNodes = cachedNodeRepository.findByNodeIdIn(nodeIds);

        logger.debug("Synchronizace požadovaných JP: " + cachedNodes.size());
        int i = 0;
        for (List<ArrCachedNode> partCachedNodes : Lists.partition(cachedNodes, SYNC_BATCH_NODE_SIZE)) {
            i++;
            logger.debug("Sestavuji JP " + ((i - 1) * SYNC_BATCH_NODE_SIZE + 1) + "-" + ((i * SYNC_BATCH_NODE_SIZE) < nodeIds.size() ? (i * SYNC_BATCH_NODE_SIZE) : nodeIds.size()));
			List<ArrCachedNode> updatedNodes = updateCachedNodes(partCachedNodes);
			cachedNodeRepository.save(updatedNodes);
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
		cachedNodeRepository.save(cachedNodes);
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

        List<ArrNode> nodes = nodeRepository.findAll(nodeIds);
        Map<Integer, List<ArrDescItem>> nodeIdItems = createNodeDescItemMap(nodeIds);
        Map<Integer, List<ArrNodeRegister>> nodeIdNodeRegisters = createNodeNodeRegisterMap(nodeIds);
        Map<Integer, List<ArrDaoLink>> nodeIdDaoLinks = createNodeDaoLinkMap(nodeIds);

        for (ArrNode node : nodes) {
            Integer nodeId = node.getNodeId();

			// serialize node data
            CachedNode cn = new CachedNode(nodeId, node.getUuid());
            cn.setDescItems(nodeIdItems.get(nodeId));
            cn.setNodeRegisters(nodeIdNodeRegisters.get(nodeId));
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
        List<ArrNode> nodes = nodeRepository.findAll(nodeIds);
        Map<Integer, List<ArrDescItem>> nodeIdItems = createNodeDescItemMap(nodeIds);
        Map<Integer, List<ArrNodeRegister>> nodeIdNodeRegisters = createNodeNodeRegisterMap(nodeIds);
        Map<Integer, List<ArrDaoLink>> nodeIdDaoLinks = createNodeDaoLinkMap(nodeIds);

        for (ArrNode node : nodes) {
            Integer nodeId = node.getNodeId();

            CachedNode cn = new CachedNode(nodeId, node.getUuid());
            cn.setDescItems(nodeIdItems.get(nodeId));
            cn.setNodeRegisters(nodeIdNodeRegisters.get(nodeId));
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

    private Map<Integer, List<ArrNodeRegister>> createNodeNodeRegisterMap(final Collection<Integer> nodeIds) {
        List<ArrNodeRegister> nodeRegisters = nodeRegisterRepository.findByNodeIdInAndDeleteChangeIsNull(nodeIds);

        Map<Integer, List<ArrNodeRegister>> nodeIdNodeRegisters = new HashMap<>();
        for (ArrNodeRegister nodeRegister : nodeRegisters) {
            nodeRegister = HibernateUtils.unproxy(nodeRegister);
            List<ArrNodeRegister> registers = nodeIdNodeRegisters.get(nodeRegister.getNodeId());
            if (registers == null) {
                registers = new ArrayList<>();
                nodeIdNodeRegisters.put(nodeRegister.getNodeId(), registers);
            }
            registers.add(nodeRegister);
        }
        return nodeIdNodeRegisters;
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
		RuleSystemProvider rsp = sdp.getRuleSystems();

        Map<ArrDescItem, Integer> itemPacketsMap = new HashMap<>();
        Map<ArrDescItem, Integer> itemPartiesMap = new HashMap<>();
        Map<ArrDescItem, Integer> itemRecordsMap = new HashMap<>();
        Map<ArrDescItem, Integer> itemFilesMap = new HashMap<>();
        Map<ArrDaoLink, Integer> daoLinksMap = new HashMap<>();
        Map<ArrNodeRegister, Integer> nodeRegistersMap = new HashMap<>();

		for (RestoredNode restoredNode : cachedNodes) {
			ArrNode node = restoredNode.getNode();
			if (CollectionUtils.isNotEmpty(restoredNode.getDescItems())) {
				for (ArrDescItem descItem : restoredNode.getDescItems()) {
					// set node
					descItem.setNode(node);

					loadDescItemType(descItem, rsp);

					ArrData data = descItem.getData();
					if (data != null) {
						// restore dataType
						if (data instanceof ArrDataPacketRef) {
							itemPacketsMap.put(descItem, ((ArrDataPacketRef) data).getPacketId());
						} else if (data instanceof ArrDataPartyRef) {
							itemPartiesMap.put(descItem, ((ArrDataPartyRef) data).getPartyId());
						} else if (data instanceof ArrDataRecordRef) {
							itemRecordsMap.put(descItem, ((ArrDataRecordRef) data).getRecordId());
						} else if (data instanceof ArrDataFileRef) {
							itemFilesMap.put(descItem, ((ArrDataFileRef) data).getFileId());
						} else if (data instanceof ArrDataUnitdate) {
							loadUnitdate((ArrDataUnitdate) data);
						}
					}
                }
            }
			if (CollectionUtils.isNotEmpty(restoredNode.getDaoLinks())) {
				for (ArrDaoLink daoLink : restoredNode.getDaoLinks()) {
                    daoLinksMap.put(daoLink, daoLink.getDaoId());
                }
            }
			if (CollectionUtils.isNotEmpty(restoredNode.getNodeRegisters())) {
				for (ArrNodeRegister nodeRegister : restoredNode.getNodeRegisters()) {
                    nodeRegistersMap.put(nodeRegister, nodeRegister.getRecordId());
                }
            }
        }

		fillArrPackets(itemPacketsMap);
        fillParParties(itemPartiesMap);
        fillRegRecords(itemRecordsMap);
        fillArrFiles(itemFilesMap);
        fillArrDaoLinks(daoLinksMap);
        fillArrNodeRegisters(nodeRegistersMap);
    }

	/**
	 * Load description item type from rule system provider
	 *
	 * @param descItem
	 * @param rsp
	 */
	private void loadDescItemType(ArrDescItem descItem, RuleSystemProvider rsp) {
		Validate.notNull(descItem.getItemTypeId());
		RuleSystemItemType itemType = rsp.getItemType(descItem.getItemTypeId());
		Validate.notNull(itemType);

		descItem.setItemType(itemType.getEntity());

		Integer itemSpecId = descItem.getItemSpecId();
		if (itemSpecId != null) {
			RulItemSpec itemSpec = itemType.getItemSpecById(itemSpecId);
			Validate.notNull(itemSpec);
			descItem.setItemSpec(itemSpec);
		}

		// Restore dataType
		ArrData data = descItem.getData();
		if (data != null) {
			loadDataType(data, itemType);
		}

	}

	/**
	 * Load data type and set it
	 *
	 * @param data
	 * @param itemType
	 */
	private void loadDataType(ArrData data, RuleSystemItemType itemType) {
		DataType dataType = itemType.getDataType();
		// check that item type match
		Validate.isTrue(dataType.getId() == data.getDataTypeId());

		data.setDataType(dataType.getEntity());
	}

	/**
	 * Load unit date fields
	 *
	 * Method sets proper calendar type.
	 *
	 * @param data
	 */
	private void loadUnitdate(ArrDataUnitdate data) {
		CalendarType calendarType = CalendarType.fromId(data.getCalendarTypeId());
		Validate.notNull(calendarType);
		data.setCalendarType(calendarType.getEntity());
	}

    /**
     * Vyplnění návazných entity {@link RegRecord}.
     *
     * @param nodeRegistersMap mapa entit k vyplnění
     */
    private void fillArrNodeRegisters(final Map<ArrNodeRegister, Integer> nodeRegistersMap) {
        if (nodeRegistersMap.size() == 0) {
            return;
        }
        List<RegRecord> records = regRecordRepository.findAll(nodeRegistersMap.values());
        Map<Integer, RegRecord> recordsMapFound = new HashMap<>();
        for (RegRecord record : records) {
            recordsMapFound.put(record.getRecordId(), record);
        }

        for (Map.Entry<ArrNodeRegister, Integer> entry : nodeRegistersMap.entrySet()) {
            ArrNodeRegister nodeRegister = entry.getKey();
            nodeRegister.setRecord(recordsMapFound.get(entry.getValue()));
        }
    }

    /**
     * Vyplnění návazných entity {@link ArrDao}.
     *
     * @param daoLinksMap mapa entit k vyplnění
     */
    private void fillArrDaoLinks(final Map<ArrDaoLink, Integer> daoLinksMap) {
        if (daoLinksMap.size() == 0) {
            return;
        }
        List<ArrDao> daos = daoRepository.findAll(daoLinksMap.values());
        Map<Integer, ArrDao> daosMapFound = new HashMap<>();
        for (ArrDao dao : daos) {
            daosMapFound.put(dao.getDaoId(), dao);
        }

        for (Map.Entry<ArrDaoLink, Integer> entry : daoLinksMap.entrySet()) {
            ArrDaoLink daoLink = entry.getKey();
            daoLink.setDao(daosMapFound.get(entry.getValue()));
        }
    }

    /**
     * Vyplnění návazných entity {@link ArrFile}.
     *
     * @param itemFilesMap mapa entit k vyplnění
     */
    private void fillArrFiles(final Map<ArrDescItem, Integer> itemFilesMap) {
        if (itemFilesMap.size() == 0) {
            return;
        }
        List<ArrFile> files = fundFileRepository.findAll(itemFilesMap.values());
        Map<Integer, ArrFile> itemFilesMapFound = new HashMap<>();
        for (ArrFile file : files) {
            itemFilesMapFound.put(file.getFileId(), file);
        }

        for (Map.Entry<ArrDescItem, Integer> entry : itemFilesMap.entrySet()) {
            ArrDescItem descItem = entry.getKey();
            ((ArrDataFileRef) descItem.getData()).setFile(itemFilesMapFound.get(entry.getValue()));
        }
    }


    /**
     * Vyplnění návazných entity {@link RegRecord}.
     *
     * @param itemRecordsMap mapa entit k vyplnění
     */
    private void fillRegRecords(final Map<ArrDescItem, Integer> itemRecordsMap) {
        if (itemRecordsMap.size() == 0) {
            return;
        }
        List<RegRecord> records = regRecordRepository.findAll(itemRecordsMap.values());
        Map<Integer, RegRecord> recordsMapFound = new HashMap<>();
        for (RegRecord record : records) {
            recordsMapFound.put(record.getRecordId(), record);
        }

        for (Map.Entry<ArrDescItem, Integer> entry : itemRecordsMap.entrySet()) {
            ArrDescItem descItem = entry.getKey();
            ((ArrDataRecordRef) descItem.getData()).setRecord(recordsMapFound.get(entry.getValue()));
        }
    }

    /**
     * Vyplnění návazných entity {@link ParParty}.
     *
     * @param itemPartiesMap mapa entit k vyplnění
     */
    private void fillParParties(final Map<ArrDescItem, Integer> itemPartiesMap) {
        if (itemPartiesMap.size() == 0) {
            return;
        }
        List<ParParty> parties = partyRepository.findAll(itemPartiesMap.values());
        Map<Integer, ParParty> partiesMapFound = new HashMap<>();
        for (ParParty party : parties) {
            partiesMapFound.put(party.getPartyId(), party);
        }

        for (Map.Entry<ArrDescItem, Integer> entry : itemPartiesMap.entrySet()) {
            ArrDescItem descItem = entry.getKey();
            ((ArrDataPartyRef) descItem.getData()).setParty(partiesMapFound.get(entry.getValue()));
        }
    }

    /**
     * Vyplnění návazných entity {@link ArrPacket}.
     *
     * @param itemPacketsMap mapa entit k vyplnění
     */
    private void fillArrPackets(final Map<ArrDescItem, Integer> itemPacketsMap) {
        if (itemPacketsMap.size() == 0) {
            return;
        }
        List<ArrPacket> packets = packetRepository.findAll(itemPacketsMap.values());
        Map<Integer, ArrPacket> packetsMapFound = new HashMap<>();
        for (ArrPacket packet : packets) {
            packetsMapFound.put(packet.getPacketId(), packet);
        }

        for (Map.Entry<ArrDescItem, Integer> entry : itemPacketsMap.entrySet()) {
            ArrDescItem descItem = entry.getKey();
            ((ArrDataPacketRef) descItem.getData()).setPacket(packetsMapFound.get(entry.getValue()));
        }
    }

    /**
     * Uložení záznamů.
     *
     * @param cachedNodes seznam ukládaných objektů
     */
    private void saveNodesInternal(final Collection<CachedNode> cachedNodes) {
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
		cachedNodeRepository.flush();
    }

    /**
     * Serializace objektu.
     *
     * @param cachedNode serializovaný objekt
     * @return výsledek serializace
     */
    private String serialize(final CachedNode cachedNode) {
        try {
            return mapper.writeValueAsString(cachedNode);
        } catch (JsonProcessingException e) {
            throw new SystemException("Nastal problém při serializaci objektu", e);
        }
    }

    /**
	 * Deserializace objektu.
	 *
	 * @param cachedNode.getData()
	 *            serializovaný objekt
	 * @return sestavený objekt
	 */
	private RestoredNode deserialize(final ArrCachedNode cachedNode) {
        try {
			RestoredNode restoredNode = mapper.readValue(cachedNode.getData(), RestoredNode.class);
			// restore node ref
			restoredNode.setNodeId(cachedNode.getNodeId());
			restoredNode.setNode(cachedNode.getNode());
            // Set descItems as empty collection
            if (restoredNode.getDescItems() == null) {
                restoredNode.setDescItems(Collections.emptyList());
            }
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
	 * @param cachedNodes
	 *            seznam zakládaných objektů
	 */
	private void createEmptyNodes(final Collection<ArrNode> nodes) {
		List<ArrCachedNode> records = new ArrayList<>(nodes.size());

		for (ArrNode node : nodes) {
			CachedNode cachedNode = new CachedNode();
			String data = serialize(cachedNode);

			ArrCachedNode record = new ArrCachedNode();
			record.setNode(node);
			record.setData(data);
			records.add(record);
		}
		cachedNodeRepository.save(records);
	}
}
