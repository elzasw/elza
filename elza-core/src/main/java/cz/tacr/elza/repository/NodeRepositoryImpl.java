package cz.tacr.elza.repository;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.Query;
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.api.IUnitdate;
import cz.tacr.elza.common.db.QueryResults;
import cz.tacr.elza.controller.vo.filter.SearchParam;
import cz.tacr.elza.controller.vo.filter.SearchParamType;
import cz.tacr.elza.controller.vo.filter.TextSearchParam;
import cz.tacr.elza.controller.vo.filter.UnitdateCondition;
import cz.tacr.elza.controller.vo.filter.UnitdateSearchParam;
import cz.tacr.elza.domain.ArrCachedNode;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.domain.vo.ArrFundToNodeList;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;
import cz.tacr.elza.exception.InvalidQueryException;
import cz.tacr.elza.filter.DescItemTypeFilter;
import cz.tacr.elza.service.cache.CachedNode;
import cz.tacr.elza.service.cache.NodeCacheService;

/**
 * Custom node repository implementation
 */
@Component
public class NodeRepositoryImpl implements NodeRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private NodeCacheService nodeCacheService;
    
    private SearchSession searchSession = null;

    private SearchPredicateFactory searchPredicateFactory = null;

    // getFullTextEntityManager() -> getSearchSession();
    private SearchSession getSearchSession() {
		if (searchSession == null) {
			searchSession = Search.session(entityManager);
    	}
    	return searchSession;
    }

    private SearchPredicateFactory getSearchPredicateFactory() {
    	if (searchPredicateFactory == null) {
    		searchPredicateFactory = getSearchSession().scope(ArrDescItem.class).predicate();
    	}
    	return searchPredicateFactory;
    }
    
    @Override
    public List<ArrNode> findNodesByDirection(ArrNode node, ArrFundVersion version, RelatedNodeDirection direction) {
    	Validate.notNull(node, "JP musí být vyplněna");
    	Validate.notNull(version, "Verze AS musí být vyplněna");
    	Validate.notNull(direction, "Směr musí být vyplněn");

    	ArrLevel level = levelRepository.findByNode(node, version.getLockChange());
    	Collection<ArrLevel> levels = levelRepository.findLevelsByDirection(level, version, direction);

    	List<ArrNode> result = levels.stream().map(l -> l.getNode()).collect(Collectors.toList());
      return result;
    }


    @Override
    public Collection<ArrFundToNodeList> findFundToNodeListByFulltext(final String text, final Collection<ArrFund> fundList) {
    	SearchPredicateFactory factory = getSearchSession().scope(ArrCachedNode.class).predicate();

        SearchPredicate textPredicate = null;
        if (text != null) {
            textPredicate = createTextQuery(text, factory);
        }

        SearchPredicate fundIdsPredicate = null;
        if (fundList != null) {
            fundIdsPredicate = createFundIdsQuery(fundList.stream().map(o -> o.getFundId()).collect(Collectors.toList()), factory);
        } else {
        	fundIdsPredicate = factory.matchAll().toPredicate();
        }

        SearchPredicate finalPredicate = factory.bool().must(textPredicate).must(fundIdsPredicate).toPredicate();

        SearchResult<ArrCachedNode> resultList = getSearchSession().search(ArrCachedNode.class).where(finalPredicate).fetchAll();
        Map<Integer, ArrFundToNodeList> fundToNodeListMap = new HashMap<>();

        resultList.hits().forEach(arrCachedNode -> {
        	CachedNode cachedNode = nodeCacheService.getCachedNode(arrCachedNode);
        	ArrFundToNodeList fundToNodeList = fundToNodeListMap.get(cachedNode.getFundId());
        	if (fundToNodeList == null) {
        		fundToNodeList = new ArrFundToNodeList(cachedNode.getFundId(), new ArrayList<>());
        		fundToNodeListMap.put(cachedNode.getFundId(), fundToNodeList);
        	}
        	fundToNodeList.getNodeIdList().add(arrCachedNode.getNodeId());
        });

        return fundToNodeListMap.values();
    }

    @Override
    public QueryResults<ArrDescItemInfo> findFundIdsByFulltext(final String text,
                                                               final Collection<ArrFund> fundList,
                                                               final Integer size, final Integer offset) {
        SearchPredicateFactory factory = getSearchPredicateFactory();

        SearchPredicate textPredicate = null;
        if (text != null) {
            textPredicate = createTextQuery(text, factory);
        }

        SearchPredicate fundIdsPredicate = null;
        if (fundList != null) {
            fundIdsPredicate = createFundIdsQuery(fundList.stream().map(o -> o.getFundId()).collect(Collectors.toList()), factory);
        } else {
        	fundIdsPredicate = factory.matchAll().toPredicate();
        }

        SearchPredicate searchPredicate;
        if (textPredicate == null && fundIdsPredicate == null) {
            searchPredicate = factory.matchAll().toPredicate();
        } else {
            searchPredicate = factory.bool().must(textPredicate).must(fundIdsPredicate).toPredicate();
        }

        List<ArrDescItemInfo> itemList = findNodeIdsByValidDescItems(null, searchPredicate, size, offset);

        QueryResults<ArrDescItemInfo> result = new QueryResults<>(itemList.size(), itemList);
        return result;
    }

	/**
  	 * Vyhledávání id uzlu podle textu ve stromu fondu.
  	 *
  	 * @param text   search text
  	 * @param fundId id fondu
  	 * @return seznam id
  	 */
    @Override
    public Set<Integer> findByFulltext(String text, Integer fundId) {
    	Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");

    	SearchPredicateFactory factory = getSearchSession().scope(ArrCachedNode.class).predicate();

    	SearchPredicate textPredicate = createTextQuery(text, factory);
        SearchPredicate fundIdPredicate = factory.bool().should(factory.match().field(ArrDescItem.FIELD_FUND_ID).matching(fundId)).toPredicate();
        SearchPredicate finalPredicate = factory.bool().must(textPredicate).must(fundIdPredicate).toPredicate();

        SearchResult<ArrCachedNode> resultList = getSearchSession().search(ArrCachedNode.class).where(finalPredicate).fetchAll();

    	return resultList.hits().stream().map(i -> i.getNodeId()).collect(Collectors.toSet());
    }

	/**
  	 * Vyhledávání podle textu ve stromu fondu.
  	 * Vrátí id nodů které mají danou hodnotu v dané verzi.
  	 *
  	 * @param text   search text
  	 * @param fundId id fondu
  	 * @param lockChangeId
  	 * @return seznam id
  	 */
    @Override
    public Set<Integer> findByFulltextAndVersionLockChangeId(String text, Integer fundId, Integer lockChangeId) {
    	Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");

    	SearchPredicate descItemIdsPredicate = createDescItemIdsByDataQuery(text, fundId);

    	List<ArrDescItemInfo> result = findNodeIdsByValidDescItems(lockChangeId, descItemIdsPredicate, null, null);

    	return result.stream().map(i -> i.getNodeId()).collect(Collectors.toSet());
    }

    /**
  	 * Vyhledávání podle lucene query ve stromu fondu.
     * 
  	 * @param text	 lucene query text
  	 * @param fundId id fondu
  	 * @param lockChangeId
  	 * @return seznam id
     */
    @Override
    public Set<Integer> findByLuceneQueryAndVersionLockChangeId(String queryText, Integer fundId, Integer lockChangeId) throws InvalidQueryException {
    	Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");

    	SearchPredicate descItemIdsPredicate = createDescItemIdsByLuceneQuery(queryText, fundId);

    	List<ArrDescItemInfo> result = findNodeIdsByValidDescItems(lockChangeId, descItemIdsPredicate, null, null);

    	return result.stream().map(i -> i.getNodeId()).collect(Collectors.toSet());
    }

    /**
     * Pokročilé vyhledávání podle řetězce ve stromu fondu.
     * 
     * @param searchParams
  	 * @param fundId
  	 * @param lockChangeId
  	 * @return seznam id
     */
    @Override
    public Set<Integer> findBySearchParamsAndVersionLockChangeId(List<SearchParam> searchParams, Integer fundId, Integer lockChangeId) {
    	Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");
    	Assert.notEmpty(searchParams, "Musí být vyplněn alespoň jeden parametr vyhledávání");

    	List<TextSearchParam> textParams = new LinkedList<>();
    	List<UnitdateSearchParam> dateParams = new LinkedList<>();
    	for (SearchParam searchParam : searchParams) {
    		if (SearchParamType.TEXT == searchParam.getType()) {
    			textParams.add((TextSearchParam) searchParam);
    		} else if (SearchParamType.UNITDATE == searchParam.getType()) {
    			dateParams.add((UnitdateSearchParam) searchParam);
    		}
    	}

    	Set<Integer> textNodeIds = null;
    	if (!textParams.isEmpty()) {
    		textNodeIds = findByTextSearchParamsAndVersionLockChangeId(textParams, fundId, lockChangeId);
    	}
    	Set<Integer> dateNodeIds = null;
    	if (!dateParams.isEmpty()) {
    		dateNodeIds = findByDateSearchParamsAndVersionLockChangeId(dateParams, fundId, lockChangeId);
    	}

    	Set<Integer> result;
    	if (textNodeIds != null && dateNodeIds != null) {
    		textNodeIds.retainAll(dateNodeIds);
    		result = textNodeIds;
    	} else if (textNodeIds != null) {
    		result = textNodeIds;
    	} else {
    		result = dateNodeIds;
    	}

    	return result;
    }

	/**
  	 * Najde id nodů vyhovující předaným parametrům.
	 *
  	 * @param searchParams podmínky
  	 * @param fundId id archivního souboru
  	 * @param lockChangeId id změny, může být null
  	 *
  	 * @return id nodů
  	 */
    private Set<Integer> findByTextSearchParamsAndVersionLockChangeId(final List<TextSearchParam> searchParams, final Integer fundId,
         															  final Integer lockChangeId) {
    	Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");
    	Assert.notEmpty(searchParams, "Musí být vyplněn alespoň jeden parametr vyhledávání");

    	SearchPredicate descItemIdsPredicate = createDescItemIdsByTextSearchParamsDataQuery(searchParams, fundId);

    	List<ArrDescItemInfo> result = findNodeIdsByValidDescItems(lockChangeId, descItemIdsPredicate, null, null);

    	return result.stream().map(i -> i.getNodeId()).collect(Collectors.toSet());
    }

    /**
     * Najde id nodů vyhovující předaným parametrům.
     *
     * @param searchParams podmínky
     * @param fundId id archivního souboru
     * @param lockChangeId id změny, může být null
     *
     * @return id nodů
     */
    private Set<Integer> findByDateSearchParamsAndVersionLockChangeId(final List<UnitdateSearchParam> searchParams, final Integer fundId,
                                                                      final Integer lockChangeId) {
        Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");
        Assert.notEmpty(searchParams, "Musí být vyplněn alespoň jeden parametr vyhledávání");

        SearchPredicate descItemIdsPredicate = createDescItemIdsByDateSearchParamsDataQuery(searchParams, fundId);

        List<ArrDescItemInfo> result = findNodeIdsByValidDescItems(lockChangeId, descItemIdsPredicate, null, null);

        return result.stream().map(i -> i.getNodeId()).collect(Collectors.toSet());
    }

    @Override
    public ScrollableResults<Integer> findUncachedNodes() {
        // přepsáno z NOT IN z důvodu optimalizace na LEFT JOIN
		String hql = "SELECT n.nodeId FROM arr_node n LEFT JOIN arr_cached_node cn ON cn.nodeId = n.nodeId WHERE cn IS NULL";

		// get Hibernate session
		Session session = entityManager.unwrap(Session.class);
		ScrollableResults<Integer> result = session.createQuery(hql, Integer.class).setCacheMode(CacheMode.IGNORE).scroll(ScrollMode.FORWARD_ONLY);

		return result;
    }

    /**
     * Najde predikát vyhovující předaným parametrům (UnitdateSearchParam, fundId).
     *
     * @param searchParams podmínky
     * @param fundId id archivního souboru
     *
     * @return id nodů
     */
    private SearchPredicate createDescItemIdsByDateSearchParamsDataQuery(final List<UnitdateSearchParam> searchParams,
                                                                      	 final Integer fundId) {
    	SearchPredicateFactory factory = getSearchPredicateFactory();
        BooleanPredicateClausesStep<?> bool = factory.bool();

        for (UnitdateSearchParam searchParam : searchParams) {
            String value = searchParam.getValue();
            if (StringUtils.isNotBlank(value)) {
            	SearchPredicate datePredicate = createDateQuery(value, searchParam.getCondition(), factory);
                bool.must(datePredicate);
            }
        }

        if (!bool.hasClause()) {
            return null;
        }

        SearchPredicate fundIdPredicate = factory.bool().should(factory.match().field(ArrDescItem.FIELD_FUND_ID).matching(fundId)).toPredicate();
        return factory.bool().must(bool.toPredicate()).must(fundIdPredicate).toPredicate();
	}

    /**
     * Vytvoří lucene predikát na hledání arr_data podle datace.
     *
     * @param value datace
     * @param condition typ podmínky
     * @param factory search predicate factory
     *
     * @return predikát
     */
    private SearchPredicate createDateQuery(final String value, final UnitdateCondition condition, final SearchPredicateFactory factory) {
        Assert.notNull(value, "Hodnota musí být vyplněna");
        Assert.notNull(condition, "Podmínka musí být vyplněna");

        IUnitdate unitdate = new ArrDataUnitdate();
        UnitDateConvertor.convertToUnitDate(value, unitdate);

        LocalDateTime fromDate = LocalDateTime.parse(unitdate.getValueFrom(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        long secondsFrom = CalendarConverter.toSeconds(fromDate);

        LocalDateTime toDate = LocalDateTime.parse(unitdate.getValueTo(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        long secondsTo = CalendarConverter.toSeconds(toDate);

        BooleanPredicateClausesStep<?> bool;
        switch (condition) {
            case CONTAINS:
            	SearchPredicate fromPredicate = factory.range().field(ArrDescItem.NORMALIZED_FROM_ATT).atLeast(secondsFrom).toPredicate();
            	SearchPredicate toPredicate = factory.range().field(ArrDescItem.NORMALIZED_TO_ATT).atMost(secondsTo).toPredicate();
            	bool = factory.bool().must(fromPredicate).must(toPredicate);
                break;
            case GE:
            	bool = factory.bool().must(factory.range().field(ArrDescItem.NORMALIZED_FROM_ATT).atLeast(secondsFrom).toPredicate());
                break;
            case LE:
            	bool = factory.bool().must(factory.range().field(ArrDescItem.NORMALIZED_TO_ATT).atMost(secondsTo).toPredicate());
                break;
            default:
                throw new IllegalStateException("Neznámý typ podmínky " + condition);
        }

        return bool.toPredicate();
    }

	/**
     * Najde predikát vyhovující předaným parametrům (TextSearchParam, fundId).
     *
     * @param searchParams podmínky
     * @param fundId id archivního souboru
     *
     * @return predikát
     */
    private SearchPredicate createDescItemIdsByTextSearchParamsDataQuery(final List<TextSearchParam> searchParams,
                                                               			 final Integer fundId) {
    	SearchPredicateFactory factory = getSearchPredicateFactory();
        BooleanPredicateClausesStep<?> bool = factory.bool();

        for (TextSearchParam searchParam : searchParams) {
            String value = searchParam.getValue();
            if (StringUtils.isNotBlank(value)) {
                SearchPredicate textPredicate = createTextQuery(value, factory);
                bool.must(textPredicate);
            }
        }

        if (!bool.hasClause()) {
            return null;
        }

        SearchPredicate fundIdPredicate = factory.bool().should(factory.match().field(ArrDescItem.FIELD_FUND_ID).matching(fundId)).toPredicate();
        return factory.bool().must(bool.toPredicate()).must(fundIdPredicate).toPredicate();
    }

	/**
     * Vyhledá id atributů podle předané hodnoty. Hledá napříč archivními pomůckami a jejich verzemi.
     *
     * @param text hodnota podle které se hledá
     * @param fundId id fondu
     *
     * @return id atributů které mají danou hodnotu
     */
	private SearchPredicate createDescItemIdsByDataQuery(final String text, final Integer fundId) {

        if (StringUtils.isBlank(text)) {
            return null;
        }

        SearchPredicateFactory factory = getSearchPredicateFactory();
        SearchPredicate textPredicate = createTextQuery(text, factory);
        SearchPredicate fundIdPredicate = factory.bool().should(factory.match().field(ArrDescItem.FIELD_FUND_ID).matching(fundId)).toPredicate();

        return factory.bool().must(textPredicate).must(fundIdPredicate).toPredicate();
    }

	private SearchPredicate createDescItemIdsQuery(Collection<Integer> descItemIds) {
        SearchPredicateFactory factory = getSearchPredicateFactory();
    	BooleanPredicateClausesStep<?> bool = factory.bool();
        for (Integer descItemId : new HashSet<>(descItemIds)) {
        	bool.should(factory.match().field(ArrDescItem.FIELD_ITEM_ID).matching(descItemId));
        }
    	return bool.toPredicate();
	}

	private SearchPredicate createFundIdsQuery(final Collection<Integer> fundIds, final SearchPredicateFactory factory) {
    	BooleanPredicateClausesStep<?> bool = factory.bool();
        for (Integer fundId : new HashSet<>(fundIds)) {
            bool.should(factory.match().field(ArrDescItem.FIELD_FUND_ID).matching(fundId));
        }
        return bool.toPredicate();
    }

    /**
     * Vyhledá predikát podle předané hodnoty. Hledá napříč archivními pomůckami a jejich verzemi.
     * Vyhledávání probíhá podle lucene dotazu.
     *
     * @param queryText lucene dotaz (např: +specification:448 -fulltextValue:ddd*)
     * @param fundId    id fondu
     * @return predikát
     * @throws InvalidQueryException neplatný lucene dotaz
     */
    private SearchPredicate createDescItemIdsByLuceneQuery(final String queryText, final Integer fundId) {

        if (StringUtils.isBlank(queryText)) {
            return null;
        }

        StandardQueryParser parser = new StandardQueryParser();
        parser.setAllowLeadingWildcard(true);

        HashMap<String, PointsConfig> stringNumericConfigHashMap = new HashMap<>();
        PointsConfig intConfig = new PointsConfig(NumberFormat.getIntegerInstance(), Integer.class);
        PointsConfig longConfig = new PointsConfig(NumberFormat.getNumberInstance(), Long.class);
        stringNumericConfigHashMap.put(ArrDescItem.SPECIFICATION_ATT, intConfig);
        stringNumericConfigHashMap.put(ArrDescItem.NORMALIZED_FROM_ATT, longConfig);
        stringNumericConfigHashMap.put(ArrDescItem.NORMALIZED_TO_ATT, longConfig);

        parser.setPointsConfigMap(stringNumericConfigHashMap);

        SearchPredicateFactory factory = getSearchPredicateFactory();
        try {

			Query textQuery = parser.parse(queryText, ArrDescItem.FULLTEXT_ATT);
			SearchPredicate fromLuceneQuery = factory.extension(LuceneExtension.get()).fromLuceneQuery(textQuery).toPredicate();
	        SearchPredicate fundIdPredicate = factory.bool().should(factory.match().field(ArrDescItem.FIELD_FUND_ID).matching(fundId)).toPredicate();
	        return factory.bool().must(fromLuceneQuery).must(fundIdPredicate).toPredicate();

        } catch (QueryNodeException e) {
            throw new InvalidQueryException(e);
        }
    }

	/**
     * Vytvoří lucene dotaz na hledání arr_data podle hodnoty.
     *
     * @param text hodnota
     * @param queryBuilder query builder
     *
     * @return dotaz
     */
	private SearchPredicate createTextQuery(final String text, final SearchPredicateFactory factory) {
        if (text == null) {
            return null;
        }
        /* rozdělení zadaného výrazu podle mezer */
        String[] tokens = StringUtils.split(text.toLowerCase(), ' ');

        /* hledání výsledků pomocí AND (must) tak že každý obsahuje dané části zadaného výrazu */
        BooleanPredicateClausesStep<?> bool = factory.bool();
        for (String token : tokens) {
            String searchValue = "*" + token + "*";
            SearchPredicate predicate = factory.bool().should(factory.wildcard().field(ArrDescItem.FULLTEXT_ATT).matching(searchValue)).toPredicate();
            bool.must(predicate);
        }

        return bool.toPredicate();
	}

    /**
     * Vyhledá id nodů podle platných atributů. Hledá napříč archivními pomůckami.
     *
     * @param lockChangeId id změny uzavření verze archivní pomůcky, může být null
     * @param descItemIdsQuery id atributů pro které se mají hledat nody
     *
     * @return id nodů které mají před danou změnou nějaký atribut
     */
    private List<ArrDescItemInfo> findNodeIdsByValidDescItems(final Integer lockChangeId,
                                                              final SearchPredicate descItemIdsPredicate,
                                                              final Integer size, final Integer offset) {
        if (descItemIdsPredicate == null) {
            return Collections.emptyList();
        }

        SearchPredicate changePredicate = createChangeQuery(lockChangeId);

        SearchPredicate finalPredicate = getSearchPredicateFactory().bool()
        									.must(changePredicate)
        									.must(descItemIdsPredicate)
        									.toPredicate();

        SearchResult<ArrDescItem> resultList;
        if (size == null || offset == null) {
        	resultList = getSearchSession().search(ArrDescItem.class).where(finalPredicate).fetchAll();
        } else {
        	resultList = getSearchSession().search(ArrDescItem.class).where(finalPredicate).fetch(offset, size);
        }

        return resultList.hits().stream()
                .map(row -> new ArrDescItemInfo(row.getItemId(), row.getNodeId(), row.getFundId(), 0f))
                .collect(Collectors.toList());
	}

    /**
     * Vytvoří query pro hledání podle aktuální nebo uzavžené verze.
     *
     * @param lockChangeId id verze, může být null
     *
     * @return query
     */
    private SearchPredicate createChangeQuery(final Integer lockChangeId) {

    	SearchPredicateFactory factory = getSearchPredicateFactory();
    	SearchPredicate nullDeleteChangePredicate = factory.bool().mustNot(factory.exists().field(ArrDescItem.FIELD_DELETE_CHANGE_ID)).toPredicate();

        if (lockChangeId == null) {
            // deleteChange is null
            return nullDeleteChangePredicate;
        }

        SearchPredicate createChangePredicate = factory.range().field(ArrDescItem.FIELD_CREATE_CHANGE_ID).lessThan(lockChangeId).toPredicate();
        SearchPredicate deleteChangePredicate = factory.range().field(ArrDescItem.FIELD_DELETE_CHANGE_ID).greaterThan(lockChangeId).toPredicate();

        // createChangeId < lockChangeId and (deleteChange is null or deleteChange > lockChangeId)
        SearchPredicate deleteQuery = factory.bool().should(nullDeleteChangePredicate).should(deleteChangePredicate).toPredicate();
        return factory.bool().must(createChangePredicate).must(deleteQuery).toPredicate();
    }

    @Override
    public Set<Integer> findNodeIdsByFilters(final ArrFundVersion version, 
    										 final List<DescItemTypeFilter> filters) {
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notEmpty(filters, "Musí být vyplněn alespoň jeden filter");

        Integer fundId = version.getFund().getFundId();
        Integer lockChangeId = version.getLockChange() == null ? null : version.getLockChange().getChangeId();

        Map<Integer, Set<Integer>> nodeIdToDescItemIds = findDescItemIdsByFilters(filters, fundId, lockChangeId);
        if (nodeIdToDescItemIds == null || nodeIdToDescItemIds.isEmpty()) {
            return Collections.emptySet();
        }

        Set<Integer> nodeIds = new HashSet<>();
        Set<Integer> descItemIds = new HashSet<>();
        nodeIdToDescItemIds.forEach((nodeId, diIds) -> {
            if (CollectionUtils.isEmpty(diIds)) {
                nodeIds.add(nodeId);
            } else {
                descItemIds.addAll(diIds);
            }
        });

        if (!descItemIds.isEmpty()) {

        	SearchPredicate descItemIdsPredicate = createDescItemIdsQuery(descItemIds);

            List<ArrDescItemInfo> list = findNodeIdsByValidDescItems(lockChangeId, descItemIdsPredicate, null, null);

            nodeIds.addAll(list.stream().map(i -> i.getNodeId()).collect(Collectors.toList()));

        }
        return nodeIds;
	}

    private Map<Integer, Set<Integer>> findDescItemIdsByFilters(final List<DescItemTypeFilter> filters, 
    															final Integer fundId, 
    															final Integer lockChangeId) {
        if (CollectionUtils.isEmpty(filters)) {
            return null;
        }

        Map<Integer, Set<Integer>> allDescItemIds = null;
        for (DescItemTypeFilter filter : filters) {
            Map<Integer, Set<Integer>> nodeIdToDescItemIds = filter.resolveConditions(getSearchSession(), fundId, lockChangeId);

            if (allDescItemIds == null) {
                allDescItemIds = new HashMap<>(nodeIdToDescItemIds);
            } else {
                Set<Integer> existingNodes = new HashSet<>(allDescItemIds.keySet());
                existingNodes.retainAll(nodeIdToDescItemIds.keySet());

                Map<Integer, Set<Integer>> updatedAllDescItemIds = new HashMap<>(nodeIdToDescItemIds.size());
                for (Integer nodeId : existingNodes) {
                    Set<Integer> rowDescItemIds = nodeIdToDescItemIds.get(nodeId);
                    Set<Integer> existingDescItemIds = allDescItemIds.get(nodeId);

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
