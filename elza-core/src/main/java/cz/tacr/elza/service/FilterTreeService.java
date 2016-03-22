package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.FilterTools;
import cz.tacr.elza.controller.vo.FilterNode;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.domain.vo.TitleValue;
import cz.tacr.elza.exception.FilterExpiredException;
import cz.tacr.elza.repository.DescItemTypeRepository;
import cz.tacr.elza.repository.NodeRepository;


/**
 * Servistní třída pro filtrování uzlů.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 17.03.2016
 */
@Service
@Configuration
public class FilterTreeService {


    @Autowired
    private LevelTreeCacheService levelTreeCacheService;

    @Autowired
    private LevelTreeCacheWalker levelTreeCacheWalker;


    @Autowired
    private DescItemTypeRepository descItemTypeRepository;

    @Autowired
    private DescriptionItemService descriptionItemService;


    @Autowired
    private NodeRepository nodeRepository;

    /**
     * Provede filtraci uzlů podle filtru a uloží všechny filtrované id do session. ID jsou seřazeny podle výskytu ve
     * stromu.
     *
     * @param version verze stromu
     * @return počet všech záznamů splňujících filtry
     */
    public int filterData(ArrFundVersion version, final Object filter) {

        Map<Integer, TreeNode> versionTreeCache = levelTreeCacheService.getVersionTreeCache(version);
        TreeNode rootNode = versionTreeCache.get(version.getRootNode().getNodeId());

        LinkedHashSet<Integer> versionIdsTable = levelTreeCacheWalker.walkThroughDFS(rootNode);

        FilterTreeSession session = storeFilteredTreeIntersection(version.getFundVersionId(), versionIdsTable,
                versionIdsTable);
        return session.getFilteredIds().size();
    }


    /**
     * Do filtrovaného seznamu načte hodnoty atributů a vrátí podstránku záznamů.
     *
     * @param version         verze
     * @param page            číslo stránky, od 0
     * @param pageSize        velikost stránky
     * @param descItemTypeIds id typů atributů, které chceme načíst
     * @return seznam uzlů s hodnotami atributů
     */
    public List<FilterNode> getFilteredData(final ArrFundVersion version,
                                                              final int page,
                                                              final int pageSize,
                                                              final Set<Integer> descItemTypeIds)
            throws FilterExpiredException {

        Map<String, RulDescItemType> descItemTypeMap = new HashMap<>();
        for (RulDescItemType descItemType : descItemTypeRepository.findAll(descItemTypeIds)) {
            descItemTypeMap.put(descItemType.getCode(), descItemType);
        }

        ArrayList<Integer> filteredIds = getUserFilterSession().getFilteredIds();
        Integer filteredVersionId = getUserFilterSession().getVersionId();
        if (filteredIds == null || filteredVersionId != version.getFundVersionId()) {
            throw new FilterExpiredException();
        }


        ArrayList<Integer> subIds = FilterTools.getSublist(page, pageSize, filteredIds);


        Map<Integer, Map<String, TitleValue>> nodeValuesMap = Collections.EMPTY_MAP;
        if (!subIds.isEmpty() && !descItemTypeIds.isEmpty()) {
            nodeValuesMap = descriptionItemService.createNodeValuesMap(new HashSet<>(subIds), null,
                    new HashSet<>(descItemTypeMap.values()), version);
        }


        return createResult(subIds, levelTreeCacheService.getVersionTreeCache(version), descItemTypeMap, nodeValuesMap);
    }

