package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.controller.vo.NodeItemWithParent;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.core.security.AuthMethod;
import cz.tacr.elza.core.security.AuthParam;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulPolicyType;
import cz.tacr.elza.domain.RulRuleSet;
import cz.tacr.elza.domain.UIVisiblePolicy;
import cz.tacr.elza.domain.UsrPermission;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PolicyTypeRepository;
import cz.tacr.elza.repository.VisiblePolicyRepository;
import cz.tacr.elza.service.eventnotification.EventNotificationService;
import cz.tacr.elza.service.eventnotification.events.EventType;
import cz.tacr.elza.service.eventnotification.events.EventVisiblePolicy;

/**
 * Serviska pro správu oprávnění pro zobrazování chyb.
 *
 * @author Martin Šlapa
 * @since 29.03.2016
 */
@Service
public class PolicyService {

    /**
     * Maximální počet uzlů, které je možné poslat na invalidaci stavu.
     * Při překročení se invalidují zobrazené ve verzi.
     */
    public static final int MAX_SEND_NODE = 100;

    @Autowired
    private PolicyTypeRepository policyTypeRepository;

    @Autowired
    private VisiblePolicyRepository visiblePolicyRepository;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private LevelTreeCacheWalker levelTreeCacheWalker;

    @Autowired
    private EventNotificationService eventNotificationService;

    @Autowired
    private NodeRepository nodeRepository;

