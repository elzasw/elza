package cz.tacr.elza.print;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import cz.tacr.elza.service.StructObjService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import cz.tacr.elza.common.db.HibernateUtils;
import cz.tacr.elza.config.export.ExportConfig;
import cz.tacr.elza.core.ElzaLocale;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.core.data.StructType;
import cz.tacr.elza.core.fund.FundTree;
import cz.tacr.elza.core.fund.FundTreeProvider;
import cz.tacr.elza.core.fund.TreeNode;
import cz.tacr.elza.dataexchange.output.filters.ApplyFilter;
import cz.tacr.elza.dataexchange.output.filters.FilterRule;
import cz.tacr.elza.dataexchange.output.filters.FilterRules;
import cz.tacr.elza.dataexchange.output.filters.ReplaceItem;
import cz.tacr.elza.dataexchange.output.filters.SoiLoadDispatcher;
import cz.tacr.elza.dataexchange.output.sections.StructObjectInfoLoader;
import cz.tacr.elza.dataexchange.output.writer.StructObjectInfo;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataStructureRef;
import cz.tacr.elza.domain.ArrFile;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.ArrStructuredObject;
import cz.tacr.elza.domain.ParInstitution;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulOutputType;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.print.item.ItemSpec;
import cz.tacr.elza.print.item.ItemType;
import cz.tacr.elza.print.item.convertors.ItemConvertorContext;
import cz.tacr.elza.print.item.convertors.OutputItemConvertor;
import cz.tacr.elza.print.party.Institution;
import cz.tacr.elza.repository.ApBindingRepository;
import cz.tacr.elza.repository.ApBindingStateRepository;
import cz.tacr.elza.repository.ApIndexRepository;
import cz.tacr.elza.repository.ApItemRepository;
import cz.tacr.elza.repository.ApStateRepository;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.ExceptionThrow;
import cz.tacr.elza.repository.FundRepository;
import cz.tacr.elza.repository.InstitutionRepository;
import cz.tacr.elza.repository.StructuredItemRepository;
import cz.tacr.elza.repository.StructuredObjectRepository;
import cz.tacr.elza.service.cache.NodeCacheService;
import cz.tacr.elza.service.cache.RestoredNode;
import cz.tacr.elza.service.output.OutputParams;

public class OutputModel implements Output, NodeLoader, ItemConvertorContext {

    private static final Logger logger = LoggerFactory.getLogger(OutputModel.class);

    /* internal fields */

    private ArrFundVersion fundVersion;

    private StaticDataProvider sdp;

    /* general description */

    private List<cz.tacr.elza.print.item.Item> outputItems;

    private String name;

    private String internalCode;

    private String type;

    private String typeCode;

    private Fund fund;

    private UUID uuid;

    private String appVersion;

    /* lookups */

    private final Map<Integer, ItemType> itemTypeIdMap = new HashMap<>();

    private final Map<Integer, ItemSpec> itemSpecIdMap = new HashMap<>();

    private final Map<Integer, Record> apIdMap = new HashMap<>();

    private final Map<Integer, RecordType> apTypeIdMap = new HashMap<>();

    private final Map<Integer, Node> nodeIdMap = new HashMap<>();

    private final Map<Integer, Structured> structObjIdMap = new HashMap<>();

    private final Map<Integer, File> fileIdMap = new HashMap<>();

    private final Map<Integer, Fund> fundIdMap = new HashMap<>();

    final Set<Integer> restrictedNodeIds = new HashSet<>();

    /**
     * Filtered records have references to initialized Nodes (RecordWithLinks) which is reason why
     * we keep only last loaded instance instead of complete map.
     */
    private FilteredRecords lastFilteredRecords;

    /* managed components */

    private final StaticDataService staticDataService;

    private final FundRepository fundRepository;

    private final FundTreeProvider fundTreeProvider;

    private final NodeCacheService nodeCacheService;

    private final InstitutionRepository institutionRepository;

    private final ApStateRepository apStateRepository;

    private final ApBindingRepository bindingRepository;

    private final ApItemRepository itemRepository;

