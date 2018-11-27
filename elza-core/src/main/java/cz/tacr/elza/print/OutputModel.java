package cz.tacr.elza.print;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.data.PartyType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.fund.FundTree;
import cz.tacr.elza.core.fund.FundTreeProvider;
import cz.tacr.elza.core.fund.TreeNode;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNodeOutput;
import cz.tacr.elza.domain.ArrNodeRegister;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrStructuredObject;
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
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemSpec;
import cz.tacr.elza.print.item.ItemStructuredRef;
import cz.tacr.elza.print.item.ItemType;
import cz.tacr.elza.print.item.convertors.ItemConvertor;
import cz.tacr.elza.print.item.convertors.ItemConvertorContext;
import cz.tacr.elza.print.item.convertors.OutputItemConvertor;
import cz.tacr.elza.print.party.Dynasty;
import cz.tacr.elza.print.party.Event;
import cz.tacr.elza.print.party.Institution;
import cz.tacr.elza.print.party.Party;
import cz.tacr.elza.print.party.PartyGroup;
import cz.tacr.elza.print.party.PartyInitHelper;
import cz.tacr.elza.print.party.PartyName;
import cz.tacr.elza.print.party.Person;
import cz.tacr.elza.print.party.Relation;
import cz.tacr.elza.print.party.RelationTo;
import cz.tacr.elza.print.party.RelationToType;
import cz.tacr.elza.print.party.RelationType;
import cz.tacr.elza.repository.ApDescriptionRepository;
import cz.tacr.elza.repository.ApExternalIdRepository;
import cz.tacr.elza.repository.ApNameRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;
import cz.tacr.elza.service.output.OutputParams;

public class OutputModel implements Output, NodeLoader, ItemConvertorContext {

    private static final Logger logger = LoggerFactory.getLogger(OutputModel.class);

    /* internal fields */

    private ArrFundVersion fundVersion;

    private StaticDataProvider staticData;

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

    private final Map<Integer, RelationToType> relationRoleTypeIdMap = new HashMap<>();

    private final Map<Integer, Structured> structObjIdMap = new HashMap<>();

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

    private final InstitutionRepository institutionRepository;

    private final ApDescriptionRepository apDescRepository;

    private final ApNameRepository apNameRepository;

    private final ApExternalIdRepository apEidRepository;

    /**
     * Provider for attachments
     */
    private AttPageProvider attPageProvider;

    private ElzaLocale elzaLocale;

    public OutputModel(StaticDataService staticDataService,
            ElzaLocale elzaLocale,
                       FundTreeProvider fundTreeProvider,
                       NodeCacheService nodeCacheService,
                       InstitutionRepository institutionRepository,
                       ApDescriptionRepository apDescRepository,
                       ApNameRepository apNameRepository,
            ApExternalIdRepository apEidRepository,
            AttPageProvider attPageProvider) {
        this.staticDataService = staticDataService;
        this.elzaLocale = elzaLocale;
        this.fundTreeProvider = fundTreeProvider;
        this.nodeCacheService = nodeCacheService;
        this.institutionRepository = institutionRepository;
        this.apDescRepository = apDescRepository;
        this.apNameRepository = apNameRepository;
        this.apEidRepository = apEidRepository;
        this.attPageProvider = attPageProvider;
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
        return StringUtils.EMPTY;
    }

    @Override
    public NodeIterator createFlatNodeIterator() {
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
        FilteredRecords filteredAPs = new FilteredRecords(elzaLocale, typeCode);

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
            Node node = new Node(nodeId);
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
            ItemConvertor conv = new OutputItemConvertor();
            List<Item> items = descItems.stream()
                    .map(i -> {
                        Item item = conv.convert(i, this);
                        // add packet reference
                        if (item instanceof ItemStructuredRef) {
                            item.getValue(Structured.class).addNodeId(node.getNodeId());
                        }
                        return item;
                    })
                    .filter(Objects::nonNull)
                    .sorted(Item::compareTo)
                    .collect(Collectors.toList());
            node.setItems(items);
        }

