package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.google.common.eventbus.Subscribe;

import cz.tacr.elza.EventBusListener;
import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.config.ConfigView;
import cz.tacr.elza.config.view.LevelConfig;
import cz.tacr.elza.config.view.ViewTitles;
import cz.tacr.elza.controller.ArrangementController.Depth;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.AccordionNodeVO;
import cz.tacr.elza.controller.vo.ArrDigitizationRequestVO;
import cz.tacr.elza.controller.vo.ArrRequestVO;
import cz.tacr.elza.controller.vo.NodeConformityVO;
import cz.tacr.elza.controller.vo.NodeItemWithParent;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.TreeNodeWithFundVO;
import cz.tacr.elza.controller.vo.WfSimpleIssueVO;
import cz.tacr.elza.controller.vo.nodes.ArrNodeExtendVO;
import cz.tacr.elza.controller.vo.nodes.NodeData;
import cz.tacr.elza.controller.vo.nodes.NodeDataParam;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrDao;
import cz.tacr.elza.domain.ArrDao.DaoType;
import cz.tacr.elza.domain.ArrDaoLink;
import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.domain.WfIssue;
import cz.tacr.elza.domain.vo.TitleValue;
import cz.tacr.elza.domain.vo.TitleValues;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.DaoLinkRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.LevelRepositoryCustom;
import cz.tacr.elza.repository.LevelRepositoryCustom.LevelInfo;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.security.UserDetail;
import cz.tacr.elza.security.UserPermission;
import cz.tacr.elza.service.event.CacheInvalidateEvent;
import cz.tacr.elza.service.eventnotification.EventChangeMessage;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;
import cz.tacr.elza.service.eventnotification.events.EventAddNode;
import cz.tacr.elza.service.eventnotification.events.EventDeleteNode;
import cz.tacr.elza.service.eventnotification.events.EventIdInVersion;
import cz.tacr.elza.service.eventnotification.events.EventNodeMove;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.eventnotification.events.EventVersion;
import cz.tacr.elza.service.vo.TitleItemsByType;


/**
 * Servistní třída pro načtení a cachování uzlů ve stromu daných verzí.
 *
 * @since 11.01.2016
 */
@Service
@EventBusListener
public class LevelTreeCacheService implements NodePermissionChecker {

    private static final Logger logger = LoggerFactory.getLogger(LevelTreeCacheService.class);

    /**
     * Maximální počet verzí stromů ukládaných současně v paměti.
     */
    @Value("${elza.levelTreeCache.size:30}")
    private int maxCacheSize = 30;

    /**
     * Příznak zobrazení DaoId.
     */
    @Value("${elza.levelTreeCache.display.daoId:true}")
    private boolean displayDaoId = true;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ClientFactoryVO clientFactoryVO;

    @Autowired
    private ConfigView configView;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private ArrangementService arrangementService;

    @Autowired
    private ArrangementFormService formService;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private IssueDataService issueDataService;

    @Autowired
    private UserService userService;

    @Autowired
    private DaoLinkRepository daoLinkRepository;

    /**
     * Cache stromu pro danou verzi. (id verze -> nodeid uzlu -> uzel).
     * Maximální počet záznamů v cache {@link #maxCacheSize}.
     */
    private CapacityMap<Integer, Map<Integer, TreeNode>> versionCache = new CapacityMap<Integer, Map<Integer, TreeNode>>();

    @Subscribe
    public synchronized void invalidateCache(final CacheInvalidateEvent cacheInvalidateEvent) {
        if (cacheInvalidateEvent.contains(CacheInvalidateEvent.Type.LEVEL_TREE)) {
            logger.info("Invalidating LevelTreeCacheService");
            versionCache = new CapacityMap<>();
        }
    }

    /**
     * Vytvoří plochý seznam stromu podle otevřených uzlů v dané verzi.
     *
     * @param versionId   verze stromu
     * @param nodeId      id uzlu, jehož podstrom chceme vrátit. Pokud není uvedeno, vrací se od kořene stromu.
     * @param expandedIds seznam otevřených uzlů
     * @param includeIds  seznam uzlů, které chceme zviditelnit ve stromu (jejich předci budou rozbaleni, pokud nejsou)
     * @return data stromu pro otevřené uzly
     */
    public TreeData getFaTree(final Integer versionId,
                              @Nullable final Integer nodeId,
                              final Set<Integer> expandedIds,
                              final Set<Integer> includeIds) {

        ArrFundVersion version = arrangementService.getFundVersion(versionId);
        Map<Integer, TreeNode> treeMap = getVersionTreeCache(version);

        Set<Integer> parentNodeIds = getAllParentsWithoutError(includeIds, treeMap);
        if (nodeId == null) {
            // add root node
            parentNodeIds.add(version.getRootNode().getNodeId());
        } else {
            parentNodeIds.add(nodeId);
        }
        if (expandedIds != null) {
            // pridani rodicu rozbalenych uzlu
            parentNodeIds.addAll(getAllParentsWithoutError(expandedIds, treeMap));
            parentNodeIds.addAll(expandedIds);
        }

        Map<Integer, TreeNode> expandedNodes = new TreeMap<Integer, TreeNode>();
        for (Integer expandedId : parentNodeIds) {
            TreeNode treeNode = treeMap.get(expandedId);
            if(treeNode != null){
                createExpandedTreeNodeMap(treeNode, expandedNodes);
            }
        }

        //seřazené otevřené nody tak jak půjdou v seznamu po sobě
        TreeSet<TreeNode> expandedSort = new TreeSet<>(expandedNodes.values());
        Iterator<TreeNode> expandedIterator = expandedSort.iterator();

        LinkedHashMap<Integer, TreeNode> nodesMap = new LinkedHashMap<>();

        if (nodeId == null) {
            TreeNode nextNode = expandedIterator.next();
            nodesMap.put(nextNode.getId(), nextNode);   //přidáme kořen na začátek seznamu
            addNodesToResultList(nodesMap, nextNode, expandedIterator);

            while (expandedIterator.hasNext()) {
                addNodesToResultList(nodesMap, expandedIterator.next(), expandedIterator);

            }
        } else {
            while (expandedIterator.hasNext()) {
                TreeNode next = expandedIterator.next();
                if (next.getId().equals(nodeId)) {
                    addNodesToResultList(nodesMap, next, expandedIterator);
                    break;
                }
            }
        }

        TreeNode rootNode = nodeId == null ? treeMap.get(version.getRootNode().getNodeId())
                                           : treeMap.get(nodeId);

        NodeParam param = NodeParam.create()
                .name()
                .icon()
                .referenceMark();

        LinkedHashMap<Integer, Node> nodes = getNodes(nodesMap, rootNode, param, version);

        boolean fullArrPerm = userService.hasFullArrPerm(version.getFundId());
        return new TreeData(convertToTreeNodeWithPerm(nodes.values(), version, fullArrPerm), parentNodeIds, fullArrPerm);
    }

