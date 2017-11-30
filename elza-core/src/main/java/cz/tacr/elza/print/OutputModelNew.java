package cz.tacr.elza.print;

import java.time.ZoneId;
import java.util.ArrayList;
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
import org.apache.commons.lang3.Validate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.ibm.wsdl.OutputImpl;

import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.core.data.RuleSystemProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.tree.FundTree;
import cz.tacr.elza.core.tree.FundTreeProvider;
import cz.tacr.elza.core.tree.TreeNode;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataCoordinates;
import cz.tacr.elza.domain.ArrDataDecimal;
import cz.tacr.elza.domain.ArrDataFileRef;
import cz.tacr.elza.domain.ArrDataInteger;
import cz.tacr.elza.domain.ArrDataJsonTable;
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
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputItem;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.ParDynasty;
import cz.tacr.elza.domain.ParEvent;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.ParInstitutionType;
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.item.AbstractItem;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemCoordinates;
import cz.tacr.elza.print.item.ItemDecimal;
import cz.tacr.elza.print.item.ItemEnum;
import cz.tacr.elza.print.item.ItemFileRef;
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
import cz.tacr.elza.print.party.PartyInitHelper;
import cz.tacr.elza.print.party.Person;
import cz.tacr.elza.print.party.Relation;
import cz.tacr.elza.print.party.RelationTo;
import cz.tacr.elza.print.party.RelationToType;
import cz.tacr.elza.print.party.RelationType;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.RestoredNode;
import cz.tacr.elza.utils.HibernateUtils;

public class OutputModelNew implements Output {

    private final List<Item> outputItems = new ArrayList<>();

    /* general description */

    private String name;

    private String internalCode;

    private String type;

    private String typeCode;

    private Fund fund;

    /* lookups */

    private final Map<Integer, ItemType> itemTypeIdMap = new HashMap<>();

    private final Map<Integer, ItemSpec> itemSpecIdMap = new HashMap<>();

    private final Map<Integer, Record> apIdMap = new HashMap<>();

    private final Map<Integer, RecordType> apTypeIdMap = new HashMap<>();

    private final Map<Integer, Party> partyIdMap = new HashMap<>();

    private final Map<Integer, RelationType> relationTypeIdMap = new HashMap<>();

    private final Map<Integer, RelationToType> relationRoleTypeIdMap = new HashMap<>();

    private final Map<Integer, Packet> packetIdMap = new HashMap<>();

    private final Map<Integer, File> fileIdMap = new HashMap<>();

    /* spring components */

    private final StaticDataService staticDataService;

    private final OutputService outputService;

    private final FundTreeProvider fundTreeProvider;

    public OutputModelNew(StaticDataService staticDataService,
                          OutputService outputService,
                          FundTreeProvider fundTreeProvider,
                          OutputDefinitionRepository outputDefinitionRepository) {
        this.staticDataService = staticDataService;
        this.outputService = outputService;
        this.fundTreeProvider = fundTreeProvider;
    }

    @Override
    public Fund getFund() {
        return fund;
    }

