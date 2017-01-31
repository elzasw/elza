package cz.tacr.elza.service.cache;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.google.common.collect.Lists;
import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrItemPacketRef;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.repository.CachedNodeRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.NodeRegisterRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PacketRepository;
import cz.tacr.elza.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
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

/**
 * Serviska pro cachování dat jednotky popisu.
 * <p>
 * - sestavuje jednotný objekt {@link CachedNode}, který se při ukládání do DB serializuje do JSON
 * - pro určení, co se má serializovat se využívá interface {@link NodeCacheSerializable} + základní primitivní typy
 * - při spuštění synchronizace {@link #syncCache()} je zamknuta cache pro čtení
 *
 * @author Martin Šlapa
 * @since 26.01.2017
 */
@Service
public class NodeCacheService {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwl.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwl.writeLock();

    private final ObjectMapper mapper;

    private static final int SYNC_BATCH_NODE_SIZE = 1000;

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
    private NodeRepository nodeRepository;

    @Autowired
    private CachedNodeRepository cachedNodeRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private ItemService itemService;

    public NodeCacheService() {
        mapper = new ObjectMapper();
        mapper.setVisibility(new InterfaceVisibilityChecker(NodeCacheSerializable.class));
    }

    @Async("syncCacheTaskExecutor")
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

    private List<ArrCachedNode> createCachedNodes(final List<Integer> nodeIds) {
        List<ArrCachedNode> result = new ArrayList<>(nodeIds.size());

        List<ArrDescItem> descItems = descItemRepository.findByNodeIdsAndDeleteChangeIsNull(nodeIds);
        itemService.loadData(descItems);

        Map<Integer, List<ArrDescItem>> nodeIdItems = new HashMap<>();
        for (ArrDescItem descItem : descItems) {
            List<ArrDescItem> items = nodeIdItems.get(descItem.getNodeId());
            if (items == null) {
                items = new ArrayList<>();
                nodeIdItems.put(descItem.getNodeId(), items);
            }
            items.add(descItem);
        }


        List<ArrNodeRegister> nodeRegisters = nodeRegisterRepository.findByNodeIdInAndDeleteChangeIsNull(nodeIds);

        Map<Integer, List<ArrNodeRegister>> nodeIdNodeRegisters = new HashMap<>();
        for (ArrNodeRegister nodeRegister : nodeRegisters) {
            List<ArrNodeRegister> registers = nodeIdNodeRegisters.get(nodeRegister.getNodeId());
            if (registers == null) {
                registers = new ArrayList<>();
                nodeIdNodeRegisters.put(nodeRegister.getNodeId(), registers);
            }
            registers.add(nodeRegister);
        }

        List<ArrDaoLink> daoLinks = daoLinkRepository.findByNodeIdInAndDeleteChangeIsNull(nodeIds);

        Map<Integer, List<ArrDaoLink>> nodeIdDaoLinks = new HashMap<>();
        for (ArrDaoLink daoLink : daoLinks) {
            List<ArrDaoLink> links = nodeIdDaoLinks.get(daoLink.getNodeId());
            if (links == null) {
                links = new ArrayList<>();
                nodeIdDaoLinks.put(daoLink.getNodeId(), links);
            }
            links.add(daoLink);
        }

        for (Integer nodeId : nodeIds) {
            ArrCachedNode cachedNode = new ArrCachedNode();
            ArrNode node = nodeRepository.getOne(nodeId);
            cachedNode.setNode(node);
            CachedNode cn = new CachedNode(nodeId);
            cn.setDescItems(nodeIdItems.get(nodeId));
            cn.setNodeRegisters(nodeIdNodeRegisters.get(nodeId));
            cn.setDaoLinks(nodeIdDaoLinks.get(nodeId));
            cachedNode.setData(serialize(cn));
            result.add(cachedNode);
        }

        return result;
    }

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

    private CachedNode getNodeInternal(final Integer nodeId) {
        ArrCachedNode cachedNode = cachedNodeRepository.findOneByNodeId(nodeId);
        CachedNode result = deserialize(cachedNode.getData());
        reloadCachedNodes(Collections.singletonList(result));
        return result;
    }