    private final ApBindingStateRepository bindingStateRepository;

    private final ApIndexRepository indexRepository;

    private final StructuredObjectRepository structObjRepos;

    private final DaoLinkRepository daoLinkRepository;

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

    private OutputItemConvertor itemConvertor = new OutputItemConvertor(this);

    private OutputFilterConfig outputFilterConfig;

    private FilterRules filterRules;

    private StructObjectInfoLoader soiLoader;

    private Map<Integer, StructObjectInfo> structRestrDefsMap = new HashMap<>();

    private OutputContext outputContext;

    private final ExportConfig exportConfig;

    private StructObjService structObjService;

    public OutputModel(final OutputContext outputContext,
                       final StaticDataService staticDataService,
                       final ElzaLocale elzaLocale,
                       final FundRepository fundRepository,
                       final FundTreeProvider fundTreeProvider,
                       final NodeCacheService nodeCacheService,
                       final InstitutionRepository institutionRepository,
                       final ApStateRepository apStateRepository,
                       final ApBindingRepository bindingRepository,
                       final AttPageProvider attPageProvider,
                       final StructuredObjectRepository structObjRepos,
                       final StructuredItemRepository structItemRepos,
                       final ApItemRepository itemRepository,
                       final ApBindingStateRepository bindingStateRepository,
                       final ApIndexRepository indexRepository,
                       final DaoLinkRepository daoLinkRepository,
                       final ExportConfig exportConfig,
                       final StructObjService structObjService,
                       final EntityManager em) {
        this.outputContext = outputContext;
        this.staticDataService = staticDataService;
        this.elzaLocale = elzaLocale;
        this.fundRepository = fundRepository;
        this.fundTreeProvider = fundTreeProvider;
        this.nodeCacheService = nodeCacheService;
        this.institutionRepository = institutionRepository;
        this.apStateRepository = apStateRepository;
        this.bindingRepository = bindingRepository;
        this.attPageProvider = attPageProvider;
        this.structObjRepos = structObjRepos;
        this.structItemRepos = structItemRepos;
        this.itemRepository = itemRepository;
        this.bindingStateRepository = bindingStateRepository;
        this.indexRepository = indexRepository;
        this.daoLinkRepository = daoLinkRepository;
        this.exportConfig = exportConfig;
        this.structObjService = structObjService;
        this.soiLoader = new StructObjectInfoLoader(em, 1, staticDataService.getData());
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
    public List<cz.tacr.elza.print.item.Item> getItems() {
        return outputItems;
    }

    public UUID getUuid() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        return uuid;
    }

    public String getAppVersion() {
        if (appVersion == null) {
            appVersion = staticDataService.getAppVersion();
        }
        return appVersion;
    }

    @Override
    public List<cz.tacr.elza.print.item.Item> getItems(Collection<String> typeCodes) {
        Validate.notNull(typeCodes);

        return outputItems.stream().filter(item -> {
            String tc = item.getType().getCode();
            return typeCodes.contains(tc);
        }).collect(Collectors.toList());
    }

    @Override
    public List<cz.tacr.elza.print.item.Item> getItemsWithout(Collection<String> typeCodes) {
        Validate.notNull(typeCodes);

        return outputItems.stream().filter(item -> {
            String tc = item.getType().getCode();
            return !typeCodes.contains(tc);
        }).collect(Collectors.toList());
    }

    @Override
    public List<Record> getParties(Collection<String> typeCodes) {
        Validate.notNull(typeCodes);

        Set<Integer> distinctPartyIds = new HashSet<>();
        List<Record> parties = new ArrayList<>();

        for (cz.tacr.elza.print.item.Item item : outputItems) {
            String tc = item.getType().getCode();
            if (!typeCodes.contains(tc)) {
                continue;
            }
            Record party = item.getValue(Record.class);
            if (distinctPartyIds.add(party.getId())) {
                parties.add(party);
            }
        }

        return parties;
    }

