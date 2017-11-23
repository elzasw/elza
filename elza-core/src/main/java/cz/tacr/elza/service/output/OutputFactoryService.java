package cz.tacr.elza.service.output;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.print.*;
import cz.tacr.elza.print.item.*;
import cz.tacr.elza.print.party.Institution;
import cz.tacr.elza.print.party.Party;
import cz.tacr.elza.print.party.PartyGroup;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.ItemService;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.RegistryService;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Factory pro vytvoření struktury pro výstupy
 *
 */

@Service
public class OutputFactoryService implements NodeLoader {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Map<Integer, Packet> packetMap = new HashMap<>();

    private Map<Integer, Node> nodeMap = new HashMap<>();

    private Map<Integer, ArrCalendarType> calendarTypes = new HashMap<>();

    public void reset() {
        packetMap.clear();
        nodeMap.clear();
    }

    @Autowired
    private OutputGeneratorWorkerFactory outputGeneratorFactory;

    @Autowired
    private CalendarTypeRepository calendarTypeRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ItemService itemService;

    @Autowired
    private OutputService outputService;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private NodeCacheService nodeCacheService;

    public OutputFactoryService() {
    }

    /**
     * Factory metoda pro vytvoření logické struktury Output struktury
     *
     * @param arrOutput databázová položka s definicí požadovaného výstupu
     * @return struktura pro použití v šablonách
     */
    public OutputImpl createOutput(final ArrOutput arrOutput) {
        reset();

        // naplnit output
        final OutputImpl output = outputGeneratorFactory.getOutput(arrOutput);
        output.setName(arrOutput.getOutputDefinition().getName());
        output.setInternal_code(arrOutput.getOutputDefinition().getInternalCode());
        output.setTypeCode(arrOutput.getOutputDefinition().getOutputType().getCode());
        output.setType(arrOutput.getOutputDefinition().getOutputType().getName());

        // fund
        final ArrFund arrFund = arrOutput.getOutputDefinition().getFund();
        ArrFundVersion arrFundVersion = arrangementService.getOpenVersionByFundId(arrFund.getFundId());
        final Fund fund = createFund(null, arrFund, arrFundVersion);
        output.setFund(fund);

        // zařadit do výstupu rootNode fundu
        final ArrNode arrFundRootNode = arrFundVersion.getRootNode();
        ArrLevel arrRootLevel = levelRepository.findNodeInRootTreeByNodeId(arrFundRootNode, arrFundRootNode, arrFundVersion.getLockChange());
        NodeId rootNodeId = createNodeId(arrRootLevel, output, null);
        fund.setRootNodeId(rootNodeId);

        // plnit institution
        final ParInstitution arrFundInstitution = arrFund.getInstitution();
        final Institution institution = createInstitution(arrFundInstitution, output);
        fund.setInstitution(institution);

        // zařadit items přímo přiřazené na output
        addOutputItems(arrOutput, output, arrFundVersion);

        // zařadit strom nodes
        createNodeIdTree(arrOutput, output);

        return output;
    }

    private void createNodeIdTree(final ArrOutput arrOutput, final OutputImpl output) {
        List<ArrNode> nodes = outputService.getNodesForOutput(arrOutput);
        nodes.sort((n1, n2) -> n1.getNodeId().compareTo(n2.getNodeId()));

        ArrChange lockChange = output.getArrFundVersion().getLockChange();
        ArrNode rootNode = nodeRepository.findOne(output.getFund().getRootNodeId().getArrNodeId());
        for (ArrNode arrNode : nodes) {
            output.addDirectNodeIdentifier(arrNode.getNodeId());

            ArrLevel arrLevel = levelRepository.findNodeInRootTreeByNodeId(arrNode, rootNode, lockChange);
            addParentNodeByNode(arrLevel, output);
        }
    }

    /**
     * Naplní do výstupu hodnoty atributů přiřazené na definici výstupu.
     */
    private void addOutputItems(final ArrOutput arrOutput, final OutputImpl output, final ArrFundVersion arrFundVersion) {
        final List<ArrOutputItem> outputItems = outputService.getOutputItemsInner(arrFundVersion, arrOutput.getOutputDefinition());
        for (ArrOutputItem arrOutputItem : outputItems) {
            final AbstractItem item = getItem(arrOutputItem.getItemId(), output);
            output.getItems().add(item);
        };
    }

