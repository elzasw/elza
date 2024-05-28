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
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.session.SearchSession;
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
 * @update Sergey Iryupin
 * @since 24. 5. 2024
 */
public class DescItemTypeFilter {

    /** Typ hodnoty na který se má filtr aplikovat. */
    private RulItemType descItemType;

    /** Seznam ID specifikací filtru */
    private List<Integer> itemSpecIds;

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
    public DescItemTypeFilter(final RulItemType descItemType,
    				          final List<Integer> itemSpecIds,  
            			      final List<DescItemCondition> valuesConditions,
            			      final List<DescItemCondition> specsConditions,
            			      final List<DescItemCondition> conditions) {
        Assert.notNull(descItemType, "Typ atributu musí být vyplněn");
        this.descItemType = descItemType;
        this.itemSpecIds = itemSpecIds;
        this.valuesConditions = valuesConditions;
        this.specsConditions = specsConditions;
        this.conditions = conditions;
    }

    /**
     * Vyhodnotí podmínky.
     *
     * @param searchSession
     * @param fundId
     * @param entityManager
     * @param lockChangeId
     *
     * @return id nodů se seznamem id hodnot
     */
	public Map<Integer, Set<Integer>> resolveConditions(final SearchSession searchSession, 
														final Integer fundId, 
														final Integer lockChangeId) {
        Map<Integer, Set<Integer>> valuesResult = resolveSectionConditions(valuesConditions, searchSession, fundId, lockChangeId);
        Map<Integer, Set<Integer>> specsResult = resolveSectionConditions(specsConditions, searchSession, fundId, lockChangeId);
        Map<Integer, Set<Integer>> logicalResult = resolveSectionConditions(conditions, searchSession, fundId, lockChangeId);

        return joinQueriesResultsAnd(valuesResult, specsResult, logicalResult);
    }

    private Map<Integer, Set<Integer>> resolveSectionConditions(final List<DescItemCondition> sectionConditions, 
    															final SearchSession searchSession,
            													final Integer fundId, 
            													final Integer lockChangeId) {
        FilterQueries filterQueries = createFilterQueries(searchSession, sectionConditions, fundId, lockChangeId);

        Map<Integer, Set<Integer>> nodeIdToDescItemIds = processLuceneQueries(searchSession, fundId, filterQueries.getLucenePredicates());

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
    private Map<Integer, Set<Integer>> joinQueriesResultsAnd(final Map<Integer, Set<Integer>> valuesResult,
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
     * @param searchSession
     * @param fundId
     * @param lucenePredicates
     * @return
     */
    private Map<Integer, Set<Integer>> processLuceneQueries(final SearchSession session, 
    														final Integer fundId,
															final List<SearchPredicate> lucenePredicates) {
    	Map<Integer, Set<Integer>> nodeIdToDescItemIds = null;
    	if (!lucenePredicates.isEmpty()) {
        	SearchPredicateFactory factory = session.scope(ArrDescItem.class).predicate();
    		BooleanPredicateClausesStep<?> bool = factory.bool();
    		bool.must(factory.match().field(ArrDescItem.FIELD_DESC_ITEM_TYPE_ID).matching(descItemType.getItemTypeId()));
    		bool.must(factory.match().field(ArrDescItem.FIELD_FUND_ID).matching(fundId));
    		if (itemSpecIds != null) {
    			BooleanPredicateClausesStep<?> specs = factory.bool();
    			itemSpecIds.forEach(specId -> specs.should(factory.match().field(ArrDescItem.SPECIFICATION_ATT).matching(specId)));
    			bool.must(specs);
    		}

    		lucenePredicates.forEach(p -> bool.must(p));

    		SearchResult<ArrDescItem> descItems = session.search(ArrDescItem.class)
    				.where(bool.toPredicate())
    				.fetchAll();

    		nodeIdToDescItemIds = new HashMap<>(descItems.hits().size());
    		for (ArrDescItem descItem : descItems.hits()) {
              Integer nodeId = descItem.getNodeId();
              Integer descItemId = descItem.getItemId();
              Set<Integer> descItemIds = nodeIdToDescItemIds.computeIfAbsent(nodeId, k -> new HashSet<>());
              descItemIds.add(descItemId);
    		}
    	}
		return nodeIdToDescItemIds;
	}

    /**
     * Vytvoří VO ve kterém budou rozdělené lucene a hibernate dotazy.

     * @param searchSession
     * @param sectionConditions
     * @param fundId
     * @param lockChangeId
     * @return
     */
    private FilterQueries createFilterQueries(final SearchSession searchSession,
    									     final List<DescItemCondition> sectionConditions, 
    										 final Integer fundId, 
    										 final Integer lockChangeId) {
        List<SearchPredicate> searchPredicates = new LinkedList<>();
        List<jakarta.persistence.Query> hibernateQueries = new LinkedList<>();
        FilterQueries filterQueries = new FilterQueries(searchPredicates, hibernateQueries);
    	SearchPredicateFactory factory = searchSession.scope(ArrDescItem.class).predicate();
    	EntityManager entityManager = searchSession.toEntityManager();

        if (sortConditions(sectionConditions, factory, fundId, lockChangeId, searchPredicates, hibernateQueries, entityManager)) {
            return new FilterQueries(Collections.emptyList(), Collections.emptyList());
        }

        return filterQueries;
    }

    /** @return příznak zda je v podmínkách podmínka typu SelectsNothingCondition */
    private boolean sortConditions(final List<DescItemCondition> descItemConditions,
    							   final SearchPredicateFactory factory,
            					   final Integer fundId, 
            					   final Integer lockChangeId, 
            					   final List<SearchPredicate> searchPredicates,
            					   final List<jakarta.persistence.Query> hibernateQueries,
            					   final EntityManager entityManager) {
    	for (DescItemCondition condition : descItemConditions) {
            if (condition instanceof LuceneDescItemCondition) {
                LuceneDescItemCondition luceneCondition = (LuceneDescItemCondition) condition;
                searchPredicates.add(luceneCondition.createSearchPredicate(factory));
            } else if (condition instanceof SelectsNothingCondition) {
                return true;
            } else {
                HibernateDescItemCondition hibernateCondition = (HibernateDescItemCondition) condition;
                hibernateQueries.add(hibernateCondition.createHibernateQuery(entityManager, fundId, descItemType.getItemTypeId(), lockChangeId));
            }
        }

        return false;
    }
}
