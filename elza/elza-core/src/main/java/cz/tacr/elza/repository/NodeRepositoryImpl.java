package cz.tacr.elza.repository;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

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
import org.hibernate.CacheMode;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.search.engine.ProjectionConstants;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
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
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrLevel;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.convertor.CalendarConverter;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.domain.vo.RelatedNodeDirection;
import cz.tacr.elza.exception.InvalidQueryException;
import cz.tacr.elza.filter.DescItemTypeFilter;

/**
 * Custom node repository implementation
 */
@Component
public class NodeRepositoryImpl implements NodeRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LevelRepository levelRepository;

    private FullTextEntityManager fullTextEntityManager = null;

    private FullTextEntityManager getFullTextEntityManager() {
        if (fullTextEntityManager == null) {
            fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
        }
        return fullTextEntityManager;
    }

    @Override
    public List<ArrNode> findNodesByDirection(final ArrNode node,
                                              final ArrFundVersion version,
                                              final RelatedNodeDirection direction) {
        Assert.notNull(node, "JP musí být vyplněna");
        Assert.notNull(version, "Verze AS musí být vyplněna");
        Assert.notNull(direction, "Směr musí být vyplněn");


        ArrLevel level = levelRepository.findByNode(node, version.getLockChange());
        Collection<ArrLevel> levels = levelRepository.findLevelsByDirection(level, version, direction);

        List<ArrNode> nodes = new ArrayList<>(levels.size());
        levels.forEach(l -> nodes.add(l.getNode()));
        return nodes;
    }

    /*
    @Override
    public List<ArrFundToNodeList> findFundIdsByFulltext(final String text, final Collection<ArrFund> fundList) {

        Assert.notEmpty(fundList, "Nebyl vyplněn identifikátor AS");

        if (fundList.isEmpty()) {
            return Collections.emptyList();
        }

        List<Facet> facets = countItemsByFundId(text, fundList.stream().map(o -> o.getFundId()).collect(toSet()));

        return facets.stream().map(facet -> new ArrFundToNodeList(Integer.valueOf(facet.getValue()), facet.getCount())).collect(toList());
    }
    */

    @Override
    public QueryResults<ArrDescItemInfo> findFundIdsByFulltext(final String text,
                                                                 final Collection<ArrFund> fundList,
                                                                 Integer size,
                                                                 Integer offset) {
        FullTextQueryContext<ArrDescItem> ctx = new FullTextQueryContext<>(ArrDescItem.class);
        ctx.setOffset(offset);
        ctx.setPageSize(size);

        QueryBuilder queryBuilder = ctx.getQueryBuilder();
        
        Query textQuery;
        if(text!=null) {
            textQuery = createTextQuery(text, queryBuilder);
        } else {
            textQuery = null;
        }
        Query fundIdsQuery;
        if(fundList==null) {
            fundIdsQuery = null;
        } else {
            fundIdsQuery = createFundIdsQuery(fundList.stream().map(o -> o.getFundId()).collect(toList()),
                                              queryBuilder);
        }

        Query query;

        if (textQuery == null && fundIdsQuery == null) {
            query = queryBuilder.all().createQuery();
        } else {
            query = queryBuilder.bool().must(textQuery).must(fundIdsQuery).createQuery();
        }

        /*
        if(from == null) {
            from = 0;
        }
        if(max == null) {
            max = Integer.MAX_VALUE;
        }
        return findFundIdsByFulltext(text, fundList).subList(from, Math.min(from + max, fundList.size()));
        */

        List<ArrDescItemInfo> itemList = findNodeIdsByValidDescItems(null, query, ctx);

        QueryResults<ArrDescItemInfo> result = new QueryResults<>(ctx.getResultSize(), itemList);

        // mapa ( fundId -> nodeId )
        /*
        Map<Integer, Set<Integer>> fundNodeMap = itemList.stream().collect(groupingBy(
                                                                                      ArrDescItemInfo::getFundId,
                                                                                      Collectors.mapping(
                                                                                                         ArrDescItemInfo::getNodeId,
                                                                                                         toSet())));
                                                                                                         */
        /*List<ArrFundToNodeList> result = fundNodeMap.entrySet().stream().map(entry -> {
            return new ArrFundToNodeList(entry.getKey(), entry.getValue().stream().collect(toList()));
        }).collect(toList());*/
        
        return result;
    }

    /**
     * Vrátí id nodů které mají danou hodnotu v dané verzi.
     *
     * @param text The query text.
     */
    @Override
    public Set<Integer> findByFulltextAndVersionLockChangeId(final String text, final Integer fundId, final Integer lockChangeId) {

        Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");

        FullTextQueryContext<ArrDescItem> ctx = new FullTextQueryContext<>(ArrDescItem.class);

        Query descItemIdsQuery = createDescItemIdsByDataQuery(text, fundId, ctx.getQueryBuilder());

        List<ArrDescItemInfo> result = findNodeIdsByValidDescItems(lockChangeId, descItemIdsQuery, ctx);

        return result.stream().map(i -> i.getNodeId()).collect(toSet());
    }

    @Override
    public Set<Integer> findByLuceneQueryAndVersionLockChangeId(final String queryText, final Integer fundId, final Integer lockChangeId)
            throws InvalidQueryException {

        Assert.notNull(fundId, "Nebyl vyplněn identifikátor AS");

        FullTextQueryContext<ArrDescItem> ctx = new FullTextQueryContext<>(ArrDescItem.class);

        Query descItemIdsQuery = createDescItemIdsByLuceneQuery(queryText, fundId, ctx.getQueryBuilder());

        List<ArrDescItemInfo> result = findNodeIdsByValidDescItems(lockChangeId, descItemIdsQuery, ctx);

        return result.stream().map(i -> i.getNodeId()).collect(toSet());
    }

    @Override
    public Set<Integer> findBySearchParamsAndVersionLockChangeId(final List<SearchParam> searchParams, final Integer fundId,
            final Integer lockChangeId) {
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

    @Override
	public ScrollableResults findUncachedNodes() {

        // přepsáno z NOT IN z důvodu optimalizace na LEFT JOIN
		String hql = "SELECT n.nodeId FROM arr_node n LEFT JOIN arr_cached_node cn ON cn.nodeId = n.nodeId WHERE cn IS NULL";

		// get Hibernate session
		Session session = entityManager.unwrap(Session.class);
		ScrollableResults scrollableResults = session.createQuery(hql).setCacheMode(CacheMode.IGNORE)
		        .scroll(ScrollMode.FORWARD_ONLY);

		return scrollableResults;
		/*
		List<Object[]> resultList = query.getResultList();

		Map<Integer, List<Integer>> result = new HashMap<>();
		for (Object[] o : resultList) {
		    Integer fundId = ((Number)o[0]).intValue();
		    Integer nodeId = ((Number)o[1]).intValue();

		    List<Integer> nodeIds = result.get(fundId);
		    if (nodeIds == null) {
		        nodeIds = new ArrayList<>();
		        result.put(fundId, nodeIds);
		    }

		    nodeIds.add(nodeId);
		}

		return result;*/
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

        FullTextQueryContext<ArrDescItem> ctx = new FullTextQueryContext<>(ArrDescItem.class);

        Query descItemIdsQuery = createDescItemIdsByTextSearchParamsDataQuery(searchParams, fundId);

        List<ArrDescItemInfo> result = findNodeIdsByValidDescItems(lockChangeId, descItemIdsQuery, ctx);

        return result.stream().map(i -> i.getNodeId()).collect(toSet());
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

        FullTextQueryContext<ArrDescItem> ctx = new FullTextQueryContext<>(ArrDescItem.class);

        Query descItemIdsQuery = createDescItemIdsByDateSearchParamsDataQuery(searchParams, fundId);

        List<ArrDescItemInfo> result = findNodeIdsByValidDescItems(lockChangeId, descItemIdsQuery, ctx);

        return result.stream().map(i -> i.getNodeId()).collect(toSet());
    }

    /**
     * Najde id atributů vyhovující předaným parametrům.
     *
     * @param searchParams podmínky
     * @param fundId id archivního souboru
     *
     * @return id nodů
     */
    @SuppressWarnings("unchecked")
    private Query createDescItemIdsByDateSearchParamsDataQuery(final List<UnitdateSearchParam> searchParams,
                                                                      final Integer fundId) {
        Class<ArrDescItem> entityClass = ArrDescItem.class;
        QueryBuilder queryBuilder = createQueryBuilder(entityClass);

        BooleanJunction<BooleanJunction> dateBool = queryBuilder.bool();

        for (UnitdateSearchParam searchParam : searchParams) {
            String value = searchParam.getValue();
            if (StringUtils.isNotBlank(value)) {
                Query dateQuery = createDateQuery(value, searchParam.getCondition(), queryBuilder);
                dateBool.must(dateQuery);
            }
        }

        if (dateBool.isEmpty()) {
            return null;
        }

        Query fundIdQuery = queryBuilder.keyword().onField(ArrDescItem.FIELD_FUND_ID).matching(fundId).createQuery();
        return queryBuilder.bool().must(dateBool.createQuery()).must(fundIdQuery).createQuery();
    }

    /**
     * Vytvoří lucene dotaz na hledání arr_data podle datace.
     *
     * @param value datace
     * @param condition typ podmínky
     * @param queryBuilder query builder
     *
     * @return dotaz
     */
    private Query createDateQuery(final String value, final UnitdateCondition condition, final QueryBuilder queryBuilder) {
        Assert.notNull(value, "Hodnota musí být vyplněna");
        Assert.notNull(condition, "Podmínka musí být vyplněna");

        IUnitdate unitdate = new ArrDataUnitdate();
        UnitDateConvertor.convertToUnitDate(value, unitdate);

        LocalDateTime fromDate = LocalDateTime.parse(unitdate.getValueFrom(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        long secondsFrom = CalendarConverter.toSeconds(fromDate);

        LocalDateTime toDate = LocalDateTime.parse(unitdate.getValueTo(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        long secondsTo = CalendarConverter.toSeconds(toDate);

        Query query;
        switch (condition) {
            case CONTAINS:
			Query fromQuery = queryBuilder.range().onField(ArrDescItem.NORMALIZED_FROM_ATT).above(secondsFrom)
			        .createQuery();
			Query toQuery = queryBuilder.range().onField(ArrDescItem.NORMALIZED_TO_ATT).below(secondsTo).createQuery();
                query = queryBuilder.bool().must(fromQuery).must(toQuery).createQuery();
                break;
            case GE:
			query = queryBuilder.range().onField(ArrDescItem.NORMALIZED_FROM_ATT).above(secondsFrom).createQuery();
                break;
            case LE:
			query = queryBuilder.range().onField(ArrDescItem.NORMALIZED_TO_ATT).below(secondsTo).createQuery();
                break;
            default:
                throw new IllegalStateException("Neznámý typ podmínky " + condition);
        }

        return query;
    }

    /**
     * Najde id atributů vyhovující předaným parametrům.
     *
     * @param searchParams podmínky
     * @param fundId id archivního souboru
     *
     * @return id nodů
     */
    @SuppressWarnings("unchecked")
    private Query createDescItemIdsByTextSearchParamsDataQuery(final List<TextSearchParam> searchParams,
                                                               final Integer fundId) {
        Class<ArrDescItem> entityClass = ArrDescItem.class;
        QueryBuilder queryBuilder = createQueryBuilder(entityClass);

        BooleanJunction<BooleanJunction> textBool = queryBuilder.bool();

        for (TextSearchParam searchParam : searchParams) {
            String value = searchParam.getValue();
            if (StringUtils.isNotBlank(value)) {
                Query textQuery = createTextQuery(value, queryBuilder);
                textBool.must(textQuery);
            }
        }

        if (textBool.isEmpty()) {
            return null;
        }

        Query fundIdQuery = queryBuilder.keyword().onField(ArrDescItem.FIELD_FUND_ID).matching(fundId).createQuery();
        return queryBuilder.bool().must(textBool.createQuery()).must(fundIdQuery).createQuery();
    }

    /**
     * Vyhledá id atributů podle předané hodnoty. Hledá napříč archivními pomůckami a jejich verzemi.
     *
     * @param text hodnota podle které se hledá
     * @param fundId id fondu
     *
     * @return id atributů které mají danou hodnotu
     */
    private Query createDescItemIdsByDataQuery(final String text, final Integer fundId, QueryBuilder queryBuilder) {

        if (StringUtils.isBlank(text)) {
            return null;
        }

        Query textQuery = createTextQuery(text, queryBuilder);
        Query fundIdQuery = queryBuilder.keyword().onField(ArrDescItem.FIELD_FUND_ID).matching(fundId).createQuery();
        Query query = queryBuilder.bool().must(textQuery).must(fundIdQuery).createQuery();

        return query;
    }

    private Query createFundIdsQuery(Collection<Integer> fundIds, QueryBuilder queryBuilder) {
        // fundId je kodovany jako numeric, nelze pouzit matching()
        // return queryBuilder.keyword().onField("fundId").matching("(" + StringUtils.join(fundIds, " ") + ")").createQuery();
        BooleanJunction<BooleanJunction> result = queryBuilder.bool();
        for (Integer fundId : new HashSet<>(fundIds)) {
            result.should(queryBuilder.range().onField(ArrDescItem.FIELD_FUND_ID).from(fundId).to(fundId).createQuery());
        }
        return result.createQuery();
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
    private Query createDescItemIdsByLuceneQuery(final String queryText, final Integer fundId, QueryBuilder queryBuilder) {

        if (StringUtils.isBlank(queryText)) {
            return null;
        }

        StandardQueryParser parser = new StandardQueryParser();
        parser.setAllowLeadingWildcard(true);

        // Po přechodu na lucene 6.6.0 se Použije tento kód
//        HashMap<String, PointsConfig> stringNumericConfigHashMap = new HashMap<>();
//        PointsConfig intConfig = new PointsConfig(NumberFormat.getIntegerInstance(), Integer.class);
//        PointsConfig longConfig = new PointsConfig(NumberFormat.getNumberInstance(), Long.class);
//        stringNumericConfigHashMap.put("specification", intConfig);
//        stringNumericConfigHashMap.put("normalizedFrom", longConfig);
//        stringNumericConfigHashMap.put("normalizedTo", longConfig);
//        parser.setPointsConfigMap(stringNumericConfigHashMap);
        HashMap<String, NumericConfig> stringNumericConfigHashMap = new HashMap<>();
		stringNumericConfigHashMap.put(ArrDescItem.SPECIFICATION_ATT,
		        new NumericConfig(1, NumberFormat.getIntegerInstance(), FieldType.NumericType.INT));
		stringNumericConfigHashMap.put(ArrDescItem.NORMALIZED_FROM_ATT,
		        new NumericConfig(16, NumberFormat.getNumberInstance(), FieldType.NumericType.LONG));
        stringNumericConfigHashMap.put(ArrDescItem.NORMALIZED_TO_ATT,
                                       new NumericConfig(16, NumberFormat.getNumberInstance(),
                                               FieldType.NumericType.LONG));

        stringNumericConfigHashMap.put(ArrDescItem.FIELD_FUND_ID,
                new NumericConfig(32, NumberFormat.getNumberInstance(), FieldType.NumericType.INT));
        stringNumericConfigHashMap.put(ArrDescItem.FIELD_NODE_ID,
                new NumericConfig(32, NumberFormat.getNumberInstance(), FieldType.NumericType.INT));
        stringNumericConfigHashMap.put(ArrDescItem.FIELD_DESC_ITEM_TYPE_ID,
                new NumericConfig(32, NumberFormat.getNumberInstance(), FieldType.NumericType.INT));
        stringNumericConfigHashMap.put(ArrDescItem.FIELD_CREATE_CHANGE_ID,
                new NumericConfig(32, NumberFormat.getNumberInstance(), FieldType.NumericType.INT));
        stringNumericConfigHashMap.put(ArrDescItem.FIELD_DELETE_CHANGE_ID,
		        new NumericConfig(32, NumberFormat.getNumberInstance(), FieldType.NumericType.INT));

        parser.setNumericConfigMap(stringNumericConfigHashMap);

        try {

			Query textQuery = parser.parse(queryText, ArrDescItem.FULLTEXT_ATT);
			Query fundIdQuery = queryBuilder.keyword().onField(ArrDescItem.FIELD_FUND_ID).matching(fundId).createQuery();
            return queryBuilder.bool().must(textQuery).must(fundIdQuery).createQuery();

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
    private Query createTextQuery(final String text, final QueryBuilder queryBuilder) {
        if (text == null) {
            return null;
        }
        /* rozdělení zadaného výrazu podle mezer */
        String[] tokens = StringUtils.split(text.toLowerCase(), ' ');

        /* hledání výsledků pomocí AND (must) tak že každý obsahuje dané části zadaného výrazu */
        BooleanJunction<BooleanJunction> textConditions = queryBuilder.bool();
        for (String token : tokens) {
            String searchValue = "*" + token + "*";
			Query createQuery = queryBuilder.keyword().wildcard().onField(ArrDescItem.FULLTEXT_ATT)
			        .matching(searchValue).createQuery();
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
        return getFullTextEntityManager().createFullTextQuery(query, entityClass);
    }

    /**
     * Vytvoří query builder pro danou třídu.
     *
     * @param entityClass třída
     *
     * @return query builder
     */
    private QueryBuilder createQueryBuilder(final Class<?> entityClass) {
        return getFullTextEntityManager().getSearchFactory().buildQueryBuilder().forEntity(entityClass).get();
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
                                                              final Query descItemIdsQuery,
                                                              FullTextQueryContext<ArrDescItem> ctx) {

        if (descItemIdsQuery == null) {
            return Collections.emptyList();
        }

        QueryBuilder queryBuilder = ctx.getQueryBuilder();

        Query changeQuery = createChangeQuery(queryBuilder, lockChangeId);
        // Query validDescItemInVersionQuery = createValidDescItemInVersionQuery(queryBuilder);

        Query query = queryBuilder.bool()
                .must(changeQuery)
                .must(descItemIdsQuery)
                // .must(validDescItemInVersionQuery)
                .createQuery();

        FullTextQuery fullTextQuery = ctx.createFullTextQuery(query).setProjection(
                                                                                   ArrDescItem.FIELD_ITEM_ID,
                                                                                   ArrDescItem.FIELD_NODE_ID,
                                                                                   ArrDescItem.FIELD_FUND_ID,
                                                                                   ProjectionConstants.SCORE);
        if (ctx.getOffset() != null) {
            fullTextQuery.setFirstResult(ctx.getOffset());
        }
        if (ctx.getPageSize() != null) {
            fullTextQuery.setMaxResults(ctx.getPageSize());
        }

        int totalResultSize = fullTextQuery.getResultSize();
        ctx.setResultSize(totalResultSize);

        @SuppressWarnings("unchecked")
        List<Object[]> resultList = fullTextQuery.getResultList();

        return resultList.stream()
                .map(row -> new ArrDescItemInfo((Integer) row[0], (Integer) row[1], (Integer) row[2],
                        (Float) row[3]))
                .collect(toList());
    }

    /**
     * Vyhledá id atributů podle předané hodnoty. Hledá napříč archivními pomůckami a jejich verzemi.
     *
     * @param fundId id fondu
     * @param text hodnota podle které se hledá
     * @return id atributů které mají danou hodnotu
     */
    /*
    private List<Facet> countItemsByFundId(final String text, final Collection<Integer> fundIds) {
    
        if (StringUtils.isBlank(text) || CollectionUtils.isEmpty(fundIds)) {
            return Collections.emptyList();
        }
    
        FullTextQueryContext<ArrDescItem> ctx = new FullTextQueryContext<>(ArrDescItem.class);
    
        QueryBuilder queryBuilder = ctx.getQueryBuilder();
    
        Query changeQuery = createChangeQuery(queryBuilder, null);
        Query textQuery = createTextQuery(text, queryBuilder);
        Query fundIdQuery = createFundIdsQuery(fundIds, queryBuilder);
        // Query validDescItemInVersionQuery = createValidDescItemInVersionQuery(queryBuilder);
    
        Query query = queryBuilder.bool()
                .must(changeQuery)
                .must(textQuery)
                .must(fundIdQuery)
                // .must(validDescItemInVersionQuery)
                .createQuery();
    
        // FullTextQuery fullTextQuery = ctx.createFullTextQuery(query).setProjection(ArrDescItem.ITEM_ID, ArrDescItem.NODE_ID, ArrDescItem.FUND_ID);
        FullTextQuery fullTextQuery = ctx.createFullTextQuery(query).setProjection(ArrDescItem.FIELD_ITEM_ID);
    
        List<Object[]> resultList = fullTextQuery.getResultList();
    
        final String fundFacet = "fund_facet";
    
        FacetManager facetManager = fullTextQuery.getFacetManager();
    
        facetManager.enableFaceting(queryBuilder.facet().name(fundFacet).onField(ArrDescItem.FIELD_FUND_ID_STRING)
                .discrete()
                .includeZeroCounts(false)
                .orderedBy(FacetSortOrder.COUNT_DESC)
                // .maxFacetCount(20)
                .createFacetingRequest());
    
        return facetManager.getFacets(fundFacet);
    }*/

    /*
    private Query createValidDescItemInVersionQuery(QueryBuilder queryBuilder) {
        return queryBuilder.all().createQuery();
    }
    */

    /**
     * Vytvoří query pro hledání podle aktuální nebo uzavžené verze.
     *
     * @param lockChangeId id verze, může být null
     *
     * @return query
     */
    private Query createChangeQuery(final QueryBuilder queryBuilder, final Integer lockChangeId) {

        Query nullDeleteChangeQuery = queryBuilder.range().onField(ArrDescItem.FIELD_DELETE_CHANGE_ID).from(Integer.MAX_VALUE).to(Integer.MAX_VALUE).createQuery();

        if (lockChangeId == null) {
            // deleteChange is null
            return nullDeleteChangeQuery;
        }

        Query createChangeQuery = queryBuilder.range().onField(ArrDescItem.FIELD_CREATE_CHANGE_ID).below(lockChangeId).excludeLimit().createQuery();
        Query deleteChangeQuery = queryBuilder.range().onField(ArrDescItem.FIELD_DELETE_CHANGE_ID).above(lockChangeId).excludeLimit().createQuery();

        // createChangeId < lockChangeId and (deleteChange is null or deleteChange > lockChangeId)
        Query deleteQuery = queryBuilder.bool().should(nullDeleteChangeQuery).should(deleteChangeQuery).createQuery();
        return queryBuilder.bool().must(createChangeQuery).must(deleteQuery).createQuery();
    }

    @PostConstruct
    private void buildFullTextEntityManager() {
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
    }

    @Override
    public Set<Integer> findNodeIdsByFilters(final ArrFundVersion version, final List<DescItemTypeFilter> filters) {
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

            FullTextQueryContext<ArrDescItem> ctx = new FullTextQueryContext<>(ArrDescItem.class);

            Query descItemIdsQuery = createDescItemIdsQuery(descItemIds, ctx.getQueryBuilder());

            List<ArrDescItemInfo> list = findNodeIdsByValidDescItems(lockChangeId, descItemIdsQuery, ctx);

            nodeIds.addAll(list.stream().map(i -> i.getNodeId()).collect(toList()));
        }
        return nodeIds;
    }

    private Map<Integer, Set<Integer>> findDescItemIdsByFilters(final List<DescItemTypeFilter> filters, final Integer fundId, final Integer lockChangeId) {
        if (CollectionUtils.isEmpty(filters)) {
            return null;
        }

        Map<Integer, Set<Integer>> allDescItemIds = null;
        for (DescItemTypeFilter filter : filters) {
            QueryBuilder queryBuilder = createQueryBuilder(ArrDescItem.class);
            Map<Integer, Set<Integer>> nodeIdToDescItemIds = filter.resolveConditions(getFullTextEntityManager(), queryBuilder, fundId, entityManager, lockChangeId);

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

    private Query createDescItemIdsQuery(Collection<Integer> descItemIds, QueryBuilder queryBuilder) {
        try {
            // itemId jakozto ID field je v indexu ulozeny stringove, ale matching() v Hibernate Search nefunguje s hodnotami typu Integer
            // Query descItemIdsQuery = queryBuilder.keyword().onField("itemId").matching("(" + StringUtils.join(descItemIds, " ") + ")").createQuery();
            StandardQueryParser parser = new StandardQueryParser();
            return parser.parse('(' + StringUtils.join(descItemIds, ' ') + ')', ArrDescItem.FIELD_ITEM_ID);
        } catch (QueryNodeException e) {
            throw new InvalidQueryException(e);
        }
    }

    private class FullTextQueryContext<T> {

        private final Class<T> entityClass;
        private final QueryBuilder queryBuilder;

        Integer offset;
        Integer pageSize;
        Integer resultSize;

        public Integer getOffset() {
            return offset;
        }

        public void setOffset(Integer offset) {
            this.offset = offset;
        }

        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }

        public Integer getResultSize() {
            return resultSize;
        }

        public void setResultSize(Integer resultSize) {
            this.resultSize = resultSize;
        }

        /**
         * Vytvoří query builder pro danou třídu.
         *
         * @param entityClass třída
         */
        public FullTextQueryContext(Class<T> entityClass) {
            this.entityClass = entityClass;
            this.queryBuilder = createQueryBuilder(entityClass);
        }

        public Class<T> getEntityClass() {
            return entityClass;
        }

        public QueryBuilder getQueryBuilder() {
            return queryBuilder;
        }

        /**
         * Vytvoří hibernate jpa query z lucene query.
         *
         * @param query lucene qery
         * @return hibernate jpa query
         */
        public FullTextQuery createFullTextQuery(Query query) {
            return getFullTextEntityManager().createFullTextQuery(query, entityClass);
        }
    }

}
