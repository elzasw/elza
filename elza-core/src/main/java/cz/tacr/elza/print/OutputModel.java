package cz.tacr.elza.print;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.core.data.RuleSystem;
import cz.tacr.elza.core.data.RuleSystemItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.fund.FundTree;
import cz.tacr.elza.core.fund.FundTreeProvider;
import cz.tacr.elza.core.fund.TreeNode;
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
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrOutputDefinition;
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
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;
import cz.tacr.elza.service.output.OutputParams;

public class OutputModel implements Output, NodeLoader {

    private static final Logger logger = LoggerFactory.getLogger(OutputModel.class);

    /* internal fields */

    private StaticDataProvider staticData;

    private RuleSystem ruleSystem;

    private ArrFundVersion fundVersion;

    /* general description */

    private List<Item> outputItems;

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

    /**
     * Filtered records have references to initialized Nodes (RecordWithLinks) which is reason why
     * we keep only last loaded instance instead of complete map.
     */
    private FilteredRecords lastFilteredRecords;

    /* managed components */

    private final StaticDataService staticDataService;

    private final FundTreeProvider fundTreeProvider;

    private final NodeCacheService nodeCacheService;

    public OutputModel(StaticDataService staticDataService,
                          FundTreeProvider fundTreeProvider,
                          NodeCacheService nodeCacheService) {
        this.staticDataService = staticDataService;
        this.fundTreeProvider = fundTreeProvider;
        this.nodeCacheService = nodeCacheService;
    }

    public boolean isInitialized() {
        return fundVersion != null;
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
    public List<Item> getItems(Collection<String> typeCodes) {
        Validate.notNull(typeCodes);

        return outputItems.stream().filter(item -> {
            String tc = item.getType().getCode();
            return typeCodes.contains(tc);
        }).collect(Collectors.toList());
    }

    @Override
    public List<Item> getItemsWithout(Collection<String> typeCodes) {
        Validate.notNull(typeCodes);

        return outputItems.stream().filter(item -> {
            String tc = item.getType().getCode();
            return !typeCodes.contains(tc);
        }).collect(Collectors.toList());
    }

    @Override
    public List<Party> getParties(Collection<String> typeCodes) {
        Validate.notNull(typeCodes);

        Set<Integer> distinctPartyIds = new HashSet<>();
        List<Party> parties = new ArrayList<>();

        for (Item item : outputItems) {
            String tc = item.getType().getCode();
            if (!typeCodes.contains(tc)) {
                continue;
            }
            Party party = item.getValue(Party.class);
            if (distinctPartyIds.add(party.getPartyId())) {
                parties.add(party);
            }
        }

        return parties;
    }

    @Override
    public Item getSingleItem(String typeCode) {
        Validate.notEmpty(typeCode);

        Item found = null;
        for (Item item : outputItems) {
            if (typeCode.equals(item.getType().getCode())) {
                // check if item already found
                if (found != null) {
                    throw new IllegalStateException("Multiple items with same code exists: " + typeCode);
                }
                found = item;
            }
        }
        return found;
    }

    @Override
    public String getSingleItemValue(String itemTypeCode) {
        Item found = getSingleItem(itemTypeCode);
        if (found != null) {
            return found.getSerializedValue();
        }
        return null;
    }

    @Override
    public NodeIterator getNodesDFS() {
        Iterator<NodeId> nodeIdIterator = fund.getRootNodeId().getIteratorDFS();
        return new NodeIterator(this, nodeIdIterator);
    }

    @Override
    public FilteredRecords getRecordsByType(String typeCode) {
        if (lastFilteredRecords == null || !lastFilteredRecords.getFilterType().equals(typeCode)) {
            lastFilteredRecords = filterRecords(typeCode);
        }
        return lastFilteredRecords;
    }

    /**
     * Prepare filtered list of records
     */
    private FilteredRecords filterRecords(String typeCode) {
        FilteredRecords filteredAPs = new FilteredRecords(typeCode);

        // add all nodes
        Iterator<NodeId> nodeIdIterator = fund.getRootNodeId().getIteratorDFS();
        NodeIterator nodeIterator = new NodeIterator(this, nodeIdIterator);
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.next();
            filteredAPs.addNode(node);
        }

        // sort collection
        filteredAPs.nodesAdded();

        return filteredAPs;
    }

    @Override
    public List<Node> loadNodes(Collection<NodeId> nodeIds) {
        Validate.isTrue(TransactionSynchronizationManager.isActualTransactionActive());
        Validate.isTrue(isInitialized());

        List<Integer> arrNodeIds = new ArrayList<>(nodeIds.size());
        List<Node> nodes = new ArrayList<>(nodeIds.size());

        for (NodeId nodeId : nodeIds) {
            arrNodeIds.add(nodeId.getArrNodeId());
            Node node = new Node(nodeId, this);
            nodes.add(node);
        }

        if (fundVersion.getLockChange() != null) {
            throw new NotImplementedException("Load nodes for closed fund version not implemented");
        }

        Map<Integer, RestoredNode> cachedNodeMap = nodeCacheService.getNodes(arrNodeIds);
        for (Node node : nodes) {
            Integer arrNodeId = node.getNodeId().getArrNodeId();
            RestoredNode cachedNode = cachedNodeMap.get(arrNodeId);
            Validate.notNull(cachedNode);
            initNode(node, cachedNode);
        }

        return nodes;
    }

