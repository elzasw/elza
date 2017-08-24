package cz.tacr.elza.filter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.util.Assert;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.filter.condition.DescItemCondition;
import cz.tacr.elza.filter.condition.HibernateDescItemCondition;
import cz.tacr.elza.filter.condition.LuceneDescItemCondition;
import cz.tacr.elza.filter.condition.SelectsNothingCondition;

/**
 * Skupina filtrů pro typ atributu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 13. 4. 2016
 */
public class DescItemTypeFilter {

    /** Typ hodnoty na který se má filtr aplikovat. */
    private RulItemType descItemType;

    /** Třída na kterou se bude aplikovat filtr. */
    private Class<?> cls;

    /** Podmínky pro seznam hodnot. */
    private List<DescItemCondition> valuesConditions;

    /** Podmínky pro seznam specifikací. */
    private List<DescItemCondition> specsConditions;

    /** Logické podmínky. */
    private List<DescItemCondition> conditions;

    /**
     * Konstruktor pro podmínky.
     *
     * @param descItemType typ atributu
     * @param cls třída na kterou se mají podmínky aplikovat
     * @param conditions podmínky
     */
    public DescItemTypeFilter(
            final RulItemType descItemType,
            final Class<?> cls,
            final List<DescItemCondition> valuesConditions,
            final List<DescItemCondition> specsConditions,
            final List<DescItemCondition> conditions) {
        Assert.notNull(descItemType, "Typ atributu musí být vyplněn");
        Assert.notNull(cls, "Třída musí být vyplněna");

        this.descItemType = descItemType;
        this.cls = cls;
        this.valuesConditions = valuesConditions;
        this.specsConditions = specsConditions;
        this.conditions = conditions;
    }

    /**
     * Vyhodnotí podmínky.
     *
     * @param fullTextEntityManager
     * @param queryBuilder
     * @param fundId
     * @param entityManager
     * @param lockChangeId
     *
     * @return id nodů se seznamem id hodnot
     */
    public Map<Integer, Set<String>> resolveConditions(final FullTextEntityManager fullTextEntityManager, final QueryBuilder queryBuilder,
            final Integer fundId, final EntityManager entityManager, final Integer lockChangeId) {
        Map<Integer, Set<String>> valuesResult = resolveSectionConditions(valuesConditions, fullTextEntityManager, queryBuilder, fundId, entityManager, lockChangeId);
        Map<Integer, Set<String>> specsResult = resolveSectionConditions(specsConditions, fullTextEntityManager, queryBuilder, fundId, entityManager, lockChangeId);
        Map<Integer, Set<String>> logicalResult = resolveSectionConditions(conditions, fullTextEntityManager, queryBuilder, fundId, entityManager, lockChangeId);

        return joinQueriesResultsAnd(valuesResult, specsResult, logicalResult);
    }

    private Map<Integer, Set<String>> resolveSectionConditions(final List<DescItemCondition> sectionConditions, final FullTextEntityManager fullTextEntityManager,
            final QueryBuilder queryBuilder, final Integer fundId, final EntityManager entityManager, final Integer lockChangeId) {
        FilterQueries filterQueries = createFilterQuries(sectionConditions, queryBuilder, entityManager, fundId, lockChangeId);

        Map<Integer, Set<String>> nodeIdToDescItemIds = processLuceneQueries(fullTextEntityManager, queryBuilder,
                fundId, filterQueries.getLuceneQueries());

        Set<Integer> nodeIds = processHibernateQueries(filterQueries.getHibernateQueries());

        return joinQueriesResultsOr(nodeIdToDescItemIds, nodeIds);
    }

