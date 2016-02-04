package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import cz.tacr.elza.controller.ArrangementController.Depth;
import cz.tacr.elza.controller.ArrangementController.TreeNodeFulltext;
import cz.tacr.elza.controller.config.ClientFactoryVO;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.ArrNodeConformityExt;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemRepositoryCustom;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.LevelRepositoryCustom;
import cz.tacr.elza.service.eventnotification.EventChangeMessage;
import cz.tacr.elza.service.eventnotification.events.AbstractEventSimple;
import cz.tacr.elza.service.eventnotification.events.AbstractEventVersion;
import cz.tacr.elza.service.eventnotification.events.EventAddNode;
import cz.tacr.elza.service.eventnotification.events.EventDeleteNode;
import cz.tacr.elza.service.eventnotification.events.EventIdInVersion;
import cz.tacr.elza.service.eventnotification.events.EventNodeMove;
import cz.tacr.elza.service.eventnotification.events.EventType;


/**
 * Servistní třída pro načtení a cachování uzlů ve stromu daných verzí.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 11.01.2016
 */
@Service
@EventBusListener
public class LevelTreeCacheService {

    final Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private ClientFactoryVO clientFactoryVO;

    @Value("${elza.treenode.title}")
    private String titleDescItemTypeCode = null;

    @Value("${elza.treenode.defaultTitle}")
    private String defaultNodeTitle = "";