    private Institution createInstitution(final ParInstitution arrFundInstitution, final OutputImpl output) {
        final Institution institution = new Institution();
        institution.setTypeCode(arrFundInstitution.getInstitutionType().getCode());
        institution.setType(arrFundInstitution.getInstitutionType().getName());
        institution.setCode(arrFundInstitution.getInternalCode());

        // partyGroup k instituci
        final ParParty parParty = arrFundInstitution.getParty();
        final Party party = output.getParty(parParty);

        // Check party type
        if(! (party instanceof PartyGroup)) {
            throw new IllegalStateException("Party for institution is not GROUP_PARTY, partyId = "+parParty.getPartyId());
        }
        // create party group
        final PartyGroup partyGroup = (PartyGroup)party;
        institution.setPartyGroup(partyGroup);

        return institution;
    }

    private Fund createFund(final NodeId rootNodeId, final ArrFund arrFund, final ArrFundVersion arrFundVersion) {
        Fund fund = new Fund(rootNodeId, arrFundVersion);
        fund.setName(arrFund.getName());
        fund.setCreateDate(Date.from(arrFund.getCreateDate().atZone(ZoneId.systemDefault()).toInstant()));
        fund.setDateRange(arrFundVersion.getDateRange());
        fund.setInternalCode(arrFund.getInternalCode());

        return fund;
    }

    /**
     * Metoda vytvoří strukturu nodů vč. nadřazených až k rootu a vč. stromu všech potomků
     * Ke každému node vytvoří i příslušné items.
     *
     * @param arrLevel node přímo přiřazený k outputu
     * @param output  výstup, ke kterému se budou nody zařazovat
     */
    private void addParentNodeByNode(final ArrLevel arrLevel, final OutputImpl output) {
        // získat seznam rodičů node a zařadit
        final ArrFundVersion arrFundVersion = output.getFund().getArrFundVersion();
        ArrNode arrNode = arrLevel.getNode();
        final List<ArrLevel> levelList = levelRepository.findAllParentsByNodeAndVersion(arrNode, arrFundVersion);
        levelList.sort((l1, l2) -> l1.getNode().getNodeId().compareTo(l2.getNode().getNodeId()));

        for (ArrLevel arrParentLevel : levelList) {
            ArrNode parentNode = arrParentLevel.getNodeParent();
            createNodeId(arrParentLevel, output, parentNode);
        }

        if (levelList.size() > 0) {
            // získat node vč potomků a atributů
            ArrNode parentNode = levelList.get(levelList.size() - 1).getNode();
            getNodeIdWithChildren(arrLevel, output, parentNode);
        } else {
            getNodeIdWithChildren(arrLevel, output, null);
        }
    }

    /**
     * Vytvoří node vč. celého stromu potomků.
     * Ke každému node vytvoří i příslušné items.
     *
     * @param arrLevel zdrojový level
     * @param output  výstup, ke kterému se budou nody zařazovat
     */
    private void getNodeIdWithChildren(final ArrLevel arrLevel, final OutputImpl output, final ArrNode parentNode) {
        ArrNode arrNode = arrLevel.getNode();
        createNodeId(arrLevel, output, parentNode);

        // získat children
        final ArrFundVersion arrFundVersion = output.getFund().getArrFundVersion();
        final ArrChange arrChange = arrFundVersion.getLockChange();

        final List<ArrLevel> allChildrenByNode = levelRepository.findByParentNode(arrNode, arrChange);
        for (ArrLevel arrChildLevel : allChildrenByNode) {
            getNodeIdWithChildren(arrChildLevel, output, arrNode);
        }
    }

    /**
     * Vytvoří {@link NodeId}.
     *
     * @param arrLevel    zdrojová úroveň
     * @param output      výstup, ke kterému se budou nody zařazovat
     * @param arrParentNode  nadřazený uzel
     * @return node vč. items
     */
    private NodeId createNodeId(final ArrLevel arrLevel, final OutputImpl output, final ArrNode arrParentNode) {
        NodeId parent = null;
        Integer depth = 1;
        if (arrParentNode != null) {
            parent = output.getNodeId(arrParentNode.getNodeId());
            depth = parent.getDepth() + 1;
        }
        Integer parentNodeId = null;
        if (arrParentNode != null) {
            parentNodeId = arrParentNode.getNodeId();
        }
        Integer nodeIdentifier = arrLevel.getNode().getNodeId();
        Integer position = arrLevel.getPosition();
        NodeId nodeId = outputGeneratorFactory.getNodeId(output, nodeIdentifier, parentNodeId, position, depth);

        nodeId = output.addNodeId(nodeId);
        if (parent != null) {
            parent.getChildren().add(nodeId);
        }
        return nodeId;
    }