    /**
     * Spojí výsledky z obou typů dotazů.
     *
     * @param nodeIdToDescItemIds id nodů se seznamem id hodnot z lucene queries
     * @param nodeIds id nodů z hibernate queries
     */
    private Map<Integer, Set<String>> joinQueriesResultsOr(final Map<Integer, Set<String>> nodeIdToDescItemIds,
            final Set<Integer> nodeIds) {
        if (nodeIdToDescItemIds == null && nodeIds == null) {
            return null;
        }

        Map<Integer, Set<String>> result = null;
        if (nodeIdToDescItemIds == null) {
            result = new HashMap<>();
            for (Integer nodeId : nodeIds) {
                result.put(nodeId, new HashSet<>());
            }
        } else if (nodeIds != null) {
            result = new HashMap<>(nodeIdToDescItemIds);
            for (Integer nodeId : nodeIds) {
                if (!nodeIdToDescItemIds.containsKey(nodeId)) {
                    result.put(nodeId, new HashSet<>());
                }
            }
        } else {
            result = new HashMap<>(nodeIdToDescItemIds);
        }

        return result;
    }

    /**
     * Spojí výsledky z obou typů dotazů. Vrátí jen ty výsledky které našly obě skupiny dotazů.
     *
     * @param nodeIdToDescItemIds id nodů se seznamem id hodnot z lucene queries
     * @param nodeIds id nodů z hibernate queries
     * @param expressionsNodeIdToDescItemIds
     */
    private Map<Integer, Set<String>> joinQueriesResultsAnd(
            final Map<Integer, Set<String>> valuesResult,
            final Map<Integer, Set<String>> specsResult,
            final Map<Integer, Set<String>> logicalResult) {
        // pokud není naplněna ani jedna mapa, vrátímě prázdný výsledek
        if (valuesResult == null && specsResult == null && logicalResult == null) {
            return Collections.emptyMap();
        }

        List<Map<Integer, Set<String>>> listOfResultMaps = new LinkedList<>();
        if (valuesResult != null) {
            listOfResultMaps.add(valuesResult);
        }
        if (specsResult != null) {
            listOfResultMaps.add(specsResult);
        }
        if (logicalResult != null) {
            listOfResultMaps.add(logicalResult);
        }

        // pokud je naplněna jen jedna mapa tak ji vrátíme
        if (listOfResultMaps.size() == 1) {
            return listOfResultMaps.iterator().next();
        }

        // pokud je naplněno více map, uděláme průnik
        Set<Integer> resultNodeIds = null; // průnik id nodů ze všech výsledků
        for (Map<Integer, Set<String>> resultMap : listOfResultMaps) {
            if (resultNodeIds == null) {
                resultNodeIds = new HashSet<>(resultMap.keySet());
            } else {
                resultNodeIds.retainAll(resultMap.keySet());
            }
        }

        Map<Integer, Set<String>> result = new HashMap<>();
        for (Integer nodeId : resultNodeIds) {
            result.put(nodeId, new HashSet<>());
            for (Map<Integer, Set<String>> resultMap : listOfResultMaps) {
                result.get(nodeId).addAll(resultMap.get(nodeId));
            }
        }

        return result;
    }

    /**
     * Najde id nodů které vyhovují podmínkám.
     *
     * @param hibernateQueries
     */
    private Set<Integer> processHibernateQueries(final List<javax.persistence.Query> hibernateQueries) {
        Set<Integer> nodeIds = null;
        if (!hibernateQueries.isEmpty()) {
            for (javax.persistence.Query q : hibernateQueries){
                List<Integer> resultList = q.getResultList();

                if (nodeIds == null) {
                    nodeIds = new HashSet<>(resultList);
                } else {
                    nodeIds.retainAll(resultList);
                }
            }
        }
        return nodeIds;
    }

