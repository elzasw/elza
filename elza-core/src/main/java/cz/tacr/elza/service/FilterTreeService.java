package cz.tacr.elza.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import cz.tacr.elza.domain.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import cz.tacr.elza.ElzaTools;
import cz.tacr.elza.FilterTools;
import cz.tacr.elza.controller.ArrangementController;
import cz.tacr.elza.controller.vo.FilterNode;
import cz.tacr.elza.controller.vo.FilterNodePosition;
import cz.tacr.elza.controller.vo.TreeNode;
import cz.tacr.elza.controller.vo.TreeNodeClient;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.vo.DescItemValues;
import cz.tacr.elza.domain.vo.TitleValue;
import cz.tacr.elza.domain.vo.TitleValues;
import cz.tacr.elza.exception.FilterExpiredException;
import cz.tacr.elza.filter.DescItemTypeFilter;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.ItemTypeRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.repository.PacketTypeRepository;


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
    private ItemTypeRepository itemTypeRepository;
    @Autowired
    private DescriptionItemService descriptionItemService;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private DataRepository dataRepository;
    @Autowired
    private PacketTypeRepository packetTypeRepository;
    @Autowired
    private ItemSpecRepository itemSpecRepository;
    @Autowired
    private ArrangementService arrangementService;

    /**
     * Provede filtraci uzlů podle filtru a uloží všechny filtrované id do session. ID jsou seřazeny podle výskytu ve
     * stromu.
     *
     * @param version verze stromu
     * @param descItemFilters filtry
     *
     * @return počet všech záznamů splňujících filtry
     */
    public int filterData(final ArrFundVersion version, final  List<DescItemTypeFilter> descItemFilters) {
        Map<Integer, TreeNode> versionTreeCache = levelTreeCacheService.getVersionTreeCache(version);
        TreeNode rootNode = versionTreeCache.get(version.getRootNode().getNodeId());

        LinkedHashSet<Integer> versionIdsTable = levelTreeCacheWalker.walkThroughDFS(rootNode);

        if (CollectionUtils.isNotEmpty(descItemFilters)) {
            Set<Integer> nodeIdsByFilters = nodeRepository.findNodeIdsByFilters(version, descItemFilters);

            Iterator<Integer> iterator = versionIdsTable.iterator();
            while (iterator.hasNext()) {
                Integer nodeId = iterator.next();

                if (!nodeIdsByFilters.contains(nodeId)) {
                    iterator.remove();
                }
            }
        }

        FilterTreeSession session = storeFilteredTreeIntersection(version.getFundVersionId(), versionIdsTable,
                versionIdsTable);
        return session.getFilteredIds(version.getFundVersionId()).size();
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

        Map<String, RulItemType> descItemTypeMap = new HashMap<>();
        for (RulItemType descItemType : itemTypeRepository.findAll(descItemTypeIds)) {
            descItemTypeMap.put(descItemType.getCode(), descItemType);
        }

        ArrayList<Integer> filteredIds = getUserFilterSession().getFilteredIds(version.getFundVersionId());
        ArrayList<Integer> subIds = FilterTools.getSublist(page, pageSize, filteredIds);


        Map<Integer, Map<String, TitleValues>> nodeValuesMap = Collections.EMPTY_MAP;
        if (!subIds.isEmpty() && !descItemTypeIds.isEmpty()) {
            nodeValuesMap = descriptionItemService.createNodeValuesMap(new HashSet<>(subIds), null,
                    new HashSet<>(descItemTypeMap.values()), version);
        }


        return createResult(version, subIds, levelTreeCacheService.getVersionTreeCache(version), descItemTypeMap, nodeValuesMap);
    }

    /**
     * Ve filtrovaném seznamu najde uzly podle fulltextu. Vrací seřazený seznam uzlů podle jejich indexu v seznamu
     * všech
     * filtrovaných uzlů.
     *
     * @param version  verze stromu
     * @param fulltext fulltext
     * @param luceneQuery v hodnotě fulltext je lucene query (např: +specification:*čís* -fulltextValue:ddd), false - normální fulltext
     * @return seznam uzlů a jejich indexu v seznamu filtrovaných uzlů, seřazené podle indexu
     * @throws FilterExpiredException není nastaven filtr, nejprve zavolat {@link #filterData(ArrFundVersion, Object)}
     */
    public List<FilterNodePosition> getFilteredFulltextIds(final ArrFundVersion version, final String fulltext,
                                                           final boolean luceneQuery)
            throws FilterExpiredException {
        Assert.notNull(version);

        TreeSet<FilterNodePosition> result = new TreeSet<>((a, b) -> a.getIndex().compareTo(b.getIndex()));

        ArrayList<Integer> filteredIds = getUserFilterSession().getFilteredIds(version.getFundVersionId());

        //filtrované id vložíme do mapy s jejich pozicí (id uzlu -> pozice v seznamu) pro rychlejší procházení
        Map<Integer, Integer> filteredPositionMap = new HashMap<>();
        int index = 0;
        for (Integer filteredId : filteredIds) {
            filteredPositionMap.put(filteredId, index++);
        }

        //seznam id nalezených fulltextem
        Collection<Integer> fulltextIds;
        if (StringUtils.isBlank(fulltext)) {
            fulltextIds = filteredIds;
        } else if (luceneQuery) {
            fulltextIds = arrangementService
                    .findNodeIdsByLuceneQuery(version, version.getRootNode().getNodeId(), fulltext,
                            ArrangementController.Depth.SUBTREE);
        } else {

            fulltextIds = arrangementService
                    .findNodeIdsByFulltext(version, version.getRootNode().getNodeId(), fulltext,
                            ArrangementController.Depth.SUBTREE);
        }


        for (Integer fulltextId : fulltextIds) {
            Integer position = filteredPositionMap.get(fulltextId);
            if (position != null) {
                result.add(new FilterNodePosition(fulltextId, position));
            }
        }

        return new ArrayList<>(result);
    }


    public List<String> filterUniqueValues(final ArrFundVersion version,
                                           final RulItemType descItemType,
                                           @Nullable final Set<Integer> specIds,
                                           @Nullable final String fulltext,
                                           final int max) {

        Assert.notNull(version);
        Assert.notNull(descItemType);

        Class<? extends ArrData> dataTypeClass = descriptionItemService.getDescItemDataTypeClass(descItemType);
        if (dataTypeClass.equals(ArrDataPacketRef.class)) {
            Assert.notEmpty(specIds);
            boolean withoutType = FilterTools.removeNullValues(specIds);
            Set<RulPacketType> packetTypes = new HashSet<>(packetTypeRepository.findAll(specIds));
            return dataRepository.findUniquePacketValuesInVersion(version, descItemType, dataTypeClass, packetTypes,
                    withoutType, fulltext, max);
        } else {
            Set<RulItemSpec> specs = null;
            boolean withoutSpec = false;
            if (descItemType.getUseSpecification()) {
                Assert.notEmpty(specIds);
                withoutSpec = FilterTools.removeNullValues(specIds);
                specs = new HashSet<>(itemSpecRepository.findAll(specIds));
            }

            return dataRepository.findUniqueSpecValuesInVersion(version, descItemType, dataTypeClass, specs, withoutSpec,
                    fulltext, max);
        }
    }


    /**
     * Vytvoří výslednou mapu.
     *
     *
     * @param version
     * @param filteredIds     id filtrovaných uzlů
     * @param descItemTypeMap mapa typů atributů (kod typu -> typ)
     * @param valuesMap       mapa nalezených hodnot atributů (id uzlu -> kod typu -> hodnota atributu)
     * @return seznam uzlů s hodnotami atributů
     */
    private List<FilterNode> createResult(final ArrFundVersion version, final List<Integer> filteredIds,
                                          final Map<Integer, TreeNode> versionCache,
                                          final Map<String, RulItemType> descItemTypeMap,
                                          final Map<Integer, Map<String, TitleValues>> valuesMap) {

        List<FilterNode> result = new ArrayList<>(filteredIds.size());

        //načtení verzí všech uzlů
        Set<Integer> parentIds = new HashSet<>();
        for (Integer filteredId : filteredIds) {
            TreeNode node = versionCache.get(filteredId);
            TreeNode parentNode = node.getParent();
            if (parentNode != null) {
                parentIds.add(parentNode.getId());
            }
        }
        Map<Integer, ArrNode> filterIdsMap = ElzaTools
                .createEntityMap(nodeRepository.findAll(filteredIds), n -> n.getNodeId());

        Map<Integer, TreeNodeClient> parentIdsMap = ElzaTools.createEntityMap(
                levelTreeCacheService.getNodesByIds(parentIds, version.getFundVersionId()),
                n -> n.getId()
        );

        for (Integer filteredId : filteredIds) {

            //vytvoření mapy hodnot podle typu atributu
            Map<Integer, DescItemValues> nodeValuesMap = new HashMap<>();
            Map<String, TitleValues> nodeValues = valuesMap.get(filteredId);
            if (nodeValues != null) {
                for (Map.Entry<String, TitleValues> nodeValueEntry : nodeValues.entrySet()) {
                    DescItemValues values = new DescItemValues();
                    Integer descItymTypeId = descItemTypeMap.get(nodeValueEntry.getKey()).getItemTypeId();


                    Iterator<TitleValue> valueIterator = nodeValueEntry.getValue().getValues().iterator();
                    while (valueIterator.hasNext()) {
                        TitleValue titleValue = valueIterator.next();

                        //values.addValue(DescItemValue.create(titleValue));
                        values.addValue(titleValue);
                        nodeValuesMap.put(descItymTypeId, values);
                    }
                }
            }


            //najdeme objekt uzlu a jeho rodiče, abychom získali jejich verzi
            TreeNode treeNode = versionCache.get(filteredId);
            TreeNode treeParentNode = treeNode.getParent();

            ArrNode arrNode = filterIdsMap.get(treeNode.getId());
            ArrNodeVO arrNodeVo = new ArrNodeVO(arrNode.getNodeId(), arrNode.getVersion());
            TreeNodeClient arrParentNodeVo = null;
            if(treeParentNode != null)  {
                arrParentNodeVo = parentIdsMap.get(treeParentNode.getId());
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

        public ArrayList<Integer> getFilteredIds(final Integer versionId) throws FilterExpiredException {
            if (filteredIds == null || !this.versionId.equals(versionId)) {
                throw new FilterExpiredException();
            }

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

