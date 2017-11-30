package cz.tacr.elza.service.output.dev;

import java.io.InputStream;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrCalendarType;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
import cz.tacr.elza.domain.ArrDataNull;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDataString;
import cz.tacr.elza.domain.ArrDataText;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDataUnitid;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.domain.RulTemplate;
import cz.tacr.elza.exception.ProcessException;
import cz.tacr.elza.print.Fund;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.OutputImpl;
import cz.tacr.elza.print.Packet;
import cz.tacr.elza.print.Record;
import cz.tacr.elza.print.UnitDate;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemCoordinates;
import cz.tacr.elza.print.item.ItemDecimal;
import cz.tacr.elza.print.item.ItemEnum;
import cz.tacr.elza.print.item.ItemFile;
import cz.tacr.elza.print.item.ItemInteger;
import cz.tacr.elza.print.item.ItemJsonTable;
import cz.tacr.elza.print.item.ItemPacketRef;
import cz.tacr.elza.print.item.ItemPartyRef;
import cz.tacr.elza.print.item.ItemRecordRef;
import cz.tacr.elza.print.item.ItemString;
import cz.tacr.elza.print.item.ItemText;
import cz.tacr.elza.print.item.ItemType;
import cz.tacr.elza.print.item.ItemUnitId;
import cz.tacr.elza.print.item.ItemUnitdate;
import cz.tacr.elza.print.party.Institution;
import cz.tacr.elza.print.party.Party;
import cz.tacr.elza.print.party.PartyGroup;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.ItemRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.RegistryService;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;
import cz.tacr.elza.service.output.OutputGeneratorWorkerFactory;
import cz.tacr.elza.utils.HibernateUtils;

public class ModelOutputGenerator implements OutputGenerator {

    private final ArrOutputDefinition outputDefinition;

    private final OutputImpl outputModel;

    @Autowired
    private StaticDataService staticDataService;

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
    private ItemRepository itemRepository;

    @Autowired
    private OutputService outputService;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private RegistryService registryService;

    @Autowired
    private NodeCacheService nodeCacheService;

    @Override
    public void init(ArrOutputDefinition outputDefinition, int outputId) {
        this.outputDefinition = outputDefinition;
        this.outputModel = createOutputModel(outputId);
    }

    @Override
    public void generate() {
        logger.info("Spuštěno generování výstupu pro arr_output id={}", arrOutputId);

        final ArrOutput arrOutput = outputRepository.findOne(arrOutputId);
        final ArrOutputDefinition arrOutputDefinition = outputDefinitionRepository.findByOutputId(arrOutput.getOutputId());
        change = createChange(userId);

        try {
            final RulTemplate rulTemplate = arrOutputDefinition.getTemplate();
            Assert.notNull(rulTemplate, "Výstup nemá definovanou šablonu (ArrOutputDefinition.template je null).");

            // sestavení outputu
            logger.info("Sestavování modelu výstupu výstupu pro arr_output id={} spuštěno", arrOutputId);
            final OutputImpl output = outputFactoryService.createOutput(arrOutput);
            logger.info("Sestavování modelu výstupu výstupu pro arr_output id={} dokončeno", arrOutputId);

            // skutečné vytvoření výstupného souboru na základě definice
            logger.info("Spuštěno generování souboru pro arr_output id={}", arrOutputId);
            final InputStream content = getContent(arrOutputDefinition, rulTemplate, output);

            // Uložení do výstupní struktury a DMS
            storeOutputInDms(arrOutputDefinition, rulTemplate, content);

            content.close();

            waitForGeneratorThread();

            if (exception != null) {
                throw exception;
            }

            arrOutputDefinition.setError(null);
        } catch (Throwable ex) {
            throw new ProcessException(arrOutputId, ex);
        }

    }