    /**
     * Init output node from node cache.
     */
    private void initNode(Node node, CachedNode cachedNode) {
        // set node items
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

        // set direct node AP
        List<ArrNodeRegister> arrNodeAPs = cachedNode.getNodeRegisters();
        if (arrNodeAPs != null) {
            List<Record> nodeAPs = new ArrayList<>(arrNodeAPs.size());
            for (ArrNodeRegister nodeAP : arrNodeAPs) {
                Record ap = getAP(nodeAP.getRecord());
                nodeAPs.add(ap);
            }
            node.setNodeAPs(nodeAPs);
        }
    }

    /**
     * Initializes output model. Must be called during transaction.
     */
    public void init(OutputParams params) {
        Validate.isTrue(TransactionSynchronizationManager.isActualTransactionActive());
        Validate.isTrue(!isInitialized());

        logger.info("Output model initialization started, outputDefinitionId:{}", params.getDefinitionId());

        // prepare internal fields
        this.fundVersion = params.getFundVersion();
        this.staticData = staticDataService.getData();
        this.ruleSystem = staticData.getRuleSystems().getByRuleSetId(fundVersion.getRuleSetId());


        // init general description
        ArrOutputDefinition definition = params.getDefinition();
        RulOutputType outputType = definition.getOutputType();
        this.name = definition.getName();
        this.internalCode = definition.getInternalCode();
        this.typeCode = outputType.getCode();
        this.type = outputType.getName();

        // init node id tree
        NodeId rootNodeId = createNodeIdTree(params.getOutputNodes(), params.getFundVersionId());

        // init fund
        ArrFund arrFund = definition.getFund();
        this.fund = new Fund(rootNodeId);
        this.fund.setName(arrFund.getName());
        this.fund.setInternalCode(arrFund.getInternalCode());
        this.fund.setCreateDate(Date.from(arrFund.getCreateDate().atZone(ZoneId.systemDefault()).toInstant()));
        this.fund.setDateRange(fundVersion.getDateRange());

        // init fund institution
        ParInstitution parInstit = arrFund.getInstitution();
        Institution institution = new Institution(parInstit.getInternalCode(), parInstit.getInstitutionType());
        Party party = getParty(parInstit.getParty());
        institution.setPartyGroup((PartyGroup) party);
        this.fund.setInstitution(institution);

        // init direct items
        outputItems = params.getOutputItems().stream()
                .filter(i -> !i.isUndefined())
                .map(this::createItem)
                .collect(Collectors.toList());

        logger.info("Output model initialization ended, outputDefinitionId:{}", params.getDefinitionId());
    }

    /**
     * Creates output tree with root same as {@link ArrFundVersion#getRootNodeId()}. Tree contains
     * output subtrees and their paths to fund root.
     *
     * @return NodeId tree root.
     */
    private NodeId createNodeIdTree(List<ArrNodeOutput> outputNodes, Integer fundVersionId) {
        FundTree fundTree = fundTreeProvider.getFundTree(fundVersionId);

        Map<Integer, NodeId> nodeIdMap = new HashMap<>();

        for (ArrNodeOutput outputNode : outputNodes) {
            TreeNode treeNode = fundTree.getNode(outputNode.getNodeId());

            NodeId nodeId = createNodeIdWithParents(treeNode, nodeIdMap);

            treeNode.getChildren().forEach(child -> initNodeIdSubtree(child, nodeId, nodeIdMap));
        }

        return nodeIdMap.get(fundTree.getRoot().getNodeId());
    }

    /**
     * Creates NodeId with all parent nodes up to root.
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
     * node id cannot be null thus method is not suitable for root node.
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
        ap = Record.newInstance(regAP, apType);

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
        parParty = HibernateUtils.unproxy(parParty);
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
                ArrDataUnitdate dataUnitdate = (ArrDataUnitdate) data;
                UnitDate unitDate = new UnitDate(dataUnitdate);
                return new ItemUnitdate(unitDate);
            case TEXT:
                ArrDataText text = (ArrDataText) data;
                return new ItemString(text.getValue());
            case STRING:
                ArrDataString str = (ArrDataString) data;
                return new ItemString(str.getValue());
            case RECORD_REF:
                ArrDataRecordRef apRef = (ArrDataRecordRef) data;
                Record ap = getAP(apRef.getRecord());
                return new ItemRecordRef(ap);
            case PARTY_REF:
                ArrDataPartyRef partyRef = (ArrDataPartyRef) data;
                Party party = getParty(partyRef.getParty());
                return new ItemPartyRef(party);
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

    private ItemPacketRef createItemPacketRef(ArrDataPacketRef data) {
        Packet packet = packetIdMap.get(data.getPacketId());
        if (packet == null) {
            ArrPacket arrPacket = data.getPacket();
            packet = Packet.newInstance(arrPacket, ruleSystem, this);
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