    /**
     * Načte seznam nodů podle jejich id v dané verzi AP. Vrácený seznam je ve stejném pořadí jako id.
     *
     * @param nodeIds   id uzlů
     * @param versionId id verze stromu
     * @return nalezené uzly
     */
    public List<TreeNodeVO> getNodesByIds(final Collection<Integer> nodeIds,
                                          final Integer versionId) {

        ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);
        return getNodesByIds(nodeIds, version);
    }

    public List<TreeNodeVO> getNodesByIds(final Collection<Integer> nodeIds,
                                          final ArrFundVersion version) {
        Map<Integer, TreeNode> versionTreeCache = getVersionTreeCache(version);

        Map<Integer, TreeNode> subTreeMap = new LinkedHashMap<>();
        for (Integer nodeId : nodeIds) {
            TreeNode treeNode = versionTreeCache.get(nodeId);
            if (treeNode != null) {
                TreeNode parent = treeNode;
                while (parent != null && subTreeMap.get(parent) == null) {
                    subTreeMap.put(parent.getId(), parent);
                    parent = parent.getParent();
                }
            } else {
                logger.error("Node not found in levelTreeCache, nodeId: {}", nodeId);
                throw new SystemException("Node not found, nodeId: " + nodeId, BaseCode.INVALID_STATE)
                        .set("nodeId", nodeId);
            }
        }
        return getNodesByIds(nodeIds, version, subTreeMap);
    }

    public List<TreeNodeVO> getNodesByIds(final Collection<Integer> nodeIds,
                                          final ArrFundVersion version,
                                          Map<Integer, TreeNode> subTreeMap) {
        Map<Integer, TitleItemsByType> valuesMap = createValuesMap(subTreeMap, version, null);
        Map<Integer, TreeNodeVO> clientMap = createNodesWithTitles(subTreeMap, valuesMap, null, version);

        List<TreeNodeVO> result = new LinkedList<>();
        ViewTitles viewTitles = configView.getViewTitles(version.getRuleSetId(), version.getFund().getFundId());

        for (Integer nodeId : nodeIds) {
            TreeNode treeNode = subTreeMap.get(nodeId);
            if(treeNode != null){
                String[] referenceMark = createClientReferenceMarkFromRoot(treeNode, viewTitles, valuesMap);
                TreeNodeVO clientNode = clientMap.get(nodeId);
                clientNode.setReferenceMark(referenceMark);
                result.add(clientNode);
            } else {
                logger.error("Node not found in levelTreeCache, nodeId: {}", nodeId);
                throw new SystemException("Node not found, nodeId: " + nodeId, BaseCode.INVALID_STATE)
                        .set("nodeId", nodeId);
            }
        }

        return result;
    }

    /**
     * Vyhledá a seřadí JP.
     *
     * @param nodeIds        seznam id potřebných uzlů
     * @param fundVersion    verze AS
     * @return seznam JP
     */
    public List<NodeItemWithParent> getNodeItemsWithParents(final List<Integer> nodeIds,
                                                            final ArrFundVersion fundVersion) {

        List<Integer> nodeIdsSort = sortNodesByTreePosition(nodeIds, fundVersion);

        Map<Integer, TreeNode> versionTreeCache = getVersionTreeCache(fundVersion);
        List<Integer> allNodeIds = new ArrayList<>();

        for (Integer nodeId : nodeIdsSort) {
            TreeNode treeNode = versionTreeCache.get(nodeId);
            allNodeIds.add(nodeId);
            TreeNode parent = treeNode.getParent();
            if (parent != null) {
                allNodeIds.add(parent.getId());
            }
        }

        Collection<TreeNodeVO> treeNodes = getFaTreeNodes(fundVersion.getFundVersionId(), allNodeIds);
        Map<Integer, TreeNodeVO> mapTreeNodes = treeNodes.stream().collect(Collectors.toMap(TreeNodeVO::getId, (p) -> p));

        List<NodeItemWithParent> nodeItemWithParents = new ArrayList<>(nodeIdsSort.size());
        for (Integer nodeId : nodeIdsSort) {
            NodeItemWithParent nodeItemWithParent = new NodeItemWithParent();
            nodeItemWithParent.setId(nodeId);
            nodeItemWithParent.setName(mapTreeNodes.get(nodeId).getName());
            TreeNode parent = versionTreeCache.get(nodeId).getParent();
            if (parent != null) {
                nodeItemWithParent.setParentNode(mapTreeNodes.get(parent.getId()));
            }
            nodeItemWithParents.add(nodeItemWithParent);
        }

        return nodeItemWithParents;
    }

    /**
     * Najde v cache seznam rodičů daného uzlu. Seřazeno od prvního rodiče po kořen stromu.
     *
     * @param node    JP
     * @param fundVersion verze stromu
     * @return seznam rodičů
     */
    public Collection<TreeNodeVO> getNodeParents(final Map<Integer, TreeNode> treeMap, final TreeNode node, final ArrFundVersion fundVersion) {

        LinkedHashMap<Integer, TreeNode> parentMap = new LinkedHashMap<>();
        LinkedList<TreeNode> parents = new LinkedList<>();

        TreeNode parent = node.getParent();
        while (parent != null) {
            parents.addFirst(parent);
            parent = parent.getParent();
        }

        for (TreeNode p : parents) {
            parentMap.put(p.getId(), p);
        }

        Map<Integer, TitleItemsByType> valuesMap = createValuesMap(parentMap, fundVersion, node);
        Map<Integer,TreeNodeVO> resultMap = createNodesWithTitles(parentMap, valuesMap, node, fundVersion);

        List<TreeNodeVO> result = new ArrayList<>(resultMap.values());

        Collections.reverse(result);
        return result;
    }


    /**
     * Přidá do výsledného seznamu data uzlu a jeho dětí rekurzivně.
     *
     * @param nodesMap            výsledný seznam
     * @param node                  uzel, jehož potomky budeme přidávat
     * @param expandedNodesIterator iterátor otevřených uzlů
     * @return následující otevřený uzel
     */
    private TreeNode addNodesToResultList(final LinkedHashMap<Integer, TreeNode> nodesMap,
                                          final TreeNode node,
                                          final Iterator<TreeNode> expandedNodesIterator) {
        if (!expandedNodesIterator.hasNext()) {
            for (TreeNode child : node.getChilds()) {
                nodesMap.put(child.getId(), child);
            }
            return null;
        }

        TreeNode nextNode = expandedNodesIterator.next();

        //pokud má aktuální uzel a další otevřený uzel stejného rodiče, musíme postupně přidávat, protože v jeho dětech jsou další otevřené uzly
        if (nextNode.getParent().equals(node)) {
            for (TreeNode child : node.getChilds()) {
                nodesMap.put(child.getId(), child);
                //až najdeme další otevřený uzel v potomcích, přidáme jeho potomky do seznamu
                if (child.equals(nextNode)) {
                    nextNode = addNodesToResultList(nodesMap, nextNode, expandedNodesIterator);
                }
            }
        } else {
            //pokud potomci nejsou otevření, můžeme je všechny přidat do seznamu
            for (TreeNode child : node.getChilds()) {
                nodesMap.put(child.getId(), child);
            }
        }

        return nextNode;
    }


    /**
     * Vytvoří množinu rodičovských uzlů, které musejí být viditelné pro zobrazení vybraných uzlů.
     *
     * @param includedIds seznam id uzlů, které mají být viditelné
     * @param treeMap     mapa všech uzlů ve stromu
     * @return množinu nodeid uzlů, které musejí být rozbalené, aby byly vybrané uzly viditelné
     */
    private Set<Integer> getAllParentsWithoutError(final Set<Integer> includedIds,
                                                    final Map<Integer, TreeNode> treeMap) {
        Set<Integer> result = new HashSet<>();

        if (CollectionUtils.isNotEmpty(includedIds)) {

            for (Integer includedId : includedIds) {
                TreeNode node = treeMap.get(includedId);
                if (node == null) {
                    // pripadne neexistujici uzly jsou ignorovany
                    // jejich ID mohou zustat v cache klienta
                    continue;
                }
                TreeNode parent = node.getParent();
                while (parent != null) {
                    result.add(parent.getId());
                    parent = parent.getParent();
                }
            }
        }
        return result;
    }


    /**
     * Vytvoří cache stromu. Načte všechny položky stormu a vytvoří z nich mapu uzlů s potomky a rodiči.
     *
     * @param version verze stromu
     * @return mapu všech uzlů stromu (nodeid uzlu --> uzel)
     */
    private Map<Integer, TreeNode> createVersionTreeCache(final ArrFundVersion version) {
        Validate.notNull(version, "Verze AS musí být vyplněna");

        Integer rootId = version.getRootNode().getNodeId();
        ArrChange change = version.getLockChange();

        //všechny uzly stromu
        return createTreeNodeMap(change, rootId);
    }

    public Map<Integer, TreeNode> createTreeNodeMap(ArrChange change, Integer rootNodeId) {

        //kořen
        LevelRepositoryCustom.LevelInfo rootInfo = new LevelRepositoryCustom.LevelInfo(rootNodeId, 0, null);

        List<LevelRepositoryCustom.LevelInfo> levelInfos = levelRepository.readTree(change, rootNodeId);


        // mapa všech základních dat uzlů
        // toMap zajisti provedeni kontroly duplicit
        Map<Integer, LevelRepositoryCustom.LevelInfo> levelInfoMap = levelInfos.stream()
                .collect(Collectors.toMap(LevelRepositoryCustom.LevelInfo::getNodeId, Function.identity()));
        levelInfoMap.put(rootNodeId, rootInfo);

        //výsledná mapa
        Map<Integer, TreeNode> allMap = new HashMap<>();
        for (LevelRepositoryCustom.LevelInfo levelInfo : levelInfoMap.values()) {
            createTreeNodeMap(levelInfo, levelInfoMap, allMap);
        }

        //seřazení dětí všech uzlů podle pozice
        Comparator<TreeNode> comparator = (o1, o2) -> {
            int ret = o1.getPosition().compareTo(o2.getPosition());
            if (ret == 0) {
                // check same position
                logger.error("Two nodes on same position, nodeId: {}, nodeId: {}",
                             o1.getId(), o2.getId());
                throw new SystemException("Two nodes on same position.", BaseCode.DB_INTEGRITY_PROBLEM)
                        .set("nodeId", o1.getId())
                        .set("otherNodeId", o2.getId());
            }
            return ret;
        };
        for (TreeNode treeNode : allMap.values()) {
            treeNode.getChilds().sort(comparator);
        }

        initReferenceMarksAndDepth(allMap.get(rootNodeId));
        return allMap;
    }


    /**
     * Vytvoří mapu všech uzlů ve stromě. Přiřazuje děti a rodiče všech uzlů.
     *
     * @param levelInfo    data uzlu
     * @param levelInfoMap mapa všech dat uzlů
     * @param allNodesMap  mapa všech vytvořených uzlů
     * @return vytvořený uzel
     */
    // TODO: rework with recursive query
    private TreeNode createTreeNodeMap(final LevelRepositoryCustom.LevelInfo levelInfo,
                                       final Map<Integer, LevelRepositoryCustom.LevelInfo> levelInfoMap,
                                       final Map<Integer, TreeNode> allNodesMap) {
        TreeNode result = allNodesMap.get(levelInfo.getNodeId());
        if (result != null) {
            return result;
        }

        result = new TreeNode(levelInfo.getNodeId(), levelInfo.getPosition());
        allNodesMap.put(levelInfo.getNodeId(), result);

        if (levelInfo.getParentId() != null) {
            LevelInfo parentInfo = levelInfoMap.get(levelInfo.getParentId());
            Validate.notNull(parentInfo, "Missing node info, id: %s", levelInfo.getParentId());

            TreeNode parentNode = createTreeNodeMap(parentInfo, levelInfoMap, allNodesMap);
            result.setParent(parentNode);
            parentNode.addChild(result);
        }

        return result;
    }

    /**
     * Projde celým stromem od kořene a nastaví referenční označení.
     * @param rootNode kořen stromu
     */
    private void initReferenceMarksAndDepth(final TreeNode rootNode) {

        rootNode.setReferenceMark(new Integer[0]);
        rootNode.setDepth(1);
        int childPosition = 1;
        for (TreeNode child : rootNode.getChilds()) {
            initReferenceMarkAndDepth(child, childPosition++);
        }
    }

    /**
     * Zkopíruje referenční označení z rodiče a nastaví pozici na poslední pozici referenčního označení.
     *
     * @param treeNode uzel
     * @param position pozice uzlu v seznamu sourozenců
     */
    private void initReferenceMarkAndDepth(final TreeNode treeNode, final int position) {

        Integer[] parentReferenceMark = treeNode.getParent().getReferenceMark();
        Integer[] referenceMark = Arrays.copyOf(parentReferenceMark, parentReferenceMark.length + 1);
        referenceMark[parentReferenceMark.length] = position;
        treeNode.setReferenceMark(referenceMark);
        treeNode.setDepth(referenceMark.length + 1);

        int childPosition = 1;
        for (TreeNode child : treeNode.getChilds()) {
            initReferenceMarkAndDepth(child, childPosition++);
        }
    }

    /**
     * Vytvoří mapu všech rozbalených uzlů ve stromě. Přiřazuje děti a rodiče všech uzlů.
     *
     * @param treeNode             rozbalený uzel
     * @param expandedTreeNodesMap mapa všech rozbalených uzlů
     * @return nová instance rozbaleného uzlu a jeho rodičů.
     */
    private TreeNode createExpandedTreeNodeMap(final TreeNode treeNode,
                                               final Map<Integer, TreeNode> expandedTreeNodesMap) {
        TreeNode result = expandedTreeNodesMap.get(treeNode.getId());
        if (result != null) {
            return result;
        }

        result = new TreeNode(treeNode.getId(), treeNode.getPosition());
        result.setDepth(treeNode.getDepth());

        //nastavíme původní děti, které budou použity na vytvoření výsledného seznamu
        result.setChilds(treeNode.getChilds());

        expandedTreeNodesMap.put(result.getId(), result);

        if (treeNode.getParent() != null) {
            TreeNode parentNode = createExpandedTreeNodeMap(treeNode.getParent(), expandedTreeNodesMap);
            result.setParent(parentNode);
        }

        return result;
    }

    /**
     * Vytvoří cache stromu dané verze a uloží jej do cache. Druhé volání již vrací nacachovaná data.
     *
     * @param version verze stromu
     * @return mapa všech uzlů stromu (nodeid uzlu -> uzel)
     */
    synchronized public Map<Integer, TreeNode> getVersionTreeCache(final ArrFundVersion version) {
        Validate.notNull(version, "Verze AS není vyplněna");
        Map<Integer, TreeNode> versionTreeMap = versionCache.get(version.getFundVersionId());

        if (versionTreeMap == null) {
            versionTreeMap = createVersionTreeCache(version);
        }

        //při každém přístupu vyjmeme verzi a vložíme na začátek,
        //aby při překročení kapacity byla vyhozena z mapy vždy ta nejstarší (viz inicializace mapy - CapacityMap)
        versionCache.remove(version.getFundVersionId());
        versionCache.put(version.getFundVersionId(), versionTreeMap);


        return versionTreeMap;
    }

    /**
     * Invalidace všech verzí AS v cache.
     *
     * @param fund archivní soubor
     */
    synchronized public void invalidateFundVersion(final ArrFund fund) {
        Validate.notNull(fund, "AS musí být vyplněn");
        for (ArrFundVersion fundVersion: fund.getVersions()) {
            versionCache.remove(fundVersion.getFundVersionId());
        }
    }

    /**
     * Invalidace verze AS v cache.
     *
     * @param fundVersion verze archivního souboru
     */
    synchronized public void invalidateFundVersion(final ArrFundVersion fundVersion) {
        Validate.notNull(fundVersion, "Verze AS není vyplněna");
        versionCache.remove(fundVersion.getFundVersionId());
    }

    /**
     * Invalidace verze AS v cache.
     *
     * @param fundVersionId
     *            verze archivního souboru
     */
    synchronized public void refreshFundVersion(final Integer fundVersionId) {
        Validate.notNull(fundVersionId, "Verze AS není vyplněna");

        ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);

        invalidateFundVersion(fundVersion);
        getVersionTreeCache(fundVersion);
    }

    @Subscribe
    public void onDataUpdate(final EventChangeMessage changeMessage) {
    	Objects.requireNonNull(changeMessage);

        try {

            List<AbstractEventSimple> events = changeMessage.getEvents();
            for (AbstractEventSimple event : events) {
                logger.trace("Přijetí události: {}", event.getEventType());

                processEvent(event);
            }
        } catch (Exception e) {
            logger.error("Exception during onDataUpdate", e);
            throw e;
        }
    }