    public OutputImpl createOutputModel(int outputId) {
        // init common
        OutputImpl model = new OutputImpl(outputId);
        model.setName(outputDefinition.getName());
        model.setInternal_code(outputDefinition.getInternalCode());
        model.setTypeCode(outputDefinition.getOutputType().getCode());
        model.setType(outputDefinition.getOutputType().getName());

        // create root node id
        ArrFund arrFund = outputDefinition.getFund();
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(arrFund.getFundId());
        ArrLevel rootLevel = levelRepository.findByNode(fundVersion.getRootNode(), fundVersion.getLockChange());
        NodeId rootNodeId = createNodeId(rootLevel, model, null);

        // init fund
        Fund fund = new Fund(rootNodeId, fundVersion);
        fund.setName(arrFund.getName());
        fund.setCreateDate(Date.from(arrFund.getCreateDate().atZone(ZoneId.systemDefault()).toInstant()));
        fund.setDateRange(fundVersion.getDateRange());
        fund.setInternalCode(arrFund.getInternalCode());
        model.setFund(fund);

        // init institution
        ParInstitution parInstitution = arrFund.getInstitution();
        Institution institution = createInstitution(parInstitution, model);
        fund.setInstitution(institution);

        // init output items
        readOutputItems(model, fundVersion);

        // zařadit strom nodes
        createNodeIdTree(arrOutput, model);

        return model;
    }

    private void readOutputItems(OutputImpl outputModel, ArrFundVersion fundVersion) {
        List<ArrOutputItem> outputItems = outputService.getOutputItemsInner(fundVersion, outputDefinition);
        for (ArrOutputItem outputItem : outputItems) {
            if (outputItem.isUndefined()) {
                continue;
            }
            AbstractItem item = createItem(outputModel, outputItem);
            outputModel.addItem(item);
        }
    }

    private AbstractItem createItem(OutputImpl outputModel, ArrItem arrItem) {
        AbstractItem item = getItemByType(outputModel, arrItem);

        RulItemSpec rulItemSpec = arrItem.getItemSpec(); // TODO: static data
        if (rulItemSpec != null) {
            ItemSpec itemSpec = outputModel.getItemSpec(rulItemSpec);
            item.setSpecification(itemSpec);
        }

        RulItemType rulItemType = arrItem.getItemType(); // TODO: static data
        ItemType itemType = outputModel.getItemType(rulItemType);
        item.setType(itemType);
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
        Validate.isTrue(HibernateUtils.isInitialized(data));


        switch() {

        }

        AbstractItem item;
        if (data instanceof ArrDataUnitid) {
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

    private void createNodeIdTree(final ArrOutput arrOutput, final OutputImpl output) {
        List<ArrNode> nodes = outputService.getNodesForOutput(arrOutput);
        // TODO: incorrect node order
        nodes.sort((n1, n2) -> n1.getNodeId().compareTo(n2.getNodeId()));

        ArrChange lockChange = output.getArrFundVersion().getLockChange();
        for (ArrNode arrNode : nodes) {
            output.addDirectNodeIdentifier(arrNode.getNodeId());

            ArrLevel arrLevel = levelRepository.findByNode(arrNode, lockChange);
            addParentNodeByNode(arrLevel, output);
        }
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

    /**
     * Metoda vytvoří strukturu nodů vč. nadřazených až k rootu a vč. stromu všech potomků
     * Ke každému node vytvoří i příslušné items.
     *
     * @param arrLevel node přímo přiřazený k outputu
     * @param output  výstup, ke kterému se budou nody zařazovat
     */
    private void addParentNodeByNode(final ArrLevel arrLevel, final OutputImpl output) {
        // získat seznam rodičů node a zařadit
        final ArrChange lockChange = output.getArrFundVersion().getLockChange();
        final List<ArrLevel> levelList = levelRepository.findAllParentsByNodeId(arrLevel.getNodeId(), lockChange, true);

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
        NodeId nodeId = new NodeId(output, nodeIdentifier, parentNodeId, position, depth);

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
            node = new Node(nodeId, output);
            nodeMap.put(nodeId.getArrNodeId(), node);
        }
        return node;
    }



    private AbstractItem getItemFile(final ArrDataFileRef itemData) {
        final ArrFile arrFile = itemData.getFile();
        final ItemFile itemFile = new ItemFile(arrFile);
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
        if (record == null) {
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
        Map<Integer, RestoredNode> nodeData = nodeCacheService.getNodes(requestNodeIds);
        mapNodes.forEach((nodeId, node) -> {
            // find node in nodeData
            RestoredNode cachedNode = nodeData.get(nodeId);
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
                    // docasne reseni pro nereportovani nedefinovanych poli
                    .filter(arrDescItem -> !arrDescItem.isUndefined())
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
}