    /**
     * Vytvoří výslednou mapu.
     *
     * @param filteredIds     id filtrovaných uzlů
     * @param descItemTypeMap mapa typů atributů (kod typu -> typ)
     * @param valuesMap       mapa nalezených hodnot atributů (id uzlu -> kod typu -> hodnota atributu)
     * @return seznam uzlů s hodnotami atributů
     */
    private List<FilterNode> createResult(final List<Integer> filteredIds,
                                          final Map<Integer, TreeNode> versionCache,
                                          final Map<String, RulDescItemType> descItemTypeMap,
                                          final Map<Integer, Map<String, TitleValue>> valuesMap) {

        List<FilterNode> result = new ArrayList<>(filteredIds.size());

        //načtení verzí všech uzlů a jejich rodičů
        Set<Integer> filterWithParentIds = new HashSet<>();
        for (Integer filteredId : filteredIds) {
            TreeNode node = versionCache.get(filteredId);
            TreeNode parentNode = node.getParent();
            filterWithParentIds.add(node.getId());
            if (parentNode != null) {
                filterWithParentIds.add(parentNode.getId());
            }
        }
        Map<Integer, ArrNode> filterWithParentIdsMap = ElzaTools
                .createEntityMap(nodeRepository.findAll(filterWithParentIds), n -> n.getNodeId());


        for (Integer filteredId : filteredIds) {

            //vytvoření mapy hodnot podle typu atributu
            Map<Integer, String> nodeValuesMap = new HashMap<>();
            Map<String, TitleValue> nodeValues = valuesMap.get(filteredId);
            if (nodeValues != null) {
                for (Map.Entry<String, TitleValue> nodeValueEntry : nodeValues.entrySet()) {
                    Integer descItymTypeId = descItemTypeMap.get(nodeValueEntry.getKey()).getDescItemTypeId();
                    String value = nodeValueEntry.getValue().getValue();
                    nodeValuesMap.put(descItymTypeId, value);
                }
            }


            //najdeme objekt uzlu a jeho rodiče, abychom získali jejich verzi
            TreeNode treeNode = versionCache.get(filteredId);
            TreeNode treeParentNode = treeNode.getParent();

            ArrNode arrNode = filterWithParentIdsMap.get(treeNode.getId());
            ArrNodeVO arrNodeVo = new ArrNodeVO(arrNode.getNodeId(), arrNode.getVersion());
            ArrNodeVO arrParentNodeVo = null;
            if(treeParentNode != null)  {
                ArrNode arrParentNode = filterWithParentIdsMap.get(treeParentNode.getId());
                arrParentNodeVo = new ArrNodeVO(arrParentNode.getNodeId(), arrParentNode.getVersion());

            }


            result.add(new FilterNode(arrNodeVo, arrParentNodeVo, nodeValuesMap));
        }

        return result;
    }

    /**
     * Uloží nalezené vyfiltrované id od session.
     *
     * @param versionId id verze stromu
     * @param treeIdsSorted množina všech id uzlů ve stromu, seřazené
     * @param filteredIds   množina id uzlů splňujících filtrovací podmínky
     * @return session uživatele
     */
    private FilterTreeSession storeFilteredTreeIntersection(final Integer versionId,
                                                            final LinkedHashSet<Integer> treeIdsSorted,
                                                            final Set<Integer> filteredIds) {

        treeIdsSorted.retainAll(filteredIds);

        FilterTreeSession session = getUserFilterSession();
        session.setData(versionId, new ArrayList<>(treeIdsSorted));

        return session;
    }


    /**
     * @return vrací session uživatele
     */
    @Bean
    @Scope("session")
    protected FilterTreeSession getUserFilterSession() {
        return new FilterTreeSession();
    }


    /**
     * Session uživatele s filtrovanými id.
     */
    @Component
    private static class FilterTreeSession {

        private Integer versionId;

        /**
         * Filtrované id ve stromu, seřazené.
         */
        private ArrayList<Integer> filteredIds;

        public FilterTreeSession() {
        }

        public ArrayList<Integer> getFilteredIds() {
            return filteredIds;
        }

        public Integer getVersionId() {
            return versionId;
        }

        public void setData(final Integer versionId, final ArrayList<Integer> filteredIds){
            this.versionId = versionId;
            this.filteredIds = filteredIds;
        }
    }

}

