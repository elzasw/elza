package cz.tacr.elza.service;

import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.domain.*;
import cz.tacr.elza.repository.PolicyTypeRepository;
import cz.tacr.elza.repository.VisiblePolicyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Serviska pro správu oprávnění pro zobrazování chyb.
 *
 * @author Martin Šlapa
 * @since 29.03.2016
 */
@Service
public class PolicyService {

    @Autowired
    private PolicyTypeRepository policyTypeRepository;

    @Autowired
    private VisiblePolicyRepository visiblePolicyRepository;

    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private LevelTreeCacheWalker levelTreeCacheWalker;

    /**
     * Vrací typy oprávnění podle verze fondu.
     *
     * @param fundVersion verze fondu
     * @return seznam typů oprávnění
     */
    public List<RulPolicyType> getPolicyTypes(final ArrFundVersion fundVersion) {
        Assert.notNull(fundVersion);
        RulRuleSet ruleSet = fundVersion.getRuleSet();
        return policyTypeRepository.findByRuleSet(ruleSet);
    }

    /**
     * Získání nastavení oprávnění pro uzel.
     *
     * @param node           node ke kterému hledám oprávnění
     * @param fundVersion    verze fondu
     * @param includeParents zohlednit zděděné oprávnění od rodičů?
     * @return mapa typů a jejich zobrazení
     */
    public Map<RulPolicyType, Boolean> getVisiblePolicy(final ArrNode node,
                                                        final ArrFundVersion fundVersion,
                                                        final Boolean includeParents) {
        Assert.notNull(node);
        Assert.notNull(fundVersion);
        Assert.notNull(includeParents);

        RulRuleSet ruleSet = fundVersion.getRuleSet();
        List<RulPolicyType> policyTypes = policyTypeRepository.findByRuleSet(ruleSet);

        List<Integer> nodeIds = new ArrayList<>();
        nodeIds.add(node.getNodeId());

        // chci zohlednit i zděděné oprávnění od rodičů
        if (includeParents) {
            Collection<TreeNodeClient> nodeParents = levelTreeCacheService.getNodeParents(node.getNodeId(), fundVersion.getFundVersionId());
            nodeIds.addAll(nodeParents.stream().map(TreeNodeClient::getId).collect(Collectors.toList()));
        }

        List<UIVisiblePolicy> visiblePolicies = visiblePolicyRepository.findByNodeIds(nodeIds, policyTypes);

        Map<Integer, Boolean> policyTypeIdsVisible = new HashMap<>();
        for (Integer nodeId : nodeIds) {
            // získání pouze typů k nodu
            List<UIVisiblePolicy> nodeVisiblePolicies = visiblePolicies.stream()
                    .filter(visiblePolicy -> nodeId.equals(visiblePolicy.getNode().getNodeId())).
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

        return policyTypeVisible;
    }

    /**
     * Nastaví/smazaní viditelnost typu oprávnění.
     *
     * @param node             node ke kterému se hodnota vztahuje.
     * @param fundVersion      verze fondu
     * @param policyTypeIdsMap
     * @param includeSubtree   nastavit oprávnění včetně celého postromu
     */
    public void setVisiblePolicy(final ArrNode node,
                                 final ArrFundVersion fundVersion,
                                 final Map<Integer, Boolean> policyTypeIdsMap,
                                 final Boolean includeSubtree) {
        Assert.notNull(node);
        Assert.notNull(fundVersion);
        Assert.notNull(policyTypeIdsMap);
        Assert.notNull(includeSubtree);

        List<RulPolicyType> policyTypes = policyTypeRepository.findByIds(policyTypeIdsMap.keySet());

        Assert.isTrue(policyTypes.size() == policyTypeIdsMap.size(), "Neplatný identifikátor typu oprávnění " + policyTypeIdsMap);

        Map<RulPolicyType, Boolean> policyTypeMap = new HashMap<>();
        for (RulPolicyType policyType : policyTypes) {
            policyTypeMap.put(policyType, policyTypeIdsMap.get(policyType.getPolicyTypeId()));
        }

        if (includeSubtree) {
            Map<Integer, TreeNode> versionTreeCache = levelTreeCacheService.getVersionTreeCache(fundVersion);
            TreeNode treeNode = versionTreeCache.get(node.getNodeId());

            LinkedHashSet<Integer> versionIdsTable = levelTreeCacheWalker.walkThroughDFS(treeNode);
            List<UIVisiblePolicy> visiblePolicies = visiblePolicyRepository.findByFundAndPolicyTypes(fundVersion.getFund());

            List<UIVisiblePolicy> deleteVisiblePolicies = visiblePolicies.stream()
                    .filter(visiblePolicy -> versionIdsTable.contains(visiblePolicy.getNode().getNodeId())).
                            collect(Collectors.toCollection(LinkedList::new));

            // smazání všech včetně podstromu
            visiblePolicyRepository.delete(deleteVisiblePolicies);
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

        visiblePolicyRepository.save(visiblePolicies);
    }

}
