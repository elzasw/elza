package cz.tacr.elza.print;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cz.tacr.elza.core.data.CalendarType;
import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.tree.FundTree;
import cz.tacr.elza.core.tree.FundTreeProvider;
import cz.tacr.elza.core.tree.TreeNode;
import cz.tacr.elza.domain.ArrChange;
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
import cz.tacr.elza.domain.ParParty;
import cz.tacr.elza.domain.ParPartyGroup;
import cz.tacr.elza.domain.ParPartyName;
import cz.tacr.elza.domain.ParPerson;
import cz.tacr.elza.domain.ParRelation;
import cz.tacr.elza.domain.ParRelationClassType;
import cz.tacr.elza.domain.ParRelationEntity;
import cz.tacr.elza.domain.ParRelationRoleType;
import cz.tacr.elza.domain.ParRelationType;
import cz.tacr.elza.domain.RegRecord;
import cz.tacr.elza.domain.RegRegisterType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
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
import cz.tacr.elza.print.item.ItemType;
import cz.tacr.elza.print.item.ItemUnitId;
import cz.tacr.elza.print.item.ItemUnitdate;
import cz.tacr.elza.print.party.Dynasty;
import cz.tacr.elza.print.party.Event;
import cz.tacr.elza.print.party.Institution;
import cz.tacr.elza.print.party.Party;
import cz.tacr.elza.print.party.PartyGroup;
import cz.tacr.elza.print.party.PartyInitHelper;
import cz.tacr.elza.print.party.PartyName;
import cz.tacr.elza.print.party.Person;
import cz.tacr.elza.print.party.Relation;
import cz.tacr.elza.print.party.RelationRoleType;
import cz.tacr.elza.print.party.RelationTo;
import cz.tacr.elza.print.party.RelationType;
import cz.tacr.elza.repository.OutputDefinitionRepository;
import cz.tacr.elza.service.OutputService;
import cz.tacr.elza.service.RegistryService;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;
import cz.tacr.elza.utils.HibernateUtils;

public class OutputModelNew implements Output {

    private StaticDataProvider staticData;

    private RuleSystem ruleSystem;

    /* general description */

    private final List<Item> outputItems = new ArrayList<>();

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

    private final Map<Integer, RelationRoleType> relationRoleTypeIdMap = new HashMap<>();

    private final Map<Integer, Packet> packetIdMap = new HashMap<>();

    private final Map<Integer, File> fileIdMap = new HashMap<>();

    /* spring components */

    private final StaticDataService staticDataService;

    private final OutputService outputService;

    private final FundTreeProvider fundTreeProvider;

    private final NodeCacheService nodeCacheService;

    private final RegistryService registryService;