    /**
     * @param nodeId ID Požadovaného node (odpovídá ID arrNode)
     * @param output output pod který node patří
     * @return Node pro tisk
     */
    public Node getNode(final NodeId nodeId, final OutputImpl output) {
        Node node = nodeMap.get(nodeId.getArrNodeId());
        if (node == null) {
            node = outputGeneratorFactory.getNode(nodeId, output);
            nodeMap.put(nodeId.getArrNodeId(), node);
        }
        return node;
    }

    /**
     * Vytvoří item podle zdrojového typu.
     *
     * @param arrItemId zdrojový item
     * @param output  výstup, ke kterému se budou items zařazovat
     * @return item
     */
    @Transactional(readOnly = true)
    public AbstractItem getItem(final Integer arrItemId, final OutputImpl output) {
        final ArrItem arrItem = itemService.loadDataById(arrItemId);

        AbstractItem item = createItem(output, arrItem);
        return item;
    }

    /**
     * Vytvoří item podle zdrojového typu.
     *
     * @param arrItem zdrojová item
     * @param output   výstup, ke kterému se budou items zařazovat
     * @param nodeId     node, ke kterému se budou nody zařazovat, pokud je null jde o itemy přiřazené přímo k output
     * @return item
     */
    private AbstractItem getItemByType(final OutputImpl output, final ArrItem arrItem) {
        final ArrData data = arrItem.getData();

        AbstractItem item;
        if (data == null) {
            item = new ItemString(data.toString());
        } else if (data instanceof ArrDataUnitid) {
            item = getItemUnitid((ArrDataUnitid) data);
        } else if (data instanceof ArrDataUnitdate) {
            item = getItemUnitdate((ArrDataUnitdate) data);
        } else if (data instanceof ArrDataText && arrItem.getItemType().getDataType().getCode().equals("TEXT")) {
            item = getItemUnitText((ArrDataText) data);
        } else if (data instanceof ArrDataString) {
            item = getItemUnitString((ArrDataString) data);
        } else if (data instanceof ArrDataRecordRef) {
            item = getItemUnitRecordRef(output, (ArrDataRecordRef) data);
        } else if (data instanceof ArrDataPartyRef) {
            item = getItemUnitPartyRef(output, (ArrDataPartyRef) data);
        } else if (data instanceof ArrDataPacketRef) {
            item = getItemUnitPacketRef((ArrDataPacketRef) data);
        } else if (data instanceof ArrDataJsonTable) {
            item = getItemUnitJsonTable(output, arrItem.getItemType(), (ArrDataJsonTable) data);
        } else if (data instanceof ArrDataInteger) {
            item = getItemUnitInteger((ArrDataInteger) data);
        } else if (data instanceof ArrDataText && arrItem.getItemType().getDataType().getCode().equals("FORMATTED_TEXT")) {
            item = getItemUnitFormatedText((ArrDataText) data);
        } else if (data instanceof ArrDataFileRef) {
            item = getItemFile((ArrDataFileRef) data);
        } else if (data instanceof ArrDataNull && arrItem.getItemType().getDataType().getCode().equals("ENUM")) {
            item = ItemEnum.newInstance();
        } else if (data instanceof ArrDataDecimal) {
            item = getItemUnitDecimal((ArrDataDecimal) data);
        } else if (data instanceof ArrDataCoordinates) {
            item = getItemUnitCoordinates((ArrDataCoordinates) data);
        } else {
            logger.warn("Neznámý datový typ hodnoty Item ({}) je zpracován jako string.", data.getClass().getName());
            item = new ItemString(data.toString());
        }

        item.setPosition(arrItem.getPosition());
        item.setUndefined(arrItem.isUndefined());

        return item;
    }

    private AbstractItem getItemFile(final ArrDataFileRef itemData) {
        final ArrFile arrFile = itemData.getFile();
        final ItemFile itemFile = new ItemFile(arrFile);
        itemFile.setFileId(arrFile.getFileId());
        itemFile.setName(arrFile.getName());
        itemFile.setFileName(arrFile.getFileName());
        itemFile.setFileSize(arrFile.getFileSize());
        itemFile.setMimeType(arrFile.getMimeType());
        itemFile.setPagesCount(arrFile.getPagesCount());

        return itemFile;
    }

