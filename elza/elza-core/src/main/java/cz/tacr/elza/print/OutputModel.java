package cz.tacr.elza.print;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.core.fund.FundTree;
import cz.tacr.elza.core.fund.FundTreeProvider;
import cz.tacr.elza.core.fund.TreeNode;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.item.Item;
import cz.tacr.elza.print.item.ItemSpec;
import cz.tacr.elza.print.item.ItemType;
import cz.tacr.elza.print.item.convertors.ItemConvertor;
import cz.tacr.elza.print.item.convertors.ItemConvertorContext;
import cz.tacr.elza.print.item.convertors.OutputItemConvertor;
import cz.tacr.elza.print.part.Part;
import cz.tacr.elza.print.party.Institution;
import cz.tacr.elza.repository.*;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;
import cz.tacr.elza.service.output.OutputParams;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

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

    private final Map<Integer, Node> nodeIdMap = new HashMap<>();

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

    private final ApStateRepository apStateRepository;

    private final ApBindingRepository bindingRepository;

    private final ApPartRepository partRepository;

    private final ApItemRepository itemRepository;

    private final ApBindingStateRepository bindingStateRepository;

    private final StructuredObjectRepository structObjRepos;

    /**
     * Provider for attachments
     */
    private AttPageProvider attPageProvider;

    private ElzaLocale elzaLocale;

    /**
     * Collection of start nodes
     */
    private List<Integer> startNodes = new ArrayList<>();

    private StructuredItemRepository structItemRepos;

    private OffsetDateTime changeDateTime;

    public OutputModel(final StaticDataService staticDataService,
                       final ElzaLocale elzaLocale,
                       final FundTreeProvider fundTreeProvider,
                       final NodeCacheService nodeCacheService,
                       final InstitutionRepository institutionRepository,
                       final ApStateRepository apStateRepository,
                       final ApBindingRepository bindingRepository,
                       final AttPageProvider attPageProvider,
                       final StructuredObjectRepository structObjRepos,
                       final StructuredItemRepository structItemRepos,
                       final ApPartRepository partRepository,
                       final ApItemRepository itemRepository,
                       final ApBindingStateRepository bindingStateRepository) {
        this.staticDataService = staticDataService;
        this.elzaLocale = elzaLocale;
        this.fundTreeProvider = fundTreeProvider;
        this.nodeCacheService = nodeCacheService;
        this.institutionRepository = institutionRepository;
        this.apStateRepository = apStateRepository;
        this.bindingRepository = bindingRepository;
        this.attPageProvider = attPageProvider;
        this.structObjRepos = structObjRepos;
        this.structItemRepos = structItemRepos;
        this.partRepository = partRepository;
        this.itemRepository = itemRepository;
        this.bindingStateRepository = bindingStateRepository;
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
    public OffsetDateTime getChangeDateTime() {
        return changeDateTime;
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

    private List<Item> convertItems(List<? extends IntItem> srcItems) {
        ItemConvertor conv = new OutputItemConvertor();
        List<Item> result = srcItems.stream()
                .map(i -> conv.convert(i, this))
                /*
                // add packet reference
                if (item instanceof ItemStructuredRef) {
                    item.getValue(Structured.class).addNodeId(node.getNodeId());
                }*/
                .filter(Objects::nonNull)
                .sorted(Item::compareTo)
                .collect(Collectors.toList());
        return result;
    }

    /**
     * Init output node from node cache.
     */
    private void initNode(Node node, CachedNode cachedNode) {
        // set node items
        List<ArrDescItem> descItems = cachedNode.getDescItems();
        if (descItems != null) {
            List<Item> items = convertItems(descItems);
            node.setItems(items);
        }
    }

    /**
     * Initializes output model. Must be called inside transaction.
     */
    public void init(OutputParams params) {
        Validate.isTrue(TransactionSynchronizationManager.isActualTransactionActive());
        Validate.isTrue(!isInitialized());
        Validate.notNull(params);
        Validate.notNull(params.getChange());

        logger.info("Output model initialization started, outputId:{}", params.getOutputId());

        // prepare internal fields
        this.fundVersion = params.getFundVersion();
        this.staticData = staticDataService.getData();

        // init general description
        ArrOutput output = params.getOutput();
        RulOutputType outputType = output.getOutputType();
        this.name = output.getName();
        this.internalCode = output.getInternalCode();
        this.typeCode = outputType.getCode();
        this.type = outputType.getName();
        this.changeDateTime = params.getChange().getChangeDate();

        // init node id tree
        NodeId rootNodeId = createNodeIdTree(params.getOutputNodeIds(), params.getFundVersionId());

        // init fund
        ArrFund arrFund = output.getFund();
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

        logger.info("Output model initialization ended, outputId:{}", params.getOutputId());
    }

    private Institution createInstitution(ArrFund arrFund) {

        ParInstitution parInst = institutionRepository.findByFundFetchTypeAndAccessPoint(arrFund);
        Institution inst = new Institution(parInst.getInternalCode(), parInst.getInstitutionType());

        inst.setRecord(getRecord(parInst.getAccessPoint()));

        return inst;
    }

    /**
     * Creates output tree with root same as {@link ArrFundVersion#getRootNodeId()}. Tree contains
     * output subtrees and their paths to fund root.
     *
     * @return NodeId tree root.
     */
    private NodeId createNodeIdTree(List<Integer> outputNodeIds, Integer fundVersionId) {
        FundTree fundTree = fundTreeProvider.getFundTree(fundVersionId);

        Map<Integer, NodeId> nodeIdMap = new HashMap<>();

        startNodes.addAll(outputNodeIds);
        for (Integer outputNodeId : outputNodeIds) {
            TreeNode treeNode = fundTree.getNode(outputNodeId);

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
                    .set("outputName", name);
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

        ApState apState = apStateRepository.findLastByAccessPoint(ap);

        RecordType type = getAPType(apState.getApTypeId());
        record = new Record(ap, type, staticData, apStateRepository, bindingRepository, partRepository, bindingStateRepository);
        List<ApPart> apParts = partRepository.findValidPartByAccessPoint(ap);
        List<ApItem> apItems = itemRepository.findValidItemsByAccessPoint(ap);
        List<Part> parts = new ArrayList<>(apParts.size());
        for (ApPart apPart : apParts) {
            Part part = new Part(apPart, staticData);
            List<ApItem> partItems = new ArrayList<>();
            for(ApItem apItem : apItems) {
                if(apItem.getPart().getPartId().intValue() == part.getPartId()) {
                    partItems.add(apItem);
                }
            }
           // List<ApItem> apItems = itemRepository.findValidItemsByPartId(part.getPartId());

            part.setItems(convertItems(partItems));
            if(part.getPartId() == ap.getPreferredPart().getPartId()) {
                record.setPreferredPart(part);
            }
            parts.add(part);
        }
        parts = Collections.unmodifiableList(parts);
        record.setParts(parts);

        // add to lookup
        apIdMap.put(ap.getAccessPointId(), record);


        return record;
    }

    @Override
    public Node getNode(ArrNode arrNode) {
        Node node = nodeIdMap.get(arrNode.getNodeId());

        if (node != null) {
            return node;
        }

        //TODO implement
        return null;
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

    @Override
    public Locale getLocale() {
        return elzaLocale.getLocale();
    }

    @Transactional(value = TxType.MANDATORY)
    @Override
    public List<Structured> createStructObjList(String structTypeCode) {
        // get struct item
        StructType structType = staticData.getStructuredTypeByCode(structTypeCode);
        List<ArrStructuredObject> sobs = structObjRepos
                .findStructureDataBySubtreeNodeIds(this.startNodes,
                        structType.getStructuredTypeId(),
                        false);

        List<Structured> result = new ArrayList<>(sobs.size());
        for (ArrStructuredObject sob : sobs) {
            Structured s = Structured.newInstance(sob, this);
            result.add(s);
        }
        return result;
    }

    @Override
    public List<Item> loadStructItems(Integer structObjId) {
        List<ArrStructuredItem> items = structItemRepos.findByStructuredObjectAndDeleteChangeIsNullFetchData(
                structObjId);
        List<Item> result = convertItems(items);
        return result;
    }
}