private void processEvent(AbstractEventSimple event) {
    //projdeme všechny změny, které jsou změny ve stromu uzlů verze a aktualizujeme cache verzí
    if (EventVersion.class.isAssignableFrom(event.getClass())) {

        switch (event.getEventType()) {
        case NODE_DELETE:
            break;
        case ADD_LEVEL_AFTER:
        case ADD_LEVEL_BEFORE:
        case ADD_LEVEL_UNDER:
            logger.debug("Zpracování události: {}", event.getEventType());

            EventAddNode eventAddNode = (EventAddNode) event;
            actionAddLevel(eventAddNode.getNode().getNodeId(), eventAddNode.getStaticNode().getNodeId(),
                           eventAddNode.getVersionId(), event.getEventType());
            break;
        case MOVE_LEVEL_AFTER:
        case MOVE_LEVEL_BEFORE:
        case MOVE_LEVEL_UNDER:
            logger.debug("Zpracování události: {}", event.getEventType());

            EventNodeMove eventNodeMove = (EventNodeMove) event;
            List<Integer> transportIds = eventNodeMove.getTransportLevels().stream()
                    .map(n -> n.getNodeId()).collect(Collectors.toList());
            actionMoveLevel(eventNodeMove.getStaticLevel().getNodeId(), transportIds,
                            eventNodeMove.getVersionId(), event.getEventType());

            break;
        case DELETE_LEVEL:
            logger.debug("Zpracování události: {}", event.getEventType());

            EventDeleteNode eventIdInVersion = (EventDeleteNode) event;
            actionDeleteLevel(eventIdInVersion.getNodeId(), eventIdInVersion.getVersionId());
            break;
        case BULK_ACTION_STATE_CHANGE:
            logger.debug("Zpracování události: {}", event.getEventType());

            EventIdInVersion bulkActionStateChangeEvent = (EventIdInVersion) event;
            if (bulkActionStateChangeEvent.getState().equals(ArrBulkActionRun.State.FINISHED.toString())) {
                if (bulkActionStateChangeEvent.getCode().equals("PERZISTENTNI_RAZENI")) {
                    refreshFundVersion(bulkActionStateChangeEvent.getVersionId());
                        }
            }
            break;
        }

        }
    }

    /**
     * Najde rodiče pro předaná id nodů. Vrátí mapu objektů ve kterém je id nodu a jeho rodič.
     *
     * @param nodeIds id nodů
     * @param version verze AP
     *
     * @return mapu id nodů a jejich rodičů
     */
    public Map<Integer, TreeNodeVO> findParentsWithTitles(final Collection<Integer> nodeIds,
                                                          final ArrFundVersion version) {
        Validate.notNull(nodeIds, "Nebyly vyplněny identifikátory JP");
        Validate.notNull(version, "Verze AS musí být vyplněna");

        Map<Integer, TreeNode> versionTreeCache = getVersionTreeCache(version);
        Map<Integer, TreeNode> nodeIdParentMap = new HashMap<>(nodeIds.size());
        Map<Integer, TreeNode> parentIdParentMap = new HashMap<>(nodeIds.size());

        for (Integer nodeId : nodeIds) {
            TreeNode treeNode = versionTreeCache.get(nodeId);
            if (treeNode == null) {
                logger.error("Node is not in active tree, nodeId: {}", nodeId);
                throw new BusinessException("Node is not in active tree", BaseCode.INVALID_STATE)
                        .set("nodeId", nodeId);
            }
            TreeNode parent = treeNode.getParent();
            if (parent != null) {
                parentIdParentMap.put(parent.getId(), parent);
                nodeIdParentMap.put(nodeId, parent);
            }
        }

        Map<Integer, TitleItemsByType> valuesMap = createValuesMap(parentIdParentMap, version, null);
        Map<Integer, TreeNodeVO> parentIdTreeNodeClientMap = createNodesWithTitles(parentIdParentMap, valuesMap,
                null, version);

        Map<Integer, TreeNodeVO> result = new HashMap<>(nodeIds.size());
        for (Integer nodeId : nodeIdParentMap.keySet()) {
            Integer parentId = nodeIdParentMap.get(nodeId).getId();
            TreeNodeVO parentTreeNodeClient = parentIdTreeNodeClientMap.get(parentId);

            result.put(nodeId, parentTreeNodeClient);
        }

        return result;
    }

    /**
     * Provede načtení popisků uzlů pro uzly, které budou odeslány do klienta a vytvoří výsledné odesílané objekty.
     *
     * @param treeNodeMap    seřazená mapa uzlů tak jak budou odeslány (nodeid -> uzel)
     * @param valuesMapParam mapa načtených hodnot pro uzly, pokud není nastaveno, bude spočítáno
     * @param subtreeRoot kořenový uzel, pod kterým chceme spočítat referenční označení (nemusí být kořen stromu).
     *                    Pokud není nastaveno, není počítáno referenční označení
     * @param version        verze stromu
     * @return seznam rozbalených uzlů s potomky seřazen
     */
    private Map<Integer, TreeNodeVO> createNodesWithTitles(final Map<Integer, TreeNode> treeNodeMap,
                                                           @Nullable final Map<Integer, TitleItemsByType> valuesMapParam,
                                                           final TreeNode subtreeRoot,
                                                           final ArrFundVersion version) {
        Validate.notNull(treeNodeMap, "Mapa nesmí být null");
        Validate.notNull(version, "Verze AS musí být vyplněna");

        Map<Integer, ArrDao> daoLevelMap = new HashMap<>();
        // read nodes
        List<ArrNode> nodes = new ArrayList<>(treeNodeMap.size());
        Set<Integer> nodeIds = treeNodeMap.keySet();
        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(nodeIds);
        while (iterator.hasNext()) {
            List<Integer> nodeIdsSublist = iterator.next();

            nodes.addAll(nodeRepository.findAllById(nodeIdsSublist));
            // read dao links
            if (displayDaoId) {
                List<ArrDaoLink> daoLinks = daoLinkRepository.findByNodeIdsAndFetchDao(nodeIdsSublist);
                for (ArrDaoLink daoLink : daoLinks) {
                    if (daoLink.getDao().getDaoType().equals(DaoType.LEVEL)) {
                        daoLevelMap.put(daoLink.getNodeId(), daoLink.getDao());
                    }
                }
            }
        }
        Map<Integer, ArrNode> nodeMap = new HashMap<>(nodes.size());
        for (ArrNode node : nodes) {
            nodeMap.put(node.getNodeId(), node);
        }

        // create titles
        ViewTitles viewTitles = configView
                .getViewTitles(version.getRuleSetId(), version.getFundId());
        Map<Integer, TitleItemsByType> valuesMap = valuesMapParam;
        if (valuesMap == null) {
            valuesMap = createValuesMap(treeNodeMap, version, subtreeRoot);
        }
        Integer levelTypeId = viewTitles.getLevelTypeId();

        Map<Integer, TreeNodeVO> result = new LinkedHashMap<>(treeNodeMap.size());

        String[] rootReferenceMark = new String[0];
        if (subtreeRoot != null) {
            rootReferenceMark = createClientReferenceMarkFromRoot(subtreeRoot,
                                                                  viewTitles, valuesMap);
        }
        String[] parentReferenceMark = rootReferenceMark;

        for (TreeNode treeNode : treeNodeMap.values()) {
            if (subtreeRoot != null && treeNode.getParent() != null) {
                if (treeNode.getParent().equals(subtreeRoot)) {
                    parentReferenceMark = rootReferenceMark;
                } else {
                    parentReferenceMark = result.get(treeNode.getParent().getId()).getReferenceMark();
                }
            }

            TreeNodeVO client = new TreeNodeVO(treeNode.getId(), treeNode.getDepth(), null,
                    !treeNode.getChilds().isEmpty(),
                    treeNode.getReferenceMark(), nodeMap.get(treeNode.getId()).getVersion());
            if (subtreeRoot != null) {
                String[] referenceMark = createClientNodeReferenceMark(treeNode, levelTypeId, viewTitles, valuesMap,
                        parentReferenceMark);
                client.setReferenceMark(referenceMark);
            }

            result.put(treeNode.getId(), client);
        }

        if (result.isEmpty()) {
            return result;
        }


        for (TreeNodeVO treeNodeClient : result.values()) {
            TitleItemsByType descItemCodeToValueMap = valuesMap.get(treeNodeClient.getId());
            ArrDao dao = daoLevelMap.get(treeNodeClient.getId());
            fillValues(version, descItemCodeToValueMap, viewTitles, treeNodeClient, dao);
        }

        return result;
    }

    private List<RulItemType> getDescriptionItemTypes(final ViewTitles viewTitles) {
        Set<Integer> typeIds = viewTitles.getAllItemTypeIds();

        if (typeIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<RulItemType> result = new ArrayList<>();

        StaticDataProvider data = staticDataService.getData();
        for (Integer typeId : typeIds) {
            ItemType rsit = data.getItemTypeById(typeId);
            if (rsit == null) {
                logger.warn("Nepodařilo se nalézt typ atributu, kód=" + typeId + ". Změňte kód v konfiguraci.");
                continue;
            }
            result.add(rsit.getEntity());
        }
        return result;
    }

    /**
     * Nastavení hodnot (icon, name) ve třídě TreeNodeVO.
     *
     * @param version
     * @param descItemCodeToValueMap
     * @param viewTitles
     * @param treeNodeClient
     * @param dao
     *            Volitelný hlavní dao, může být null
     */
    private void fillValues(final ArrFundVersion version,
                            final TitleItemsByType descItemCodeToValueMap,
                            final ViewTitles viewTitles,
                            final TreeNodeVO treeNodeClient,
                            final ArrDao dao) {

        if (descItemCodeToValueMap != null) {
            String icon = getIcon(descItemCodeToValueMap, viewTitles);
            treeNodeClient.setIcon(icon);
        }

        String defaultTitle;
        if (treeNodeClient.getDepth() > 1) {
            defaultTitle = createDefaultTitle(viewTitles, treeNodeClient.getId());
        } else {
            defaultTitle = createRootTitle(version.getFund(), viewTitles, treeNodeClient.getId());
        }

        ArrDao displayDao = displayDaoId? dao : null;

        treeNodeClient.setName(viewTitles.getTreeItem().build(descItemCodeToValueMap, displayDao, defaultTitle));
    }

    /**
     * Vytvoření výchozího popisku.
     *
     * @param viewTitles data pro zobrazení
     * @param id         identifikátor JP
     * @return text popisku
     */
    private String createDefaultTitle(final ViewTitles viewTitles, final Integer id) {
        String defaultTitle = viewTitles.getDefaultTitle();
        defaultTitle = StringUtils.isEmpty(defaultTitle) ? "JP <" + id + ">" : defaultTitle;
        return defaultTitle;
    }

    /**
     * Načte hodnoty atributů podle nastavení pro dané uzly.
     *
     * @param treeNodeMap mapa uzlů
     * @param version     verze ap
     * @param subtreeRoot kořenový uzel, pod kterým chceme spočítat referenční označení (nemusí být kořen stromu)
     * @return hodnoty atributů pro uzly
     */
    private Map<Integer, TitleItemsByType> createValuesMap(final Map<Integer, TreeNode> treeNodeMap,
                                                                  final ArrFundVersion version,
                                                                  final TreeNode subtreeRoot) {

        ViewTitles viewTitles = configView
                .getViewTitles(version.getRuleSetId(), version.getFund().getFundId());
        List<RulItemType> descItemTypes = getDescriptionItemTypes(viewTitles);

        return descriptionItemService.createNodeValuesByItemTypeCodeMap(treeNodeMap.keySet(), descItemTypes,
                                                                        version.getLockChangeId(), subtreeRoot);
    }




    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////CACHE UPDATE///////////////////////////////////////////////////////

    /**
     * Aktualizace cache po smazání uzlu.
     *
     * @param nodeId
     * @param fundVersionId
     */
    synchronized private void actionDeleteLevel(final Integer nodeId, final Integer fundVersionId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);

        Map<Integer, TreeNode> versionTreeMap = getVersionTreeCache(fundVersion);


        if (versionTreeMap != null) {
            TreeNode deleteNode = versionTreeMap.get(nodeId);
            TreeNode parent = deleteNode.getParent();
            if (parent != null) {
                int deleteNodeIndex = parent.getChilds().indexOf(deleteNode);
                parent.getChilds().remove(deleteNode);
                repositionList(parent.getChilds());

                if (parent.getChilds().size() > deleteNodeIndex) {
                    initReferenceMarkLower(parent.getChilds(), parent.getChilds().get(deleteNodeIndex));
                }
            }
            removeFromCacheTree(versionTreeMap, deleteNode);
        }
    }

    /**
     * Rekurzivně smaže uzel a jeho potomky z cache.
     *
     * @param versionTreeMap cache
     * @param deleteNode     uzel ke smazání
     */
    private void removeFromCacheTree(final Map<Integer, TreeNode> versionTreeMap,
                                     final TreeNode deleteNode) {
        versionTreeMap.remove(deleteNode.getId());
        for (TreeNode child : deleteNode.getChilds()) {
            removeFromCacheTree(versionTreeMap, child);
        }
    }

    /**
     * Aktualizace cache po přidání uzlu do cache
     *
     * @param newNodeId
     *            id přidaného uzlu
     * @param staticId
     *            id statického uzlu
     * @param versionId
     *            verzes tromu
     * @param addLevelType
     *            typ akce
     */
    synchronized private void actionAddLevel(final Integer newNodeId,
                                             final Integer staticId,
                                             final Integer fundVersionId,
                                             final EventType addLevelType) {
        ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);

        Map<Integer, TreeNode> versionTreeMap = getVersionTreeCache(fundVersion);
        if (versionTreeMap == null) {
            // tree for fund version not found, what should we do?
            // simply return
            return;
        }
        // check if node already in the tree
        // it means that tree was constructed including this new level
        TreeNode newNode = versionTreeMap.get(newNodeId);
        if (newNode != null) {
            return;
        }

        TreeNode staticNode = versionTreeMap.get(staticId);
        switch (addLevelType) {
        case ADD_LEVEL_BEFORE: {
            newNode = createEmptyTreeNode(newNodeId, staticNode.getParent());

            LinkedList<TreeNode> parentChilds = staticNode.getParent().getChilds();
            int staticNodeIndex = parentChilds.indexOf(staticNode);
            parentChilds.add(staticNodeIndex, newNode);
            repositionList(parentChilds);
            initReferenceMarkLower(parentChilds, newNode);
            break;
        }
        case ADD_LEVEL_AFTER: {
            newNode = createEmptyTreeNode(newNodeId, staticNode.getParent());

            LinkedList<TreeNode> parentChilds = staticNode.getParent().getChilds();
            int staticNodeIndex = parentChilds.indexOf(staticNode) + 1;
            parentChilds.add(staticNodeIndex, newNode);
            repositionList(parentChilds);
            initReferenceMarkLower(parentChilds, newNode);
            break;
        }
        case ADD_LEVEL_UNDER: {
            newNode = createEmptyTreeNode(newNodeId, staticNode);

            LinkedList<TreeNode> parentChilds = staticNode.getChilds();
            parentChilds.addLast(newNode);
            repositionList(parentChilds);
            initReferenceMarkAndDepth(newNode, newNode.getPosition());
            break;
        }
        default:
            throw new IllegalArgumentException("Neplatny typ přidání nodu " + addLevelType);
        }
        versionTreeMap.put(newNode.getId(), newNode);
    }

    /**
     * Aktualizace cache pro přesunutí uzlu.
     *
     * @param staticId
     *            id statického nodu
     * @param nodeIds
     *            seznam id nodů k přesunutí
     * @param fundVersionId
     *            verze stromu
     * @param moveLevelType
     *            typ události
     */
    synchronized private void actionMoveLevel(final Integer staticId,
                                              final List<Integer> nodeIds,
                                              final Integer fundVersionId,
                                              final EventType moveLevelType) {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return;
        }

        ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);
        Map<Integer, TreeNode> versionTreeMap = getVersionTreeCache(fundVersion);
        if (versionTreeMap != null) {
            TreeNode staticNode = versionTreeMap.get(staticId);
            TreeNode staticParent = moveLevelType == EventType.MOVE_LEVEL_UNDER ? staticNode : staticNode.getParent();


            List<TreeNode> transportNodes = new ArrayList<>(nodeIds.size());

            for (Integer transportNodeId : nodeIds) {
                TreeNode transportNode = versionTreeMap.get(transportNodeId);
                transportNode.getParent().getChilds().remove(transportNode);
                transportNodes.add(transportNode);
            }
            TreeNode transportParent = transportNodes.get(0).getParent();
            repositionList(transportParent.getChilds());

            boolean siblingMove = transportParent.equals(staticParent);
            if (!siblingMove && !transportParent.getChilds().isEmpty()) {
                //při přesunu přečíslujeme všechny node, které jsou pod rodičem, jehož děti přesouváme
                initReferenceMarkLower(transportParent.getChilds(), transportParent.getChilds().getFirst());
            }


            switch (moveLevelType) {
                case MOVE_LEVEL_BEFORE: {
                    LinkedList<TreeNode> staticParentChilds = staticParent.getChilds();
                    int staticIndex = staticParentChilds.indexOf(staticNode);

                    for (TreeNode transportNode : transportNodes) {
                        transportNode.setParent(staticParent);
                    }

                    staticParentChilds.addAll(staticIndex, transportNodes);
                    repositionList(staticParentChilds);


                    break;
                }
                case MOVE_LEVEL_AFTER: {
                    LinkedList<TreeNode> staticParentChilds = staticParent.getChilds();
                    int staticIndex = staticParentChilds.indexOf(staticNode) + 1;

                    for (TreeNode transportNode : transportNodes) {
                        transportNode.setParent(staticParent);
                    }

                    staticParentChilds.addAll(staticIndex, transportNodes);
                    repositionList(staticParentChilds);
                    break;
                }
                case MOVE_LEVEL_UNDER: {
                    LinkedList<TreeNode> staticParentChilds = staticNode.getChilds();

                    for (TreeNode transportNode : transportNodes) {
                        transportNode.setParent(staticNode);
                        staticParentChilds.addLast(transportNode);
                    }

                    repositionList(staticParentChilds);
                    break;
                }
            }


            //přečíslujeme všechny prvky pod prvním přidaným (včetně) -> pokud přesouváme na stejné úrovni, přečíslujeme všechny
            if (siblingMove) {
                initReferenceMarkLower(staticParent.getChilds(), staticParent.getChilds().getFirst());
            } else {
                initReferenceMarkLower(staticParent.getChilds(), transportNodes.get(0));
            }


        }
    }

    /**
     * Přečísluje všechny uzly od jedné
     * @param childs uzly k přečíslování
     */
    private void repositionList(final Collection<TreeNode> childs) {
        int position = 1;
        for (TreeNode child : childs) {
            child.setPosition(position++);
        }
    }


    /**
     * Projde celý seznam a až narazí na zadaný uzel, začne od něho aktualizovat hierarchicky referenční označení.
     *
     * @param childs          seznam všech potomků, ve kterých hledáme
     * @param firstReposition první uzel, od kterého se začnou aktualizovat označení
     */
    private void initReferenceMarkLower(final LinkedList<TreeNode> childs, final TreeNode firstReposition) {

        boolean initReference = false;
        int position = firstReposition.getPosition();
        for (TreeNode child : childs) {
            initReference = initReference || child.equals(firstReposition);
            if (initReference) {
                initReferenceMarkAndDepth(child, position++);
            }
        }
    }


    /**
     * Vytvoří nový prázdný uzel.
     *
     * @param nodeId     id uzlu
     * @param parentNode rodič, do kterého bude přidán nový potomek
     * @return nový uzel
     */
    private TreeNode createEmptyTreeNode(final Integer nodeId, final TreeNode parentNode) {
        TreeNode newNode = new TreeNode(nodeId, 0);
        newNode.setParent(parentNode);
        newNode.setDepth(parentNode.getDepth() + 1);
        return newNode;
    }

    /**
     * Získání informací o uzlech.
     *
     * @param versionId id verze stromu
     * @param nodeIds   seznam id uzlů
     * @return informace
     */
    public Collection<TreeNodeVO> getFaTreeNodes(final Integer versionId, final Collection<Integer> nodeIds) {
        ArrFundVersion version = arrangementService.getFundVersion(versionId);
        Map<Integer, TreeNode> treeMap = getVersionTreeCache(version);
        LinkedHashMap<Integer, TreeNode> nodesMap = new LinkedHashMap<>();

        for (Integer nodeId : nodeIds) {
            TreeNode treeNode = treeMap.get(nodeId);
            if (treeNode != null) {
                nodesMap.put(nodeId, treeNode);
            } else {
                logger.warn("Uzel s identifikátorem " + nodeId + " neexistuje ve verzi " + versionId);
            }
        }

        return createNodesWithTitles(nodesMap, null, null, version).values();
    }



    public List<Integer> sortNodesByTreePosition(final Collection<Integer> nodeIds, final ArrFundVersion version) {
        List<TreeNodeVO> nodes = getNodesByIds(nodeIds, version);

        nodes.sort((node1, node2) -> {
            Integer[] referenceMark1 = node1.getReferenceMarkInt();
            Integer[] referenceMark2 = node2.getReferenceMarkInt();

            Integer l1 = referenceMark1.length;
            Integer l2 = referenceMark2.length;
            int i = 0;
            while (i < l1 && i < l2) {
                Integer position1 = referenceMark1[i];
                Integer position2 = referenceMark2[i];

                int comparisonResult = position1.compareTo(position2);
                if (comparisonResult == 0) {
                    i++;
                } else {
                    return comparisonResult;
                }
            }

            return l1.compareTo(l2);
        });

        return nodes.stream().map(TreeNodeVO::getId).collect(Collectors.toList());
    }

    /**
     * Získání dat pro JP (formálář, rodiče, potomky, sourozence, ...)
     *
     * @param param parametry požadovaných dat
     * @param userDetail přihlášený uživatel
     * @return požadovaná data
     */
    public NodeData getNodeData(final NodeDataParam param, @Nullable UserDetail userDetail) {

    	Objects.requireNonNull(param);
    	Objects.requireNonNull(param.getFundVersionId());

        ArrFundVersion fundVersion = arrangementService.getFundVersion(param.getFundVersionId());
        Map<Integer, TreeNode> treeMap = getVersionTreeCache(fundVersion);

        TreeNode node;
        if (param.getNodeId() != null) {
            node = treeMap.get(param.getNodeId());
        } else if (param.getNodeIndex() != null) {
            TreeNode parentNode;
            if (param.getParentNodeId() == null) {
                parentNode = treeMap.get(fundVersion.getRootNodeId());
            } else {
                parentNode = treeMap.get(param.getParentNodeId());
            }
            node = parentNode.getChilds().get(param.getNodeIndex());
        } else {
            throw new SystemException("Není zvolen identifikátor JP nebo její index", BaseCode.INVALID_STATE);
        }

        if (node == null) {
            throw new SystemException("Node does not exist", BaseCode.ID_NOT_EXIST)
                    .set("nodeId", param.getNodeId());
        }

        NodeData result = new NodeData();

        TreeNode parentNode = node.getParent();
        if (parentNode == null) { // pokud nemá rodiče, jedná se o kořen a ten je vždy pouze jediný
            result.setNodeIndex(0);
            result.setNodeCount(1);
        } else {
            result.setNodeIndex(parentNode.getChilds().indexOf(node));
            result.setNodeCount(parentNode.getChilds().size());
        }

        if (BooleanUtils.isTrue(param.getFormData())) {
            result.setFormData(formService.getNodeFormData(fundVersion, node.getId()));
        }

        if (BooleanUtils.isTrue(param.getParents())) {
            result.setParents(getNodeParents(treeMap, node, fundVersion));
        }

        if (BooleanUtils.isTrue(param.getChildren())) {
            result.setChildren(getChildren(node, fundVersion));
        }

        Integer siblingsFrom = param.getSiblingsFrom();
        String fulltext = StringUtils.isEmpty(param.getSiblingsFilter()) ? null : param.getSiblingsFilter().trim();
        int maxCount = param.getSiblingsMaxCount() == null || param.getSiblingsMaxCount() > 1000 ? 1000 : param.getSiblingsMaxCount();
        if (siblingsFrom != null) {
            if (siblingsFrom < 0) {
                throw new IllegalArgumentException("Index pro sourozence nesmí být záporný: " + siblingsFrom);
            }
            LevelTreeCacheService.Siblings siblings = getNodeSiblings(node, fundVersion, siblingsFrom, maxCount, fulltext, userDetail);
            result.setNodeIndex(siblings.getNodeIndex());
            result.setNodeCount(siblings.getSiblingsCount());
            result.setSiblings(siblings.getSiblings());
        } else if (fulltext != null) { // pokud je zafiltrováno, je nutné brát výsledky (index + počet sourozenů) vzhledem k filtru
            LevelTreeCacheService.Siblings siblings = getNodeSiblings(node, fundVersion, 0, maxCount, fulltext, userDetail);
            result.setNodeIndex(siblings.getNodeIndex());
            result.setNodeCount(siblings.getSiblingsCount());
        }

        return result;
    }

    private Integer calcLastSiblingsFrom(final TreeNode node, final int maxCount) {
        TreeNode parentNode = node.getParent();
        List<TreeNode> childs;
        if (parentNode == null) {
            childs = Collections.singletonList(node);
        } else {
            childs = parentNode.getChilds();
        }
        int diff = childs.size() - (childs.size() % maxCount);
        if (diff == childs.size()) {
            diff -= maxCount / 2;
        }
        return Math.max(diff, 0);
    }

    /**
     * Získání seznamu přímých potomků.
     *
     * @param node        uzel, jehož potomky budeme chceme
     * @param fundVersion verze AS
     * @return seznam přímých potomků
     */
    private Collection<TreeNodeVO> getChildren(final TreeNode node, final ArrFundVersion fundVersion) {

        LinkedHashMap<Integer, TreeNode> nodesMap = new LinkedHashMap<>();
        for (TreeNode treeNode : node.getChilds()) {
            nodesMap.put(treeNode.getId(), treeNode);
        }

        NodeParam param = NodeParam.create()
                .name()
                .icon()
                .referenceMark();

        boolean fullArrPerm = userService.hasFullArrPerm(fundVersion.getFundId());
        return convertToTreeNodeWithPerm(getNodes(nodesMap, node, param, fundVersion).values(), fundVersion, fullArrPerm);
    }

    /**
     * Konverze požadovaných objektů do objektů pro strom s vyhodnoceným oprávněním.
     *
     * @param nodes JP k převodu
     * @param version     verze AS
     * @param fullArrPerm máme oprávnění pořádat v celém AS
     * @return převedené JP
     */
    private List<TreeNodeVO> convertToTreeNodeWithPerm(final Collection<Node> nodes, final ArrFundVersion version, final boolean fullArrPerm) {
        List<TreeNodeVO> result = new ArrayList<>(nodes.size());

        Set<Integer> nodeIds = new HashSet<>();
        for (Node node : nodes) {
            nodeIds.add(node.getId());
        }

        Map<Integer, Boolean> permNodeIdMap = fullArrPerm ? Collections.emptyMap() : calcPermNodeIdMap(version, nodeIds);

        for (Node node : nodes) {
            TreeNodeVO treeNode = new TreeNodeVO();
            treeNode.setId(node.getId());
            treeNode.setName(node.getName());
            treeNode.setReferenceMark(node.getReferenceMark());
            treeNode.setHasChildren(node.isHasChildren());
            treeNode.setIcon(node.getIcon());
            treeNode.setDepth(node.getDepth());
            treeNode.setVersion(node.getVersion());
            treeNode.setArrPerm(fullArrPerm ? true : permNodeIdMap.get(node.getId()));
            result.add(treeNode);
        }
        return result;
    }

    /**
     * Zjištění oprávnění pro JP ve verzi.
     *
     * @param version verze AS
     * @param nodeIds požadované JP pro které napočítáváme oprávnění pro pořádání.
     * @return mapa výsledků JP->true/false
     */
    public Map<Integer, Boolean> calcPermNodeIdMap(final ArrFundVersion version, final Set<Integer> nodeIds) {

        Collection<UserPermission> userPermission = userService.getLoggedUserDetail().getUserPermission();
        Set<Integer> permNodeIds = new HashSet<>();
        for (UserPermission permission : userPermission) {
            if (permission.getPermission() == UsrPermission.Permission.FUND_ARR_NODE) {
                permNodeIds.addAll(permission.getNodeIdsByFund(version.getFundId()));
            }
        }
        Map<Integer, TreeNode> versionTreeCache = getVersionTreeCache(version);

        Map<Integer, Boolean> result = new HashMap<>();

        // pro všechny požadované JP vyhodnotím oprávnění
        for (Integer nodeId : nodeIds) {
            if (permNodeIds.contains(nodeId)) { // pokud existuje oprávnění přímo na tuto JP
                result.put(nodeId, true);
            } else { // jinak hledám některého z předků, jestli mají toto oprávnění
                boolean nodeIdResult = false;
                TreeNode treeNode = versionTreeCache.get(nodeId).getParent();
                while (treeNode != null) {
                    Integer parentNodeId = treeNode.getId();
                    if (permNodeIds.contains(parentNodeId)) {
                        nodeIdResult = true;
                        break;
                    }
                    treeNode = treeNode.getParent();
                }
                result.put(nodeId, nodeIdResult);
            }
        }
        return result;
    }

    @Override
    public boolean checkPermissionInTree(final Integer nodeId) {
        ArrNode one = arrangementService.getNode(nodeId);
        ArrFundVersion fundVersion = fundVersionRepository.findByFundIdAndLockChangeIsNull(one.getFundId());
        return calcPermNodeIdMap(fundVersion, Collections.singleton(nodeId)).get(nodeId);
    }

    /**
     * Konverze požadovaných objektů do objektů pro accordion.
     *
     * @param nodes JP k převodu
     * @param nodeToIssueMap
     * @return převedené JP
     */
    private List<AccordionNodeVO> convertToAccordionNode(final Collection<Node> nodes, Map<Integer, List<WfIssue>> nodeToIssueMap) {
        return nodes.stream().map(n -> {

            AccordionNodeVO accordionNode = new AccordionNodeVO();
            accordionNode.setId(n.getId());
            accordionNode.setAccordionLeft(n.getAccordionLeft());
            accordionNode.setAccordionRight(n.getAccordionRight());
            accordionNode.setReferenceMark(n.getReferenceMark());
            accordionNode.setDigitizationRequests(n.getDigitizationRequests());
            accordionNode.setNodeConformity(n.getNodeConformity());
            accordionNode.setVersion(n.getVersion());

            List<WfIssue> issues = nodeToIssueMap.getOrDefault(n.getId(), Collections.emptyList());
            accordionNode.setIssues(issues.stream().map(WfSimpleIssueVO::newInstance).collect(Collectors.toList()));

            return accordionNode;

        }).collect(Collectors.toList());
    }

    /**
     * Získání informací o JP ve verzi.
     *
     * <ul>Vyplněné položky:
     *  <li> název
     *  <li> ikona
     *  <li> accordion
     *
     * @param nodeId      identifkátor požadované JP
     * @param fundVersion verze AS
     * @return nalezená JP
     */
    public Node getSimpleNode(final Integer nodeId, final ArrFundVersion fundVersion) {
        Map<Integer, TreeNode> treeMap = getVersionTreeCache(fundVersion);
        TreeNode treeNode = treeMap.get(nodeId);
        Validate.notNull(treeNode, "Neplatný identifikátor JP: " + nodeId);
        NodeParam param = NodeParam.create()
                .name()
                .icon()
                .accordion();

        LinkedHashMap<Integer, TreeNode> nodesMap = new LinkedHashMap<>();
        nodesMap.put(nodeId, treeNode);
        return getNodes(nodesMap, treeNode.getParent(), param, fundVersion).get(nodeId);
    }

    /**
     * Získání informací o JP ve verzi.
     *
     * <ul>Vyplněné položky:
     *  <li> název
     *  <li> ikona
     *  <li> accordion
     *
     * @param nodeId      identifkátor požadované JP
     * @param fundVersionId id verze AS
     * @return nalezená JP
     */
    public ArrNodeExtendVO getSimpleNode(final Integer fundVersionId, final Integer nodeId) {
        ArrFundVersion fundVersion = arrangementService.getFundVersion(fundVersionId);
        Map<Integer, TreeNode> treeMap = getVersionTreeCache(fundVersion);
        TreeNode treeNode = treeMap.get(nodeId);
        Validate.notNull(treeNode, "Neplatný identifikátor JP: " + nodeId);
        NodeParam param = NodeParam.create()
                .name()
                .icon()
                .accordion();

        LinkedHashMap<Integer, TreeNode> nodesMap = new LinkedHashMap<>();
        nodesMap.put(nodeId, treeNode);
        Node tempResult = getNodes(nodesMap, treeNode.getParent(), param, fundVersion).get(nodeId);
        return new ArrNodeExtendVO(tempResult.getId(),tempResult.getName(),tempResult.getUuid(), fundVersion.getFund().getName());
    }

    public List<TreeNodeWithFundVO> getTreeNodesWithFunds(final Collection<Integer> nodeIds) {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return Collections.emptyList();
        }

        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(nodeIds);
        Set<ArrFundVersion> fundVersions = new HashSet<>();
        while (iterator.hasNext()) {
            fundVersions.addAll(fundVersionRepository.findVersionsByNodeIds(iterator.next()));
        }

        List<TreeNodeWithFundVO> result = new ArrayList<>();
        List<Integer> resolveNodeIds = new ArrayList<>(nodeIds);

        for (ArrFundVersion fundVersion : fundVersions) {
            Map<Integer, TreeNode> treeMap = getVersionTreeCache(fundVersion);

            Iterator<Integer> it = resolveNodeIds.iterator();
            while (it.hasNext()) {
                Integer nodeId = it.next();
                TreeNode treeNode = treeMap.get(nodeId);
                if (treeNode == null) {
                    continue; // není v tomto AS, přeskakujeme
                }
                it.remove(); // zpracováváme
                NodeParam param = NodeParam.create()
                        .name()
                        .referenceMark()
                        .icon();
                LinkedHashMap<Integer, TreeNode> nodesMap = new LinkedHashMap<>();
                nodesMap.put(nodeId, treeNode);
                Node node = getNodes(nodesMap, treeNode.getParent(), param, fundVersion).get(nodeId);
                result.add(TreeNodeWithFundVO.newInstance(node, fundVersion));
            }
        }

        if (resolveNodeIds.size() > 0) {
            // může nastat pokud se maže celý podstrom, který obsahuje nějaké odkazující JP
            logger.debug("JP {} nebyly nalezeny v žádném stromu", resolveNodeIds);
        }

        return result;
    }

    /**
     * Parametry vyplnění pro požadované JP.
     */
    public static class NodeParam {

        private boolean name = false;
        private boolean accordion = false;
        private boolean icon = false;
        private boolean referenceMark = false;
        private boolean nodeConformity = false;
        private boolean digitizationRequest = false;

        public NodeParam() {
        }

        public static NodeParam create() {
            return new NodeParam();
        }

        public NodeParam name() {
            name = true;
            return this;
        }

        public boolean isName() {
            return name;
        }

        public NodeParam accordion() {
            accordion = true;
            return this;
        }

        public boolean isAccordion() {
            return accordion;
        }

        public NodeParam icon() {
            icon = true;
            return this;
        }

        public boolean isIcon() {
            return icon;
        }

        public NodeParam referenceMark() {
            referenceMark = true;
            return this;
        }

        public boolean isReferenceMark() {
            return referenceMark;
        }

        public NodeParam nodeConformity() {
            nodeConformity = true;
            return this;
        }

        public boolean isNodeConformity() {
            return nodeConformity;
        }

        public NodeParam digitizationRequest() {
            digitizationRequest = true;
            return this;
        }

        public boolean isDigitizationRequest() {
            return digitizationRequest;
        }
    }

    /**
     * Context for method GetNodes
     *
     *
     */
    public static class GetNodesCtx {

        /**
         * Created nodes
         */
        LinkedHashMap<Integer, Node> nodeMap = new LinkedHashMap<>();

        /**
         * Request parameters
         */
        private NodeParam param;
        private ArrFundVersion fundVersion;

        private ViewTitles viewTitles;

        private Map<Integer, ArrNode> arrNodeMap;

        public GetNodesCtx(final NodeParam param,
                           final ArrFundVersion fundVersion,
                           final ViewTitles viewTitles,
                           final Map<Integer, ArrNode> arrNodeMap) {
            this.param = param;
            this.fundVersion = fundVersion;
            this.viewTitles = viewTitles;
            this.arrNodeMap = arrNodeMap;
        }

        ViewTitles getViewTitles() {
            return viewTitles;
        }

        NodeParam getParam() {
            return param;
        }

        ArrFundVersion getFundVersion() {
            return fundVersion;
        }

        public void addNode(Node node) {
            nodeMap.put(node.getId(), node);
        }

        LinkedHashMap<Integer, Node> getNodes() {
            return nodeMap;
        }

    }

    /**
     * Interní třída pro JP.
     */
    public static class Node {

        /**
         * Nodeid uzlu.
         */
        private Integer id;

        /**
         * Hloubka zanoření ve stromu.
         */
        private Integer depth;

        /**
         * Název uzlu.
         */
        private String name;

        /**
         * UUID
         */
        private String uuid;

        /**
         * Popisek v akordeonu - levá strana.
         */
        private String accordionLeft;

        /**
         * Popisek v akordeonu - pravá strana.
         */
        private String accordionRight;

        /**
         * Ikonka.
         */
        private String icon;

        /**
         * True - uzel má další potomky, false - uzel nemá další potomky.
         */
        private boolean hasChildren;

        /**
         * Referenční označení. Od kořene k uzlu.
         */
        private String[] referenceMark;

        /**
         * Verze uzlu.
         */
        private Integer version;

        /**
         * Informace o stavu JP.
         */
        private NodeConformityVO nodeConformity;

        /**
         * Seznam otevřených požadavků na digitalizaci.
         */
        private List<ArrDigitizationRequestVO> digitizationRequests;

        public Node(final Integer id, final Integer version, final String uuid) {
            this.id = id;
            this.version = version;
            this.uuid = uuid;
        }

        public Integer getId() {
            return id;
        }

        public Integer getDepth() {
            return depth;
        }

        public String getName() {
            return name;
        }

        public String getUuid() { return uuid; }

        public String getAccordionLeft() {
            return accordionLeft;
        }

        public String getAccordionRight() {
            return accordionRight;
        }

        public String getIcon() {
            return icon;
        }

        public boolean isHasChildren() {
            return hasChildren;
        }

        public String[] getReferenceMark() {
            return referenceMark;
        }

        public Integer getVersion() {
            return version;
        }

        public NodeConformityVO getNodeConformity() {
            return nodeConformity;
        }

        public List<ArrDigitizationRequestVO> getDigitizationRequests() {
            return digitizationRequests;
        }

        public void setDepth(final Integer depth) {
            this.depth = depth;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public void setAccordionLeft(final String accordionLeft) {
            this.accordionLeft = accordionLeft;
        }

        public void setAccordionRight(final String accordionRight) {
            this.accordionRight = accordionRight;
        }

        public void setIcon(final String icon) {
            this.icon = icon;
        }

        public void setHasChildren(final boolean hasChildren) {
            this.hasChildren = hasChildren;
        }

        public void setReferenceMark(final String[] referenceMark) {
            this.referenceMark = referenceMark;
        }

        public void setNodeConformity(final NodeConformityVO nodeConformity) {
            this.nodeConformity = nodeConformity;
        }

        public void setDigitizationRequests(final List<ArrDigitizationRequestVO> digitizationRequests) {
            this.digitizationRequests = digitizationRequests;
        }
    }

    /**
     * Získání JP podle parametrů.
     *
     * @param treeNodeMap mapa položek JP ve verzi
     * @param subtreeRoot položka pro podstrom
     * @param param       parametry pro získání JP
     * @param fundVersion verze AS
     * @return nalezené JP
     */
    private LinkedHashMap<Integer, Node> getNodes(final LinkedHashMap<Integer, TreeNode> treeNodeMap,
                                                  final @Nullable TreeNode subtreeRoot,
                                                  final NodeParam param,
                                                  final ArrFundVersion fundVersion) {
        Validate.notNull(fundVersion, "Verze AS musí být vyplněna");

        ViewTitles viewTitles = configView.getViewTitles(fundVersion.getRuleSetId(), fundVersion.getFundId());

        Set<Integer> nodeIds = treeNodeMap.keySet();
        Map<Integer, ArrNode> arrNodeMap = createNodeMap(nodeIds);

        GetNodesCtx getNodesCtx = new GetNodesCtx(param, fundVersion, viewTitles, arrNodeMap);

        Map<Integer, TitleItemsByType> nodeValueMap = createValuesMap(treeNodeMap, fundVersion, subtreeRoot);
        Map<Integer, ArrDao> daoLevelMap = new HashMap<>();
        // read dao links
        if (displayDaoId && (param.isName() || param.isAccordion())) {
            ObjectListIterator<Integer> iteratorNodeIds = new ObjectListIterator<>(nodeIds);

            while (iteratorNodeIds.hasNext()) {
                List<Integer> partNodeIds = iteratorNodeIds.next();
                List<ArrDaoLink> daoLinks = daoLinkRepository.findByNodeIdsAndFetchDao(partNodeIds);
                for (ArrDaoLink daoLink : daoLinks) {
                    if (daoLink.getDao().getDaoType().equals(DaoType.LEVEL)) {
                        daoLevelMap.put(daoLink.getNodeId(), daoLink.getDao());
                    }
                }
            }
        }

        Map<Integer, ArrNodeConformityExt> conformityInfoForNodes = Collections.emptyMap();
        List<Integer> conformityNodeIds = Collections.emptyList();
        if (param.isNodeConformity()) {
        	RuleService ruleService = applicationContext.getBean(RuleService.class);
            conformityInfoForNodes = ruleService.getNodeConformityInfoForNodes(nodeIds, fundVersion);
            conformityNodeIds = new ArrayList<>();
        }

        Map<Integer, ArrRequestVO> requestVOMap = Collections.emptyMap();
        Map<Integer, Set<ArrDigitizationRequest>> requestMap = Collections.emptyMap();
        if (param.isDigitizationRequest()) {
            requestMap = requestService.findDigitizationRequest(nodeIds, ArrRequest.State.OPEN);
            Set<ArrRequest> requests = new HashSet<>();
            for (Set<ArrDigitizationRequest> digitizationRequests : requestMap.values()) {
                requests.addAll(digitizationRequests);
            }
            List<ArrRequestVO> requestVOs = clientFactoryVO.createRequest(null, requests, false, fundVersion);
            requestVOMap = requestVOs.stream().collect(Collectors.toMap(ArrRequestVO::getId, Function.identity()));
        }

        for (TreeNode treeNode : treeNodeMap.values()) {
            Integer id = treeNode.getId();

            ArrNode arrNode = arrNodeMap.get(id);
            ArrDao dao = daoLevelMap.get(id);

            Node node = prepareNode(getNodesCtx, treeNode, arrNode, nodeValueMap, subtreeRoot, dao);
            getNodesCtx.addNode(node);

            if (param.isNodeConformity()) {
                ArrNodeConformityExt nodeConformity = conformityInfoForNodes.get(id);
                if (nodeConformity != null) {
                    node.setNodeConformity(clientFactoryVO.createNodeConformity(nodeConformity));
                    conformityNodeIds.add(id);
                }
            }

            if (param.isDigitizationRequest()) {
                node.setDigitizationRequests(addDigitizationRequests(requestVOMap, requestMap, id));
            }
        }

        if (param.isNodeConformity() && conformityNodeIds.size() > 0) {
            Map<Integer, Map<Integer, Boolean>> nodeIdsVisiblePolicy = policyService.getVisiblePolicyIds(conformityNodeIds, fundVersion, true);
            for (Node node : getNodesCtx.getNodes().values()) {
                Map<Integer, Boolean> visiblePolicy = nodeIdsVisiblePolicy.get(node.getId());
                if (visiblePolicy != null) {
                    node.getNodeConformity().setPolicyTypeIdsVisible(visiblePolicy);
                }
            }
        }

        return getNodesCtx.getNodes();
    }

    /**
     * Prepare data for one node
     *
     * @param getNodesCtx
     *
     * @param treeNode
     * @param arrNode
     * @param nodeValueMap
     * @param dao
     * @return
     */
    private Node prepareNode(GetNodesCtx requestCtx, TreeNode treeNode, ArrNode arrNode,
                             Map<Integer, TitleItemsByType> nodeValueMap,
                             TreeNode subtreeRoot,
                             ArrDao dao) {
        final Integer id = treeNode.getId();
        final ViewTitles viewTitles = requestCtx.getViewTitles();
        final NodeParam param = requestCtx.getParam();
        final TitleItemsByType descItemCodeToValueMap = nodeValueMap.get(id);
        TreeNode parent = treeNode.getParent();

        Node node = new Node(id, arrNode.getVersion(), arrNode.getUuid());
        node.setHasChildren(!treeNode.getChilds().isEmpty());
        node.setDepth(treeNode.getDepth());

        String defaultTitle;
        if (parent == null) {
            defaultTitle = createRootTitle(requestCtx.getFundVersion().getFund(), viewTitles, id);
        } else {
            defaultTitle = createDefaultTitle(viewTitles, id);
        }

        ArrDao displayDao = displayDaoId? dao : null;

        if (param.isName()) {
            node.setName(viewTitles.getTreeItem().build(descItemCodeToValueMap, displayDao, defaultTitle));
        }
        if (param.isAccordion()) {
            node.setAccordionLeft(viewTitles.getAccordionLeft().build(descItemCodeToValueMap, displayDao, defaultTitle));
            node.setAccordionRight(viewTitles.getAccordionRight().build(descItemCodeToValueMap, displayDao, defaultTitle));
        }
        if (param.isIcon()) {
            if (descItemCodeToValueMap != null) {
                String iconName = getIcon(descItemCodeToValueMap, viewTitles);
                node.setIcon(iconName);
            }
        }

        if (param.isReferenceMark() && subtreeRoot != null) {

            String[] parentReferenceMark;
            if (parent == null || parent.equals(subtreeRoot)) {
                parentReferenceMark = createClientReferenceMarkFromRoot(subtreeRoot, viewTitles, nodeValueMap);
            } else {
                Node parentNode = requestCtx.getNodes().get(parent.getId());
                Validate.notNull(parentNode, "Parent node not found, nodeId: %i", parent.getId());
                parentReferenceMark = parentNode.getReferenceMark();
            }
            String[] referenceMark = createClientNodeReferenceMark(treeNode, viewTitles.getLevelTypeId(),
                                                                   viewTitles, nodeValueMap,
                                                                   parentReferenceMark);
            node.setReferenceMark(referenceMark);
        }

        return node;

    }

    private String createRootTitle(ArrFund fund, ViewTitles viewTitles, Integer id) {
        // try to create from node
        List<String> detailList = new ArrayList<>();
        if (fund.getFundNumber() != null) {
            detailList.add(fund.getFundNumber().toString());
        }
        if (StringUtils.isNotEmpty(fund.getInternalCode())) {
            detailList.add(fund.getInternalCode());
        }
        if (StringUtils.isNotEmpty(fund.getMark())) {
            detailList.add(fund.getMark());
        }
        String title = String.join(" ", detailList);
        if (StringUtils.isNotEmpty(title)) {
            return title;
        }
        return createDefaultTitle(viewTitles, id);
    }

    /**
     * Získání DAO k JP.
     *
     * @param requestVOMap mapa vo DAO
     * @param requestMap   mapa DAO
     * @param nodeId       identifikátor JP
     * @return seznam dig. požadavků
     */
    private List<ArrDigitizationRequestVO> addDigitizationRequests(final Map<Integer, ArrRequestVO> requestVOMap, final Map<Integer, Set<ArrDigitizationRequest>> requestMap, final Integer nodeId) {
        Collection<ArrDigitizationRequest> digitizationRequests = requestMap.get(nodeId);
        if (CollectionUtils.isNotEmpty(digitizationRequests)) {
            List<ArrDigitizationRequestVO> digitizationRequestVOs = new ArrayList<>();
            for (ArrDigitizationRequest digitizationRequest : digitizationRequests) {
                ArrRequestVO requestVO = requestVOMap.get(digitizationRequest.getRequestId());
                digitizationRequestVOs.add((ArrDigitizationRequestVO) requestVO);
            }
            return digitizationRequestVOs;
        }
        return null;
    }

    /**
     * Získání inkonky JP.
     *
     * @param itemCodeToValueMap převodní mapa pro získání ikonky
     * @param vt                 title codes for view
     * @return text ikony
     */
    @Nullable
    private String getIcon(final TitleItemsByType itemCodeToValueMap, ViewTitles vt) {

        Integer levelTypeId = vt.getLevelTypeId();
        TitleValues values = itemCodeToValueMap.getTitles(levelTypeId);
        if (values != null) {
            for (TitleValue item : values.getValues()) {
                String iconCode = item.getIconValue();
                if (StringUtils.isNotEmpty(iconCode)) {
                    LevelConfig lh = vt.getLevelHierarchy(iconCode);
                    if (lh != null) {
                        return lh.getIcon();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Sestavení mapy JP podle identifikátorů.
     *
     * @param nodeIds identifikátory JP
     * @return výsledná mapa
     */
    private Map<Integer, ArrNode> createNodeMap(final Collection<Integer> nodeIds) {
        List<ArrNode> nodes = new ArrayList<>(nodeIds.size());
        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(nodeIds);
        while (iterator.hasNext()) {
            List<Integer> nodeIdsSublist = iterator.next();
            nodes.addAll(nodeRepository.findAllById(nodeIdsSublist));
        }

        Map<Integer, ArrNode> arrNodeMap = new HashMap<>(nodes.size());
        for (ArrNode node : nodes) {
            arrNodeMap.put(node.getNodeId(), node);
        }
        return arrNodeMap;
    }

    /**
     * Získání seznam sourozenců k JP.
     *
     * @param node        uzel, jehož sourozence chceme
     * @param fundVersion verze AS
     * @param fromIndex   index JP v úrovni, od kterého chceme sourozence
     * @param maxCount    maximální počet sourozenců, které chceme
     * @param fulltextFilter    filtrování sourozenců
     * @param userDetail  přihlášený uživatel
     * @return požadovaní sourozenci
     */
    public Siblings getNodeSiblings(final TreeNode node, final ArrFundVersion fundVersion, final int fromIndex, final int maxCount, @Nullable final String fulltextFilter, @Nullable UserDetail userDetail) {
        LinkedHashMap<Integer, TreeNode> nodesMap = new LinkedHashMap<>();
        TreeNode parentNode = node.getParent();

        List<TreeNode> childs;
        if (parentNode == null) {
            childs = Collections.singletonList(node);
        } else {
            childs = parentNode.getChilds();
        }

        if (fulltextFilter != null) {
            Set<Integer> fulltextIds = arrangementService.findNodeIdsByFulltext(fundVersion, parentNode == null ? null : parentNode.getId(), fulltextFilter, Depth.ONE_LEVEL);
            childs = new ArrayList<>(childs);
            childs.removeIf(next -> !fulltextIds.contains(next.getId()));
        }

        for (int i = fromIndex; i < childs.size() && nodesMap.size() < maxCount; i++) {
            TreeNode treeNode = childs.get(i);
            if (treeNode != null) {
                nodesMap.put(treeNode.getId(), treeNode);
            } else {
                logger.warn("Uzel s identifikátorem " + node.getId() + " neexistuje ve verzi " + fundVersion.getFundVersionId());
            }
        }

        NodeParam param = NodeParam.create()
                .accordion()
                .referenceMark()
                .digitizationRequest()
                .nodeConformity();

        LinkedHashMap<Integer, Node> nodeMap = getNodes(nodesMap, parentNode, param, fundVersion);

        Map<Integer, List<WfIssue>> nodeToIssueMap = issueDataService.groupOpenIssueByNodeId(nodeMap.keySet(), userDetail);

        List<AccordionNodeVO> accordionNodes = convertToAccordionNode(nodeMap.values(), nodeToIssueMap);

        int nodeIndex = childs.indexOf(node);
        if (nodeIndex < 0) {
            nodeIndex = 0;
        }

        return new Siblings(accordionNodes, childs.size(), nodeIndex);
    }

    /**
     * Struktura pro sourozence JP.
     */
    public static class Siblings {

        /**
         * Seznam sourozenců.
         */
        private List<AccordionNodeVO> siblings;

        /**
         * Skutečný index JP.
         */
        private int nodeIndex;

        /**
         * Celkový počet sourozenců JP.
         */
        private int siblingsCount;

        public Siblings(final List<AccordionNodeVO> siblings, final int siblingsCount, final int nodeIndex) {
            if (nodeIndex < 0) {
                throw new IllegalArgumentException("nodeIndex is " + nodeIndex);
            }
            this.siblings = siblings;
            this.siblingsCount = siblingsCount;
            this.nodeIndex = nodeIndex;
        }

        public List<AccordionNodeVO> getSiblings() {
            return siblings;
        }

        public int getNodeIndex() {
            return nodeIndex;
        }

        public int getSiblingsCount() {
            return siblingsCount;
        }
    }

    /**
     * Kapacitní mapa. Při překročení kapacity odstraní z mapy nejstarší záznam (první vložený).
     */
    private class CapacityMap<K, V> extends LinkedHashMap<K, V> {

        @Override
        protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
            if (size() > maxCacheSize) {
                logger.debug("Překročena kapacita cache. Bude odpojena cache verze s id " + eldest.getKey());
                return true;
            }
            return false;
        }
    }

    /**
     * Provede vytvoření referenčního označení pro uzel. Předpokládá, že parent tohoto uzlu má načtený
     * hodnoty a vytvořené referenční označení.
     *
     * @param node                uzel
     * @param levelItemTypeId     id atributu úrovně popisu
     * @param viewTitles          nastavení načítání atributů
     * @param valuesMap           mapa načtených hodnot pro uzly
     * @param parentReferenceMark referenční označení rodiče
     * @return referenční označení uzlu
     */
    private String[] createClientNodeReferenceMark(final TreeNode node,
                                                   @Nullable final Integer levelItemTypeId,
                                                   final ViewTitles viewTitles,
                                                   final Map<Integer, TitleItemsByType> valuesMap,
                                                   final String[] parentReferenceMark) {


        String separator = " ";

        TreeNode parent = node.getParent();
        if (parent == null) {
            return new String[0];
        }

        if (parentReferenceMark.length == 0) {
            return new String[] {node.getPosition().toString()};
        }

        String[] parentMark = parentReferenceMark;
        String[] nodeMark = Arrays.copyOf(parentMark, parentMark.length + 2);

        if (levelItemTypeId != null) {

            TitleItemsByType nodeItems = valuesMap.get(node.getId());
            TitleItemsByType parentItems = valuesMap.get(parent.getId());

            TitleValues nodeTitleValue = nodeItems == null ? null : nodeItems.getTitles(levelItemTypeId);
            TitleValues parentTitleValue = parentItems == null ? null : parentItems.getTitles(levelItemTypeId);

            String nodeTypeSpec = nodeTitleValue == null ? null
                    : nodeTitleValue.getValues().iterator().next().getSpecCode();
            String parentSpecCode = parentTitleValue == null ? null
                    : parentTitleValue.getValues().iterator().next().getSpecCode();

            if (StringUtils.isNotBlank(nodeTypeSpec) && StringUtils.isNotBlank(parentSpecCode)) {
                LevelConfig levelTitlesHierarchy = viewTitles.getLevelHierarchy(nodeTypeSpec);
                if (levelTitlesHierarchy != null) {
                    separator = levelTitlesHierarchy.getSeparForParent(parentSpecCode);
                }
                if (separator == null) {
                    separator = viewTitles.getDefaultLevelSeparator();
                }
            }
        }

        nodeMark[parentMark.length] = separator;
        nodeMark[parentMark.length + 1] = node.getReferenceMark()[node.getReferenceMark().length - 1].toString();
        return nodeMark;
    }

    /**
     * Provede načtení referečního označení pro uzel. Načte označení od kořenu až po uzel.
     *
     * @param node          uzel
     * @param viewTitles    nastavení načítání atributů
     * @param valuesMap     mapa načtených hodnot pro uzly
     * @return referenční označení
     */
    private String[] createClientReferenceMarkFromRoot(final TreeNode node,
                                                       final ViewTitles viewTitles,
                                                       final Map<Integer, TitleItemsByType> valuesMap) {

        TreeNode parent = node.getParent();
        if (parent == null) {
            return new String[0];
        }
        final Integer levelTypeId = viewTitles.getLevelTypeId();

        String[] parentReferenceMark = createClientReferenceMarkFromRoot(parent, viewTitles, valuesMap);

        String[] referenceMark = createClientNodeReferenceMark(node, levelTypeId, viewTitles, valuesMap,
                parentReferenceMark);
        return referenceMark;
    }

    /**
     * Sestaví informace o zanoření
     *
     * Pokud node neexistuje (byl vymazan), tak je ignorovan.
     *
     * @param fundId
     *            identifikátor archivního souboru
     * @param nodeIds
     *            seznam identifikátorů jednotek popisu
     */
    public Map<Integer, TreeNodeVO> findNodeReferenceMark(@NotNull Integer fundId,
                                                          Collection<Integer> nodeIds) {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return Collections.emptyMap();
        }
        ArrFundVersion fundVersion = arrangementService.getOpenVersionByFundId(fundId);
        if (fundVersion == null) {
            logger.error("Fund not found, foundId: {}", fundId);
            throw new IllegalStateException("Fund not found, foundId: "+fundId);
        }

        // filter only existing nodes
        Map<Integer, TreeNode> versionTreeCache = getVersionTreeCache(fundVersion);
        Collection<Integer> nodeIdList = new ArrayList<>(nodeIds.size());
        Map<Integer, TreeNode> subTreeMap = new LinkedHashMap<>();
        for (Integer nodeId : nodeIds) {
            TreeNode treeNode = versionTreeCache.get(nodeId);
            if (treeNode != null) {
                nodeIdList.add(nodeId);

                TreeNode parent = treeNode;
                while (parent != null && subTreeMap.get(parent) == null) {
                    subTreeMap.put(parent.getId(), parent);
                    parent = parent.getParent();
                }
            }
        }
        List<TreeNodeVO> nodes = getNodesByIds(nodeIdList, fundVersion, subTreeMap);
        return nodes.stream().collect(Collectors.toMap(TreeNodeVO::getId, node -> node));
    }

    /**
     * Najde id všech nodů ve verzi.
     *
     * @param version verze stromu
     * @param nodeId id nodu pod kterým se má hledat
     * @param depth hloubka v jaké se mají hledat potomci
     *
     * @return id všech nodů ve verzi
     */
    public Set<Integer> getAllNodeIdsByVersionAndParent(final ArrFundVersion version, final Integer nodeId, final Depth depth) {
        Validate.notNull(version, "Verze AS musí být vyplněna");

        Map<Integer, TreeNode> versionTreeCache = getVersionTreeCache(version);

        if (nodeId == null) {
            Set<Integer> nodeIds = new HashSet<>();
            if (depth == Depth.ONE_LEVEL) {
                for (Map.Entry<Integer, TreeNode> integerTreeNodeEntry : versionTreeCache.entrySet()) {
                    if (integerTreeNodeEntry.getValue().getParent() == null) {
                        nodeIds.add(integerTreeNodeEntry.getKey());
                    }
                }
            } else {
                nodeIds.addAll(versionTreeCache.keySet());
            }
            return nodeIds;
        }

        Validate.notNull(depth, "Hlouba není vyplněna");

        if (depth == Depth.ONE_LEVEL) {
            TreeNode node = versionTreeCache.get(nodeId);
            return node.getChilds().stream().mapToInt(TreeNode::getId).boxed().collect(Collectors.toSet());
        }

        Set<Integer> nodeIds = new HashSet<>();
        TreeNode parentNode = versionTreeCache.get(nodeId);
        Queue<TreeNode> children = new LinkedList<>();
        children.add(parentNode);
        while (!children.isEmpty()) {
            TreeNode node = children.poll();

            List<TreeNode> childs = node.getChilds();
            if (childs != null) {
                node.getChilds().forEach(child -> {
                    nodeIds.add(child.getId());
                    children.add(child);
                });
            }
        }

        return nodeIds;
    }

    /**
     * Rekurzivní procházení stromu.
     *
     * @param root výchozí node
     * @param callback akce
     */
    public void walkTree(@NotNull final TreeNode root, @NotNull final Consumer<TreeNode> callback) {
        callback.accept(root);
        LinkedList<TreeNode> childs = root.getChilds();
        if (childs != null) {
            for (TreeNode child : childs) {
                walkTree(child, callback);
            }
        }
    }
}
