package cz.tacr.elza.service.output;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrItemCoordinates;
import cz.tacr.elza.domain.ArrItemData;
import cz.tacr.elza.domain.ArrItemDecimal;
import cz.tacr.elza.domain.ArrItemEnum;
import cz.tacr.elza.domain.ArrItemFileRef;
import cz.tacr.elza.domain.ArrItemFormattedText;
import cz.tacr.elza.domain.ArrItemInt;
import cz.tacr.elza.domain.ArrItemJsonTable;
import cz.tacr.elza.domain.ArrItemPacketRef;
import cz.tacr.elza.domain.ArrItemPartyRef;
import cz.tacr.elza.domain.ArrItemRecordRef;
import cz.tacr.elza.domain.ArrItemString;
import cz.tacr.elza.domain.ArrItemText;
import cz.tacr.elza.domain.ArrItemUnitdate;
import cz.tacr.elza.domain.ArrItemUnitid;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.domain.ParEvent;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPacketType;
import cz.tacr.elza.print.Fund;
import cz.tacr.elza.print.Node;
import cz.tacr.elza.print.NodeId;
import cz.tacr.elza.print.NodeLoader;
import cz.tacr.elza.print.Output;
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
import cz.tacr.elza.print.item.ItemSpec;
import cz.tacr.elza.print.item.ItemString;
import cz.tacr.elza.print.item.ItemText;
import cz.tacr.elza.print.item.ItemType;
import cz.tacr.elza.print.item.ItemUnitId;
import cz.tacr.elza.print.item.ItemUnitdate;
import cz.tacr.elza.print.party.Dynasty;
import cz.tacr.elza.print.party.Event;
import cz.tacr.elza.print.party.Institution;
import cz.tacr.elza.print.party.Party;
import cz.tacr.elza.print.party.PartyGroup;
import cz.tacr.elza.print.party.PartyName;
import cz.tacr.elza.print.party.Person;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PartyNameRepository;
import cz.tacr.elza.service.ArrangementService;
import cz.tacr.elza.service.ItemService;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.RegistryService;
import cz.tacr.elza.utils.ObjectListIterator;
import cz.tacr.elza.utils.PartyType;
import cz.tacr.elza.utils.ProxyUtils;

/**
 * Factory pro vytvoření struktury pro výstupy
 *
 */

@Service
public class OutputFactoryService implements NodeLoader {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Map<Integer, Packet> packetMap = new HashMap<>();

    private Map<Integer, Node> nodeMap = new HashMap<>();
    
    public void reset() {
        packetMap.clear();        
        nodeMap.clear();
    }

    @Autowired
    private OutputGeneratorWorkerFactory outputGeneratorFactory;

    @Autowired
    private PartyNameRepository partyNameRepository;

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

    public OutputFactoryService() {
    }

