package cz.tacr.elza.filter;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


import jakarta.persistence.EntityManager;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.data.Range;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import cz.tacr.elza.domain.ArrDescItem;
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
     * @param conditions podmínky
     */
    public DescItemTypeFilter(
            final RulItemType descItemType,
            final List<DescItemCondition> valuesConditions,
            final List<DescItemCondition> specsConditions,
            final List<DescItemCondition> conditions) {
        Assert.notNull(descItemType, "Typ atributu musí být vyplněn");

        this.descItemType = descItemType;
        this.valuesConditions = valuesConditions;
        this.specsConditions = specsConditions;
        this.conditions = conditions;
    }

    /**
     * Vyhodnotí podmínky.
     *
     * @param fundId
     * @param entityManager
     * @param lockChangeId
     *
     * @return id nodů se seznamem id hodnot
     */
    public Map<Integer, Set<Integer>> resolveConditions(final SearchSession searchSession, final SearchPredicateFactory factory,
                                                        final Integer fundId, final EntityManager entityManager, final Integer lockChangeId) {
        Map<Integer, Set<Integer>> valuesResult = resolveSectionConditions(valuesConditions, searchSession, factory, fundId, entityManager, lockChangeId);
        Map<Integer, Set<Integer>> specsResult = resolveSectionConditions(specsConditions, searchSession, factory, fundId, entityManager, lockChangeId);
        Map<Integer, Set<Integer>> logicalResult = resolveSectionConditions(conditions, searchSession, factory, fundId, entityManager, lockChangeId);

        return joinQueriesResultsAnd(valuesResult, specsResult, logicalResult);
    }

    private Map<Integer, Set<Integer>> resolveSectionConditions(final List<DescItemCondition> sectionConditions, final SearchSession searchSession,
            final SearchPredicateFactory factory, final Integer fundId, final EntityManager entityManager, final Integer lockChangeId) {
        FilterQueries filterQueries = createFilterQuries(sectionConditions, factory, entityManager, fundId, lockChangeId);

        Map<Integer, Set<Integer>> nodeIdToDescItemIds = processLucenePredicates(searchSession, factory,
                fundId, filterQueries.getLucenePredicates());

        Set<Integer> nodeIds = processHibernateQueries(filterQueries.getHibernateQueries());

        return joinQueriesResultsOr(nodeIdToDescItemIds, nodeIds);
    }

    /**
     * Spojí výsledky z obou typů dotazů.
     *
     * @param nodeIdToDescItemIds id nodů se seznamem id hodnot z lucene queries
     * @param nodeIds id nodů z hibernate queries
     */
    private Map<Integer, Set<Integer>> joinQueriesResultsOr(final Map<Integer, Set<Integer>> nodeIdToDescItemIds,
            final Set<Integer> nodeIds) {
        if (nodeIdToDescItemIds == null && nodeIds == null) {
            return null;
        }

        Map<Integer, Set<Integer>> result;
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
     */
    private Map<Integer, Set<Integer>> joinQueriesResultsAnd(
            final Map<Integer, Set<Integer>> valuesResult,
            final Map<Integer, Set<Integer>> specsResult,
            final Map<Integer, Set<Integer>> logicalResult) {
        // pokud není naplněna ani jedna mapa, vrátímě prázdný výsledek
        if (valuesResult == null && specsResult == null && logicalResult == null) {
            return Collections.emptyMap();
        }

        List<Map<Integer, Set<Integer>>> listOfResultMaps = new LinkedList<>();
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
        for (Map<Integer, Set<Integer>> resultMap : listOfResultMaps) {
            if (resultNodeIds == null) {
                resultNodeIds = new HashSet<>(resultMap.keySet());
            } else {
                resultNodeIds.retainAll(resultMap.keySet());
            }
        }

        Map<Integer, Set<Integer>> result = new HashMap<>();
        if (!CollectionUtils.isEmpty(resultNodeIds)) {
            for (Integer nodeId : resultNodeIds) {
                result.put(nodeId, new HashSet<>());
                for (Map<Integer, Set<Integer>> resultMap : listOfResultMaps) {
                    result.get(nodeId).addAll(resultMap.get(nodeId));
                }
            }
        }

        return result;
    }

    /**
     * Najde id nodů které vyhovují podmínkám.
     *
     * @param hibernateQueries
     */
    private Set<Integer> processHibernateQueries(final List<jakarta.persistence.Query> hibernateQueries) {
        Set<Integer> nodeIds = null;
        if (!hibernateQueries.isEmpty()) {
            for (jakarta.persistence.Query q : hibernateQueries){
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
     */
    private Map<Integer, Set<Integer>> processLucenePredicates(final SearchSession searchSession,
            final SearchPredicateFactory factory, final Integer fundId, final List<SearchPredicate> lucenePredicates) {
        Map<Integer, Set<Integer>> nodeIdToDescItemIds = null;
        if (!lucenePredicates.isEmpty()) {
            BooleanPredicateClausesStep<?> boolStep = factory.bool();
            boolStep.must(createDescItemTypePredicate(factory));
            boolStep.must(createFundIdSearchPredicate(factory, fundId));

            for (SearchPredicate searchPredicate : lucenePredicates) {
                boolStep.must(searchPredicate);
            }

            List<List<?>> rows = searchSession.search(ArrDescItem.class)
                    .select(f -> f.composite(f.field(ArrDescItem.FIELD_NODE_ID), f.field(ArrDescItem.FIELD_ITEM_ID)))
                    .where(boolStep.toPredicate()).fetchAllHits();

            //List<Object> rows = createFullTextQuery(fullTextEntityManager, booleanJunction.createQuery(), ArrDescItem.class).setProjection(ArrDescItem.FIELD_NODE_ID, ArrDescItem.FIELD_ITEM_ID).getResultList();

            nodeIdToDescItemIds = new HashMap<>(rows.size());
            for (List<?> row: rows) {
                if (row.size() >= 2) {
                    Integer nodeId = (Integer) row.get(0);
                    Integer descItemId = (Integer) row.get(1);
                    Set<Integer> descItemIds = nodeIdToDescItemIds.computeIfAbsent(nodeId, k -> new HashSet<>());
                    descItemIds.add(descItemId);
                }
            }
        }
        return nodeIdToDescItemIds;
    }

    /**
     * Vytvoří VO ve kterém budou rozdělené lucene a hibernate dotazy.
     * @param sectionConditions
     */
    private FilterQueries createFilterQuries(final List<DescItemCondition> sectionConditions, final SearchPredicateFactory factory, final EntityManager entityManager, final Integer fundId, final Integer lockChangeId) {
        List<SearchPredicate> lucenePredicates = new LinkedList<>();
        List<jakarta.persistence.Query> hibernateQueries = new LinkedList<>();
        FilterQueries filterQueries = new FilterQueries(lucenePredicates, hibernateQueries);

        if (sortConditions(sectionConditions, factory, entityManager, fundId, lockChangeId, lucenePredicates, hibernateQueries)) {
            return new FilterQueries(Collections.emptyList(), Collections.emptyList());
        }

        return filterQueries;
    }

    /** @return příznak zda je v podmínkách podmínka typu SelectsNothingCondition */
    private boolean sortConditions(final List<DescItemCondition> descItemConditions, final SearchPredicateFactory factory, final EntityManager entityManager,
            final Integer fundId, final Integer lockChangeId, final List<SearchPredicate> lucenePredicates,
            final List<jakarta.persistence.Query> hibernateQueries) {
        for (DescItemCondition condition : descItemConditions) {
            if (condition instanceof LuceneDescItemCondition) {
                LuceneDescItemCondition luceneCondition = (LuceneDescItemCondition) condition;
                lucenePredicates.add(luceneCondition.createLucenePredicate(factory));
            } else if (condition instanceof SelectsNothingCondition) {
                return true;
            } else {
                HibernateDescItemCondition hibernateCondition = (HibernateDescItemCondition) condition;
                hibernateQueries.add(hibernateCondition.createHibernateQuery(entityManager, fundId, descItemType.getItemTypeId(), lockChangeId));
            }
        }

        return false;
    }

    private SearchPredicate createDescItemTypePredicate(final SearchPredicateFactory factory) {
        Integer descItemTypeId = descItemType.getItemTypeId();
        return factory.range().field(ArrDescItem.FIELD_DESC_ITEM_TYPE_ID).range(Range.between(descItemTypeId, descItemTypeId)).toPredicate();
    }

    private SearchPredicate createFundIdSearchPredicate(final SearchPredicateFactory factory, final Integer fundId) {
        return factory.match().field(ArrDescItem.FIELD_FUND_ID).matching(fundId).toPredicate();
    }

    public RulItemType getDescItemType() {
        return descItemType;
    }
}