    /**
     * Cache stromu pro danou verzi. (id verze -> nodeid uzlu -> uzel)
     */
    private Map<Integer, Map<Integer, TreeNode>> versionCache = new HashMap<>();


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

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);

        Map<Integer, TreeNode> treeMap = getVersionTreeCache(version);
        Set<Integer> expandedIdsExtended = createExpandedIdsExtension(includeIds, treeMap);


        if (nodeId == null) {
            expandedIdsExtended.add(version.getRootLevel().getNode().getNodeId());
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

        TreeData treeData = new TreeData(createNodesWithTitles(nodesMap, version), expandedIdsExtended);

        addConformityInfo(treeData, version);

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

        ArrFindingAidVersion version = findingAidVersionRepository.getOneCheckExist(versionId);

        Map<Integer, TreeNode> versionTreeCache = getVersionTreeCache(version);

        Map<Integer, TreeNode> subMap = new LinkedHashMap<>();
        for (Integer nodeId : nodeIds) {
            TreeNode treeNode = versionTreeCache.get(nodeId);
            if (treeNode != null) {
                subMap.put(treeNode.getId(), treeNode);
            }
        }

        return new ArrayList<>(createNodesWithTitles(subMap, version).values());
    }

    /**
     * Přidá informace o stavu uzlů.
     *
     * @param treeData  data stromu pro otevřené uzly
     * @param version   verze stromu
     */
    private void addConformityInfo(final TreeData treeData, final ArrFindingAidVersion version) {

        ArrayList<Integer> nodeIds = treeData.getNodes().stream().map(TreeNodeClient::getId)
                .collect(Collectors.toCollection(ArrayList::new));

        Map<Integer, ArrNodeConformityExt> conformityInfoForNodes = ruleService
                .getNodeConformityInfoForNodes(nodeIds, version);

        for (TreeNodeClient treeNode: treeData.getNodes()) {
            ArrNodeConformityExt nodeConformity = conformityInfoForNodes.get(treeNode.getId());
            if (nodeConformity != null) {
                treeNode.setNodeConformity(clientFactoryVO.createNodeConformity(nodeConformity));
            }
        }
    }


    /**
     * Najde v cache seznam rodičů daného uzlu. Seřazeno od prvního rodiče po kořen stromu.
     *
     * @param nodeId    nodeid uzlu
     * @param versionId id verze stromu
     * @return seznam rodičů
     */
    public List<TreeNodeClient> getNodeParents(final Integer nodeId, final Integer versionId) {
        Assert.notNull(nodeId);
        Assert.notNull(versionId);

        ArrFindingAidVersion version = findingAidVersionRepository.findOne(versionId);
        Map<Integer, TreeNode> treeMap = getVersionTreeCache(version);

        TreeNode node = treeMap.get(nodeId);
        if (node == null) {
            throw new IllegalArgumentException("Ve verzi " + versionId + " nebyl nalezen node s id " + nodeId);
        }

        LinkedHashMap<Integer, TreeNode> parentMap = new LinkedHashMap<>();

        TreeNode parent = node.getParent();
        while (parent != null) {
            parentMap.put(parent.getId(), parent);
            parent = parent.getParent();
        }

        return createNodesWithTitles(parentMap, version);
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
    private Map<Integer, TreeNode> createVersionTreeCache(final ArrFindingAidVersion version) {
        Assert.notNull(version);

        Integer rootId = version.getRootLevel().getNode().getNodeId();

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
     * Provede načtení popisků uzlů pro uzly, které budou odeslány do klienta a vytvoří výsledné odesílané objekty.
     *
     * @param nodesMap seřazená mapa uzlů tak jak budou odeslány (nodeid -> uzel)
     * @param version  verze stromu
     * @return seznam rozbalených uzlů s potomky seřazen
     */
    private List<TreeNodeClient> createNodesWithTitles(final LinkedHashMap<Integer, TreeNode> nodesMap,
                                                       final ArrFindingAidVersion version) {
        Assert.notNull(nodesMap);
        Assert.notNull(version);

        //node id -> title info
        Map<Integer, DescItemRepositoryCustom.DescItemTitleInfo> nodeTitlesMap = new HashMap<>();

        if (StringUtils.isBlank(titleDescItemTypeCode)) {
            logger.warn("Není nastaven typ atributu, jehož hodnota bude použita pro popisek uzlu."
                    + " Nastavte kód v konfiguraci pro hodnotu 'elza.treenode.title'");
        } else {

            RulDescItemType titleDescItemType = descItemTypeRepository.findOneByCode(titleDescItemTypeCode);
            if (titleDescItemType == null) {
                logger.warn("Nepodařilo se nalézt typ atributu s kódem " + titleDescItemTypeCode + ". Změňte kód v"
                        + " konfiguraci pro hodnotu 'elza.treenode.title'");
            } else {
                nodeTitlesMap = descItemRepository
                        .findDescItemTitleInfoByNodeId(nodesMap.keySet(), titleDescItemType, version.getLockChange());
            }
        }


        List<TreeNodeClient> result = new ArrayList<>(nodesMap.size());
        for (TreeNode treeNode : nodesMap.values()) {
            DescItemRepositoryCustom.DescItemTitleInfo title = nodeTitlesMap.get(treeNode.getId());
            result.add(new TreeNodeClient(treeNode.getId(), treeNode.getDepth(),
                    title == null || title.getValue() == null ? defaultNodeTitle : title.getValue(),
                    !treeNode.getChilds().isEmpty(), treeNode.getReferenceMark(), title.getNodeVersion()));
        }

        return result;
    }


    /**
     * Vytvoří cache stromu dané verze a uloží jej do cache. Druhé volání již vrací nacachovaná data.
     *
     * @param version verze stromu
     * @return mapa všech uzlů stromu (nodeid uzlu -> uzel)
     */
    synchronized private Map<Integer, TreeNode> getVersionTreeCache(final ArrFindingAidVersion version) {
        Map<Integer, TreeNode> versionTreeMap = versionCache.get(version.getFindingAidVersionId());
        if (versionTreeMap == null) {
            versionTreeMap = createVersionTreeCache(version);
            versionCache.put(version.getFindingAidVersionId(), versionTreeMap);
        }
        return versionTreeMap;
    }

    /**
     * Provede smazání cache pro danou verzi stromu. Vynutí načtení cache při dalším volání {@link
     * #getVersionTreeCache(ArrFindingAidVersion)}
     *
     * @param versionId verze stromu
     */
    synchronized public void clearVersionCache(final Integer versionId) {
        versionCache.remove(versionId);
    }


    @Subscribe
    public void onDataUpdate(final EventChangeMessage changeMessage) {

        List<AbstractEventSimple> events = changeMessage.getEvents();
        for (AbstractEventSimple event : events) {
            logger.info("Zpracování události "+event.getEventType());
            //projdeme všechny změny, které jsou změny ve stromu uzlů verze a smažeme cache verzí
            if (AbstractEventVersion.class.isAssignableFrom(event.getClass())) {
                Integer changedVersionId = ((AbstractEventVersion) event).getVersionId();
                ArrFindingAidVersion version = findingAidVersionRepository.findOne(changedVersionId);

                //TODO nemazat celou cache, ale provádět co nejvíc změn přímo na cache
                //TODO aktualizovat referenční označení
//                clearVersionCache(changedVersionId);

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
     * Najde id všech nodů ve verzi.
     *
     * @param version verze stromu
     * @param nodeId id nodu pod kterým se má hledat
     * @param depth hloubka v jaké se mají hledat potomci
     *
     * @return id všech nodů ve verzi
     */
    public Set<Integer> getAllNodeIdsByVersionAndParent(final ArrFindingAidVersion version, final Integer nodeId, Depth depth) {
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

    /**
     * Najde rodiče pro předané id nodů. Vrátí seznam objektů ve kterém je id nodu a jeho rodič.
     *
     * @param nodeIds id nodů
     * @param version verze AP
     *
     * @return seznam id nodů a jejich rodičů
     */
    public List<TreeNodeFulltext> findParentsForNodes(Set<Integer> nodeIds, ArrFindingAidVersion version) {
        Assert.notNull(nodeIds);
        Assert.notNull(version);

        Map<Integer, TreeNode> versionTreeCache = getVersionTreeCache(version);
        Map<Integer, TreeNode> nodeIdParentMap = new HashMap<>(nodeIds.size());
        Map<Integer, TreeNode> parentIdParentMap = new HashMap<>(nodeIds.size());
        List<TreeNodeFulltext> result =  new ArrayList<>(nodeIds.size());

        for (Integer nodeId : nodeIds) {
            TreeNode treeNode = versionTreeCache.get(nodeId);
            TreeNode parent = treeNode.getParent();
            if (parent == null) {
                TreeNodeFulltext treeNodeFulltext = new TreeNodeFulltext();
                treeNodeFulltext.setNodeId(nodeId);
                result.add(treeNodeFulltext);
            } else {
                parentIdParentMap.put(parent.getId(), parent);
                nodeIdParentMap.put(nodeId, parent);
            }
        }

        Map<Integer, TreeNodeClient> parentIdTreeNodeClientMap = createNodesWithTitles(parentIdParentMap, version);

        for (Integer nodeId : nodeIdParentMap.keySet()) {
            Integer parentId = nodeIdParentMap.get(nodeId).getId();
            TreeNodeClient parentTreeNodeClient = parentIdTreeNodeClientMap.get(parentId);

            TreeNodeFulltext treeNodeFulltext = new TreeNodeFulltext();
            treeNodeFulltext.setNodeId(nodeId);
            treeNodeFulltext.setParent(parentTreeNodeClient);
            result.add(treeNodeFulltext);
        }

        return result;
    }

    private Map<Integer, TreeNodeClient> createNodesWithTitles(Map<Integer, TreeNode> treeNodeMap, ArrFindingAidVersion version) {
        Assert.notNull(treeNodeMap);
        Assert.notNull(version);

        //node id -> title info
        Map<Integer, DescItemRepositoryCustom.DescItemTitleInfo> nodeTitlesMap = new HashMap<>();

        if (StringUtils.isBlank(titleDescItemTypeCode)) {
            logger.warn("Není nastaven typ atributu, jehož hodnota bude použita pro popisek uzlu."
                    + " Nastavte kód v konfiguraci pro hodnotu 'elza.treenode.title'");
        } else {

            RulDescItemType titleDescItemType = descItemTypeRepository.findOneByCode(titleDescItemTypeCode);
            if (titleDescItemType == null) {
                logger.warn("Nepodařilo se nalézt typ atributu s kódem " + titleDescItemTypeCode + ". Změňte kód v"
                        + " konfiguraci pro hodnotu 'elza.treenode.title'");
            } else {
                nodeTitlesMap = descItemRepository
                        .findDescItemTitleInfoByNodeId(treeNodeMap.keySet(), titleDescItemType, version.getLockChange());
            }
        }


        Map<Integer, TreeNodeClient> result = new LinkedHashMap<>(treeNodeMap.size());
        for (TreeNode treeNode : treeNodeMap.values()) {
            DescItemRepositoryCustom.DescItemTitleInfo title = nodeTitlesMap.get(treeNode.getId());
            result.put(treeNode.getId(), new TreeNodeClient(treeNode.getId(), treeNode.getDepth(),
                    title == null || title.getValue() == null ? defaultNodeTitle : title.getValue(),
                    !treeNode.getChilds().isEmpty(), treeNode.getReferenceMark(), title.getNodeVersion()));
        }

        return result;
    }



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////CACHE UPDATE///////////////////////////////////////////////////////

    /**
     * Aktualizace cache po smazání uzlu.
     * @param nodeId
     * @param version
     */
    synchronized private void actionDeleteLevel(final Integer nodeId, final ArrFindingAidVersion version) {
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
                                             final ArrFindingAidVersion version,
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
                                              final ArrFindingAidVersion version,
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
            TreeNode transportParent = transportNodes.get(0);
            repositionList(transportParent.getChilds());

            boolean siblingMove = false;
            if (!transportParent.equals(staticParent) && !transportParent.getChilds().isEmpty()) {
                //při přesunu přečíslujeme všechny node, které jsou pod rodičem, jehož děti přesouváme
                initReferenceMarkLower(transportParent.getChilds(), transportParent.getChilds().getFirst());
                siblingMove = true;
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
    private void repositionList(final Collection<TreeNode> childs){
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
        for (TreeNode child : childs) {
            initReference = initReference || child.equals(firstReposition);
            if (initReference) {
                initReferenceMarkAndDepth(child, firstReposition.getPosition());
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

}




