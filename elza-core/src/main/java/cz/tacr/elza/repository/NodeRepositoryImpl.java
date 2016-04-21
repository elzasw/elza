package cz.tacr.elza.repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queries.ChainedFilter;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.filter.DescItemTypeFilter;
import cz.tacr.elza.filter.condition.DescItemCondition;
import cz.tacr.elza.utils.NodeUtils;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.11.2015
 */
@Component
public class NodeRepositoryImpl implements NodeRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LevelRepository levelRepository;

    private FullTextEntityManager fullTextEntityManager;

    @Override
    public List<ArrNode> findNodesByDirection(final ArrNode node,
                                              final ArrFundVersion version,
                                              final RelatedNodeDirection direction) {
        Assert.notNull(node);
        Assert.notNull(version);
        Assert.notNull(direction);


        ArrLevel level = levelRepository.findNodeInRootTreeByNodeId(node, version.getRootNode(),
                version.getLockChange());
        List<ArrLevel> levels = levelRepository.findLevelsByDirection(level, version, direction);

        return NodeUtils.createNodeList(levels);
    }

    /**
     * Vrátí id nodů které mají danou hodnotu v dané verzi.
     *
     * @param text The query text.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Set<Integer> findByFulltextAndVersionLockChangeId(String text, Integer fundId, Integer lockChangeId) {
        Assert.notNull(fundId);

        List<String> descItemIds = findDescItemIdsByData(text, fundId);
        if (descItemIds.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        String descItemIdsString = StringUtils.join(descItemIds, " ");
        List<Integer> nodeIds = findNodeIdsByValidDescItems(lockChangeId, descItemIdsString);

        return new HashSet<>(nodeIds);
    }

    /**
     * @return vrací seznam uzlů, které nemají žádnou vazbu na conformity info
     */
    @Override
    public List<ArrNode> findByNodeConformityIsNull() {
        String hql = "SELECT n FROM arr_node n WHERE n.nodeId NOT IN (SELECT nc.node.nodeId FROM arr_node_conformity nc) AND n.nodeId IN (SELECT l.node.nodeId FROM arr_level l WHERE l.deleteChange IS NULL)";

        javax.persistence.Query query = entityManager.createQuery(hql);

        List resultList = query.getResultList();

        return resultList;
    }

    /**
     * Vyhledá id atributů podle předané hodnoty. Hledá napříč archivními pomůckami a jejich verzemi.
     *
     * @param text hodnota podle které se hledá
     * @param fundId id fondu
     *
     * @return id atributů které mají danou hodnotu
     */
    @SuppressWarnings("unchecked")
    private List<String> findDescItemIdsByData(String text, Integer fundId) {
        if (StringUtils.isBlank(text)) {
            return Collections.EMPTY_LIST;
        }

        Class<ArrData> entityClass = ArrData.class;
        QueryBuilder queryBuilder = createQueryBuilder(entityClass);
        QueryBuilder recordQueryBuilder = createQueryBuilder(ArrDataRecordRef.class);

        Query textQuery = createTextQuery(text, queryBuilder);
        Query fundIdQuery = queryBuilder.keyword().onField("fundId").matching(fundId).createQuery();
        Query query = queryBuilder.bool().must(textQuery).must(fundIdQuery).createQuery();

        List<String> result = (List<String>) createFullTextQuery(query, entityClass).setProjection("descItemId").getResultList().stream().map(row -> {
            return ((Object[]) row)[0];
        }).collect(Collectors.toList());

        return result;
    }

    /**
     * Vytvoří lucene dotaz na hledání arr_data podle hodnoty.
     *
     * @param text hodnota
     * @param queryBuilder query builder
     *
     * @param dotaz
     */
    private Query createTextQuery(String text, QueryBuilder queryBuilder) {
        // rozdělení zadaného výrazu podle mezer a hledání výsledků pomocí OR tak že každý obsahuje alespoň jednu část zadaného výrazu
        String[] tokens = StringUtils.split(text.toLowerCase(), ' ');

        BooleanJunction<BooleanJunction> textConditions = queryBuilder.bool();
        for (String token : tokens) {
            String searchValue = "*" + token + "*";
            Query createQuery = queryBuilder.keyword().wildcard().onField(DescItemCondition.FULLTEXT_ATT).matching(searchValue).createQuery();
            textConditions.should(createQuery);
        }

        return textConditions.createQuery();
    }

    /**
     * Vytvoří hibernate jpa query z lucene query.
     *
     * @param query lucene qery
     * @param entityClass třída pro kterou je dotaz
     *
     * @return hibernate jpa query
     */
    private FullTextQuery createFullTextQuery(Query query, Class<?> entityClass) {
        FullTextQuery jpaQuery = fullTextEntityManager.createFullTextQuery(query, entityClass);
        return jpaQuery;
    }

    /**
     * Vytvoří query builder pro danou třídu.
     *
     * @param entityClasstřída
     *
     * @return query builder
     */
    private QueryBuilder createQueryBuilder(Class<?> entityClass) {
        return fullTextEntityManager.getSearchFactory().buildQueryBuilder().forEntity(entityClass).get();
    }

    /**
     * Vyhledá id nodů podle platných atributů. Hledá napříč archivními pomůckami.
     *
     * @param lockChangeId id změny uzavření verze archivní pomůcky, může být null
     * @param descItemIdsString id atributů pro které se mají hledat nody
     *
     * @return id nodů které mají před danou změnou nějaký atribut
     */
    @SuppressWarnings("unchecked")
    private List<Integer> findNodeIdsByValidDescItems(Integer lockChangeId, String descItemIdsString) {
        Class<ArrDescItem> entityClass = ArrDescItem.class;

        Filter changeFilter = createChangeFilter(lockChangeId);

        QueryBuilder queryBuilder = createQueryBuilder(entityClass);
        Query descItemIdsQuery = queryBuilder.keyword().onField("descItemIdString").matching(descItemIdsString).createQuery();
        Query validDescItemInVersionQuery = queryBuilder.all().filteredBy(changeFilter).createQuery();
        Query query = queryBuilder.bool().must(descItemIdsQuery).must(validDescItemInVersionQuery).createQuery();

        List<Integer> result = createFullTextQuery(query, entityClass).setProjection("nodeId").getResultList().stream().mapToInt(row -> {
            return (int) ((Object[]) row)[0];
        }).boxed().collect(Collectors.toList());

        return result;
    }

    /**
     * Vytvoří filtr pro hledání podle aktuální nebo uzavžené verze.
     *
     * @param lockChangeId id verze, může být null
     *
     * @return filtr
     */
    private Filter createChangeFilter(Integer lockChangeId) {
        if (lockChangeId == null) { // deleteChange is null
            return NumericRangeFilter.newIntRange("deleteChangeId", Integer.MAX_VALUE, Integer.MAX_VALUE, true, true);
        }

        // createChangeId < lockChangeId
        NumericRangeFilter<Integer> createChangeFilter = NumericRangeFilter.newIntRange("createChangeId", null,
                lockChangeId, false, false);
        // and (deleteChange is null or deleteChange < lockChangeId)
        NumericRangeFilter<Integer> nullDeleteChangeFilter = NumericRangeFilter.newIntRange("deleteChangeId", Integer.MAX_VALUE,
                Integer.MAX_VALUE, true, true);
        NumericRangeFilter<Integer> deleteChangeFilter = NumericRangeFilter.newIntRange("deleteChangeId", null,
                lockChangeId, false, false);

        Filter[] deleteChangeFilters = {nullDeleteChangeFilter, deleteChangeFilter};
        ChainedFilter orFilter = new ChainedFilter(deleteChangeFilters, ChainedFilter.OR);

        Filter[] andFilters = {createChangeFilter, orFilter};
        return new ChainedFilter(andFilters, ChainedFilter.AND);
    }

    @PostConstruct
    private void buildFullTextEntityManager() {
        fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
    }

    @Override
    public Set<Integer> findNodeIdsByFilters(ArrFundVersion version, List<DescItemTypeFilter> filters) {
        Assert.notNull(version);

        List<String> descItemIds = findDescItemIdsByFilters(filters);
        if (descItemIds.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        Integer lockChangeId = version.getLockChange() == null ? null : version.getLockChange().getChangeId();
        String descItemIdsString = StringUtils.join(descItemIds, " ");
        List<Integer> nodeIds = findNodeIdsByValidDescItems(lockChangeId, descItemIdsString);

        return new HashSet<>(nodeIds);
    }

    private List<String> findDescItemIdsByFilters(List<DescItemTypeFilter> filters) {
        if (CollectionUtils.isEmpty(filters)) {
            return Collections.EMPTY_LIST;
        }

        Map<Integer, List<String>> allDescItemIds = null;
        for (DescItemTypeFilter filter : filters) {
            QueryBuilder queryBuilder = createQueryBuilder(filter.getCls());
            Query query = filter.createLuceneQuery(queryBuilder);
            List<Object> rows = createFullTextQuery(query, ArrData.class).setProjection("nodeId", "descItemId").getResultList();
            Map<Integer, List<String>> nodeIdToDescItemIds = new HashMap<>(rows.size());
            for (Object row: rows) {
                Object[] rowArray = (Object[]) row;
                Integer nodeId = (Integer) rowArray[0];
                String descItemId = (String) rowArray[1];
                List<String> descItemIds = nodeIdToDescItemIds.get(nodeId);
                if (descItemIds == null) {
                    descItemIds = new LinkedList<>();
                    nodeIdToDescItemIds.put(nodeId, descItemIds);
                }
                descItemIds.add(descItemId);
            }

            if (allDescItemIds == null) {
                allDescItemIds = new HashMap<>(nodeIdToDescItemIds);
            } else {
                Set<Integer> existingNodes = new HashSet<>(allDescItemIds.keySet());
                existingNodes.retainAll(nodeIdToDescItemIds.keySet());

                Map<Integer, List<String>> updatedAllDescItemIds = new HashMap<>(nodeIdToDescItemIds.size());
                for (Integer nodeId : existingNodes) {
                    List<String> rowDescItemIds = nodeIdToDescItemIds.get(nodeId);
                    List<String> existingDescItemIds = allDescItemIds.get(nodeId);

                    if (existingDescItemIds == null) {
                        updatedAllDescItemIds.put(nodeId, rowDescItemIds);
                    } else if (rowDescItemIds == null) {
                        updatedAllDescItemIds.put(nodeId, existingDescItemIds);
                    } else {
                        existingDescItemIds.addAll(rowDescItemIds);
                        updatedAllDescItemIds.put(nodeId, existingDescItemIds);
                    }
                }
                allDescItemIds = updatedAllDescItemIds;
            }
        }

        List<String> result = new LinkedList<>();
        for (List<String> descItemIds : allDescItemIds.values()) {
            result.addAll(descItemIds);
        }
        return result;
    }

}