    public OutputModelNew(StaticDataService staticDataService,
                          OutputService outputService,
                          FundTreeProvider fundTreeProvider,
                          OutputDefinitionRepository outputDefinitionRepository,
                          NodeCacheService nodeCacheService,
                          RegistryService registryService) {
        this.staticDataService = staticDataService;
        this.outputService = outputService;
        this.fundTreeProvider = fundTreeProvider;
        this.nodeCacheService = nodeCacheService;
        this.registryService = registryService;
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
    public List<Item> getItems() {
        return outputItems;
    }

    @Override
    public List<Item> getItems(Collection<String> codes) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Item> getItemsWithout(Collection<String> codes) {
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
    public IteratorNodes getNodesDFS() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FilteredRecords getRecordsByType(String code) {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<Integer, Node> loadNodes(Collection<NodeId> nodeIds) {
        Validate.isTrue(TransactionSynchronizationManager.isActualTransactionActive());

        Map<Integer, Node> nodeIdMap = new HashMap<>(nodeIds.size());
        for (NodeId nodeId : nodeIds) {
            Node node = new Node(nodeId, this);
            nodeIdMap.put(nodeId.getArrNodeId(), node);
        }

        initNodesItems(nodeIdMap);
        initNodesAPs(nodeIdMap);

        return nodeIdMap;
    }

    private void initNodesItems(Map<Integer, Node> nodeIdMap) {
        Map<Integer, RestoredNode> cachedNodeMap = nodeCacheService.getNodes(nodeIdMap.keySet());
        nodeIdMap.forEach((arrNodeId, node) -> {
            RestoredNode cachedNode = cachedNodeMap.get(arrNodeId);
            List<ArrDescItem> descItems = cachedNode.getDescItems();

            if (descItems != null) {
                List<Item> items = descItems.stream()
                        .filter(arrItem -> !arrItem.isUndefined())
                        .map(arrItem -> {
                                Item item = createItem(arrItem);
                                // add packet reference
                                if (item instanceof ItemPacketRef) {
                                    item.getValue(Packet.class).addNodeId(node.getNodeId());
                                }
                                return item;
                            })
                        .sorted(Item::compareTo)
                        .collect(Collectors.toList());

                node.setItems(items);
            }
        });
    }

    private void initNodesAPs(Map<Integer, Node> nodeIdMap) {
        Map<Integer, List<RegRecord>> nodeIdregAPsMap = registryService.findByNodes(nodeIdMap.keySet());
        nodeIdMap.forEach((arrNodeId, node) -> {

            List<RegRecord> regAPs = nodeIdregAPsMap.get(arrNodeId);
            if (regAPs != null) {
                List<Record> aps = regAPs.stream().map(this::getAP).collect(Collectors.toList());
                node.setAPs(aps);
            }
        });
    }

    /**
     * Initializes output model. Must be called during transaction.
     *
     * @param fundVersion not-null
     */
    public void init(int outputDefinitionId, ArrFundVersion fundVersion) {
        Validate.isTrue(TransactionSynchronizationManager.isActualTransactionActive());

        // find output definition with fetch for model
        ArrOutputDefinition outputDefinition = outputService.getDefinitionForModel(outputDefinitionId);
        if (outputDefinition == null) {
            throw new SystemException("Output definition not found", BaseCode.ID_NOT_EXIST).set("outputDefinitionId",
                    outputDefinitionId);
        }

        // check fund version against definition
        if (fundVersion != null) {
            Integer fundId = outputDefinition.getFundId();
            Validate.isTrue(fundId.equals(fundVersion.getFundId()));
        }

        init(outputDefinition, fundVersion);
    }

    private void init(ArrOutputDefinition outputDefinition, ArrFundVersion fundVersion) {
        Validate.isTrue(fund == null); // check if not initialized

        // prepare static data
        staticData = staticDataService.getData();
        ruleSystem = staticData.getRuleSystems().getByRuleSetId(fundVersion.getRuleSetId());

        // init general description
        name = outputDefinition.getName();
        internalCode = outputDefinition.getInternalCode();
        RulOutputType outputType = outputDefinition.getOutputType();
        typeCode = outputType.getCode();
        type = outputType.getName();

        // init node id tree
        NodeId rootNodeId = createNodeIdTree(outputDefinition, fundVersion);

        // init fund
        ArrFund arrFund = outputDefinition.getFund();
        fund = new Fund(rootNodeId);
        fund.setName(arrFund.getName());
        fund.setInternalCode(arrFund.getInternalCode());
        fund.setCreateDate(Date.from(arrFund.getCreateDate().atZone(ZoneId.systemDefault()).toInstant()));
        fund.setDateRange(fundVersion.getDateRange());

        // init fund institution
        ParInstitution parInstit = arrFund.getInstitution();
        Institution institution = new Institution(parInstit.getInternalCode(), parInstit.getInstitutionType());
        Party party = getParty(parInstit.getParty());
        institution.setPartyGroup((PartyGroup) party);
        fund.setInstitution(institution);

        // init direct items
        initDirectOutputItems(outputDefinition, fundVersion.getLockChange());
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

    // TODO: record variants should be fetched
    private Record getAP(RegRecord regAP) {
        // id without fetch -> access type property
        Record ap = apIdMap.get(regAP.getRecordId());
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

        RegRegisterType regType = staticData.getRegisterTypeById(apTypeId);
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

    // TODO: party names and relations should be fetched
    private Party getParty(ParParty parParty) {
        // id without fetch -> access type property
        Party party = partyIdMap.get(parParty.getPartyId());
        if (party != null) {
            return party;
        }

        Record partyAP = getAP(parParty.getRecord());
        PartyInitHelper initHelper = new PartyInitHelper(partyAP);

        // init all party names
        for (ParPartyName parName : parParty.getPartyNames()) {
            // TODO: valid dates should be fetched
            PartyName name = PartyName.newInstance(parName, staticData);
            if (parName.getPartyNameId().equals(parParty.getPreferredNameId())) {
                initHelper.setPreferredName(name);
            } else {
                initHelper.addName(name);
            }
        }

        // init all relations
        parParty.getRelations().forEach(r -> preparePartyRelation(r, initHelper));

        // create party
        party = convertParty(parParty, initHelper);

        // add to lookup
        partyIdMap.put(parParty.getPartyId(), party);

        return party;
    }

    /**
     * Prepares output name for party init helper.
     */

    /**
     * Prepares output relation for party init helper.
     */
    // TODO: valid dates, entities and related records should be fetched
    private void preparePartyRelation(ParRelation parRelation, PartyInitHelper initHelper) {
        cz.tacr.elza.core.data.RelationType staticRelationType = staticData.getRelationTypeById(parRelation.getRelationTypeId());

        // prepare list of relationTo
        List<ParRelationEntity> entities = parRelation.getRelationEntities();
        List<RelationTo> relationsTo = new ArrayList<>(entities.size());
        for (ParRelationEntity entity : entities) {
            // create relation to
            RelationRoleType roleType = getRelationRoleType(entity.getRoleTypeId(), staticRelationType);
            Record entityAP = getAP(entity.getRecord());
            RelationTo relationTo = new RelationTo(entity, roleType, entityAP);
            relationsTo.add(relationTo);
        }

        // create relation
        RelationType type = getRelationType(staticRelationType);
        Relation relation = Relation.newInstance(parRelation, type, relationsTo);

        // resolve relation type
        ParRelationClassType parClassType = staticRelationType.getEntity().getRelationClassType();
        switch (parClassType.getCode()) {
            case ParRelationClassType.CREATION_CODE:
                initHelper.setCreation(relation);
                break;
            case ParRelationClassType.DESTRUCTION_CODE:
                initHelper.setDestruction(relation);
                break;
            default:
                initHelper.addRelation(relation);
        }
    }

    private static Party convertParty(ParParty parParty, PartyInitHelper initHelper) {
        PartyType partyType = PartyType.fromId(parParty.getPartyTypeId());
        switch (partyType) {
            case DYNASTY:
                ParDynasty parDynasty = (ParDynasty) parParty;
                return new Dynasty(parDynasty, initHelper);
            case EVENT:
                ParEvent parEvent = (ParEvent) parParty;
                return new Event(parEvent, initHelper);
            case GROUP_PARTY:
                ParPartyGroup parPartyGroup = (ParPartyGroup) parParty;
                return new PartyGroup(parPartyGroup, initHelper);
            case PERSON:
                ParPerson parPerson = (ParPerson) parParty;
                return new Person(parPerson, initHelper);
            default:
                throw new IllegalStateException("Uknown party type: " + partyType);
        }
    }

    private RelationType getRelationType(cz.tacr.elza.core.data.RelationType staticRelationType) {
        RelationType realtionType = relationTypeIdMap.get(staticRelationType.getId());
        if (realtionType != null) {
            return realtionType;
        }

        ParRelationType parRelationType = staticRelationType.getEntity();
        realtionType = new RelationType(parRelationType);

        // add to lookup
        relationTypeIdMap.put(staticRelationType.getId(), realtionType);

        return realtionType;
    }

    private RelationRoleType getRelationRoleType(Integer relationRoleTypeId,
                                                 cz.tacr.elza.core.data.RelationType staticRelationType) {
        Validate.notNull(relationRoleTypeId);

        RelationRoleType roleType = relationRoleTypeIdMap.get(relationRoleTypeId);
        if (roleType != null) {
            return roleType;
        }

        ParRelationRoleType parRoleType = staticRelationType.getRoleTypeById(relationRoleTypeId);
        roleType = new RelationRoleType(parRoleType);

        // add to lookup
        relationRoleTypeIdMap.put(relationRoleTypeId, roleType);

        return roleType;
    }

    private void initDirectOutputItems(ArrOutputDefinition outputDefinition, ArrChange lockChange) {
        List<ArrOutputItem> outputItems = outputService.getDirectOutputItems(outputDefinition, lockChange);
        for (ArrOutputItem outputItem : outputItems) {
            if (outputItem.isUndefined()) {
                continue; // skip items without data
            }
            Item item = createItem(outputItem);
            this.outputItems.add(item);
        }
    }

    private Item createItem(ArrItem arrItem) {
        RuleSystemItemType staticItemType = ruleSystem.getItemTypeById(arrItem.getItemTypeId());
        ItemType itemType = getItemType(staticItemType);

        AbstractItem item = convertItemData(arrItem.getData(), itemType);

        item.setType(Validate.notNull(itemType));
        item.setPosition(arrItem.getPosition());
        item.setUndefined(arrItem.isUndefined());

        if (arrItem.getItemSpecId() != null) {
            ItemSpec itemSpec = getItemSpec(staticItemType, arrItem.getItemSpecId());
            item.setSpecification(Validate.notNull(itemSpec));
        }

        return item;
    }

    private ItemType getItemType(RuleSystemItemType staticItemType) {
        ItemType itemType = itemTypeIdMap.get(staticItemType.getItemTypeId());
        if (itemType != null) {
            return itemType;
        }

        RulItemType rulItemType = staticItemType.getEntity();
        itemType = new ItemType(rulItemType);

        // add to lookup
        itemTypeIdMap.put(staticItemType.getItemTypeId(), itemType);

        return itemType;
    }

    private ItemSpec getItemSpec(RuleSystemItemType staticItemType, Integer itemSpecId) {
        Validate.notNull(itemSpecId);

        ItemSpec itemSpec = itemSpecIdMap.get(itemSpecId);
        if (itemSpec != null) {
            return itemSpec;
        }

        RulItemSpec rulItemSpec = staticItemType.getItemSpecById(itemSpecId);
        itemSpec = new ItemSpec(rulItemSpec);

        // add to lookup
        itemSpecIdMap.put(itemSpecId, itemSpec);

        return itemSpec;
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
                return new ItemString(text.getValue());
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
                return new ItemString(ftext.getValue());
            case FILE_REF:
                ArrDataFileRef fileRef = (ArrDataFileRef) data;
                return createItemFileRef(fileRef);
            case ENUM:
                return new ItemEnum();
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
            packet = Packet.newInstance(arrPacket, ruleSystem);
            packetIdMap.put(data.getPacketId(), packet);
        }
        return new ItemPacketRef(packet);
    }

    private ItemFileRef createItemFileRef(final ArrDataFileRef data) {
        File file = fileIdMap.get(data.getFileId());
        if (file == null) {
            ArrFile arrFile = data.getFile();
            file = new File(arrFile);
            fileIdMap.put(data.getFileId(), file);
        }
        return new ItemFileRef(file);
    }
}
