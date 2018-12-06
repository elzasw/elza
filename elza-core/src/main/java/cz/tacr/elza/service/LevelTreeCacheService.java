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
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.eventbus.Subscribe;

import cz.tacr.elza.ElzaTools;
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
import cz.tacr.elza.controller.vo.nodes.NodeData;
import cz.tacr.elza.controller.vo.nodes.NodeDataParam;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrDigitizationRequest;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.ArrRequest;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.vo.TitleValue;
import cz.tacr.elza.domain.vo.TitleValues;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.LevelRepositoryCustom;
import cz.tacr.elza.repository.NodeRepository;
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
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 11.01.2016
 */
@Service
@EventBusListener
public class LevelTreeCacheService {

    /**
     * Maximální počet verzí stromů ukládaných současně v paměti.
     */
    private static final int MAX_CACHE_SIZE = 30;

    private static final Logger logger = LoggerFactory.getLogger(LevelTreeCacheService.class);

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private RuleService ruleService;

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

    /**
     * Cache stromu pro danou verzi. (id verze -> nodeid uzlu -> uzel).
     * Maximální počet záznamů v cache {@link #MAX_CACHE_SIZE}.
     */
    private CapacityMap<Integer, Map<Integer, TreeNode>> versionCache = new CapacityMap<Integer, Map<Integer, TreeNode>>();