    /**
     * Factory metoda pro vytvoření logické struktury Output struktury
     *
     * @param arrOutput databázová položka s definicí požadovaného výstupu
     * @return struktura pro použití v šablonách
     */
    public Output createOutput(final ArrOutput arrOutput) {
        reset();

        // naplnit output
        final Output output = outputGeneratorFactory.getOutput(arrOutput);
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

    private void createNodeIdTree(final ArrOutput arrOutput, final Output output) {
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
     * Naplní do výstupu hodnoty atributů přiřazené na definicivýstupu.
     */
    private void addOutputItems(final ArrOutput arrOutput, final Output output, final ArrFundVersion arrFundVersion) {
        final List<ArrOutputItem> outputItems = outputService.getOutputItemsInner(arrFundVersion, arrOutput.getOutputDefinition());
        for (ArrOutputItem arrOutputItem : outputItems) {
            final AbstractItem item = getItem(arrOutputItem.getItemId(), output, null);
            output.getItems().add(item);
        };
    }

    private Institution createInstitution(final ParInstitution arrFundInstitution, final Output output) {
        final Institution institution = new Institution();
        institution.setTypeCode(arrFundInstitution.getInstitutionType().getCode());
        institution.setType(arrFundInstitution.getInstitutionType().getName());
        institution.setCode(arrFundInstitution.getInternalCode());

        // partyGroup k instituci
        final ParParty parParty = arrFundInstitution.getParty();
        final Party party = createParty(parParty, output);

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
    private void addParentNodeByNode(final ArrLevel arrLevel, final Output output) {
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
    private void getNodeIdWithChildren(final ArrLevel arrLevel, final Output output, final ArrNode parentNode) {
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
    private NodeId createNodeId(final ArrLevel arrLevel, final Output output, final ArrNode arrParentNode) {
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
    public Node getNode(final NodeId nodeId, final Output output) {
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
     * @param nodeId    node, ke kterému se budou nody zařazovat, pokud je null jde o itemy přiřazené přímo k output
     * @return item
     */
    @Transactional(readOnly = true)
    public AbstractItem getItem(final Integer arrItemId, final Output output, final NodeId nodeId) {
        final ArrItem arrItem = itemService.loadDataById(arrItemId);
        final AbstractItem item = getItemByType(output, nodeId, arrItem);

        // založit itemSpec
        final RulItemSpec rulItemSpec = arrItem.getItemSpec();
        if (rulItemSpec != null) {
            final ItemSpec itemSpec = createItemSpec(rulItemSpec);
            item.setSpecification(itemSpec);
        }

        // založit itemType
        final RulItemType rulItemType = arrItem.getItemType();
        final ItemType itemType = createItemType(rulItemType);
        item.setType(itemType);

        return item;
    }

    private ItemType createItemType(final RulItemType rulItemType) {
        final ItemType itemType = new ItemType();
        itemType.setName(rulItemType.getName());
        itemType.setDataType(rulItemType.getDataType().getCode());
        itemType.setShortcut(rulItemType.getShortcut());
        itemType.setDescription(rulItemType.getDescription());
        itemType.setCode(rulItemType.getCode());
        itemType.setViewOrder(rulItemType.getViewOrder());
        return itemType;
    }

    private ItemSpec createItemSpec(final RulItemSpec rulItemSpec) {
        final ItemSpec itemSpec = new ItemSpec();
        itemSpec.setName(rulItemSpec.getName());
        itemSpec.setShortcut(rulItemSpec.getShortcut());
        itemSpec.setDescription(rulItemSpec.getDescription());
        itemSpec.setCode(rulItemSpec.getCode());
        return itemSpec;
    }

    /**
     * Vytvoří item podle zdrojového typu.
     *
     * @param arrItem zdrojová item
     * @param output   výstup, ke kterému se budou items zařazovat
     * @param nodeId     node, ke kterému se budou nody zařazovat, pokud je null jde o itemy přiřazené přímo k output
     * @return item
     */
    private AbstractItem getItemByType(final Output output, final NodeId nodeId, final ArrItem arrItem) {
        final ArrItemData itemData = arrItem.getItem();

        AbstractItem item;
        if (itemData instanceof ArrItemUnitid) {
            item = getItemUnitid(nodeId, (ArrItemUnitid) itemData);
        } else if (itemData instanceof ArrItemUnitdate) {
            item = getItemUnitdate(nodeId, (ArrItemUnitdate) itemData);
        } else if (itemData instanceof ArrItemText) {
            item = getItemUnitText(nodeId, (ArrItemText) itemData);
        } else if (itemData instanceof ArrItemString) {
            item = getItemUnitString(nodeId, (ArrItemString) itemData);
        } else if (itemData instanceof ArrItemRecordRef) {
            item = getItemUnitRecordRef(output, nodeId, (ArrItemRecordRef) itemData);
        } else if (itemData instanceof ArrItemPartyRef) {
            item = getItemUnitPartyRef(output, nodeId, (ArrItemPartyRef) itemData);
        } else if (itemData instanceof ArrItemPacketRef) {
            item = getItemUnitPacketRef(nodeId, (ArrItemPacketRef) itemData);
        } else if (itemData instanceof ArrItemJsonTable) {
            item = getItemUnitJsonTable(nodeId, (ArrItemJsonTable) itemData);
        } else if (itemData instanceof ArrItemInt) {
            item = getItemUnitInteger(nodeId, (ArrItemInt) itemData);
        } else if (itemData instanceof ArrItemFormattedText) {
            item = getItemUnitFormatedText(nodeId, (ArrItemFormattedText) itemData);
        } else if (itemData instanceof ArrItemFileRef) {
            item = getItemFile(nodeId, (ArrItemFileRef) itemData);
        } else if (itemData instanceof ArrItemEnum) {
            item = ItemEnum.newInstance(nodeId);
        } else if (itemData instanceof ArrItemDecimal) {
            item = getItemUnitDecimal(nodeId, (ArrItemDecimal) itemData);
        } else if (itemData instanceof ArrItemCoordinates) {
            item = getItemUnitCoordinates(nodeId, (ArrItemCoordinates) itemData);
        } else {
            logger.warn("Neznámý datový typ hodnoty Item ({}) je zpracován jako string.", itemData.getClass().getName());
            item = new ItemString(nodeId, itemData.toString());
        }

        item.setPosition(arrItem.getPosition());

        return item;
    }

    private AbstractItem getItemFile(final NodeId nodeId, final ArrItemFileRef itemData) {
        final ArrFile arrFile = itemData.getFile();
        final ItemFile itemFile = new ItemFile(nodeId, arrFile);
        itemFile.setName(arrFile.getName());
        itemFile.setFileName(arrFile.getFileName());
        itemFile.setFileSize(arrFile.getFileSize());
        itemFile.setMimeType(arrFile.getMimeType());
        itemFile.setPagesCount(arrFile.getPagesCount());

        return itemFile;
    }

    private AbstractItem getItemUnitString(final NodeId nodeId, final ArrItemString itemData) {
        return new ItemString(nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitRecordRef(final Output output, final NodeId nodeId, final ArrItemRecordRef itemData) {
    	RegRecord regRecord = itemData.getRecord();
        final Record record = Record.newInstance(output, regRecord);
        return new ItemRecordRef(nodeId, record);
    }

    private AbstractItem getItemUnitPartyRef(final Output output, final NodeId nodeId, final ArrItemPartyRef itemData) {
        final ParParty parParty = itemData.getParty();
        Party party = createParty(parParty, output);
        return new ItemPartyRef(nodeId, party);
    }
    
    private Party createParty(final ParParty parParty, final Output output)
    {
        String partyTypeCode = parParty.getPartyType().getCode();
        PartyType partyType = PartyType.getByCode(partyTypeCode);
        
        switch (partyType) {
            case DYNASTY:
                ParDynasty parDynasty = ProxyUtils.deproxy(parParty);
                return createDynasty(parDynasty, output);
            case EVENT:
                ParEvent parEvent = ProxyUtils.deproxy(parParty);
                return createEvent(parEvent, output);
            case PARTY_GROUP:
                ParPartyGroup parPartyGroup = ProxyUtils.deproxy(parParty);
                return createPartyGroup(parPartyGroup, output);
            case PERSON:
                ParPerson parPerson = ProxyUtils.deproxy(parParty);
                return createPerson(parPerson, output);
            default :
                throw new IllegalStateException("Neznámý typ osoby " + partyType.getCode());
        }    	
    }

    private Person createPerson(final ParPerson parPerson, final Output output) {
        Person person = new Person();
        fillCommonPartyAttributes(person, parPerson, output);

        return person;
    }

    private PartyGroup createPartyGroup(final ParPartyGroup parPartyGroup, final Output output) {
        PartyGroup partyGroup = new PartyGroup();
        fillCommonPartyAttributes(partyGroup, parPartyGroup, output);

        partyGroup.setFoundingNorm(parPartyGroup.getFoundingNorm());
        partyGroup.setOrganization(parPartyGroup.getOrganization());
        partyGroup.setScope(parPartyGroup.getScope());
        partyGroup.setScopeNorm(parPartyGroup.getScopeNorm());

        return partyGroup;
    }

    private Event createEvent(final ParEvent parEvent, final Output output) {
        Event event = new Event();
        fillCommonPartyAttributes(event, parEvent, output);

        return event;
    }

    private Dynasty createDynasty(final ParDynasty parDynasty, final Output output) {
        Dynasty dynasty = new Dynasty();
        fillCommonPartyAttributes(dynasty, parDynasty, output);

        dynasty.setGenealogy(parDynasty.getGenealogy());

        return dynasty;
    }

    private void fillCommonPartyAttributes(final Party party, final ParParty parParty, final Output output) {
        party.setPreferredName(PartyName.valueOf(parParty.getPreferredName()));
        partyNameRepository.findByParty(parParty).stream()
                .filter(parPartyName -> !parPartyName.getPartyNameId().equals(parParty.getPreferredName().getPartyNameId())) // kromě preferovaného jména
                .forEach(parPartyName -> party.getNames().add(PartyName.valueOf(parPartyName)));
        party.setHistory(parParty.getHistory());
        party.setSourceInformation(parParty.getSourceInformation());
        party.setCharacteristics(parParty.getCharacteristics());        
        party.setType(parParty.getPartyType().getName());
        party.setTypeCode(parParty.getPartyType().getCode());
        
        // Prepare corresponding record
        party.setRecord(Record.newInstance(output, parParty.getRecord()));
    }

    private AbstractItem getItemUnitPacketRef(final NodeId nodeId, final ArrItemPacketRef itemData) {
        final ArrPacket arrPacket = itemData.getPacket();
        Packet packet = packetMap.get(arrPacket.getPacketId());
        if (packet == null) {
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
        return new ItemPacketRef(nodeId, packet);
    }

    private AbstractItem getItemUnitJsonTable(final NodeId nodeId, final ArrItemJsonTable itemData) {
        return new ItemJsonTable(nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitFormatedText(final NodeId nodeId, final ArrItemFormattedText itemData) {
        return new ItemText(nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitInteger(final NodeId nodeId, final ArrItemInt itemData) {
        return new ItemInteger(nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitDecimal(final NodeId nodeId, final ArrItemDecimal itemData) {
        return new ItemDecimal(nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitCoordinates(final NodeId nodeId, final ArrItemCoordinates itemData) {
        return new ItemCoordinates(nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitText(final NodeId nodeId, final ArrItemText itemData) {
        return new ItemText(nodeId, itemData.getValue());
    }

    private AbstractItem getItemUnitdate(final NodeId nodeId, final ArrItemUnitdate itemData) {
    	UnitDate data = UnitDate.valueOf(itemData);
        return new ItemUnitdate(nodeId, data);
    }

    private AbstractItem getItemUnitid(final NodeId nodeId, final ArrItemUnitid itemData) {
        return new ItemUnitId(nodeId, itemData.getValue());
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
    public Map<Integer, Node> loadNodes(final Output output, final Collection<NodeId> nodeIds) {
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
    private void fillRecords(final Output output, final Collection<NodeId> nodeIds, final Map<Integer, Node> mapNodes) {
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
                records = regRecords.stream().map(regRecord -> Record.newInstance(output, regRecord)).collect(Collectors.toList());
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
    private void fillItems(final Output output, final Collection<NodeId> nodeIds, final Map<Integer, Node> mapNodes) {
        Map<Integer, List<ArrDescItem>> descItemsByNode = arrangementService.findByNodes(mapNodes.keySet());

        List<ArrDescItem> allDescItems = new LinkedList<>();
        for (List<ArrDescItem> items : descItemsByNode.values()) {
            allDescItems.addAll(items);
        }

        ObjectListIterator<ArrDescItem> iterator = new ObjectListIterator<>(allDescItems);

        while (iterator.hasNext()) {
            List<ArrDescItem> descItems = iterator.next();
            itemService.loadData(descItems);
        }

        Map<Integer, ItemType> itemTypeMap = new HashMap<>();
        Map<Integer, ItemSpec> itemSpecMap = new HashMap<>();

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
                            AbstractItem itemByType = getItemByType(output, nodeId, arrDescItem);

                            RulItemSpec rulItemSpec = arrDescItem.getItemSpec();
                            if (rulItemSpec != null) {
                                Integer itemSpecId = rulItemSpec.getItemSpecId();
                                ItemSpec itemSpec = itemSpecMap.get(itemSpecId);
                                if (itemSpec == null) {
                                    itemSpec = createItemSpec(rulItemSpec);
                                    itemSpecMap.put(itemSpecId, itemSpec);
                                }
                                itemByType.setSpecification(itemSpec);
                            }

                            RulItemType rulItemType = arrDescItem.getItemType();
                            Integer itemTypeId = rulItemType.getItemTypeId();
                            ItemType itemType = itemTypeMap.get(itemTypeId);
                            if (itemType == null) {
                                itemType = createItemType(rulItemType);
                                itemTypeMap.put(itemTypeId, itemType);
                            }
                            itemByType.setType(itemType);

                            if (itemByType instanceof ItemPacketRef) {
                                itemByType.getValue(Packet.class).addNode(node);
                            }

                            return itemByType;
                        }).collect(Collectors.toList());
            }
            items.sort((i1,i2) -> (i1.compareToItemViewOrderPosition(i2)));
            node.setItems(items);
        }
    }


}