    @Override
    public List<Structured> getStructured(Collection<String> typeCodes) {
        Validate.notNull(typeCodes);

        Set<Integer> distinctIds = new HashSet<>();
        List<Structured> objs = new ArrayList<>();

        for (cz.tacr.elza.print.item.Item item : outputItems) {
            String tc = item.getType().getCode();
            if (!typeCodes.contains(tc)) {
                continue;
            }
            Structured sobj = item.getValue(Structured.class);
            if (distinctIds.add(sobj.getId())) {
                objs.add(sobj);
            }
        }

        return objs;
    }

    @Override
    public cz.tacr.elza.print.item.Item getSingleItem(String typeCode) {
        Validate.notEmpty(typeCode);

        cz.tacr.elza.print.item.Item found = null;
        for (cz.tacr.elza.print.item.Item item : outputItems) {
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
        cz.tacr.elza.print.item.Item found = getSingleItem(itemTypeCode);
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
        RecordsFilter filter = new RecordsFilter();
        filter.addType(typeCode);

        return getFilteredRecords(filter);
    }

    @Override
    public FilteredRecords getFilteredRecords(RecordsFilter filter) {
        if (filter == null) {
            throw new BusinessException("Filter is null", BaseCode.INVALID_STATE);
        }

        if (lastFilteredRecords == null || !lastFilteredRecords.getFilter().equals(filter)) {
            lastFilteredRecords = filterRecords(filter);
        }
        return lastFilteredRecords;
    }

    /**
     * Prepare filtered list of records
     */
    private FilteredRecords filterRecords(RecordsFilter filter) {
        FilteredRecords filteredAPs = new FilteredRecords(elzaLocale, filter);

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

    public Iterator<Record> getRecords() {
        Iterator<NodeId> nodeIdIterator = fund.getRootNodeId().getIteratorDFS();
        NodeIterator nodeIterator = new NodeIterator(this, nodeIdIterator);

        RecordIterator ri = new RecordIterator(this, nodeIterator);
        return ri;
    }

    @Override
    public List<Node> loadNodes(Collection<NodeId> nodeIds) {
        Validate.isTrue(TransactionSynchronizationManager.isActualTransactionActive());
        Validate.isTrue(isInitialized());

        List<Integer> arrNodeIds = new ArrayList<>(nodeIds.size());

        if (fundVersion.getLockChange() != null) {
            throw new NotImplementedException("Load nodes for closed fund version not implemented");
        }

        for (NodeId nodeId : nodeIds) {
            arrNodeIds.add(nodeId.getArrNodeId());
        }

        Map<Integer, RestoredNode> cachedNodeMap = nodeCacheService.getNodes(arrNodeIds);
        List<Node> nodes = new ArrayList<>(nodeIds.size());
        Map<Integer, Node> daoLinkMap = new HashMap<>();

        Map<Integer, List<ArrItem>> levelRestrMap = new HashMap<>();

        for (NodeId nodeId : nodeIds) {
            Integer arrNodeId = nodeId.getArrNodeId();
            RestoredNode cachedNode = cachedNodeMap.get(arrNodeId);
            Validate.notNull(cachedNode);

            // get list of restriction id relevant filter conditions
            List<ArrItem> restrictionItems = getRestrictionItems(cachedNode);

            // expand restriction list to include parent(s) restriction list
            if (nodeId.getParent() != null) {
                List<ArrItem> restParentItems = levelRestrMap.get(nodeId.getParent().getArrNodeId());
                if (restParentItems != null) {
                    restrictionItems.addAll(restParentItems);
                }
            }

            // if we have a list - we have to filter
            if (!restrictionItems.isEmpty()) {
                levelRestrMap.put(nodeId.getArrNodeId(), restrictionItems);

                cachedNode = filterNode(nodeId, cachedNode, restrictionItems);
                if (cachedNode == null) {
                    // if filter return null according to conditions
                    continue;
                }
            }

            Node node = createNode(nodeId, cachedNode);
            nodes.add(node);
            // prepare map for daolinks
            if (CollectionUtils.isNotEmpty(cachedNode.getDaoLinks())) {
                for (ArrDaoLink daoLink : cachedNode.getDaoLinks()) {
                    daoLinkMap.put(daoLink.getDaoLinkId(), node);
                }
            }
        }
        // read dao links
        if (!daoLinkMap.isEmpty()) {
            List<ArrDaoLink> daoLinks = daoLinkRepository.findByNodeIdsAndFetchDao(arrNodeIds);
            Validate.isTrue(daoLinks.size() == daoLinkMap.size());

            for (ArrDaoLink daoLink : daoLinks) {
                Node node = daoLinkMap.get(daoLink.getDaoLinkId());
                Validate.notNull(node);

                Dao dao = new Dao(daoLink);
                node.addDao(dao);
            }
        }

        return nodes;
    }

    /**
     * Return list of items with restriction definitions
     *
     * @param node
     * @return List<Integer>
     */
    private List<ArrItem> getRestrictionItems(RestoredNode node) {
        if (filterRules == null) {
            return Collections.emptyList();
        }
        List<ArrItem> restrictionItems = new ArrayList<>();
        for (ArrItem item : node.getDescItems()) {
            // RulItemType itemType = item.getItemType();
            for (cz.tacr.elza.core.data.ItemType i : filterRules.getRestrictionTypes()) {
                if (item.getData() == null) {
                    continue;
                }
                if (i.getItemTypeId().equals(item.getItemTypeId())) {
                    restrictionItems.add(item);
                }
            }
        }
        return restrictionItems;
    }

    private RestoredNode filterNode(NodeId nodeId, RestoredNode node, List<ArrItem> restrictionItems) {
        NodeId parentNodeId = nodeId.getParent();
        if (parentNodeId != null && restrictedNodeIds.contains(parentNodeId.getArrNodeId())) {
            restrictedNodeIds.add(nodeId.getArrNodeId());
            return null;
        }

        if (filterRules == null || CollectionUtils.isEmpty(restrictionItems)) {
            return node;
        }

        Map<cz.tacr.elza.core.data.ItemType, List<ArrItem>> itemsByType = node.getDescItems().stream()
                .collect(Collectors.groupingBy(item -> sdp.getItemTypeById(item.getItemTypeId())));

        ApplyFilter filter = new ApplyFilter();

        for (ArrItem restrictionItem : restrictionItems) {
            cz.tacr.elza.core.data.ItemType itemType = sdp.getItemTypeById(restrictionItem.getItemTypeId());

            Collection<? extends ArrItem> soiItems;
            if (itemType.getDataType().equals(DataType.STRUCTURED)) {
                ArrData data = HibernateUtils.unproxy(restrictionItem.getData());
                if (data == null) {
                    continue;
                }
                ArrDataStructureRef dsr = (ArrDataStructureRef) data;
                Integer restrictionId = dsr.getStructuredObjectId();
                StructObjectInfo soi = readSoiFromDB(restrictionId);
                soiItems = soi.getItems();
            } else {
                soiItems = node.getDescItems();
            }

            for (FilterRule rule : filterRules.getFilterRules()) {
                processRule(nodeId, rule, itemsByType, soiItems, filter);
            }
        }

        return filter.apply(node);
    }

    private void processRule(NodeId nodeId, FilterRule rule,
                             Map<cz.tacr.elza.core.data.ItemType, List<ArrItem>> itemsByType,
                             Collection<? extends ArrItem> restrItems,
                             ApplyFilter filter) {

        if (!rule.canApply(restrItems)) {
            // rule does not apply for this soi
            return;
        }

        // if we need to hide level
        if (rule.isHiddenLevel()) {
            restrictedNodeIds.add(nodeId.getArrNodeId());

            filter.hideLevel();
            return;
        }

        boolean changed = false;

        // hidden Dao
        if (rule.isHiddenDao()) {
            filter.hideDao();
            changed = true;
        }

        // check hidden items
        if (rule.getHiddenTypes() != null) {
            for (cz.tacr.elza.core.data.ItemType hiddenType : rule.getHiddenTypes()) {
                List<ArrItem> hiddenItems = itemsByType.get(hiddenType);
                if (CollectionUtils.isNotEmpty(hiddenItems)) {
                    hiddenItems.forEach(hi -> filter.addHideItem(hi));
                    changed = true;
                }
            }
        }

        // replace itemType(s)
        if (rule.getReplaceItems() != null) {
            for (ReplaceItem replaceDef : rule.getReplaceItems()) {
                List<ArrItem> replaceItems = itemsByType.get(replaceDef.getSource());
                if (CollectionUtils.isNotEmpty(replaceItems)) {
                    // source found -> store as new target
                    for (ArrItem replaceItem : replaceItems) {
                        List<ArrItem> replacedItems = itemsByType.get(replaceDef.getTarget());
                        // if exists ArrItem(s) with Target type
                        if (CollectionUtils.isNotEmpty(replacedItems)) {
                            // hide Source item
                            filter.addHideItem(replaceItem);
                            // copy from Source item
                            ArrItem copy = replaceItem.makeCopy();
                            // set Target type to copy of Source item
                            copy.setItemType(replaceDef.getTarget().getEntity());
                            filter.addItem(copy);
                            changed = true;
                        }
                    }
                }
            }
        }

        // add itemsOnChange if changed
        if (rule.getAddItemsOnChange() != null && changed) {
            rule.getAddItemsOnChange().forEach(i -> filter.addItem(i));
        }
        if (rule.getAddItems() != null) {
            rule.getAddItems().forEach(i -> filter.addItem(i));
        }
    }

    private StructObjectInfo readSoiFromDB(Integer structuredObjectId) {
        StructObjectInfo soi = structRestrDefsMap.get(structuredObjectId);
        if (soi != null) {
            return soi;
        }
        // read from DB
        SoiLoadDispatcher soiLoadDisp = new SoiLoadDispatcher();
        soiLoader.addRequest(structuredObjectId, soiLoadDisp);
        soi = soiLoadDisp.getResult();
        Validate.notNull(soi);

        structRestrDefsMap.put(structuredObjectId, soi);
        return soi;
    }

    private Node createNode(NodeId nodeId, RestoredNode cachedNode) {
        Validate.notNull(cachedNode);
        Integer fundId = cachedNode.getNode().getFundId();
        Fund fund = this.fundIdMap.computeIfAbsent(fundId, id -> {
            ArrFund arrFund = this.fundRepository.findById(id)
                    .orElseThrow(ExceptionThrow.fund(id));
            Fund f = new Fund(arrFund);
            Institution inst = createInstitution(arrFund);
            f.setInstitution(inst);
            return f;
        });

        Node node = new Node(fund, nodeId);
        node.load(cachedNode, itemConvertor);
        return node;
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
        this.sdp = staticDataService.getData();

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
        this.fund = new Fund(rootNodeId, this, arrFund);

        // init fund institution
        Institution inst = createInstitution(arrFund);
        this.fund.setInstitution(inst);
        this.fundIdMap.put(arrFund.getFundId(), fund);

        // init direct items
        this.outputItems = params.getOutputItems().stream()
                .map(i -> itemConvertor.convert(i))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // init output filters config
        this.outputFilterConfig = loadOutputFilterConfig(params.getOutputFilter());

        // init filter rules
        if (outputFilterConfig != null) {
            this.filterRules = new FilterRules(outputFilterConfig, staticDataService.getData());
        }

        logger.info("Output model initialization ended, outputId:{}", params.getOutputId());
    }

    private OutputFilterConfig loadOutputFilterConfig(Path outputFilterConfig) {
        if (outputFilterConfig == null) {
            return null;
        }
        // register type descriptors
        Constructor yamlCtor = new Constructor();
        yamlCtor.addTypeDescription(new TypeDescription(OutputFilterConfig.class, "!OutputFilterConfig"));
        Yaml yamlLoader = new Yaml(yamlCtor);

        OutputFilterConfig ofc;
        try (InputStream inputStream = new FileInputStream(outputFilterConfig.toFile())) {
            ofc = yamlLoader.load(inputStream);
        } catch (IOException e) {
            logger.error("Failed to read yaml file {}", outputFilterConfig, e);
            throw new SystemException(e);
        }

        return ofc;
    }

    private Institution createInstitution(ArrFund arrFund) {

        ParInstitution parInst = institutionRepository.findByFundFetchTypeAndAccessPoint(arrFund);
        Institution inst = new Institution(parInst.getInternalCode(), parInst.getInstitutionType());

        inst.setRecord(getRecordById(parInst.getAccessPoint().getAccessPointId()));

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
            NodeId nodeId = createNodeIdWithParents(treeNode, nodeIdMap, true);

            // convert node tree of each output node child to NodeIds
            treeNode.getChildren().forEach(child -> initNodeIdSubtree(child, nodeId, nodeIdMap));
        }

        return nodeIdMap.get(fundTree.getRoot().getNodeId());
    }

    /**
     * Creates NodeId with all parent nodes up to root.
     */
    private NodeId createNodeIdWithParents(TreeNode treeNode, Map<Integer, NodeId> nodeIdMap, boolean published) {
        Integer arrNodeId = treeNode.getNodeId();

        NodeId nodeId = nodeIdMap.get(arrNodeId);
        if (nodeId != null) {
            return nodeId;
        }

        if (treeNode.isRoot()) {
            nodeId = new NodeId(treeNode.getNodeId(), treeNode.getPosition(), published);
        } else {
            // recursively create parents up to root or existing node
            // such nodes exists or are created, created nodes are marked as not published
            NodeId parentNodeId = createNodeIdWithParents(treeNode.getParent(), nodeIdMap, false);
            nodeId = new NodeId(treeNode.getNodeId(), parentNodeId, treeNode.getPosition(), published);
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

        // method is called from published node
        NodeId nodeId = new NodeId(treeNode.getNodeId(), parentNodeId, treeNode.getPosition(), true);
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
    public Record getRecordById(Integer accessPointId) {
        // id without fetch -> access type property
        Record record = apIdMap.get(accessPointId);
        if (record != null) {
            return record;
        }

        ApState apState = apStateRepository.findLastByAccessPointId(accessPointId);

        RecordType type = getAPType(apState.getApTypeId());
        record = new Record(outputContext, apState, type,
                bindingRepository, itemRepository,
                bindingStateRepository, indexRepository,
                itemConvertor, elzaLocale);

        // add to lookup
        apIdMap.put(accessPointId, record);

        return record;
    }

    @Override
    public Node getNode(ArrNode arrNode) {
        Integer id = arrNode.getNodeId();
        Node node = nodeIdMap.get(id);

        if (node != null) {
            return node;
        }

        RestoredNode cachedNode = nodeCacheService.getNode(id);
        Validate.notNull(cachedNode);

        NodeId nodeId = new RefNodeId(id);
        node = createNode(nodeId, cachedNode);

        nodeIdMap.put(id, node);
        return node;
    }

    private RecordType getAPType(Integer apTypeId) {
        Validate.notNull(apTypeId);

        RecordType type = apTypeIdMap.get(apTypeId);
        if (type != null) {
            return type;
        }

        ApType apType = sdp.getApTypeById(apTypeId);

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

        RulItemType rulItemType = sdp.getItemTypeById(id).getEntity();
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

        RulItemSpec rulItemSpec = sdp.getItemSpecById(id);
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
        StructType structType = sdp.getStructuredTypeByCode(structTypeCode);
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
    public List<cz.tacr.elza.print.item.Item> loadStructItems(Integer structObjId) {
        List<ArrStructuredItem> items = structObjService.findByStructObjIdAndDeleteChangeIsNullFetchData(
                structObjId);
        List<cz.tacr.elza.print.item.Item> result = convert(items, itemConvertor);
        return result;
    }

    static public List<cz.tacr.elza.print.item.Item> convert(List<? extends ArrItem> items, OutputItemConvertor conv) {
        return items.stream()
                .map(i -> conv.convert(i))
                .filter(Objects::nonNull)
                .sorted(cz.tacr.elza.print.item.Item::compareTo)
                .collect(Collectors.toList());
    }

    @Override
    public ExportConfig getExportConfig() {
        return exportConfig;
    }
}