    private AbstractItem getItemUnitString(final ArrDataString itemData) {
        return new ItemString(itemData.getValue());
    }

    private AbstractItem getItemUnitRecordRef(final OutputImpl output, final ArrDataRecordRef itemData) {
        Record record = output.getRecordFromCache(itemData.getRecordId());
        if(record == null) {
            RegRecord regRecord = itemData.getRecord();
            record = output.getRecord(regRecord);
        }
        return new ItemRecordRef(record);
    }

    private AbstractItem getItemUnitPartyRef(final OutputImpl output, final ArrDataPartyRef itemData) {
        Party party = output.getPartyFromCache(itemData.getPartyId());
        if(party==null) {
            final ParParty parParty = itemData.getParty();
            party = output.getParty(parParty);
        }
        return new ItemPartyRef(party);
    }

    private AbstractItem getItemUnitPacketRef(final ArrDataPacketRef itemData) {
        Packet packet = packetMap.get(itemData.getPacketId());
        if (packet == null) {
            final ArrPacket arrPacket = itemData.getPacket();
            packet = new Packet();
            RulPacketType packetType = arrPacket.getPacketType();
            if (packetType != null) {
                packet.setType(packetType.getName());
                packet.setTypeCode(packetType.getCode());
                packet.setTypeShortcut(packetType.getShortcut());
            }
            packet.setStorageNumber(arrPacket.getStorageNumber());
            packet.setState(arrPacket.getState().name());
            packetMap.put(arrPacket.getPacketId(), packet);
        }
        return new ItemPacketRef(packet);
    }

    private AbstractItem getItemUnitJsonTable(OutputImpl output, RulItemType rulItemType, final ArrDataJsonTable itemData) {
        ItemType itemType = output.getItemType(rulItemType);
        return new ItemJsonTable(itemType.getTableDefinition(), itemData.getValue());
    }

    private AbstractItem getItemUnitFormatedText(final ArrDataText itemData) {
        return new ItemText(itemData.getValue());
    }

    private AbstractItem getItemUnitInteger(final ArrDataInteger itemData) {
        return new ItemInteger(itemData.getValue());
    }

    private AbstractItem getItemUnitDecimal(final ArrDataDecimal itemData) {
        return new ItemDecimal(itemData.getValue());
    }

    private AbstractItem getItemUnitCoordinates(final ArrDataCoordinates itemData) {
        return new ItemCoordinates(itemData.getValue());
    }

    private AbstractItem getItemUnitText(final ArrDataText itemData) {
        return new ItemText(itemData.getValue());
    }

    private AbstractItem getItemUnitdate(final ArrDataUnitdate itemData) {
        // lazy initialization of calendar types
        if(calendarTypes.size()==0) {
            calendarTypeRepository.findAll().forEach(calendarType -> calendarTypes.put(calendarType.getCalendarTypeId(), calendarType));
        }
        ArrCalendarType calendarType = calendarTypes.get(itemData.getCalendarTypeId());
        UnitDate data = UnitDate.valueOf(itemData, calendarType);
        return new ItemUnitdate(data);
    }

    private AbstractItem getItemUnitid(final ArrDataUnitid itemData) {
        return new ItemUnitId(itemData.getValue());
    }

    /**
     * Načtení požadovaných uzlů (JP) společně s daty.
     *
     * @param output  výstup
     * @param nodeIds seznam identifikátorů uzlů, které načítáme
     * @return mapa - klíč identifikátor uzlu, hodnota uzel
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Integer, Node> loadNodes(final OutputImpl output, final Collection<NodeId> nodeIds) {
        Map<Integer, Node> mapNodes = nodeIds.stream().map(nodeId -> getNode(nodeId, output)).collect(Collectors.toMap(Node::getArrNodeId, Function.identity()));

        fillItems(output, nodeIds, mapNodes);
        fillRecords(output, nodeIds, mapNodes);

        return mapNodes;
    }

    /**
     * Načtení rejstříkových hesel k jednotkám popisu.
     *
     * @param output    output pod který node patří
     * @param nodeIds   seznam identifikátorů uzlů, které načítáme
     * @param mapNodes  mapa uzlů, do kterých ukládáme
     */
    private void fillRecords(final OutputImpl output, final Collection<NodeId> nodeIds, final Map<Integer, Node> mapNodes) {
        Map<Integer, List<RegRecord>> recordsByNode = registryService.findByNodes(mapNodes.keySet());
        for (NodeId nodeId : nodeIds) {
            int arrNodeId = nodeId.getArrNodeId();
            Node node = mapNodes.get(arrNodeId);

            // prepare list of records
            List<RegRecord> regRecords = recordsByNode.get(arrNodeId);
            List<Record> records;
            if (CollectionUtils.isEmpty(regRecords)) {
                records = Collections.<Record>emptyList();
            } else {
                records = regRecords.stream().map(regRecord -> output.getRecord(regRecord)).collect(Collectors.toList());
            }

            // store list
            node.setRecords(records);
        }
    }

