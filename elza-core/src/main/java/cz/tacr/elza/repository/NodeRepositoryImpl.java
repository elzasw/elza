package cz.tacr.elza.repository;

import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.NumericConfig;
import org.apache.lucene.search.BooleanQuery;
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
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.exception.InvalidQueryException;
import cz.tacr.elza.filter.DescItemTypeFilter;
import cz.tacr.elza.filter.condition.LuceneDescItemCondition;
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
    public Set<Integer> findByFulltextAndVersionLockChangeId(final String text, final Integer fundId, final Integer lockChangeId) {
        Assert.notNull(fundId);

        List<String> descItemIds = findDescItemIdsByData(text, fundId);
        if (descItemIds.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        String descItemIdsString = StringUtils.join(descItemIds, " ");
        List<Integer> nodeIds = findNodeIdsByValidDescItems(lockChangeId, descItemIdsString);

        return new HashSet<>(nodeIds);
    }


    @SuppressWarnings("unchecked")
    @Override
    public Set<Integer> findByLuceneQueryAndVersionLockChangeId(final String queryText, final Integer fundId, final Integer lockChangeId)
            throws InvalidQueryException {
        Assert.notNull(fundId);

        List<String> descItemIds = findDescItemIdsByLuceneQuery(queryText, fundId);
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
    private List<String> findDescItemIdsByData(final String text, final Integer fundId) {
        if (StringUtils.isBlank(text)) {
            return Collections.EMPTY_LIST;
        }

        Class<ArrData> entityClass = ArrData.class;
        QueryBuilder queryBuilder = createQueryBuilder(entityClass);

        Query textQuery = createTextQuery(text, queryBuilder);
        Query fundIdQuery = queryBuilder.keyword().onField("fundId").matching(fundId).createQuery();
        Query query = queryBuilder.bool().must(textQuery).must(fundIdQuery).createQuery();

        List<String> result = (List<String>) createFullTextQuery(query, entityClass).setProjection("itemId").getResultList().stream().map(row -> {
            return ((Object[]) row)[0];
        }).collect(Collectors.toList());

        return result;
    }

    /**
     * Vyhledá id atributů podle předané hodnoty. Hledá napříč archivními pomůckami a jejich verzemi.
     * Vyhledávání probíhá podle lucene dotazu.
     *
     * @param queryText lucene dotaz (např: +specification:*čís* -fulltextValue:ddd)
     * @param fundId    id fondu
     * @return id atributů které mají danou hodnotu
     * @throws InvalidQueryException neplatný lucene dotaz
     */
    private List<String> findDescItemIdsByLuceneQuery(final String queryText, final Integer fundId)
            throws InvalidQueryException{
        if (StringUtils.isBlank(queryText)) {
            return Collections.EMPTY_LIST;
        }

        Class<ArrData> entityClass = ArrData.class;
        QueryBuilder queryBuilder = createQueryBuilder(entityClass);


        StandardQueryParser parser = new StandardQueryParser();
        parser.setAllowLeadingWildcard(true);
        HashMap<String, NumericConfig> stringNumericConfigHashMap = new HashMap<>();
        stringNumericConfigHashMap.put("specification", new NumericConfig(1, NumberFormat.getIntegerInstance(), FieldType.NumericType.INT));
        parser.setNumericConfigMap(stringNumericConfigHashMap);
        Query query;
        try {
            Query textQuery = parser.parse(queryText, "fulltextValue");
            Query fundIdQuery = queryBuilder.keyword().onField("fundId").matching(fundId).createQuery();
            query = queryBuilder.bool().must(textQuery).must(fundIdQuery).createQuery();

        } catch (QueryNodeException e) {
            throw new InvalidQueryException(e);
        }

        List<String> result = (List<String>) createFullTextQuery(query, entityClass).setProjection(
                "descItemId").getResultList().stream().map(row ->
                        ((Object[]) row)[0]
        ).collect(Collectors.toList());

        return result;
    }

    /**
     * Vytvoří lucene dotaz na hledání arr_data podle hodnoty.
     *
     * @param text hodnota
     * @param queryBuilder query builder
     *
     * @return dotaz
     */
    private Query createTextQuery(final String text, final QueryBuilder queryBuilder) {
        /** rozdělení zadaného výrazu podle mezer */
        String[] tokens = StringUtils.split(text.toLowerCase(), ' ');

        /** hledání výsledků pomocí AND (must) tak že každý obsahuje dané části zadaného výrazu */
        BooleanJunction<BooleanJunction> textConditions = queryBuilder.bool();
        for (String token : tokens) {
            String searchValue = "*" + token + "*";
            Query createQuery = queryBuilder.keyword().wildcard().onField(LuceneDescItemCondition.FULLTEXT_ATT).matching(searchValue).createQuery();
            textConditions.must(createQuery);
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
    private FullTextQuery createFullTextQuery(final Query query, final Class<?> entityClass) {
        return fullTextEntityManager.createFullTextQuery(query, entityClass);
    }

    /**
     * Vytvoří query builder pro danou třídu.
     *
     * @param entityClass třída
     *
     * @return query builder
     */
    private QueryBuilder createQueryBuilder(final Class<?> entityClass) {
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
    private List<Integer> findNodeIdsByValidDescItems(final Integer lockChangeId, final String descItemIdsString) {
        Class<ArrDescItem> entityClass = ArrDescItem.class;

        QueryBuilder queryBuilder = createQueryBuilder(entityClass);
        Query changeQuery = createChangeQuery(queryBuilder, lockChangeId);
        Query descItemIdsQuery = queryBuilder.keyword().onField("descItemIdString").matching(descItemIdsString).createQuery();
        Query validDescItemInVersionQuery = queryBuilder.all().createQuery();
        Query query = queryBuilder.bool().must(changeQuery).must(descItemIdsQuery).must(validDescItemInVersionQuery).createQuery();

        List<Integer> result = createFullTextQuery(query, entityClass).setProjection("nodeId").getResultList().stream().mapToInt(row -> {
            return (int) ((Object[]) row)[0];
        }).boxed().collect(Collectors.toList());

        return result;
    }

	/**
     * Vytvoří query pro hledání podle aktuální nebo uzavžené verze.
     *
     * @param lockChangeId id verze, může být null
     *
     * @return query
     */
    private Query createChangeQuery(final QueryBuilder queryBuilder, final Integer lockChangeId) {
        if (lockChangeId == null) { // deleteChange is null
        	return queryBuilder.range().onField("deleteChangeId").from(Integer.MAX_VALUE).to(Integer.MAX_VALUE).createQuery();
        }

        //createChangeId < lockChangeId
        Query createChangeQuery = queryBuilder.range().onField("createChangeId").below(lockChangeId).excludeLimit().createQuery();

        // and (deleteChange is null or deleteChange > lockChangeId)
        Query nullDeleteChangeQuery = queryBuilder.range().onField("deleteChangeId").from(Integer.MAX_VALUE).to(Integer.MAX_VALUE).createQuery();
        Query deleteChangeQuery = queryBuilder.range().onField("deleteChangeId").above(lockChangeId).excludeLimit().createQuery();

        Query deleteQuery = queryBuilder.bool().should(nullDeleteChangeQuery).should(deleteChangeQuery).createQuery();
        return queryBuilder.bool().must(createChangeQuery).must(deleteQuery).createQuery();
	}

    @PostConstruct
    private void buildFullTextEntityManager() {
        fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
    }

    @Override
    public Set<Integer> findNodeIdsByFilters(final ArrFundVersion version, final List<DescItemTypeFilter> filters) {
        Assert.notNull(version);
        Assert.notEmpty(filters);

        Integer fundId = version.getFund().getFundId();
        Integer lockChangeId = version.getLockChange() == null ? null : version.getLockChange().getChangeId();

        Map<Integer, List<String>> nodeIdToDescItemIds = findDescItemIdsByFilters(filters, fundId, lockChangeId);
        if (nodeIdToDescItemIds == null || nodeIdToDescItemIds.isEmpty()) {
            return Collections.EMPTY_SET;
        }

        Set<Integer> nodeIds = new HashSet<>();
        Set<String> descItemIds = new HashSet<>();
        nodeIdToDescItemIds.forEach((nodeId, diIds) -> {
            if (CollectionUtils.isEmpty(diIds)) {
                nodeIds.add(nodeId);
            } else {
                descItemIds.addAll(diIds);
            }
        });

        if (!descItemIds.isEmpty()) {
            String descItemIdsString = StringUtils.join(descItemIds, " ");
            nodeIds.addAll(findNodeIdsByValidDescItems(lockChangeId, descItemIdsString));
        }
        return nodeIds;
    }

    private Map<Integer, List<String>> findDescItemIdsByFilters(final List<DescItemTypeFilter> filters, final Integer fundId, final Integer lockChangeId) {
        if (CollectionUtils.isEmpty(filters)) {
            return null;
        }

        Map<Integer, List<String>> allDescItemIds = null;
        for (DescItemTypeFilter filter : filters) {
            QueryBuilder queryBuilder = createQueryBuilder(filter.getCls());
            Map<Integer, List<String>> nodeIdToDescItemIds = filter.resolveConditions(fullTextEntityManager, queryBuilder, fundId, entityManager, lockChangeId);

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

        return allDescItemIds;
    }
}
