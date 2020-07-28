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
import cz.tacr.elza.controller.vo.TreeNodeVO;
import cz.tacr.elza.controller.vo.filter.SearchParam;
import cz.tacr.elza.controller.vo.nodes.ArrNodeVO;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.vo.DescItemValues;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.filter.DescItemTypeFilter;
import cz.tacr.elza.repository.DataRepository;
import cz.tacr.elza.repository.ItemSpecRepository;
import cz.tacr.elza.repository.NodeRepository;
import cz.tacr.elza.service.vo.TitleItemsByType;


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
    private DescriptionItemService descriptionItemService;
    @Autowired
    private NodeRepository nodeRepository;
    @Autowired
    private DataRepository dataRepository;
    @Autowired
    private ItemSpecRepository itemSpecRepository;
    @Autowired
    private ArrangementService arrangementService;
    @Autowired
    private StaticDataService staticDataService;

    /**
     * Provede filtraci uzlů podle filtru a uloží všechny filtrované id do session. ID jsou seřazeny podle výskytu ve
     * stromu.
     *
     * @param version verze stromu
     * @param descItemFilters filtry
     * @param parentNodeId id nodu od kterého se má načítat strom, pokud je null
     * 			načítá se od kořene
     *
     * @return počet všech záznamů splňujících filtry
     */
    public int filterData(final ArrFundVersion version, final  List<DescItemTypeFilter> descItemFilters, final Integer parentNodeId) {
        LinkedHashSet<Integer> versionIdsTable = findNodeIdsByFilter(version, descItemFilters, parentNodeId);
        FilterTreeSession session = storeFilteredTreeIntersection(version.getFundVersionId(), versionIdsTable,
                versionIdsTable);
        return session.getFilteredIds(version.getFundVersionId()).size();
    }

    /**
     * Vyhledání ident. JP podle zadaných filtrů.
     *
     * @param version         verze AS, kterou prohledáváme
     * @param descItemFilters filtry, podle kterých omezujeme výsledky
     * @param parentNodeId    ident. JP, pro omezení podstromu
     * @return nalezené ident. JP
     */
    private LinkedHashSet<Integer> findNodeIdsByFilter(final ArrFundVersion version, final List<DescItemTypeFilter> descItemFilters, final Integer parentNodeId) {
        Map<Integer, TreeNode> versionTreeCache = levelTreeCacheService.getVersionTreeCache(version);
        TreeNode parentNode;
        if (parentNodeId == null) {
            parentNode = versionTreeCache.get(version.getRootNode().getNodeId());
        } else {
            parentNode = versionTreeCache.get(parentNodeId);
        }

        LinkedHashSet<Integer> versionIdsTable = levelTreeCacheWalker.walkThroughDFS(parentNode);

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
        return versionIdsTable;
    }

    /**
     * Do filtrovaného seznamu načte hodnoty atributů a vrátí podstránku záznamů.
     *
     * @param version         verze
     * @param page            číslo stránky, od 0
     * @param pageSize        velikost stránky
     * @param descItemTypeIds id typů atributů, které chceme načíst
     * @param dataExport      příznak zda se načítají data pro export
     * @return seznam uzlů s hodnotami atributů
     */
    public List<FilterNode> getFilteredData(final ArrFundVersion version,
                                            final int page,
                                            final int pageSize,
                                            final List<Integer> descItemTypeIds,
                                            final boolean dataExport) {
        ArrayList<Integer> filteredIds = getUserFilterSession().getFilteredIds(version.getFundVersionId());
        return getFilteredData(version, page, pageSize, descItemTypeIds, dataExport, filteredIds);
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
                                            final Collection<Integer> descItemTypeIds,
                                            final boolean dataExport,
                                            final ArrayList<Integer> filteredIds) {
        List<RulItemType> itemTypes = new ArrayList<>();
        StaticDataProvider data = staticDataService.getData();
        for (Integer id : descItemTypeIds) {
            ItemType rsit = data.getItemTypeById(id);
            if (rsit == null) {
                throw new BusinessException("Uknown desc item type", BaseCode.ID_NOT_EXIST).set("descItemTypeId", id);
            }
            itemTypes.add(rsit.getEntity());
        }

        ArrayList<Integer> subIds = FilterTools.getSublist(page, pageSize, filteredIds);
        Map<Integer, TitleItemsByType> nodeValuesMap = Collections.emptyMap();
        if (!subIds.isEmpty() && !itemTypes.isEmpty()) {
            nodeValuesMap = descriptionItemService.createNodeValuesByItemTypeIdMap(subIds,
                    itemTypes,
                    version.getLockChangeId(),
                    null,
                    dataExport);
        }

        return createResult(version, subIds, levelTreeCacheService.getVersionTreeCache(version), nodeValuesMap);
    }

    /**
     * Ve filtrovaném seznamu najde uzly podle fulltextu. Vrací seřazený seznam uzlů podle jejich indexu v seznamu
     * všech
     * filtrovaných uzlů.
     *
     * @param version  verze stromu
     * @param fulltext fulltext
     * @param luceneQuery v hodnotě fulltext je lucene query (např: +specification:*čís* -fulltextValue:ddd), false - normální fulltext
     * @param searchParams parametry pro rozšířené vyhledávání
     * @return seznam uzlů a jejich indexu v seznamu filtrovaných uzlů, seřazené podle indexu
     */
    public List<FilterNodePosition> getFilteredFulltextIds(final ArrFundVersion version, final String fulltext,
                                                           final boolean luceneQuery, final List<SearchParam> searchParams) {
        Assert.notNull(version, "Verze AS musí být vyplněna");

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
        if (CollectionUtils.isNotEmpty(searchParams)) {
            fulltextIds = arrangementService.findNodeIdsBySearchParams(version, null,
                    searchParams, ArrangementController.Depth.SUBTREE);
        } else if (StringUtils.isBlank(fulltext)) {
            fulltextIds = filteredIds;
        } else if (luceneQuery) {
            fulltextIds = arrangementService
                    .findNodeIdsByLuceneQuery(version, null, fulltext,
                            ArrangementController.Depth.SUBTREE);
        } else {

            fulltextIds = arrangementService
                    .findNodeIdsByFulltext(version, null, fulltext,
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

        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notNull(descItemType, "Typ atributu musí být vyplněn");

        Class<? extends ArrData> dataTypeClass = descriptionItemService.getDescItemDataTypeClass(descItemType);

        Set<RulItemSpec> specs = null;
        boolean withoutSpec = false;
        if (descItemType.getUseSpecification()) {
            Assert.notEmpty(specIds, "Musí být vyplněn alespoň jeden identifikátor specifikace");
            withoutSpec = FilterTools.removeNullValues(specIds);
            specs = new HashSet<>(itemSpecRepository.findAllById(specIds));
        }

        return dataRepository.findUniqueSpecValuesInVersion(version, descItemType, dataTypeClass, specs, withoutSpec,
                fulltext, max);
    }

    /**
     * Získání unikátních specifikací atributů podle typu.
     *
     * @param fundVersion     verze stromu
     * @param itemType        typ atributu
     * @param descItemFilters filtry, podle kterých omezujeme výsledky
     * @param nodeId          ident. JP, pro omezení podstromu
     * @return seznam hodnot
     */
    public List<Integer> findUniqueSpecIds(final ArrFundVersion fundVersion,
                                           final RulItemType itemType,
                                           final List<DescItemTypeFilter> descItemFilters,
                                           final Integer nodeId) {
        Assert.notNull(fundVersion, "Verze AS musí být vyplněna");
        Assert.notNull(itemType, "Typ atributu musí být vyplněn");

        LinkedHashSet<Integer> nodeIdsByFilter = findNodeIdsByFilter(fundVersion, descItemFilters, nodeId);

        return dataRepository.findUniqueSpecIdsInVersion(fundVersion, itemType, new ArrayList<>(nodeIdsByFilter));
    }

    /**
     * Vytvoří výslednou mapu.
     *
     *
     * @param version
     * @param filteredIds     id filtrovaných uzlů
     * @return seznam uzlů s hodnotami atributů
     */
    private List<FilterNode> createResult(final ArrFundVersion version,
                                          final List<Integer> filteredIds,
                                          final Map<Integer, TreeNode> versionCache,
                                          final Map<Integer, TitleItemsByType> nodeValuesMap) {

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
                .createEntityMap(nodeRepository.findAllById(filteredIds), ArrNode::getNodeId);

        Map<Integer, TreeNodeVO> parentIdsMap = ElzaTools.createEntityMap(
                levelTreeCacheService.getNodesByIds(parentIds, version.getFundVersionId()),
                TreeNodeVO::getId
        );

        Map<Integer, TreeNodeVO> idsMap = ElzaTools.createEntityMap(
                levelTreeCacheService.getNodesByIds(filteredIds, version.getFundVersionId()),
                TreeNodeVO::getId
        );

        for (Integer filteredId : filteredIds) {

            //vytvoření mapy hodnot podle typu atributu
            TitleItemsByType titleValuesMap = nodeValuesMap.get(filteredId);

            // prepare descItemValues
            Map<Integer, DescItemValues> descItemValuesMap = null;
            if (titleValuesMap != null) {
                descItemValuesMap = titleValuesMap.toDescItemValues();
            }

            //najdeme objekt uzlu a jeho rodiče, abychom získali jejich verzi
            TreeNode treeNode = versionCache.get(filteredId);
            TreeNode treeParentNode = treeNode.getParent();
            TreeNodeVO treeNodeClient = idsMap.get(filteredId);

            ArrNode arrNode = filterIdsMap.get(treeNode.getId());
            ArrNodeVO arrNodeVo = ArrNodeVO.valueOf(arrNode);
            TreeNodeVO arrParentNodeVo = null;
            if(treeParentNode != null)  {
                arrParentNodeVo = parentIdsMap.get(treeParentNode.getId());
            }

            result.add(new FilterNode(arrNodeVo, arrParentNodeVo, descItemValuesMap, treeNodeClient.getReferenceMark()));
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
     * Vrací novou instanci seznamu filtrovaných id ve stromu.
     * @param versionId
     * @return
     */
    public ArrayList<Integer> getFilteredIds(Integer versionId) {
        return new ArrayList<>(getUserFilterSession().getFilteredIds(versionId));
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

        public ArrayList<Integer> getFilteredIds(final Integer versionId) {
            if (filteredIds == null || !this.versionId.equals(versionId)) {
                throw new BusinessException("Chyba když nejsou nastavené filtry stromu", ArrangementCode.FILTER_EXPIRED);
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