    /**
     * Vyhledá JP, které mají ve verzi nastavené visible policy.
     *
     * @param fundVersion   verze fondu
     * @return seznam JP
     */
    public List<NodeItemWithParent> getTreePolicy(final ArrFundVersion fundVersion) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        List<Integer> nodeIds = nodeRepository.findNodeIdsForFondWithPolicy(fundVersion.getFund());
        List<NodeItemWithParent> nodeItemWithParents = levelTreeCacheService.getNodeItemsWithParents(nodeIds, fundVersion);
        return nodeItemWithParents;
    }

    /**
     * Vrací typy oprávnění podle verze fondu.
     *
     * @param fundVersion verze fondu
     * @return seznam typů oprávnění
     */
    public List<RulPolicyType> getPolicyTypes(final ArrFundVersion fundVersion) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        RulRuleSet ruleSet = fundVersion.getRuleSet();
        return policyTypeRepository.findByRuleSet(ruleSet);
    }

    /**
     * Vrací typy oprávnění.
     *
     * @return seznam typů oprávnění
     */
    public List<RulPolicyType> getPolicyTypes() {
        return policyTypeRepository.findAll();
    }

    /**
     * Získání nastavení oprávnění pro uzel.
     *
     * @param nodeId         identifikátor node ke kterému hledám oprávnění
     * @param fundVersion    verze fondu
     * @param includeParents zohlednit zděděné oprávnění od rodičů?
     * @return mapa typů a jejich zobrazení
     */
    public Map<RulPolicyType, Boolean> getVisiblePolicy(final Integer nodeId,
                                                        final ArrFundVersion fundVersion,
                                                        final Boolean includeParents) {
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(includeParents, "Vložení rodičů musí být vyplněné");
        Map<Integer, Map<RulPolicyType, Boolean>> nodeIdsMap =
                getVisiblePolicy(Arrays.asList(nodeId), fundVersion, includeParents);
        return nodeIdsMap.get(nodeId);
    }

    /**
     * Získání nastavení oprávnění pro uzly.
     *
     * @param nodeIds        identifikátor node ke kterému hledám oprávnění
     * @param fundVersion    verze fondu
     * @param includeParents zohlednit zděděné oprávnění od rodičů?
     * @return mapa uzlů map typů a jejich zobrazení
     */
    public Map<Integer, Map<RulPolicyType, Boolean>> getVisiblePolicy(final List<Integer> nodeIds,
                                                                      final ArrFundVersion fundVersion,
                                                                      final Boolean includeParents) {
        Assert.notNull(nodeIds, "Nebyly vyplněny identifikátory JP");
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(includeParents, "Vložení rodičů musí být vyplněné");

        RulRuleSet ruleSet = fundVersion.getRuleSet();
        List<RulPolicyType> policyTypes = policyTypeRepository.findByRuleSet(ruleSet);

        // mapa napočítaných oprávnění předů
        Map<Integer, Map<Integer, Boolean>> parentPolicyIds = new HashMap<>();

        Map<Integer, Map<RulPolicyType, Boolean>> result = new HashMap<>();

        Map<Integer, List<UIVisiblePolicy>> visiblePoliciesByNodes = visiblePolicyRepository.findByNodeIds(nodeIds, policyTypes).stream().collect(Collectors.groupingBy(vp -> vp.getNode().getNodeId()));
        Map<Integer, TreeNode> versionTreeCache = levelTreeCacheService.getVersionTreeCache(fundVersion);

        for (Integer nodeId : nodeIds) {
            Map<Integer, Boolean> parentPolicy = null;

            // chci zohlednit i zděděné oprávnění od předů
            if (includeParents) {
                List<Integer> parentNodeIds = getParentNodeIds(versionTreeCache, nodeId);

                // existují předci?
                if (parentNodeIds.size() > 0) {
                    Integer firstParentNodeId = parentNodeIds.get(0);

                    // hledám napočtené oprávnění
                    parentPolicy = parentPolicyIds.get(firstParentNodeId);

                    // pokud neexistuje, vypočítám ho
                    if (parentPolicy == null) {
                        parentPolicy = new HashMap<>();
                        parentPolicyIds.put(firstParentNodeId, parentPolicy);

                        // načtu oprávnění předků
                        List<UIVisiblePolicy> visiblePolicies = visiblePolicyRepository.findByNodeIds(parentNodeIds, policyTypes);

                        fillVisiblePolicyMap(parentNodeIds, visiblePolicies, parentPolicy);
                    }
                }
            }

            // načtu pro aktuální uzel
            List<UIVisiblePolicy> visiblePolicies = visiblePoliciesByNodes.get(nodeId) == null ? Collections.emptyList() : visiblePoliciesByNodes.get(nodeId);
            Map<Integer, Boolean> policyTypeIdsVisible = new HashMap<>();
            fillVisiblePolicyMap(Collections.singletonList(nodeId), visiblePolicies, policyTypeIdsVisible);

            // pokud existují práva od předka
            if (parentPolicy != null) {
                for (Map.Entry<Integer, Boolean> entry : parentPolicy.entrySet()) {
                    Boolean visible = policyTypeIdsVisible.get(entry.getKey());

                    // vkládám pouze, pokud typ ještě neexistuje
                    if (visible == null) {
                        policyTypeIdsVisible.put(entry.getKey(), entry.getValue());
                    }
                }
            }

            // vytvoří mapu podle typů
            Map<RulPolicyType, Boolean> policyTypeVisible = new HashMap<>();
            for (RulPolicyType policyType : policyTypes) {
                Boolean visible = policyTypeIdsVisible.get(policyType.getPolicyTypeId());

                // pokud neexistuje hodnota u typu, je přidána s true
                if (visible == null) {
                    visible = true;
                }

                policyTypeVisible.put(policyType, visible);
            }

            result.put(nodeId, policyTypeVisible);
        }

        return result;
    }

    /**
     * Najde v cache seznam id rodičů daného uzlu. Seřazeno od prvního id rodiče po kořen stromu.
     *
     * @param nodeId        id nodu pod kterým se má hledat
     * @return  seznam identifikátorů uzlů
     */
    public List<Integer> getParentNodeIds(final Map<Integer, TreeNode> treeMap, final Integer nodeId) {
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");

        TreeNode node = treeMap.get(nodeId);
        if (node == null) {
            throw new SystemException("Nebyl nalezen node s id " + nodeId, ArrangementCode.NODE_NOT_FOUND).set("id", nodeId);
        }

        LinkedList<Integer> parents = new LinkedList<>();

        // procházím prvky přes rodiče až ke kořeni
        TreeNode parent = node.getParent();
        while (parent != null) {
            parents.addFirst(parent.getId());
            parent = parent.getParent();
        }

        return parents;
    }

    /**
     * Provádí průchod a vyplňování typů oprávnění ke kořenu.
     *
     * @param nodeIds              seznam identifikátorů nodů - seřazený od nodu ke kořenu
     * @param visiblePolicies      seznam všech oprávnění
     * @param policyTypeIdsVisible plněný seznam
     */
    private void fillVisiblePolicyMap(final List<Integer> nodeIds,
                                      final List<UIVisiblePolicy> visiblePolicies,
                                      final Map<Integer, Boolean> policyTypeIdsVisible) {
        for (Integer nodeIdTmp : nodeIds) {
            // získání pouze typů k nodu
            List<UIVisiblePolicy> nodeVisiblePolicies = visiblePolicies.stream()
                    .filter(visiblePolicy -> nodeIdTmp.equals(visiblePolicy.getNode().getNodeId())).
                            collect(Collectors.toCollection(LinkedList::new));

            for (UIVisiblePolicy nodeVisiblePolicy : nodeVisiblePolicies) {
                Integer policyTypeId = nodeVisiblePolicy.getPolicyType().getPolicyTypeId();
                Boolean visible = policyTypeIdsVisible.get(policyTypeId);

                // vkládám pouze, pokud typ ještě neexistuje
                if (visible == null) {
                    policyTypeIdsVisible.put(policyTypeId, nodeVisiblePolicy.getVisible());
                }
            }
        }
    }

    /**
     * Získání nastavení oprávnění pro uzly.
     *
     * @param nodeId         identifikátor node ke kterému hledám oprávnění
     * @param fundVersion    verze fondu
     * @param includeParents zohlednit zděděné oprávnění od rodičů?
     * @return mapa uzlů map typů a jejich zobrazení
     */
    public Map<Integer, Boolean> getVisiblePolicyIds(final Integer nodeId,
                                                                   final ArrFundVersion fundVersion,
                                                                   final Boolean includeParents) {
        Map<RulPolicyType, Boolean> visiblePolicy = getVisiblePolicy(nodeId, fundVersion, includeParents);
        Map<Integer, Boolean> visiblePolicyIds = new HashMap<>();
        for (Map.Entry<RulPolicyType, Boolean> entry : visiblePolicy.entrySet()) {
            visiblePolicyIds.put(entry.getKey().getPolicyTypeId(), entry.getValue());
        }
        return visiblePolicyIds;
    }

    /**
     * Získání nastavení oprávnění pro uzly.
     *
     * @param nodeIds        identifikátor node ke kterému hledám oprávnění
     * @param fundVersion    verze fondu
     * @param includeParents zohlednit zděděné oprávnění od rodičů?
     * @return mapa uzlů map typů a jejich zobrazení
     */
    public Map<Integer, Map<Integer, Boolean>> getVisiblePolicyIds(final List<Integer> nodeIds,
                                                                         final ArrFundVersion fundVersion,
                                                                         final Boolean includeParents) {
        Map<Integer, Map<RulPolicyType, Boolean>> visiblePolicy = getVisiblePolicy(nodeIds, fundVersion, includeParents);
        Map<Integer, Map<Integer, Boolean>> visiblePolicyIds = new HashMap<>();
        for (Map.Entry<Integer, Map<RulPolicyType, Boolean>> entry : visiblePolicy.entrySet()) {
            Map<Integer, Boolean> visiblePolicyVisible = new HashMap<>();
            for (Map.Entry<RulPolicyType, Boolean> entryVisible : entry.getValue().entrySet()) {
                visiblePolicyVisible.put(entryVisible.getKey().getPolicyTypeId(), entryVisible.getValue());
            }
            visiblePolicyIds.put(entry.getKey(), visiblePolicyVisible);
        }
        return visiblePolicyIds;
    }

    /**
     * Nastaví/smazaní viditelnost typu oprávnění.
     *
     * @param node             node ke kterému se hodnota vztahuje.
     * @param fundVersion      verze fondu
     * @param policyTypeIdsMap
     * @param includeSubtree   nastavit oprávnění včetně celého postromu
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void setVisiblePolicy(final ArrNode node,
                                 @AuthParam(type = AuthParam.Type.FUND_VERSION) final ArrFundVersion fundVersion,
                                 final Map<Integer, Boolean> policyTypeIdsMap,
                                 final Boolean includeSubtree) {
        Assert.notNull(node, "JP musí být vyplněna");
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(policyTypeIdsMap, "Mapa nesmí být null");
        Assert.notNull(includeSubtree, "Hodnota musí být vyplněna");

        List<RulPolicyType> policyTypes = policyTypeIdsMap.size() == 0 ? new ArrayList<>() : policyTypeRepository.findByIds(policyTypeIdsMap.keySet());

        Assert.isTrue(policyTypes.size() == policyTypeIdsMap.size(), "Neplatný identifikátor typu oprávnění " + policyTypeIdsMap);

        Map<RulPolicyType, Boolean> policyTypeMap = new HashMap<>();
        for (RulPolicyType policyType : policyTypes) {
            policyTypeMap.put(policyType, policyTypeIdsMap.get(policyType.getPolicyTypeId()));
        }

        Map<Integer, TreeNode> versionTreeCache = levelTreeCacheService.getVersionTreeCache(fundVersion);
        TreeNode treeNode = versionTreeCache.get(node.getNodeId());

        if (includeSubtree) {
            LinkedHashSet<Integer> versionIdsTable = levelTreeCacheWalker.walkThroughDFS(treeNode);
            List<UIVisiblePolicy> visiblePolicies = visiblePolicyRepository.findByFund(fundVersion.getFund());

            List<UIVisiblePolicy> deleteVisiblePolicies = visiblePolicies.stream()
                    .filter(visiblePolicy -> versionIdsTable.contains(visiblePolicy.getNode().getNodeId())).
                            collect(Collectors.toCollection(LinkedList::new));

            // smazání všech včetně podstromu
            visiblePolicyRepository.deleteAll(deleteVisiblePolicies);
        } else {
            // smazání všech aktuálně přidaných k node
            visiblePolicyRepository.deleteByNode(node);
        }

        // přidání nového nastavení
        List<UIVisiblePolicy> visiblePolicies = new ArrayList<>(policyTypeMap.size());
        for (Map.Entry<RulPolicyType, Boolean> entry : policyTypeMap.entrySet()) {
            UIVisiblePolicy visiblePolicy = new UIVisiblePolicy();
            visiblePolicy.setPolicyType(entry.getKey());
            visiblePolicy.setNode(node);
            visiblePolicy.setVisible(entry.getValue());
            visiblePolicies.add(visiblePolicy);
        }

        visiblePolicyRepository.saveAll(visiblePolicies);

        List<Integer> parentNodeIds = new ArrayList<>();
        if (treeNode.getParent() != null) {
            parentNodeIds.add(treeNode.getParent().getId());
            addParentNodeIds(treeNode, parentNodeIds);
        }

        if (parentNodeIds.size() > MAX_SEND_NODE || treeNode.getParent() == null) {
            eventNotificationService.publishEvent(new EventVisiblePolicy(EventType.VISIBLE_POLICY_CHANGE,
                    fundVersion.getFundVersionId(),
                    EventVisiblePolicy.InvalidateNodes.ALL));
        } else {
            Integer[] nodeIds = new Integer[parentNodeIds.size()];
            parentNodeIds.toArray(nodeIds);
            eventNotificationService.publishEvent(new EventVisiblePolicy(EventType.VISIBLE_POLICY_CHANGE,
                    fundVersion.getFundVersionId(),
                    EventVisiblePolicy.InvalidateNodes.LIST,
                    nodeIds));
        }
    }

    /**
     * Rekurzivní přidání identifikátorů uzlů, pokud mají nějakého potomka.
     *
     * @param treeNode      procházený uzel
     * @param parentNodeIds seznam přidaných identifikátorů uzlů
     */
    private void addParentNodeIds(final TreeNode treeNode, final List<Integer> parentNodeIds) {
        LinkedList<TreeNode> childs = treeNode.getChilds();
        if (childs != null && childs.size() > 0 && parentNodeIds.size() <= MAX_SEND_NODE) {
            parentNodeIds.add(treeNode.getId());
            for (TreeNode child : childs) {
                addParentNodeIds(child, parentNodeIds);
            }
        }
    }


    /**
     * Smaže všechna nastavení archivního fondu.
     *
     * @param fund archivní fond
     */
    @AuthMethod(permission = {UsrPermission.Permission.FUND_ARR_ALL, UsrPermission.Permission.FUND_ARR})
    public void deleteFundVisiblePolicies(@AuthParam(type = AuthParam.Type.FUND) final ArrFund fund) {
        Assert.notNull(fund, "AS musí být vyplněn");

        List<UIVisiblePolicy> policies = visiblePolicyRepository.findByFund(fund);
        if (!policies.isEmpty()) {
            visiblePolicyRepository.deleteAll(policies);
        }
    }

}
