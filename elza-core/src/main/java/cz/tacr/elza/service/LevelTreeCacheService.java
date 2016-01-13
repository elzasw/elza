package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.controller.vo.TreeData;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.domain.ArrFindingAidVersion;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.repository.DescItemRepository;
import cz.tacr.elza.repository.DescItemRepositoryCustom;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.FindingAidVersionRepository;
import cz.tacr.elza.repository.LevelRepository;
import cz.tacr.elza.repository.LevelRepositoryCustom;


/**
 * Servistní třída pro načtení a cachování uzlů ve stromu daných verzí.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 11.01.2016
 */
@Service
public class LevelTreeCacheService {

    final Log logger = LogFactory.getLog(this.getClass());

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private FindingAidVersionRepository findingAidVersionRepository;

    @Autowired
    private DescItemRepository descItemRepository;

    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Value("${elza.treenode.title}")
    private String titleDescItemTypeCode = null;


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
            createExpandedTreeNodeMap(treeMap.get(expandedId), expandedNodes);
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

        return new TreeData(createNodesWithTitles(nodesMap, version), expandedIdsExtended);
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
            result.setDepth(parentNode.getDepth() + 1);
            result.setParent(parentNode);
            parentNode.addChild(result);
        } else {
            result.setDepth(1);
        }

        return result;
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

        Map<Integer, DescItemRepositoryCustom.DescItemTitleInfo> nodeTitlesMap = Collections.EMPTY_MAP;

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
            result.add(
                    new TreeNodeClient(treeNode.getId(), treeNode.getDepth(),
                            title == null ? null : title.getValue(), !treeNode.getChilds().isEmpty()));
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
     * @param version verze stromu
     */
    synchronized public void clearVersionCache(final ArrFindingAidVersion version) {
        versionCache.remove(version.getFindingAidVersionId());
    }

}