        // set direct node AP
        List<ArrNodeRegister> arrNodeAPs = cachedNode.getNodeRegisters();
        if (arrNodeAPs != null) {
            List<Record> nodeAPs = new ArrayList<>(arrNodeAPs.size());
            for (ArrNodeRegister nodeAP : arrNodeAPs) {
                Record ap = getRecord(nodeAP.getRecord());
                nodeAPs.add(ap);
            }
            node.setNodeAPs(nodeAPs);
        }
    }

    /**
     * Initializes output model. Must be called inside transaction.
     */
    public void init(OutputParams params) {
        Validate.isTrue(TransactionSynchronizationManager.isActualTransactionActive());
        Validate.isTrue(!isInitialized());

        logger.info("Output model initialization started, outputDefinitionId:{}", params.getDefinitionId());

        // prepare internal fields
        this.fundVersion = params.getFundVersion();
        this.staticData = staticDataService.getData();

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
        this.fund = new Fund(rootNodeId, this);
        this.fund.setName(arrFund.getName());
        this.fund.setInternalCode(arrFund.getInternalCode());
        this.fund.setCreateDate(Date.from(arrFund.getCreateDate().atZone(ZoneId.systemDefault()).toInstant()));
        this.fund.setDateRange(fundVersion.getDateRange());

        // init fund institution
        Institution inst = createInstitution(arrFund);
        this.fund.setInstitution(inst);

        // init direct items
        ItemConvertor conv = new OutputItemConvertor();
        this.outputItems = params.getOutputItems().stream()
                .map(i -> conv.convert(i, this))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        logger.info("Output model initialization ended, outputDefinitionId:{}", params.getDefinitionId());
    }

    private Institution createInstitution(ArrFund arrFund) {
        ParInstitution parInst = institutionRepository.findByFundFetchTypeAndParty(arrFund);
        PartyGroup pg = (PartyGroup) getParty(parInst.getParty());

        Institution inst = new Institution(parInst.getInternalCode(), parInst.getInstitutionType());
        inst.setPartyGroup(pg);

        return inst;
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

            // convert output node to NodeId with all parents up to root
            NodeId nodeId = createNodeIdWithParents(treeNode, nodeIdMap);

            // convert node tree of each output node child to NodeIds
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

    @Override
    public Record getRecord(ApAccessPoint ap) {
        // id without fetch -> access type property
        Record record = apIdMap.get(ap.getAccessPointId());
        if (record != null) {
            return record;
        }

        RecordType type = getAPType(ap.getApTypeId());
        record = new Record(ap, type, staticData, apDescRepository, apNameRepository, apEidRepository);

        // add to lookup
        apIdMap.put(ap.getAccessPointId(), record);

        return record;
    }

    private RecordType getAPType(Integer apTypeId) {
        Validate.notNull(apTypeId);

        RecordType type = apTypeIdMap.get(apTypeId);
        if (type != null) {
            return type;
        }

        ApType apType = staticData.getApTypeById(apTypeId);

        // add to lookup
        RecordType parentType = null;
        Integer apParentTypeId = apType.getParentApTypeId();
        if (apParentTypeId != null) {
            parentType = getAPType(apParentTypeId);
        }
        type = RecordType.newInstance(parentType, apType);

        // add to lookup
        apTypeIdMap.put(apTypeId, type);

        return type;
    }

    // TODO: party names and relations should be fetched
    @Override
    public Party getParty(ParParty parParty) {
        // id without fetch -> access type property
        Party party = partyIdMap.get(parParty.getPartyId());
        if (party != null) {
            return party;
        }

        Record partyAP = getRecord(parParty.getAccessPoint());
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
            RelationToType roleType = getRelationRoleType(entity.getRoleTypeId(), staticRelationType);
            Record entityAP = getRecord(entity.getAccessPoint());
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

    /**
     * Convert party to output specific object
     *
     * @param party
     *            have to be non null
     * @param initHelper
     * @return
     */
    private static Party convertParty(ParParty party, PartyInitHelper initHelper) {
        // input data have to be initialized
        party = HibernateUtils.unproxyInitialized(party);

        PartyType partyType = PartyType.fromId(party.getPartyTypeId());
        switch (partyType) {
            case DYNASTY:
            ParDynasty parDynasty = (ParDynasty) party;
                return new Dynasty(parDynasty, initHelper);
            case EVENT:
            ParEvent parEvent = (ParEvent) party;
                return new Event(parEvent, initHelper);
            case GROUP_PARTY:
            ParPartyGroup parPartyGroup = (ParPartyGroup) party;
                return new PartyGroup(parPartyGroup, initHelper);
            case PERSON:
            ParPerson parPerson = (ParPerson) party;
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

    private RelationToType getRelationRoleType(Integer relationRoleTypeId, cz.tacr.elza.core.data.RelationType staticRelationType) {
        Validate.notNull(relationRoleTypeId);

        RelationToType roleType = relationRoleTypeIdMap.get(relationRoleTypeId);
        if (roleType != null) {
            return roleType;
        }

        ParRelationRoleType parRoleType = staticRelationType.getRoleTypeById(relationRoleTypeId);
        roleType = new RelationToType(parRoleType);

        // add to lookup
        relationRoleTypeIdMap.put(relationRoleTypeId, roleType);

        return roleType;
    }

    @Override
    public ItemType getItemTypeById(Integer id) {
        ItemType itemType = itemTypeIdMap.get(id);
        if (itemType != null) {
            return itemType;
        }

        RulItemType rulItemType = staticData.getItemTypeById(id).getEntity();
        itemType = new ItemType(rulItemType);

        // add to lookup
        itemTypeIdMap.put(id, itemType);

        return itemType;
    }

    @Override
    public ItemSpec getItemSpecById(Integer id) {
        ItemSpec itemSpec = itemSpecIdMap.get(id);
        if (itemSpec != null) {
            return itemSpec;
        }

        RulItemSpec rulItemSpec = staticData.getItemSpecById(id);
        itemSpec = new ItemSpec(rulItemSpec);

        // add to lookup
        itemSpecIdMap.put(id, itemSpec);

        return itemSpec;
    }

    @Override
    public Structured getStructured(ArrStructuredObject structObj) {
        Validate.isTrue(HibernateUtils.isInitialized(structObj));

        Structured result = structObjIdMap.get(structObj.getStructuredObjectId());
        if (result == null) {
            result = Structured.newInstance(structObj, this);

            // add to lookup
            structObjIdMap.put(structObj.getStructuredObjectId(), result);
        }

        return result;
    }

    @Override
    public File getFile(ArrFile arrFile) {

        File file = fileIdMap.get(arrFile.getFileId());
        if (file != null) {
            return file;
        }

        file = new File(arrFile);

        // add to lookup
        fileIdMap.put(arrFile.getFileId(), file);

        return file;
    }

    @Override
    public List<AttPagePlaceHolder> getAttPagePlaceHolders(final String itemTypeCode) {

        if (attPageProvider == null) {
            return Collections.emptyList();
        }

        return attPageProvider.getAttPagePlaceHolders(itemTypeCode);
    }
}