    private Map<Integer, CachedNode> getNodesInternal(final Collection<Integer> nodeIds) {
        List<ArrCachedNode> cachedNodes = cachedNodeRepository.findByNodeIdIn(nodeIds);
        Map<Integer, CachedNode> result = new HashMap<>(cachedNodes.size());
        for (ArrCachedNode cachedNode : cachedNodes) {
            result.put(cachedNode.getNodeId(), deserialize(cachedNode.getData()));
        }
        reloadCachedNodes(result.values());
        return result;
    }

    private void reloadCachedNodes(final Collection<CachedNode> cachedNodes) {

        Map<ArrDescItem, Integer> itemTypesMap = new HashMap<>();
        Map<ArrDescItem, Integer> itemSpecsMap = new HashMap<>();

        Map<ArrDescItem, Integer> itemPacketsMap = new HashMap<>();

        for (CachedNode cachedNode : cachedNodes) {
            for (ArrDescItem descItem : cachedNode.getDescItems()) {
                if (descItem.getItemTypeId() != null) {
                    itemTypesMap.put(descItem, descItem.getItemTypeId());
                }
                if (descItem.getItemSpecId() != null) {
                    itemSpecsMap.put(descItem, descItem.getItemSpecId());
                }

                if (descItem.getItem() instanceof ArrItemPacketRef) {
                    itemPacketsMap.put(descItem, ((ArrItemPacketRef) descItem.getItem()).getPacketId());
                }

            }
        }

        fillRulItemTypes(itemTypesMap);
        fillRulItemSpecs(itemSpecsMap);
        fillArrPackets(itemPacketsMap);
    }

    private void fillArrPackets(final Map<ArrDescItem, Integer> itemPacketsMap) {
        List<ArrPacket> packets = packetRepository.findAll(itemPacketsMap.values());
        Map<Integer, ArrPacket> packetsMapFound = new HashMap<>();
        for (ArrPacket packet : packets) {
            packetsMapFound.put(packet.getPacketId(), packet);
        }

        for (Map.Entry<ArrDescItem, Integer> entry : itemPacketsMap.entrySet()) {
            ArrDescItem descItem = entry.getKey();
            ((ArrItemPacketRef) descItem.getItem()).setPacket(packetsMapFound.get(entry.getValue()));
        }
    }

    private void fillRulItemSpecs(final Map<ArrDescItem, Integer> itemSpecsMap) {
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

    private void fillRulItemTypes(final Map<ArrDescItem, Integer> itemTypesMap) {
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

    public void createNode(final Integer nodeId) {
        readLock.lock();
        try {
            logger.debug(">createNode(nodeId:" + nodeId + ")");
            createNodeInternal(nodeId);
            logger.debug("<createNode(nodeId:" + nodeId + ")");
        } finally {
            readLock.unlock();
        }
    }

    private void createNodeInternal(final Integer nodeId) {

    }

/*
    public CachedNode changeNode() {
        return null;
    }

    public void deleteNode() {

    }

    public CachedNode createAttribute() {
        return null;
    }

    public CachedNode changeAttribute() {
        return null;
    }

    public void deleteAttribute() {

    }

    public CachedNode changeRegisterLink() {
        return null;
    }

    public CachedNode changeDaoLink() {
        return null;
    }*/

    private String serialize(final CachedNode cachedNode) {
        try {
            return mapper.writeValueAsString(cachedNode);
        } catch (JsonProcessingException e) {
            throw new SystemException("Nastal problém při serializaci objektu", e);
        }
    }

    private CachedNode deserialize(final String data) {
        try {
            return mapper.readValue(data, CachedNode.class);
        } catch (IOException e) {
            throw new SystemException("Nastal problém při deserializaci objektu", e);
        }
    }

    public static class InterfaceVisibilityChecker extends VisibilityChecker.Std {

        private final Set<Class> classes;

        public InterfaceVisibilityChecker(Class<?>... clazzes) {
            super(JsonAutoDetect.Visibility.PUBLIC_ONLY);
            classes = new HashSet<>();
            Collections.addAll(classes, clazzes);
            classes.add(String.class);
            classes.add(Number.class);
            classes.add(Boolean.class);
            classes.add(Iterable.class);
        }

        @Override
        public boolean isGetterVisible(Method m) {
            for (Class aClass1 : classes) {
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
