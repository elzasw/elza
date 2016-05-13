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
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.google.common.eventbus.Subscribe;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.EventBusListener;
import cz.tacr.elza.config.ConfigView;
import cz.tacr.elza.config.ConfigView.ViewTitles;
import cz.tacr.elza.controller.ArrangementController.Depth;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.NodeItemWithParent;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.vo.TitleValue;
import cz.tacr.elza.domain.vo.TitleValues;
import cz.tacr.elza.repository.CalendarTypeRepository;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FundVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.LevelRepositoryCustom;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.eventnotification.EventChangeMessage;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;
import cz.tacr.elza.service.eventnotification.events.EventAddNode;
import cz.tacr.elza.service.eventnotification.events.EventDeleteNode;
import cz.tacr.elza.service.eventnotification.events.EventNodeMove;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.eventnotification.events.EventVersion;
import cz.tacr.elza.utils.ObjectListIterator;


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

    final Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private FundVersionRepository fundVersionRepository;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private ClientFactoryVO clientFactoryVO;

    @Autowired
    private ConfigView configView;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private CalendarTypeRepository calendarTypeRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;

    @Autowired
    private PolicyService policyService;

    @Value("${elza.treenode.defaultTitle}")
    private String defaultNodeTitle = "";


    /**
     * Cache stromu pro danou verzi. (id verze -> nodeid uzlu -> uzel).
     * Maximální počet záznamů v cache {@link #MAX_CACHE_SIZE}.
     */
    private CapacityMap<Integer, Map<Integer, TreeNode>> versionCache = new CapacityMap<Integer, Map<Integer, TreeNode>>();


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

        TreeData treeData = new TreeData(createNodesWithTitles(nodesMap, null, rootNode, version).values(), expandedIdsExtended);

        addConformityInfo(treeData.getNodes(), version);

        return treeData;
    }

    /**
     * Načte seznam nodů podle jejich id v dané verzi AP. Vrácený seznam je ve stejném pořadí jako id.
     *
     * @param nodeIds   id uzlů
     * @param versionId id verze stromu
     * @return nalezené uzly
     */
    public List<TreeNodeClient> getNodesByIds(final Collection<Integer> nodeIds, final Integer versionId) {

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

        TreeNode subtreeNode = null;
        Map<Integer, Map<String, TitleValues>> valuesMap = createValuesMap(subMap, version, subtreeNode);
        Map<Integer, TreeNodeClient> clientMap = createNodesWithTitles(subMap, valuesMap, null, version);

        List<TreeNodeClient> result = new LinkedList<>();
        ViewTitles viewTitles = configView.getViewTitles(version.getRuleSet().getCode(), version.getFund().getFundId());

        String levelTypeCode = viewTitles.getHierarchyLevelType();

        for (Integer nodeId : nodeIds) {
            TreeNode treeNode = versionTreeCache.get(nodeId);
            if(treeNode != null){
                String[] referenceMark = createClientReferenceMarkFromRoot(treeNode, levelTypeCode,
                        viewTitles, valuesMap);
                TreeNodeClient clientNode = clientMap.get(nodeId);
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
    public List<NodeItemWithParent> getNodeItemsWithParents(final Set<Integer> nodeIds, final ArrFundVersion fundVersion) {

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

        Collection<TreeNodeClient> treeNodes = getFaTreeNodes(fundVersion.getFundVersionId(), allNodeIds);
        Map<Integer, TreeNodeClient> mapTreeNodes = treeNodes.stream().collect(Collectors.toMap(TreeNodeClient::getId, (p) -> p));

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
     * Přidá informace o stavu uzlů.
     *
     * @param nodes     uzly pro vyplnění conformity info
     * @param version   verze stromu
     */
    private void addConformityInfo(final Collection<TreeNodeClient> nodes, final ArrFundVersion version) {

        ArrayList<Integer> nodeIds = nodes.stream().map(TreeNodeClient::getId)
                .collect(Collectors.toCollection(ArrayList::new));

        Map<Integer, ArrNodeConformityExt> conformityInfoForNodes = ruleService
                .getNodeConformityInfoForNodes(nodeIds, version);

        List<Integer> conformityNodeIds = new ArrayList<>();
        for (TreeNodeClient treeNode: nodes) {
            ArrNodeConformityExt nodeConformity = conformityInfoForNodes.get(treeNode.getId());
            if (nodeConformity != null) {
                treeNode.setNodeConformity(clientFactoryVO.createNodeConformity(nodeConformity));
                conformityNodeIds.add(treeNode.getId());
            }
        }

        Map<Integer, Map<Integer, Boolean>> nodeIdsVisiblePolicy = policyService.getVisiblePolicyIds(conformityNodeIds, version, true);
        for (TreeNodeClient node : nodes) {
            Map<Integer, Boolean> visiblePolicy = nodeIdsVisiblePolicy.get(node.getId());
            if (visiblePolicy != null) {
                node.getNodeConformity().setPolicyTypeIdsVisible(visiblePolicy);
            }
        }

    }

    /**
     * Najde v cache seznam id rodičů daného uzlu. Seřazeno od prvního id rodiče po kořen stromu.
     *
     * @param nodeId        id nodu pod kterým se má hledat
     * @param fundVersion   verze AS
     * @return  seznam identifikátorů uzlů
     */
    public List<Integer> getParentNodeIds(final Integer nodeId, final ArrFundVersion fundVersion) {
        Assert.notNull(nodeId);
        Assert.notNull(fundVersion);

        // mapa nodů z cache
        Map<Integer, TreeNode> treeMap = getVersionTreeCache(fundVersion);

        TreeNode node = treeMap.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Ve verzi " + fundVersion.getFundVersionId()
                    + " nebyl nalezen node s id " + nodeId);
        }

        LinkedList<Integer> parents = new LinkedList();

        // procházím prvky přes rodiče až ke kořeni
        TreeNode parent = node.getParent();
        while (parent != null) {
            parents.addFirst(parent.getId());
            parent = parent.getParent();
        }

        return parents;
    }

    /**
     * Najde v cache seznam rodičů daného uzlu. Seřazeno od prvního rodiče po kořen stromu.
     *
     * @param nodeId    nodeid uzlu
     * @param versionId id verze stromu
     * @return seznam rodičů
     */
    public Collection<TreeNodeClient> getNodeParents(final Integer nodeId, final Integer versionId) {
        Assert.notNull(nodeId);
        Assert.notNull(versionId);

        ArrFundVersion version = fundVersionRepository.findOne(versionId);
        Map<Integer, TreeNode> treeMap = getVersionTreeCache(version);

        TreeNode node = treeMap.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Ve verzi " + versionId + " nebyl nalezen node s id " + nodeId);
        }

        LinkedHashMap<Integer, TreeNode> parentMap = new LinkedHashMap<>();
        LinkedList<TreeNode> parents = new LinkedList();


        TreeNode parent = node.getParent();
        while (parent != null) {
            parents.addFirst(parent);
            parent = parent.getParent();
        }

        for (TreeNode p : parents) {
            parentMap.put(p.getId(), p);
        }


        TreeNode subtreeNode = treeMap.get(nodeId);
        Map<Integer, Map<String, TitleValues>> valuesMap = createValuesMap(parentMap, version, subtreeNode);
        Map<Integer,TreeNodeClient> resultMap = createNodesWithTitles(parentMap, valuesMap, subtreeNode, version);

        List<TreeNodeClient> result = new ArrayList<>(resultMap.values());

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
        Assert.notNull(version);

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

    @Subscribe
    public void onDataUpdate(final EventChangeMessage changeMessage) {

        List<AbstractEventSimple> events = changeMessage.getEvents();
        for (AbstractEventSimple event : events) {
            logger.info("Zpracování události "+event.getEventType());
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
    public Map<Integer, TreeNodeClient> findParentsWithTitles(final Set<Integer> nodeIds, final ArrFundVersion version) {
        Assert.notNull(nodeIds);
        Assert.notNull(version);

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

        Map<Integer, Map<String, TitleValues>> valuesMap = createValuesMap(parentIdParentMap, version, null);
        Map<Integer, TreeNodeClient> parentIdTreeNodeClientMap = createNodesWithTitles(parentIdParentMap, valuesMap,
                null, version);

        Map<Integer, TreeNodeClient> result = new HashMap<>(nodeIds.size());
        for (Integer nodeId : nodeIdParentMap.keySet()) {
            Integer parentId = nodeIdParentMap.get(nodeId).getId();
            TreeNodeClient parentTreeNodeClient = parentIdTreeNodeClientMap.get(parentId);

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
    private Map<Integer, TreeNodeClient> createNodesWithTitles(final Map<Integer, TreeNode> treeNodeMap,
                                                               @Nullable final Map<Integer, Map<String, TitleValues>> valuesMapParam,
                                                               final TreeNode subtreeRoot,
                                                               final ArrFundVersion version) {
        Assert.notNull(treeNodeMap);
        Assert.notNull(version);

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
                .getViewTitles(version.getRuleSet().getCode(), version.getFund().getFundId());
        Map<Integer, Map<String, TitleValues>> valuesMap = valuesMapParam;
        if (valuesMap == null) {
            valuesMap = createValuesMap(treeNodeMap, version, subtreeRoot);
        }


        Map<Integer, TreeNodeClient> result = new LinkedHashMap<Integer, TreeNodeClient>(treeNodeMap.size());


        String[] rootReferenceMark = new String[0];
        if (subtreeRoot != null) {
            rootReferenceMark = createClientReferenceMarkFromRoot(subtreeRoot,
                    viewTitles.getHierarchyLevelType(), viewTitles, valuesMap);
        }
        String[] parentReferenceMark = rootReferenceMark;
        String levelType = viewTitles.getHierarchyLevelType();

        for (TreeNode treeNode : treeNodeMap.values()) {
            if (subtreeRoot != null && treeNode.getParent() != null) {
                if (treeNode.getParent().equals(subtreeRoot)) {
                    parentReferenceMark = rootReferenceMark;
                } else {
                    parentReferenceMark = result.get(treeNode.getParent().getId()).getReferenceMark();
                }
            }


            TreeNodeClient client = new TreeNodeClient(treeNode.getId(), treeNode.getDepth(),
                    null, !treeNode.getChilds().isEmpty(), treeNode.getReferenceMark(),
                    nodeMap.get(treeNode.getId()).getVersion());
            if (subtreeRoot != null) {
                String[] referenceMark = createClientNodeReferenceMark(treeNode, levelType, viewTitles, valuesMap,
                        parentReferenceMark);
                client.setReferenceMark(referenceMark);
            }

            result.put(treeNode.getId(), client);
        }

        if (result.isEmpty()) {
            return result;
        }


        for (TreeNodeClient treeNodeClient : result.values()) {
            Map<String, TitleValues> descItemCodeToValueMap = valuesMap.get(treeNodeClient.getId());
            fillValues(descItemCodeToValueMap, viewTitles, treeNodeClient);
        }

        return result;
    }

    private Set<RulDescItemType> getDescriptionItemTypes(final ViewTitles viewTitles) {
        Set<String> descItemTypeCodes = getDescItemTypeCodes(viewTitles);

        if (!descItemTypeCodes.isEmpty()) {
            Set<RulDescItemType> descItemTypes = descItemTypeRepository.findByCode(descItemTypeCodes);
            if (descItemTypes.size() != descItemTypeCodes.size()) {
                List<String> foundCodes = descItemTypes.stream().map(RulDescItemType::getCode).collect(Collectors.toList());
                Collection<String> missingCodes = new HashSet<>(descItemTypeCodes);
                missingCodes.removeAll(foundCodes);

                logger.warn("Nepodařilo se nalézt typy atributů s kódy " + StringUtils.join(missingCodes, ", ") + ". Změňte kódy v"
                        + " konfiguraci.");
            }

            return descItemTypes;
        }

        return new HashSet<>();
    }

    private Set<String> getDescItemTypeCodes(final ViewTitles viewTitles) {
        Set<String> descItemTypeCodes = new HashSet<>();

        if (!CollectionUtils.isEmpty(viewTitles.getAccordionLeft())) {
            descItemTypeCodes.addAll(viewTitles.getAccordionLeft());
        }

        if (!CollectionUtils.isEmpty(viewTitles.getAccordionRight())) {
            descItemTypeCodes.addAll(viewTitles.getAccordionRight());
        }
        if (!CollectionUtils.isEmpty(viewTitles.getTreeItem())) {
            descItemTypeCodes.addAll(viewTitles.getTreeItem());
        }
        if (viewTitles.getHierarchy() != null) {
            Set<String> keySet = viewTitles.getHierarchy().keySet();
            if (keySet.size() > 0) {
                descItemTypeCodes.add(keySet.iterator().next());
            }
        }

        if(viewTitles.getHierarchyLevelType() != null){
            descItemTypeCodes.add(viewTitles.getHierarchyLevelType());
        }

        return descItemTypeCodes;
    }

    private String createTitle(final List<String> codes, final Map<String, TitleValues> descItemCodeToValueMap, final boolean useDefaultTitle, final boolean isIconTitle) {
        List<String> titles = new ArrayList<String>();

        if (codes != null) {
            for (String descItemCode : codes) {
                TitleValues titleValues = descItemCodeToValueMap.get(descItemCode);
                if (titleValues != null) {
                    TitleValue titleValue = titleValues.getValues().iterator().next();

                    String value;
                    if (isIconTitle) {
                        value = titleValue.getIconValue();
                    } else {
                        value = titleValue.getValue();
                    }
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

    private void fillValues(final Map<String, TitleValues> descItemCodeToValueMap, final ViewTitles viewTitles,
            final TreeNodeClient treeNodeClient) {
        if (descItemCodeToValueMap != null) {
            treeNodeClient.setAccordionLeft(createTitle(viewTitles.getAccordionLeft(), descItemCodeToValueMap, true, false));
            treeNodeClient.setAccordionRight(createTitle(viewTitles.getAccordionRight(), descItemCodeToValueMap, false, false));
            treeNodeClient.setName(createTitle(viewTitles.getTreeItem(), descItemCodeToValueMap, true, false));

            if (viewTitles.getHierarchy() != null) {
                Set<String> keySet = viewTitles.getHierarchy().keySet();
                if (keySet.size() > 0) {
                    List<String> codes = new ArrayList<String>(1);
                    codes.add(keySet.iterator().next());
                    String iconCode = createTitle(codes, descItemCodeToValueMap, false, true);
                    Collection<Map<String, ConfigView.ConfigViewTitlesHierarchy>> hierarchyList = viewTitles
                            .getHierarchy().values();
                    Map<String, ConfigView.ConfigViewTitlesHierarchy> hierarchyType = hierarchyList.iterator().next();
                    ConfigView.ConfigViewTitlesHierarchy hierarchySpec = hierarchyType.get(iconCode);
                    if (hierarchySpec != null) {
                        treeNodeClient.setIcon(hierarchySpec.getIcon());
                    }
                }
            }
        } else {
            treeNodeClient.setAccordionLeft(defaultNodeTitle);
            treeNodeClient.setName(defaultNodeTitle);
        }
    }

    /**
     * Načte hodnoty atributů podle nastavení pro dané uzly.
     *
     * @param treeNodeMap mapa uzlů
     * @param version     verze ap
     * @param subtreeRoot kořenový uzel, pod kterým chceme spočítat referenční označení (nemusí být kořen stromu)
     * @return hodnoty atributů pro uzly
     */
    private Map<Integer, Map<String, TitleValues>> createValuesMap(final Map<Integer, TreeNode> treeNodeMap,
                                                                  final ArrFundVersion version,
                                                                  final TreeNode subtreeRoot) {

        ViewTitles viewTitles = configView
                .getViewTitles(version.getRuleSet().getCode(), version.getFund().getFundId());
        Set<RulDescItemType> descItemTypes = getDescriptionItemTypes(viewTitles);

        return descriptionItemService.createNodeValuesMap(treeNodeMap.keySet(), subtreeRoot, descItemTypes, version);
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
    public Collection<TreeNodeClient> getFaTreeNodes(final Integer versionId, final List<Integer> nodeIds) {
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

        Collection<TreeNodeClient> nodes = createNodesWithTitles(nodesMap, null, null, version).values();
        addConformityInfo(nodes, version);
        return nodes;
    }



    public List<Integer> sortNodesByTreePosition(final Set<Integer> nodeIds, final ArrFundVersion version) {
        List<TreeNodeClient> nodes = getNodesByIds(nodeIds, version.getFundVersionId());

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

        return nodes.stream().map(TreeNodeClient::getId).collect(Collectors.toList());
    }


    /**
     * Kapacitní mapa. Při překročení kapacity odstraní z mapy nejstarší záznam (první vložený).
     */
    private class CapacityMap<K, V> extends LinkedHashMap<K, V> {

        @Override
        protected boolean removeEldestEntry(final Map.Entry eldest) {
            if (size() > MAX_CACHE_SIZE) {
                logger.info("Překročena kapacita cache. Bude odpojena cache verze s id " + eldest.getKey());
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
                                                   @Nullable final String levelTypeCode,
                                                   final ViewTitles viewTitles,
                                                   final Map<Integer, Map<String, TitleValues>> valuesMap,
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

        if(levelTypeCode != null) {

            Map<String, TitleValues> nodeValues = valuesMap.get(node.getId());
            Map<String, TitleValues> parentValues = valuesMap.get(node.getParent().getId());

            TitleValues nodeTitleValue = nodeValues == null ? null : nodeValues.get(levelTypeCode);
            TitleValues parentTitleValue = parentValues == null ? null : parentValues.get(levelTypeCode);


            String nodeType = nodeTitleValue == null ? null : nodeTitleValue.getValues().iterator().next().getSpecCode();
            String parentType = parentTitleValue == null ? null : parentTitleValue.getValues().iterator().next().getSpecCode();

            if (StringUtils.isNotBlank(nodeType) && StringUtils.isNotBlank(parentType)) {
                ConfigView.ConfigViewTitlesHierarchy levelTitlesHierarchy = viewTitles
                        .getLevelTitlesHierarchy(nodeType);
                if (StringUtils.equalsIgnoreCase(nodeType, parentType)) {
                    separator = levelTitlesHierarchy.getSeparatorOther();
                } else {
                    separator = levelTitlesHierarchy.getSeparatorFirst();
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
                                                       @Nullable final String levelTypeCode,
                                                       final ViewTitles viewTitles,
                                                       final Map<Integer, Map<String, TitleValues>> valuesMap) {

        TreeNode parent = node.getParent();
        if (parent == null) {
            return new String[0];
        }

        String[] parentReferenceMark = createClientReferenceMarkFromRoot(parent, levelTypeCode, viewTitles, valuesMap);

        String[] referenceMark = createClientNodeReferenceMark(node, levelTypeCode, viewTitles, valuesMap,
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
        Assert.notNull(version);

        Map<Integer, TreeNode> versionTreeCache = getVersionTreeCache(version);

        if (nodeId == null) {
            return new HashSet<>(versionTreeCache.keySet());
        }

        Assert.notNull(depth);

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