    @Subscribe
    public synchronized void invalidateCache(final CacheInvalidateEvent cacheInvalidateEvent) {
        if (cacheInvalidateEvent.contains(CacheInvalidateEvent.Type.LEVEL_TREE)) {
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


        Set<Integer> nodesToExpandIDs = new HashSet<>();
        if (expandedIds != null) {
            nodesToExpandIDs.addAll(expandedIds);
        }

        ArrFundVersion version = fundVersionRepository.findOne(versionId);

        Map<Integer, TreeNode> treeMap = getVersionTreeCache(version);
        Set<Integer> expandedIdsExtended = createExpandedIdsExtension(includeIds, treeMap);


        if (nodeId == null) {
            expandedIdsExtended.add(version.getRootNode().getNodeId());
        } else {
            //pokud vracíme podstrom, přidáme pro jistotu nodeid do otevřených uzlů
            nodesToExpandIDs.add(nodeId);
        }

        //do rozbalených uzlů přidáme ty, které je nutné rozbalit, aby byly included vidět
        nodesToExpandIDs.addAll(expandedIdsExtended);


        Map<Integer, TreeNode> expandedNodes = new TreeMap<Integer, TreeNode>();
        for (Integer expandedId : nodesToExpandIDs) {
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

        return new TreeData(convertToTreeNode(nodes.values()), expandedIdsExtended);
    }

    /**
     * Načte seznam nodů podle jejich id v dané verzi AP. Vrácený seznam je ve stejném pořadí jako id.
     *
     * @param nodeIds   id uzlů
     * @param versionId id verze stromu
     * @return nalezené uzly
     */
    public List<TreeNodeVO> getNodesByIds(final Collection<Integer> nodeIds, final Integer versionId) {

        ArrFundVersion version = fundVersionRepository.getOneCheckExist(versionId);

        Map<Integer, TreeNode> versionTreeCache = getVersionTreeCache(version);

        Map<Integer, TreeNode> subMap = new LinkedHashMap<>();
        for (Integer nodeId : nodeIds) {
            TreeNode treeNode = versionTreeCache.get(nodeId);
            if (treeNode != null) {

                TreeNode parent = treeNode;
                while(parent != null){
                    subMap.put(parent.getId(), parent);
                    parent = parent.getParent();
                }
            }
        }

        Map<Integer, TitleItemsByType> valuesMap = createValuesMap(subMap, version, null);
        Map<Integer, TreeNodeVO> clientMap = createNodesWithTitles(subMap, valuesMap, null, version);

        List<TreeNodeVO> result = new LinkedList<>();
        ViewTitles viewTitles = configView.getViewTitles(version.getRuleSetId(), version.getFund().getFundId());

        Integer levelTypeId = viewTitles.getLevelTypeId();

        for (Integer nodeId : nodeIds) {
            TreeNode treeNode = versionTreeCache.get(nodeId);
            if(treeNode != null){
                String[] referenceMark = createClientReferenceMarkFromRoot(treeNode, levelTypeId,
                        viewTitles, valuesMap);
                TreeNodeVO clientNode = clientMap.get(nodeId);
                clientNode.setReferenceMark(referenceMark);
                result.add(clientNode);
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
    private Set<Integer> createExpandedIdsExtension(final Set<Integer> includedIds,
                                                    final Map<Integer, TreeNode> treeMap) {
        Set<Integer> result = new HashSet<>();

        if (CollectionUtils.isNotEmpty(includedIds)) {

            for (Integer includedId : includedIds) {
                TreeNode node = treeMap.get(includedId);
                if (node == null) {
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
        Assert.notNull(version, "Verze AS musí být vyplněna");

        Integer rootId = version.getRootNode().getNodeId();

        //kořen
        LevelRepositoryCustom.LevelInfo rootInfo = new LevelRepositoryCustom.LevelInfo(rootId, 0, null);

        //všechny uzly stromu
        List<LevelRepositoryCustom.LevelInfo> levelInfos = levelRepository.readTree(version);


        //výsledná mapa
        Map<Integer, TreeNode> allMap = new HashMap<>();


        //mapa všech základních dat uzlů
        Map<Integer, LevelRepositoryCustom.LevelInfo> levelInfoMap = ElzaTools
                .createEntityMap(levelInfos, (i) -> i.getNodeId());
        levelInfoMap.put(rootId, rootInfo);

        for (LevelRepositoryCustom.LevelInfo levelInfo : levelInfoMap.values()) {
            createTreeNodeMap(levelInfo, levelInfoMap, allMap);
        }


        //seřazení dětí všech uzlů podle pozice
        Comparator<TreeNode> comparator = (o1, o2) -> o1.getPosition().compareTo(o2.getPosition());
        for (TreeNode treeNode : allMap.values()) {
            treeNode.getChilds().sort(comparator);
        }

        initReferenceMarksAndDepth(allMap.get(rootId));


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
            TreeNode parentNode = createTreeNodeMap(levelInfoMap.get(levelInfo.getParentId()), levelInfoMap,
                    allNodesMap);
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
        Assert.notNull(fund, "AS musí být vyplněn");
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
        Assert.notNull(fundVersion, "Verze AS není vyplněna");
        versionCache.remove(fundVersion.getFundVersionId());
    }

    /**
     * Invalidace verze AS v cache.
     *
     * @param fundVersion verze archivního souboru
     */
    synchronized public void refreshFundVersion(final ArrFundVersion fundVersion) {
        Assert.notNull(fundVersion, "Verze AS není vyplněna");
        invalidateFundVersion(fundVersion);
        getVersionTreeCache(fundVersion);
    }

    @Subscribe
    public void onDataUpdate(final EventChangeMessage changeMessage) {

        List<AbstractEventSimple> events = changeMessage.getEvents();
        for (AbstractEventSimple event : events) {
            logger.debug("Zpracování události "+event.getEventType());
            //projdeme všechny změny, které jsou změny ve stromu uzlů verze a smažeme cache verzí
            if (EventVersion.class.isAssignableFrom(event.getClass())) {
                Integer changedVersionId = ((EventVersion) event).getVersionId();
                ArrFundVersion version = fundVersionRepository.findOne(changedVersionId);

                switch (event.getEventType()) {
                    case NODE_DELETE:
                        break;
                    case ADD_LEVEL_AFTER:
                    case ADD_LEVEL_BEFORE:
                    case ADD_LEVEL_UNDER:
                        EventAddNode eventAddNode = (EventAddNode) event;
                        actionAddLevel(eventAddNode.getNode().getNodeId(), eventAddNode.getStaticNode().getNodeId(),
                                version, event.getEventType());
                        break;
                    case MOVE_LEVEL_AFTER:
                    case MOVE_LEVEL_BEFORE:
                    case MOVE_LEVEL_UNDER:
                        EventNodeMove eventNodeMove = (EventNodeMove) event;
                        List<Integer> transportIds = eventNodeMove.getTransportLevels().stream()
                                .map(n -> n.getNodeId()).collect(Collectors.toList());
                        actionMoveLevel(eventNodeMove.getStaticLevel().getNodeId(), transportIds,
                                version, event.getEventType());

                        break;
                    case DELETE_LEVEL:
                        EventDeleteNode eventIdInVersion = (EventDeleteNode) event;
                        actionDeleteLevel(eventIdInVersion.getNodeId(), version);
                        break;
                    case BULK_ACTION_STATE_CHANGE:
                        EventIdInVersion bulkActionStateChangeEvent = (EventIdInVersion) event;
                        if (bulkActionStateChangeEvent.getState().equals(ArrBulkActionRun.State.FINISHED.toString())) {
                            if (bulkActionStateChangeEvent.getCode().equals("PERZISTENTNI_RAZENI")) {
                                refreshFundVersion(version);
                            }
                        }
                        break;
                }

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
        Assert.notNull(nodeIds, "Nebyly vyplněny identifikátory JP");
        Assert.notNull(version, "Verze AS musí být vyplněna");

        Map<Integer, TreeNode> versionTreeCache = getVersionTreeCache(version);
        Map<Integer, TreeNode> nodeIdParentMap = new HashMap<>(nodeIds.size());
        Map<Integer, TreeNode> parentIdParentMap = new HashMap<>(nodeIds.size());

        for (Integer nodeId : nodeIds) {
            TreeNode treeNode = versionTreeCache.get(nodeId);
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
        Assert.notNull(treeNodeMap, "Mapa nesmí být null");
        Assert.notNull(version, "Verze AS musí být vyplněna");

        List<ArrNode> nodes = new ArrayList<>(treeNodeMap.size());
        Set<Integer> nodeIds = treeNodeMap.keySet();
        ObjectListIterator<Integer> iterator = new ObjectListIterator<>(nodeIds);
        while (iterator.hasNext()) {
            List<Integer> nodeIdsSublist = iterator.next();

            nodes.addAll(nodeRepository.findAll(nodeIdsSublist));
        }
        Map<Integer, ArrNode> nodeMap = new HashMap<>(nodes.size());
        for (ArrNode node : nodes) {
            nodeMap.put(node.getNodeId(), node);
        }

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
                                                                  levelTypeId, viewTitles, valuesMap);
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


            TreeNodeVO client = new TreeNodeVO(treeNode.getId(), treeNode.getDepth(),
                    null, !treeNode.getChilds().isEmpty(), treeNode.getReferenceMark(),
                    nodeMap.get(treeNode.getId()).getVersion());
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
            fillValues(descItemCodeToValueMap, viewTitles, treeNodeClient);
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

    private String createTitle(final List<Integer> itemTypeIds, final TitleItemsByType descItemCodeToValueMap,
                               final boolean useDefaultTitle, final String defaultNodeTitle) {
        List<String> titles = new ArrayList<String>();

        if (itemTypeIds != null) {
            for (Integer itemTypeId : itemTypeIds) {
                TitleValues titleValues = descItemCodeToValueMap.getTitles(itemTypeId);
                if (titleValues != null) {
                    TitleValue titleValue = titleValues.getValues().iterator().next();

                    String value = titleValue.getValue();
                    if (StringUtils.isNotBlank(value)) {
                        titles.add(value);
                    }
                }
            }
        }

        String title;
        if (titles.isEmpty()) {
            if (useDefaultTitle) {
                title = defaultNodeTitle;
            } else {
                title = null;
            }
        } else {
            title = StringUtils.join(titles, " ");
        }

        return title;
    }

    // ??
    private void fillValues(final TitleItemsByType descItemCodeToValueMap,
                            final ViewTitles viewTitles, final TreeNodeVO treeNodeClient) {
        String defaultTitle = createDefaultTitle(viewTitles, treeNodeClient.getId());

        if (descItemCodeToValueMap != null) {
            treeNodeClient
                    .setName(createTitle(viewTitles.getTreeItemIds(), descItemCodeToValueMap, true, defaultTitle));

            String icon = getIcon(descItemCodeToValueMap, viewTitles);
            treeNodeClient.setIcon(icon);
        } else {
            treeNodeClient.setName(defaultTitle);
        }
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
     * @param nodeId
     * @param version
     */
    synchronized private void actionDeleteLevel(final Integer nodeId, final ArrFundVersion version) {
        Map<Integer, TreeNode> versionTreeMap = getVersionTreeCache(version);


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
     * @param newNodeId    id přidaného uzlu
     * @param staticId     id statického uzlu
     * @param version      verzes tromu
     * @param addLevelType typ akce
     */
    synchronized private void actionAddLevel(final Integer newNodeId,
                                             final Integer staticId,
                                             final ArrFundVersion version,
                                             final EventType addLevelType) {
        Map<Integer, TreeNode> versionTreeMap = getVersionTreeCache(version);
        if (versionTreeMap != null) {
            TreeNode staticNode = versionTreeMap.get(staticId);
            TreeNode newNode;
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
    }

    /**
     * Aktualizace cache pro přesunutí uzlu.
     *
     * @param staticId      id statického nodu
     * @param nodeIds       seznam id nodů k přesunutí
     * @param version       verze stromu
     * @param moveLevelType typ události
     */
    synchronized private void actionMoveLevel(final Integer staticId,
                                              final List<Integer> nodeIds,
                                              final ArrFundVersion version,
                                              final EventType moveLevelType) {
        if (CollectionUtils.isEmpty(nodeIds)) {
            return;
        }

        Map<Integer, TreeNode> versionTreeMap = getVersionTreeCache(version);
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
        ArrFundVersion version = fundVersionRepository.findOne(versionId);
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
        List<TreeNodeVO> nodes = getNodesByIds(nodeIds, version.getFundVersionId());

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
     * @return požadovaná data
     */
    public NodeData getNodeData(final NodeDataParam param) {

        Validate.notNull(param);
        Validate.notNull(param.getFundVersionId());

        ArrFundVersion fundVersion = fundVersionRepository.findOne(param.getFundVersionId());
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
                throw new IllegalArgumentException("Index pro sourozence nesmí být záporný");
            }
            LevelTreeCacheService.Siblings siblings = getNodeSiblings(node, fundVersion, siblingsFrom, maxCount, fulltext);
            result.setNodeIndex(siblings.getNodeIndex());
            result.setNodeCount(siblings.getSiblingsCount());
            result.setSiblings(siblings.getSiblings());
        } else if (fulltext != null) { // pokud je zafiltrováno, je nutné brát výsledky (index + počet sourozenů) vzhledem k filtru
            LevelTreeCacheService.Siblings siblings = getNodeSiblings(node, fundVersion, 0, maxCount, fulltext);
            result.setNodeIndex(siblings.getNodeIndex());
            result.setNodeCount(siblings.getSiblingsCount());
        }

        return result;
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

        return convertToTreeNode(getNodes(nodesMap, node, param, fundVersion).values());
    }

    /**
     * Konverze požadovaných objektů do objektů pro strom.
     *
     * @param nodes JP k převodu
     * @return převedené JP
     */
    private List<TreeNodeVO> convertToTreeNode(final Collection<Node> nodes) {
        return nodes.stream().map(node -> {
            TreeNodeVO treeNode = new TreeNodeVO();
            treeNode.setId(node.getId());
            treeNode.setName(node.getName());
            treeNode.setReferenceMark(node.getReferenceMark());
            treeNode.setHasChildren(node.isHasChildren());
            treeNode.setIcon(node.getIcon());
            treeNode.setDepth(node.getDepth());
            treeNode.setVersion(node.getVersion());
            return treeNode;
        }).collect(Collectors.toList());
    }

    /**
     * Konverze požadovaných objektů do objektů pro accordion.
     *
     * @param nodes JP k převodu
     * @return převedené JP
     */
    private List<AccordionNodeVO> convertToAccordionNode(final Collection<Node> nodes) {
        return nodes.stream().map(n -> {
            AccordionNodeVO accordionNode = new AccordionNodeVO();
            accordionNode.setId(n.getId());
            accordionNode.setAccordionLeft(n.getAccordionLeft());
            accordionNode.setAccordionRight(n.getAccordionRight());
            accordionNode.setReferenceMark(n.getReferenceMark());
            accordionNode.setDigitizationRequests(n.getDigitizationRequests());
            accordionNode.setNodeConformity(n.getNodeConformity());
            accordionNode.setVersion(n.getVersion());
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

        public Node(final Integer id, final Integer version) {
            this.id = id;
            this.version = version;
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
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");

        LinkedHashMap<Integer, Node> nodeMap = new LinkedHashMap<>();
        Set<Integer> nodeIds = treeNodeMap.keySet();
        Map<Integer, ArrNode> arrNodeMap = createNodeMap(nodeIds);

        ViewTitles viewTitles = configView.getViewTitles(fundVersion.getRuleSetId(), fundVersion.getFund().getFundId());
        // read LevelTypeId
        Integer levelTypeId = viewTitles.getLevelTypeId();
        
        Map<Integer, TitleItemsByType> nodeValueMap = createValuesMap(treeNodeMap, fundVersion, subtreeRoot);

        String[] rootReferenceMark = new String[0];
        if (param.isReferenceMark() && subtreeRoot != null) {
            rootReferenceMark = createClientReferenceMarkFromRoot(subtreeRoot, levelTypeId,
                                                                  viewTitles, nodeValueMap);
        }

        String[] parentReferenceMark = rootReferenceMark;

        Map<Integer, ArrNodeConformityExt> conformityInfoForNodes = Collections.emptyMap();
        List<Integer> conformityNodeIds = Collections.emptyList();
        if (param.isNodeConformity()) {
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
            List<ArrRequestVO> requestVOs = clientFactoryVO.createRequest(requests, false, fundVersion);
            requestVOMap = requestVOs.stream().collect(Collectors.toMap(ArrRequestVO::getId, Function.identity()));
        }

        for (TreeNode treeNode : treeNodeMap.values()) {
            Integer id = treeNode.getId();

            Node node = new Node(id, arrNodeMap.get(id).getVersion());
            node.setHasChildren(!treeNode.getChilds().isEmpty());
            node.setDepth(treeNode.getDepth());

            TitleItemsByType descItemCodeToValueMap = nodeValueMap.get(id);

            String defaultTitle = createDefaultTitle(viewTitles, id);
            if (descItemCodeToValueMap != null) {
                if (param.isName()) {
                    node.setName(createTitle(viewTitles.getTreeItemIds(), descItemCodeToValueMap, true,
                                             defaultTitle));
                }
                if (param.isAccordion()) {
                    node.setAccordionLeft(createTitle(viewTitles.getAccordionLeftIds(), descItemCodeToValueMap, true,
                                                      defaultTitle));
                    node.setAccordionRight(createTitle(viewTitles.getAccordionRightIds(), descItemCodeToValueMap, false,
                                                       defaultTitle));
                }
                if (param.isIcon()) {
                    String iconName = getIcon(descItemCodeToValueMap, viewTitles);
                    node.setIcon(iconName);
                }
            } else {
                if (param.isName()) {
                    node.setName(defaultTitle);
                }
            }

            if (param.isReferenceMark() && subtreeRoot != null) {
                TreeNode parent = treeNode.getParent();
                if (parent != null) {
                    if (parent.equals(subtreeRoot)) {
                        parentReferenceMark = rootReferenceMark;
                    } else {
                        parentReferenceMark = nodeMap.get(parent.getId()).getReferenceMark();
                    }
                }
                String[] referenceMark = createClientNodeReferenceMark(treeNode, levelTypeId, viewTitles, nodeValueMap,
                                                                       parentReferenceMark);
                node.setReferenceMark(referenceMark);
            }

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

            nodeMap.put(id, node);
        }

        if (param.isNodeConformity() && conformityNodeIds.size() > 0) {
            Map<Integer, Map<Integer, Boolean>> nodeIdsVisiblePolicy = policyService.getVisiblePolicyIds(conformityNodeIds, fundVersion, true);
            for (Node node : nodeMap.values()) {
                Map<Integer, Boolean> visiblePolicy = nodeIdsVisiblePolicy.get(node.getId());
                if (visiblePolicy != null) {
                    node.getNodeConformity().setPolicyTypeIdsVisible(visiblePolicy);
                }
            }
        }

        return nodeMap;
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
     * @param defaultTitle       výchozí popisek JP
     * @param hierarchy          hierarchie stromu
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
            nodes.addAll(nodeRepository.findAll(nodeIdsSublist));
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
     * @return požadovaní sourozenci
     */
    public Siblings getNodeSiblings(final TreeNode node, final ArrFundVersion fundVersion, final int fromIndex, final int maxCount, @Nullable final String fulltextFilter) {
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

        LinkedHashMap<Integer, Node> nodes = getNodes(nodesMap, parentNode, param, fundVersion);
        Collection<Node> values = nodes.values();
        List<AccordionNodeVO> accordionNodes = convertToAccordionNode(values);

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
            if (size() > MAX_CACHE_SIZE) {
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
     * @param levelTypeCode       kod atributu úrovně popisu
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
     * @param levelTypeCode kod atributu úrovně popisu
     * @param viewTitles    nastavení načítání atributů
     * @param valuesMap     mapa načtených hodnot pro uzly
     * @return referenční označení
     */
    private String[] createClientReferenceMarkFromRoot(final TreeNode node,
                                                       @Nullable final Integer levelTypeId,
                                                       final ViewTitles viewTitles,
                                                       final Map<Integer, TitleItemsByType> valuesMap) {

        TreeNode parent = node.getParent();
        if (parent == null) {
            return new String[0];
        }

        String[] parentReferenceMark = createClientReferenceMarkFromRoot(parent, levelTypeId, viewTitles, valuesMap);

        String[] referenceMark = createClientNodeReferenceMark(node, levelTypeId, viewTitles, valuesMap,
                parentReferenceMark);
        return referenceMark;
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
        Assert.notNull(version, "Verze AS musí být vyplněna");

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

        Assert.notNull(depth, "Hlouba není vyplněna");

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
}




