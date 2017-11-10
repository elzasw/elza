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

import javax.transaction.Transactional;

import org.apache.commons.collections.CollectionUtils;
import org.castor.core.util.Assert;
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

import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDaoLink;
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
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.CachedNodeRepository;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DaoRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.FundFileRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.repository.PartyRepository;
import cz.tacr.elza.repository.RegRecordRepository;
import cz.tacr.elza.service.ItemService;
import cz.tacr.elza.utils.HibernateUtils;

/**
 * Serviska pro cachování dat jednotky popisu.
 *
 * * sestavuje jednotný objekt {@link CachedNode}, který se při ukládání do DB serializuje do JSON
 * * pro určení, co se má serializovat se využívá interface {@link NodeCacheSerializable} + základní primitivní typy
 * * při spuštění synchronizace {@link #syncCache()} je zamknuta cache pro čtení
 */
@Service
public class NodeCacheService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwl.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwl.writeLock();

    private final ObjectMapper mapper;

    /**
     * Maximální počet JP, které se mají dávkově zpracovávat pro synchronizaci.
     */
    private static final int SYNC_BATCH_NODE_SIZE = 800;

    @Autowired
    private ItemTypeRepository itemTypeRepository;

    @Autowired
    private ItemSpecRepository itemSpecRepository;

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
    private CalendarTypeRepository calendarTypeRepository;

    @Autowired
    private ItemService itemService;

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
    @Transactional
    public CachedNode getNode(final Integer nodeId) {
        readLock.lock();
        try {
            logger.debug(">getNode(nodeId:" + nodeId + ")");
            CachedNode nodeInternal = getNodeInternal(nodeId);
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
    @Transactional
    public Map<Integer, CachedNode> getNodes(final Collection<Integer> nodeIds) {
        readLock.lock();
        try {
            logger.debug(">getNodes(nodeIds:" + nodeIds + ")");
            Map<Integer, CachedNode> nodesInternal = getNodesInternal(nodeIds);
            logger.debug("<getNodes(nodeIds:" + nodeIds + ")");
            return nodesInternal;
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Založení nových záznamů v cache pro JP.
     *
     * @param cachedNodes seznam zakládaných objektů
     */
    @Transactional
    public void createNodes(final Collection<CachedNode> cachedNodes) {
        readLock.lock();
        try {
            logger.debug(">createNodes(" + cachedNodes + ")");
            createNodesInternal(cachedNodes);
            logger.debug("<createNodes(" + cachedNodes + ")");
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
            saveNodeInternal(cachedNode);
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
            cachedNodeRepository.save(updateCachedNodes(partCachedNodes));
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
        Map<Integer, List<Integer>> uncachedNodes = nodeRepository.findUncachedNodes();

        if (uncachedNodes.size() == 0) {
            logger.info("Všechny JP jsou synchronizovány");
            return;
        }

        for (Map.Entry<Integer, List<Integer>> entry : uncachedNodes.entrySet()) {
            Integer fundId = entry.getKey();
            List<Integer> nodeIds = entry.getValue();

            logger.info("Synchronizace AS(id=" + fundId + "), počet JP: " + nodeIds.size());
            int i = 0;
            for (List<Integer> partNodeIds : Lists.partition(nodeIds, SYNC_BATCH_NODE_SIZE)) {
                i++;
                logger.info("Sestavuji JP " + ((i - 1) * SYNC_BATCH_NODE_SIZE + 1) + "-" + ((i * SYNC_BATCH_NODE_SIZE) < nodeIds.size() ? (i * SYNC_BATCH_NODE_SIZE) : nodeIds.size()));

                List<ArrCachedNode> cachedNodes = createCachedNodes(partNodeIds);
                cachedNodeRepository.save(cachedNodes);
            }
        }
    }

    /**
     * Vytvoření nových záznamů podle identifikátorů JP v aktuální podobně.
     *
     * @param nodeIds seznam identifikátorů JP
     * @return seznam cache databázových objektů
     */
    private List<ArrCachedNode> createCachedNodes(final List<Integer> nodeIds) {
        List<ArrCachedNode> result = new ArrayList<>(nodeIds.size());

        Map<Integer, List<ArrDescItem>> nodeIdItems = createNodeDescItemMap(nodeIds);
        Map<Integer, List<ArrNodeRegister>> nodeIdNodeRegisters = createNodeNodeRegisterMap(nodeIds);
        Map<Integer, List<ArrDaoLink>> nodeIdDaoLinks = createNodeDaoLinkMap(nodeIds);

        for (Integer nodeId : nodeIds) {
            ArrCachedNode cachedNode = new ArrCachedNode();
            //ArrNode node = nodeRepository.getOne(nodeId);
            cachedNode.setNodeId(nodeId);
            CachedNode cn = new CachedNode(nodeId);
            cn.setDescItems(nodeIdItems.get(nodeId));
            cn.setNodeRegisters(nodeIdNodeRegisters.get(nodeId));
            cn.setDaoLinks(nodeIdDaoLinks.get(nodeId));
            cachedNode.setData(serialize(cn));
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
        Map<Integer, List<ArrDescItem>> nodeIdItems = createNodeDescItemMap(nodeIds);
        Map<Integer, List<ArrNodeRegister>> nodeIdNodeRegisters = createNodeNodeRegisterMap(nodeIds);
        Map<Integer, List<ArrDaoLink>> nodeIdDaoLinks = createNodeDaoLinkMap(nodeIds);

        for (Map.Entry<Integer, ArrCachedNode> entry : nodeCachedNodes.entrySet()) {
            Integer nodeId = entry.getKey();
            CachedNode cn = new CachedNode(nodeId);
            cn.setDescItems(nodeIdItems.get(nodeId));
            cn.setNodeRegisters(nodeIdNodeRegisters.get(nodeId));
            cn.setDaoLinks(nodeIdDaoLinks.get(nodeId));
            entry.getValue().setData(serialize(cn));
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
        //itemService.loadData(descItems);

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
    private CachedNode getNodeInternal(final Integer nodeId) {
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");
        ArrCachedNode cachedNode = cachedNodeRepository.findOneByNodeId(nodeId);
        CachedNode result = deserialize(cachedNode.getData());
        reloadCachedNodes(Collections.singletonList(result));
        return result;
    }

    /**
     * Získání sestavených cachovaných JP.
     *
     * @param nodeIds identifikátory JP
     * @return seznam JP
     */
    private Map<Integer, CachedNode> getNodesInternal(final Collection<Integer> nodeIds) {
        List<ArrCachedNode> cachedNodes = cachedNodeRepository.findByNodeIdIn(nodeIds);
        Map<Integer, CachedNode> result = new HashMap<>(cachedNodes.size());
        for (ArrCachedNode cachedNode : cachedNodes) {
            result.put(cachedNode.getNodeId(), deserialize(cachedNode.getData()));
        }
        reloadCachedNodes(result.values());
        return result;
    }

    /**
     * Metoda projde předané JP a provede donačtené návazných entit.
     *
     * @param cachedNodes seznam JP, kterým se doplňují návazné entity
     */
    private void reloadCachedNodes(final Collection<CachedNode> cachedNodes) {

        Map<ArrDescItem, Integer> itemTypesMap = new HashMap<>();
        Map<ArrDescItem, Integer> itemSpecsMap = new HashMap<>();
        Map<ArrDescItem, Integer> itemPacketsMap = new HashMap<>();
        Map<ArrDescItem, Integer> itemPartiesMap = new HashMap<>();
        Map<ArrDescItem, Integer> itemRecordsMap = new HashMap<>();
        Map<ArrDescItem, Integer> itemFilesMap = new HashMap<>();
        Map<ArrDescItem, Integer> itemUnitdateMap = new HashMap<>();
        Map<ArrDaoLink, Integer> daoLinksMap = new HashMap<>();
        Map<ArrNodeRegister, Integer> nodeRegistersMap = new HashMap<>();

        for (CachedNode cachedNode : cachedNodes) {
            ArrNode node = nodeRepository.getOne(cachedNode.getNodeId());
            if (CollectionUtils.isNotEmpty(cachedNode.getDescItems())) {
                for (ArrDescItem descItem : cachedNode.getDescItems()) {
                    descItem.setNode(node);

                    if (descItem.getItemTypeId() != null) {
                        itemTypesMap.put(descItem, descItem.getItemTypeId());
                    }

                    if (descItem.getItemSpecId() != null) {
                        itemSpecsMap.put(descItem, descItem.getItemSpecId());
                    }

                    if (descItem.getData() instanceof ArrDataPacketRef) {
                        itemPacketsMap.put(descItem, ((ArrDataPacketRef) descItem.getData()).getPacketId());
                    } else if (descItem.getData() instanceof ArrDataPartyRef) {
                        itemPartiesMap.put(descItem, ((ArrDataPartyRef) descItem.getData()).getPartyId());
                    } else if (descItem.getData() instanceof ArrDataRecordRef) {
                        itemRecordsMap.put(descItem, ((ArrDataRecordRef) descItem.getData()).getRecordId());
                    } else if (descItem.getData() instanceof ArrDataFileRef) {
                        itemFilesMap.put(descItem, ((ArrDataFileRef) descItem.getData()).getFileId());
                    } else if (descItem.getData() instanceof ArrDataUnitdate) {
                        itemUnitdateMap.put(descItem, ((ArrDataUnitdate) descItem.getData()).getCalendarTypeId());
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(cachedNode.getDaoLinks())) {
                for (ArrDaoLink daoLink : cachedNode.getDaoLinks()) {
                    daoLinksMap.put(daoLink, daoLink.getDaoId());
                }
            }
            if (CollectionUtils.isNotEmpty(cachedNode.getNodeRegisters())) {
                for (ArrNodeRegister nodeRegister : cachedNode.getNodeRegisters()) {
                    nodeRegistersMap.put(nodeRegister, nodeRegister.getRecordId());
                }
            }
        }

        fillRulItemTypes(itemTypesMap);
        fillRulItemSpecs(itemSpecsMap);
        fillArrPackets(itemPacketsMap);
        fillParParties(itemPartiesMap);
        fillRegRecords(itemRecordsMap);
        fillArrFiles(itemFilesMap);
        fillUnitdate(itemUnitdateMap);
        fillArrDaoLinks(daoLinksMap);
        fillArrNodeRegisters(nodeRegistersMap);
    }

    private void fillUnitdate(final Map<ArrDescItem, Integer> itemUnitdateMap) {
        if (itemUnitdateMap.size() == 0) {
            return;
        }
        List<ArrCalendarType> calendarTypes = calendarTypeRepository.findAll(itemUnitdateMap.values());
        Map<Integer, ArrCalendarType> calendarTypeMapFound = new HashMap<>();
        for (ArrCalendarType calendarType : calendarTypes) {
            calendarTypeMapFound.put(calendarType.getCalendarTypeId(), calendarType);
        }

        for (Map.Entry<ArrDescItem, Integer> entry : itemUnitdateMap.entrySet()) {
            ArrDescItem descItem = entry.getKey();
            ((ArrDataUnitdate) descItem.getData()).setCalendarType(calendarTypeMapFound.get(entry.getValue()));
        }
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
     * Vyplnění návazných entity {@link RulItemSpec}.
     *
     * @param itemSpecsMap mapa entit k vyplnění
     */
    private void fillRulItemSpecs(final Map<ArrDescItem, Integer> itemSpecsMap) {
        if (itemSpecsMap.size() == 0) {
            return;
        }
        List<RulItemSpec> itemSpecs = itemSpecRepository.findAll(itemSpecsMap.values());
        Map<Integer, RulItemSpec> itemSpecsMapFound = new HashMap<>();
        for (RulItemSpec itemSpec : itemSpecs) {
            itemSpecsMapFound.put(itemSpec.getItemSpecId(), itemSpec);
        }

        for (Map.Entry<ArrDescItem, Integer> entry : itemSpecsMap.entrySet()) {
            ArrDescItem descItem = entry.getKey();
            descItem.setItemSpec(itemSpecsMapFound.get(entry.getValue()));
        }
    }

    /**
     * Vyplnění návazných entity {@link RulItemType}.
     *
     * @param itemTypesMap mapa entit k vyplnění
     */
    private void fillRulItemTypes(final Map<ArrDescItem, Integer> itemTypesMap) {
        if (itemTypesMap.size() == 0) {
            return;
        }
        List<RulItemType> itemTypes = itemTypeRepository.findAll(itemTypesMap.values());
        Map<Integer, RulItemType> itemTypesMapFound = new HashMap<>();
        for (RulItemType itemType : itemTypes) {
            itemTypesMapFound.put(itemType.getItemTypeId(), itemType);
        }

        for (Map.Entry<ArrDescItem, Integer> entry : itemTypesMap.entrySet()) {
            ArrDescItem descItem = entry.getKey();
            descItem.setItemType(itemTypesMapFound.get(entry.getValue()));
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
            record.setData(serialize(cachedNodeMap.get(record.getNodeId())));
            cachedNodeRepository.saveAndFlush(record);
        }
    }


    /**
     * Uložení záznamu.
     *
     * @param cachedNode ukládaný objekt
     */
    private void saveNodeInternal(final CachedNode cachedNode) {
        saveNodesInternal(Collections.singletonList(cachedNode));
    }

    /**
     * Založení nových záznamů v cache pro JP.
     *
     * @param cachedNodes seznam zakládaných objektů
     */
    private void createNodesInternal(final Collection<CachedNode> cachedNodes) {
        List<ArrCachedNode> records = new ArrayList<>(cachedNodes.size());
        for (CachedNode cachedNode : cachedNodes) {
            ArrCachedNode record = new ArrCachedNode();
            record.setNodeId(cachedNode.getNodeId());
            record.setData(serialize(cachedNode));
            records.add(record);
        }
        cachedNodeRepository.save(records);
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
     * @param data serializovaný objekt
     * @return sestavený objekt
     */
    private CachedNode deserialize(final String data) {
        try {
            return mapper.readValue(data, CachedNode.class);
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

}