    @Override
    public String getInternalCode() {
        return internalCode;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String getTypeCode() {
        return typeCode;
    }

    @Override
    public List<Item> getItems(Collection<String> codes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Party> getParties(Collection<String> codes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Item getSingleItem(String itemTypeCode) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getSingleItemValue(String itemTypeCode) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Item> getAllItems(Collection<String> codes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Item> getItems() {
        return outputItems;
    }

    @Override
    public IteratorNodes getNodesDFS() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FilteredRecords getRecordsByType(String code) {
        // TODO Auto-generated method stub
        return null;
    }

    public void loadNodes() {

    }

    /**
     * Initializes output model. Must be called during transaction.
     */
    public void init(int outputDefinitionId, ArrFundVersion fundVersion) {
        Validate.isTrue(TransactionSynchronizationManager.isActualTransactionActive());
        Validate.isTrue(fund == null); // check if not initialized

        ArrOutputDefinition outputDefinition = outputService.getDefinitionForModel(outputDefinitionId);
        if (outputDefinition == null) {
            throw new SystemException("Output definition not found", BaseCode.ID_NOT_EXIST).set("outputDefinitionId",
                    outputDefinitionId);
        }

        ArrFund fund = outputDefinition.getFund();
        if (fundVersion != null) {
            Validate.isTrue(fundVersion.getFundId().equals(fund.getFundId()));
        }

        init(outputDefinition, fundVersion);
    }

    private void init(ArrOutputDefinition outputDefinition, ArrFundVersion fundVersion) {
        // init general
        name = outputDefinition.getName();
        internalCode = outputDefinition.getInternalCode();
        RulOutputType outputType = outputDefinition.getOutputType();
        typeCode = outputType.getCode();
        type = outputType.getName();

        // init node tree (without data)
        NodeId rootNodeId = createNodeIdTree(outputDefinition, fundVersion);

        // init fund
        ArrFund srcFund = outputDefinition.getFund();
        fund = new Fund(rootNodeId);
        fund.setName(srcFund.getName());
        fund.setInternalCode(srcFund.getInternalCode());
        fund.setCreateDate(Date.from(srcFund.getCreateDate().atZone(ZoneId.systemDefault()).toInstant()));
        fund.setDateRange(fundVersion.getDateRange());
        fund.setInstitution(createInstitution(srcFund.getInstitution()));

        initDirectOutputItems(outputDefinition, fundVersion);
    }

    /**
     * Creates output tree with root equal to {@link ArrFundVersion#getRootNodeId()} which contains
     * all connected output nodes and their subtrees.
     *
     * @return NodeId tree root.
     */
    private NodeId createNodeIdTree(ArrOutputDefinition outputDefinition, ArrFundVersion fundVersion) {
        List<ArrNodeOutput> outputNodes = outputService.getOutputNodes(outputDefinition, fundVersion.getLockChange());
        FundTree fundTree = fundTreeProvider.getFundTree(fundVersion.getFundVersionId());

        Map<Integer, NodeId> nodeIdMap = new HashMap<>();

        for (ArrNodeOutput outputNode : outputNodes) {
            TreeNode rootNode = fundTree.getNode(outputNode.getNodeId());

            NodeId nodeId = createNodeIdWithParents(rootNode, nodeIdMap);

            rootNode.getChildren().forEach(child -> initNodeIdSubtree(child, nodeId, nodeIdMap));
        }

        return nodeIdMap.get(fundTree.getRoot().getNodeId());
    }

    /**
     * Creates NodeId with all missing parent nodes.
     */
    private NodeId createNodeIdWithParents(TreeNode treeNode, Map<Integer, NodeId> nodeIdMap) {
        Integer arrNodeId = new Integer(treeNode.getNodeId());

        NodeId nodeId = nodeIdMap.get(arrNodeId);
        if (nodeId != null) {
            return nodeId;
        }

        if (treeNode.isRoot()) {
            nodeId = new NodeId(treeNode.getNodeId(), treeNode.getPosition());
        } else {
            // recursively create parents up to root or existing node
            NodeId parentNodeId = createNodeIdWithParents(treeNode.getParent(), nodeIdMap);
            nodeId = new NodeId(treeNode.getNodeId(), parentNodeId, treeNode.getPosition());
            parentNodeId.addChild(nodeId);
        }

        // add to lookup
        nodeIdMap.put(arrNodeId, nodeId);

        return nodeId;
    }

    /**
     * Initializes subtree recursively from specified node. Overlapping nodes are disallowed. Parent
     * node id cannot be null thus method is not suitable for root treeNode.
     *
     * @param parentNodeId not-null
     */
    private void initNodeIdSubtree(TreeNode treeNode, NodeId parentNodeId, Map<Integer, NodeId> nodeIdMap) {
        Integer arrNodeId = new Integer(treeNode.getNodeId());

        NodeId nodeId = new NodeId(treeNode.getNodeId(), parentNodeId, treeNode.getPosition());
        parentNodeId.addChild(nodeId);

        // add to lookup
        if (nodeIdMap.putIfAbsent(arrNodeId, nodeId) != null) {
            throw new SystemException("Node already defined for output", BaseCode.INVALID_STATE).set("nodeId", arrNodeId)
                    .set("outputDefinitionName", name);
        }

        for (TreeNode child : treeNode.getChildren()) {
            // recursively init child nodes
            initNodeIdSubtree(child, nodeId, nodeIdMap);
        }
    }

    /* factory methods */

    private Institution createInstitution(ParInstitution parInstitution) {
        Institution institution = new Institution();
        ParInstitutionType institutionType = parInstitution.getInstitutionType();
        institution.setTypeCode(institutionType.getCode());
        institution.setType(institutionType.getName());
        institution.setCode(parInstitution.getInternalCode());

        ParParty parParty = parInstitution.getParty();
        Party party = getParty(parParty);
        institution.setPartyGroup((PartyGroup) party);

        return institution;
    }

    private Record getAP(RegRecord regAP) {
        Record ap = apIdMap.get(regAP.getRecordId()); // without fetch -> access type property
        if (ap != null) {
            return ap;
        }

        RecordType apType = getAPType(regAP.getRegisterTypeId());
        ap = Record.newInstance(apType, regAP); // possible fetch from database

        // add to lookup
        apIdMap.put(regAP.getRecordId(), ap);

        return ap;
    }

    private RecordType getAPType(Integer apTypeId) {
        Validate.notNull(apTypeId);

        RecordType type = apTypeIdMap.get(apTypeId);
        if (type != null) {
            return type;
        }

        RegRegisterType regType = staticDataService.getData().getRegisterTypeById(apTypeId);
        if (Boolean.TRUE.equals(regType.getHierarchical())) {
            // recursively create parent types up to root or existing one
            RecordType parentType = getAPType(regType.getParentRegisterTypeId());
            type = RecordType.newInstance(parentType, regType);
        } else {
            type = RecordType.newInstance(null, regType);
        }

        // add to lookup
        apTypeIdMap.put(apTypeId, type);

        return type;
    }

    private Party getParty(ParParty parParty) {
        Party party = partyIdMap.get(parParty.getPartyId()); // without fetch -> access type property
        if (party != null) {
            return party;
        }

        Record ap = getAP(parParty.getRecord());
        List<Relation> relations = createRelations(parParty.getRelations());
        PartyInitHelper initHelper = new PartyInitHelper(ap, relations);
        party = convertParty(parParty, initHelper);

        // add to lookup
        partyIdMap.put(parParty.getPartyId(), party);

        return party;
    }

    private static Party convertParty(ParParty parParty, PartyInitHelper initHelper) {
        PartyType partyType = PartyType.fromId(parParty.getPartyTypeId());
        switch (partyType) {
            case DYNASTY:
                ParDynasty parDynasty = (ParDynasty) parParty;
                return Dynasty.newInstance(parDynasty, initHelper);
            case EVENT:
                ParEvent parEvent = (ParEvent) parParty;
                return Event.newInstance(parEvent, initHelper);
            case GROUP_PARTY:
                ParPartyGroup parPartyGroup = (ParPartyGroup) parParty;
                return PartyGroup.newInstance(parPartyGroup, initHelper);
            case PERSON:
                ParPerson parPerson = (ParPerson) parParty;
                return Person.newInstance(parPerson, initHelper);
            default:
                throw new IllegalStateException("Uknown party type: " + partyType);
        }
    }

    private List<Relation> createRelations(List<ParRelation> parRelations) {
        if (parRelations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Relation> relations = new ArrayList<>(parRelations.size());
        for (ParRelation parRelation : parRelations) {

            cz.tacr.elza.core.data.RelationType staticType = staticDataService.getData()
                    .getRelationTypeById(parRelation.getRelationTypeId());

            // prepare list of relationTo
            List<ParRelationEntity> entities = parRelation.getRelationEntities();
            List<RelationTo> relationsTo = new ArrayList<>(entities.size());
            for (ParRelationEntity entity : entities) {
                // create relation to
                RelationToType toType = getRelationToType(entity.getRoleTypeId(), staticType);
                Record ap = getAP(entity.getRecord());
                RelationTo relationTo = RelationTo.newInstance(entity, toType, ap);
                relationsTo.add(relationTo);
            }

            // create relation
            RelationType type = getRelationType(staticType);
            Relation relation = Relation.newInstance(parRelation, type, relationsTo);
            relations.add(relation);
        }

        return relations;
    }

    private RelationType getRelationType(cz.tacr.elza.core.data.RelationType staticType) {
        RelationType type = relationTypeIdMap.get(staticType.getId());
        if (type != null) {
            return type;
        }

        type = RelationType.newInstance(staticType.getEntity());

        // add to lookup
        relationTypeIdMap.put(staticType.getId(), type);

        return type;
    }

    private RelationToType getRelationToType(Integer relationRoleTypeId, cz.tacr.elza.core.data.RelationType staticType) {
        Validate.notNull(relationRoleTypeId);

        RelationToType type = relationRoleTypeIdMap.get(relationRoleTypeId);
        if (type != null) {
            return type;
        }

        ParRelationRoleType roleType = staticType.getRoleTypeById(relationRoleTypeId);
        type = RelationToType.newInstance(roleType);

        // add to lookup
        relationRoleTypeIdMap.put(relationRoleTypeId, type);

        return type;
    }

    private void initDirectOutputItems(ArrOutputDefinition outputDefinition, ArrFundVersion fundVersion) {
        List<ArrOutputItem> outputItems = outputService.getDirectOutputItems(outputDefinition, fundVersion.getLockChange());
        for (ArrOutputItem outputItem : outputItems) {
            if (outputItem.isUndefined()) {
                continue; // skip items without data
            }
            Item item = createItem(outputItem);
            this.outputItems.add(item);
        }
    }

    private Item createItem(ArrItem arrItem) {
        RuleSystemProvider ruleSystems = staticDataService.getData().getRuleSystems();

        RuleSystemItemType rsItemType = ruleSystems.getItemType(arrItem.getItemTypeId());
        ItemType itemType = getItemType(rsItemType);

        AbstractItem item = convertItemData(arrItem.getData(), itemType);

        item.setType(Validate.notNull(itemType));
        item.setPosition(arrItem.getPosition());
        item.setUndefined(arrItem.isUndefined());

        if (arrItem.getItemSpecId() != null) {
            ItemSpec itemSpec = getItemSpec(arrItem.getItemSpecId(), rsItemType);
            item.setSpecification(Validate.notNull(itemSpec));
        }

        return item;
    }

    private ItemType getItemType(RuleSystemItemType rsItemType) {
        ItemType type = itemTypeIdMap.get(rsItemType.getItemTypeId());
        if (type != null) {
            return type;
        }

        type = ItemType.instanceOf(rsItemType.getEntity());

        // add to lookup
        itemTypeIdMap.put(rsItemType.getItemTypeId(), type);

        return type;
    }

    private ItemSpec getItemSpec(Integer itemSpecId, RuleSystemItemType rsItemType) {
        Validate.notNull(itemSpecId);

        ItemSpec spec = itemSpecIdMap.get(itemSpecId);
        if (spec != null) {
            return spec;
        }

        RulItemSpec rulItemSpec = rsItemType.getItemSpecById(itemSpecId);
        spec = ItemSpec.instanceOf(rulItemSpec);

        // add to lookup
        itemSpecIdMap.put(itemSpecId, spec);

        return spec;
    }

    private AbstractItem convertItemData(ArrData data, ItemType itemType) {
        Validate.isTrue(HibernateUtils.isInitialized(data));

        switch (itemType.getDataType()) {
            case UNITID:
                ArrDataUnitid unitid = (ArrDataUnitid) data;
                return new ItemUnitId(unitid.getValue());
            case UNITDATE:
                ArrDataUnitdate unitdate = (ArrDataUnitdate) data;
                return createItemUnitdate(unitdate);
            case TEXT:
                ArrDataText text = (ArrDataText) data;
                return new ItemText(text.getValue());
            case STRING:
                ArrDataString str = (ArrDataString) data;
                return new ItemString(str.getValue());
            case RECORD_REF:
                ArrDataRecordRef apRef = (ArrDataRecordRef) data;
                return createItemAPRef(apRef);
            case PARTY_REF:
                ArrDataPartyRef partyRef = (ArrDataPartyRef) data;
                return createItemPartyRef(partyRef);
            case PACKET_REF:
                ArrDataPacketRef packetRef = (ArrDataPacketRef) data;
                return createItemPacketRef(packetRef);
            case JSON_TABLE:
                ArrDataJsonTable jsonTable = (ArrDataJsonTable) data;
                return new ItemJsonTable(itemType.getTableDefinition(), jsonTable.getValue());
            case INT:
                ArrDataInteger integer = (ArrDataInteger) data;
                return new ItemInteger(integer.getValue());
            case FORMATTED_TEXT:
                ArrDataText ftext = (ArrDataText) data;
                return new ItemText(ftext.getValue());
            case FILE_REF:
                ArrDataFileRef fileRef = (ArrDataFileRef) data;
                return createItemFileRef(fileRef);
            case ENUM:
                return ItemEnum.newInstance();
            case DECIMAL:
                ArrDataDecimal decimal = (ArrDataDecimal) data;
                return new ItemDecimal(decimal.getValue());
            case COORDINATES:
                ArrDataCoordinates coords = (ArrDataCoordinates) data;
                return new ItemCoordinates(coords.getValue());
            default:
                throw new SystemException("Uknown data type", BaseCode.INVALID_STATE).set("dataType", itemType.getDataType());
        }
    }

    private static ItemUnitdate createItemUnitdate(ArrDataUnitdate data) {
        CalendarType ct = CalendarType.fromId(data.getCalendarTypeId());
        UnitDate unitdate = UnitDate.valueOf(data, ct.getEntity());
        return new ItemUnitdate(unitdate);
    }

    private ItemRecordRef createItemAPRef(ArrDataRecordRef data) {
        Record ap = getAP(data.getRecord());
        return new ItemRecordRef(ap);
    }

    private ItemPartyRef createItemPartyRef(ArrDataPartyRef data) {
        Party party = getParty(data.getParty());
        return new ItemPartyRef(party);
    }

    private ItemPacketRef createItemPacketRef(ArrDataPacketRef data) {
        Packet packet = packetIdMap.get(data.getPacketId());
        if (packet == null) {
            ArrPacket arrPacket = data.getPacket();
            packet = Packet.newInstance(arrPacket);
            packetIdMap.put(data.getPacketId(), packet);
        }
        return new ItemPacketRef(packet); // TODO: addNode from every source
    }

    private ItemFileRef createItemFileRef(final ArrDataFileRef data) {
        File file = fileIdMap.get(data.getFileId());
        if (file == null) {
            ArrFile arrFile = data.getFile();
            file = File.newInstance(arrFile);
            fileIdMap.put(data.getFileId(), file);
        }
        return new ItemFileRef(file);
    }

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