    /**
     * Načtení hodnot atributu k jednotkám popisu.
     *
     * @param output   output pod který node patří
     * @param nodeIds  seznam identifikátorů uzlů, které načítáme
     * @param mapNodes mapa uzlů, do kterých ukládáme
     */
    private void fillItems(final OutputImpl output, final Collection<NodeId> nodeIds, final Map<Integer, Node> mapNodes) {
        Set<Integer> requestNodeIds = mapNodes.keySet();
        // request from cache
        Map<Integer, CachedNode> nodeData = nodeCacheService.getNodes(requestNodeIds);
        mapNodes.forEach((nodeId, node) -> {
            // find node in nodeData
            CachedNode cachedNode = nodeData.get(nodeId);
            fillNode(output, cachedNode, node);
        });
        // TODO: use old code to request nodes directly for non active version
        /*
        Map<Integer, List<ArrDescItem>> descItemsByNode = arrangementService.findByNodes();

        List<ArrDescItem> allDescItems = new LinkedList<>();
        for (List<ArrDescItem> items : descItemsByNode.values()) {
            allDescItems.addAll(items);
        }

        ObjectListIterator<ArrDescItem> iterator = new ObjectListIterator<>(allDescItems);

        while (iterator.hasNext()) {
            List<ArrDescItem> descItems = iterator.next();
            itemService.loadData(descItems);
        }

        for (NodeId nodeId : nodeIds) {
            int arrNodeId = nodeId.getArrNodeId();
            List<ArrDescItem> descItems = descItemsByNode.get(arrNodeId);

            List<Item> items;
            Node node = mapNodes.get(arrNodeId);
            if (descItems == null) {
                items = Collections.<Item>emptyList();
            } else {
                items = descItems.stream()
                        .map(arrDescItem -> {
                        	Item item = createItem(output, arrDescItem);

                            if (item instanceof ItemPacketRef) {
                            	if(node!=null) {
                            		item.getValue(Packet.class).addNode(node);
                            	}
                            }
                            return item;

                        }).collect(Collectors.toList());
                items.sort((i1,i2) -> (i1.compareToItemViewOrderPosition(i2)));
            }
            node.setItems(items);
        }*/
    }

    private void fillNode(OutputImpl output, CachedNode cachedNode, Node node) {
        List<ArrDescItem> descItems = cachedNode.getDescItems();
        List<Item> items;
        if (descItems == null) {
            items = Collections.<Item>emptyList();
        } else {
            items = descItems.stream()
                    .map(arrDescItem -> {
                        Item item = createItem(output, arrDescItem);

                        if (item instanceof ItemPacketRef) {
                            if(node!=null) {
                                item.getValue(Packet.class).addNode(node);
                            }
                        }
                        return item;

                    }).collect(Collectors.toList());
            items.sort((i1,i2) -> (i1.compareToItemViewOrderPosition(i2)));
        }
        node.setItems(items);
    }

    /**
     * Create description item for ouput
     * @param output
     * @param arrDescItem
     * @return Return item for output
     */
    private AbstractItem createItem(OutputImpl output, ArrItem arrDescItem) {
        AbstractItem item = getItemByType(output, arrDescItem);

        RulItemSpec rulItemSpec = arrDescItem.getItemSpec();
        if (rulItemSpec != null) {
            ItemSpec itemSpec = output.getItemSpec(rulItemSpec);
            item.setSpecification(itemSpec);
        }

        RulItemType rulItemType = arrDescItem.getItemType();
        ItemType itemType = output.getItemType(rulItemType);
        item.setType(itemType);
        return item;
    }


}