    /**
     * Najde id nodů a k nim seznam hodnot atributů.
     *
     * @param fullTextEntityManager
     * @param queryBuilder
     * @param fundId
     * @param luceneQueries
     */
    private Map<Integer, Set<String>> processLuceneQueries(final FullTextEntityManager fullTextEntityManager,
            final QueryBuilder queryBuilder, final Integer fundId, final List<Query> luceneQueries) {
        Map<Integer, Set<String>> nodeIdToDescItemIds = null;
        if (!luceneQueries.isEmpty()) {
            BooleanJunction<BooleanJunction> booleanJunction = queryBuilder.bool();
            booleanJunction.must(createDescItemTypeQuery(queryBuilder));
            booleanJunction.must(createFundIdQuery(queryBuilder, fundId));

            luceneQueries.forEach(q -> {
                booleanJunction.must(q);
            });

            List<Object> rows = createFullTextQuery(fullTextEntityManager, booleanJunction.createQuery(), ArrData.class).setProjection("nodeId", "itemId").getResultList();

            nodeIdToDescItemIds = new HashMap<>(rows.size());
            for (Object row: rows) {
                Object[] rowArray = (Object[]) row;
                Integer nodeId = (Integer) rowArray[0];
                String descItemId = (String) rowArray[1];
                Set<String> descItemIds = nodeIdToDescItemIds.get(nodeId);
                if (descItemIds == null) {
                    descItemIds = new HashSet<>();
                    nodeIdToDescItemIds.put(nodeId, descItemIds);
                }
                descItemIds.add(descItemId);
            }
        }
        return nodeIdToDescItemIds;
    }

    /**
     * Vytvoří VO ve kterém budou rozdělené lucene a hibernate dotazy.
     * @param sectionConditions
     */
    private FilterQueries createFilterQuries(final List<DescItemCondition> sectionConditions, final QueryBuilder queryBuilder, final EntityManager entityManager, final Integer fundId, final Integer lockChangeId) {
        List<Query> luceneQueries = new LinkedList<>();
        List<javax.persistence.Query> hibernateQueries = new LinkedList<>();
        FilterQueries filterQueries = new FilterQueries(luceneQueries, hibernateQueries);

        if (sortConditions(sectionConditions, queryBuilder, entityManager, fundId, lockChangeId, luceneQueries, hibernateQueries)) {
            return new FilterQueries(Collections.emptyList(), Collections.emptyList());
        }

        return filterQueries;
    }

    /** @return příznak zda je v podmínkách podmínka typu SelectsNothingCondition */
    private boolean sortConditions(final List<DescItemCondition> descItemConditions, final QueryBuilder queryBuilder, final EntityManager entityManager,
            final Integer fundId, final Integer lockChangeId, final List<Query> luceneQueries,
            final List<javax.persistence.Query> hibernateQueries) {
        for (DescItemCondition condition : descItemConditions) {
            if (condition instanceof LuceneDescItemCondition) {
                LuceneDescItemCondition luceneCondition = (LuceneDescItemCondition) condition;
                luceneQueries.add(luceneCondition.createLuceneQuery(queryBuilder));
            } else if (condition instanceof SelectsNothingCondition) {
                return true;
            } else {
                HibernateDescItemCondition hibernateCondition = (HibernateDescItemCondition) condition;
                hibernateQueries.add(hibernateCondition.createHibernateQuery(entityManager, fundId, descItemType.getItemTypeId(), lockChangeId));
            }
        }

        return false;
    }

    /**
     * Vytvoří hibernate jpa query z lucene query.
     *
     * @param query lucene qery
     * @param entityClass třída pro kterou je dotaz
     *
     * @return hibernate jpa query
     */
    private FullTextQuery createFullTextQuery(final FullTextEntityManager fullTextEntityManager, final Query query, final Class<?> entityClass) {
        return fullTextEntityManager.createFullTextQuery(query, entityClass);
    }

    private Query createDescItemTypeQuery(final QueryBuilder queryBuilder) {
        Integer descItemTypeId = descItemType.getItemTypeId();
        return queryBuilder.range().onField(ArrData.LUCENE_DESC_ITEM_TYPE_ID).from(descItemTypeId).to(descItemTypeId).
                createQuery();
    }

    private Query createFundIdQuery(final QueryBuilder queryBuilder, final Integer fundId) {
        return queryBuilder.keyword().onField("fundId").matching(fundId).createQuery();
    }


    public Class<?> getCls() {
        return cls;
    }

    public RulItemType getDescItemType() {
        return descItemType;
    }
}
