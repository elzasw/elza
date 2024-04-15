package cz.tacr.elza.repository;

import static cz.tacr.elza.domain.ApCachedAccessPoint.DATA;
import static cz.tacr.elza.domain.ApCachedAccessPoint.FIELD_ACCESSPOINT_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBinder.SORTABLE;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.AP_TYPE_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.INDEX;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.NM_MAIN;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.NM_MINOR;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.PREFIX_PREF;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.SCOPE_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.SEPARATOR;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.STATE;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.REV_STATE;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.USERNAME;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBinder.ANALYZED; 
import static cz.tacr.elza.domain.convertor.UnitDateConvertorConsts.DEFAULT_INTERVAL_DELIMITER;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
//import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.index.Term;
//import org.apache.lucene.queryparser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.xml.QueryBuilder;
import org.apache.lucene.search.BoostQuery;
//import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.RamUsageEstimator;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.session.SearchSession;
//import org.hibernate.search.jpa.FullTextEntityManager;
//import org.hibernate.search.jpa.FullTextQuery;
//import org.hibernate.search.jpa.Search;
//import org.hibernate.search.query.dsl.BooleanJunction;
//import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.common.db.QueryResults;
import cz.tacr.elza.controller.vo.Area;
import cz.tacr.elza.controller.vo.ExtensionFilterVO;
import cz.tacr.elza.controller.vo.RelationFilterVO;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.ItemType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.RevStateApproval;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.bridge.ApCachedAccessPointBinder;
import cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.packageimport.xml.SettingIndexSearch;
import cz.tacr.elza.service.SettingsService;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class ApCachedAccessPointRepositoryImpl implements ApCachedAccessPointRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private SettingsService settingsService;

    public static final String STAR = "*";

    /**
     * Factory for fulltext query parts
     *
     */
    class FulltextCondFactory {
        final SettingIndexSearch sis;

        FulltextCondFactory(final SettingIndexSearch sis) {
            this.sis = sis;
        }

        /**
         * Return field definition
         * 
         * @param fields
         * @param name
         * @return
         */
        @Nullable
        private SettingIndexSearch.Field getFieldSearchConfigByName(String name) {
            if (sis == null || CollectionUtils.isEmpty(sis.getFields())) {
                return null;
            }
            for (SettingIndexSearch.Field field : sis.getFields()) {
                if (field.getName().equals(name)) {
                    return field;
                }
            }
            return null;
        }

        private Query parseTransQuery(String fieldName, String value) {
//            try {
//                QueryParser queryParser = new AnalyzingQueryParser(fieldName, getAnalyzer());
//                queryParser.setAllowLeadingWildcard(true);
//                return queryParser.parse(value);
//            } catch (ParseException e) {
//                return new WildcardQuery(new Term(fieldName, value));
//            }
            return null;
        }

//        private void addWildcardQuery(BooleanJunction<BooleanJunction> query, String fieldName, String value,
//                                      boolean trans, boolean exact, boolean wildcard) {
//            float boost = 1.0f;
//            SettingIndexSearch.Field fieldSearchConfig = getFieldSearchConfigByName(fieldName);
//            if (fieldSearchConfig != null && fieldSearchConfig.getBoost() != null) {
//                boost = fieldSearchConfig.getBoost();
//            }
//            String wildCardValue = value;
//
//            if (wildcard) {
//                wildCardValue = STAR + value + STAR;
//            }
//
//            query.should(new BoostQuery(new WildcardQuery(new Term(DATA + SEPARATOR + fieldName, wildCardValue)),
//                    boost));
//            if (trans) {
//                query.should(new BoostQuery(parseTransQuery(DATA + SEPARATOR + fieldName + SEPARATOR + TRANS,
//                        wildCardValue), boost));
//            }
//            if (exact) {
//                addExactQuery(query, fieldName, value, DATA + SEPARATOR);
//            }
//        }

        /**
         * 
         * @param query
         * @param fieldName
         *            Final field name
         * @param value
         */
//        private void addExactQuery(BooleanJunction<BooleanJunction> query,
//                                   String fieldName, String value, String fieldPrefix) {
//            Float boost = null;
//            Float boostTrans = null;
//
//            SettingIndexSearch.Field fieldSearchConfig = getFieldSearchConfigByName(fieldName);
//            if (fieldSearchConfig != null) {
//                boost = fieldSearchConfig.getBoostExact();
//                boostTrans = fieldSearchConfig.getBoostTransExact();
//            }
//
//            if (boostTrans != null) {
//                String fldName = (fieldPrefix != null) ? (fieldPrefix + fieldName + SEPARATOR + TRANS)
//                        : fieldName + SEPARATOR + TRANS;
//
//                String transLitValue = removeDiacritic(value);
//                query.should(new BoostQuery(new WildcardQuery(new Term(fldName, transLitValue)),
//                        boostTrans));
//            }
//
//            if (boost != null) {
//                String fldName = (fieldPrefix != null) ? (fieldPrefix + fieldName) : fieldName;
//                query.should(new BoostQuery(new WildcardQuery(new Term(fldName, value)), boost));
//            }
//        }

        /**
         * Add exact query for int value
         * 
         * @param transQuery
         * @param fieldName
         * @param accessPointId
         */
//        public void addExactQuery(BooleanJunction<BooleanJunction> transQuery, String fieldName,
//                                  int accessPointId) {
//            Query q = NumericRangeQuery.newIntRange(fieldName,
//                                                    accessPointId, accessPointId, true, true);
//
//            SettingIndexSearch.Field fieldSearchConfig = getFieldSearchConfigByName(fieldName);
//            if (fieldSearchConfig != null) {
//                Float boost = fieldSearchConfig.getBoostExact();
//                if (boost != null) {
//                    q = new BoostQuery(q, boost);
//                }
//            }
//            transQuery.should(q);
//        }
    }

//    private FullTextEntityManager getFullTextEntityManager() {
//        return Search.getFullTextEntityManager(entityManager);
//    }

    /**
     * Vytvoří query builder pro danou třídu.
     *
     * @param entityClass třída
     *
     * @return query builder
     */
//    private QueryBuilder createQueryBuilder(final Class<?> entityClass) {
//        return getFullTextEntityManager().getSearchFactory().buildQueryBuilder().forEntity(entityClass).get();
//    }

    /**
     * Vytvoří hibernate jpa query z lucene query.
     *
     * @param query lucene query
     * @return hibernate jpa query
     */
//    private FullTextQuery createFullTextQuery(Query query, final Class<?> entityClass) {
//        return getFullTextEntityManager().createFullTextQuery(query, entityClass);
//    }

//    private Analyzer getAnalyzer() {
//        return getFullTextEntityManager().getSearchFactory().getAnalyzer("cz");
//    }

    @Override
    public QueryResults<ApCachedAccessPoint> findApCachedAccessPointisByQuery(String search,
                                                                              SearchFilterVO searchFilter,
                                                                              Collection<Integer> apTypeIdTree,
                                                                              Collection<Integer> scopeIds,
                                                                              ApState.StateApproval state,
                                                                              RevStateApproval revState,
                                                                              Integer from,
                                                                              Integer count,
                                                                              StaticDataProvider sdp) {

    	SearchSession session = Search.session(entityManager);
        SearchPredicateFactory factory = session.scope(ApCachedAccessPoint.class).predicate();
        SearchPredicate predicate = buildQueryFromParams(factory, search, searchFilter, apTypeIdTree, scopeIds, state, revState);
        SearchScope<ApCachedAccessPoint> scope = session.scope(ApCachedAccessPoint.class);
//        SortField sortField = new SortField(DATA + PREFIX_PREF + INDEX + SORTABLE, SortField.Type.STRING);

		SearchResult<ApCachedAccessPoint> result = session.search(ApCachedAccessPoint.class)
                .where(predicate)
//                .sort(scope.sort().field(sortField.getField()).desc().toSort())
//                .sort(SearchSortFactory::score)
//                .sort(f -> f.composite(b -> {
//                    b.add(f.field(sortField.getField()));
//                }))
               .fetch(from, count);

			Long hitCount = result.total().hitCount();

		return new QueryResults<ApCachedAccessPoint>(hitCount.intValue(), result.hits());

//    	  QueryBuilder queryBuilder = createQueryBuilder(ApCachedAccessPoint.class);
//        Query query = buildQueryFromParams(queryBuilder, search, searchFilter, apTypeIdTree, scopeIds, state);
//        SortField[] sortFields = new SortField[2];
//        sortFields[0] = new SortField(null, SortField.Type.SCORE);
//        sortFields[1] = new SortField(DATA + SEPARATOR + PREFIX_PREF + SEPARATOR + INDEX + SEPARATOR + SORT, SortField.Type.STRING);
//
//        Sort sort = new Sort(sortFields);
//
//        FullTextQuery fullTextQuery = createFullTextQuery(query, ApCachedAccessPoint.class);
//        fullTextQuery.setFirstResult(from);
//        fullTextQuery.setMaxResults(count);
//        fullTextQuery.setSort(sort);
//
//        // todo fetch ap_access_points
//        return new QueryResults<ApCachedAccessPoint>(fullTextQuery.getResultSize(), fullTextQuery.getResultList());
    }

    private SearchPredicate buildQueryFromParams(SearchPredicateFactory factory, 
    											 String search,
    											 SearchFilterVO searchFilter,
    											 Collection<Integer> apTypeIdTree,
    											 Collection<Integer> scopeIds,
    											 ApState.StateApproval state,
    											 RevStateApproval revState) {
        BooleanPredicateClausesStep<?> bool = factory.bool();
        boolean empty = true;

		if (searchFilter != null) {
			if (StringUtils.isNotEmpty(searchFilter.getUser())) {
				bool.should(factory.wildcard().field(USERNAME).matching(STAR + searchFilter.getUser().toLowerCase() + STAR).toPredicate());
				empty = false;
			}
			if (searchFilter.getArea() != Area.ENTITY_CODE) {
				SearchPredicate sp = process(factory, searchFilter);
				if (sp != null) {
					bool.must(sp);
					empty = false;
				}
			}
		} else {
	        if (search != null) {
	        	List<String> keyWords = getKeyWordsFromSearch(search);
	        	for (String keyWord : keyWords) {
	        		bool.must(processIndexCondDef(factory, keyWord, null));
	        	}
	        	empty = false;
	        }
		}

		if (CollectionUtils.isNotEmpty(apTypeIdTree)) {
			for (Integer typeId : apTypeIdTree) {
				bool.should(factory.match().field(AP_TYPE_ID).matching(typeId.toString()));
			}
			empty = false;
		}

		if (CollectionUtils.isNotEmpty(scopeIds)) {
			for (Integer scope : scopeIds) {
				bool.should(factory.match().field(SCOPE_ID).matching(scope.toString()));
			}
			empty = false;
		}

		if (state != null) {
			bool.must(factory.match().field(STATE).matching(state.name().toLowerCase()));
        	empty = false;
		}

		if (revState != null) {
			bool.must(factory.match().field(REV_STATE).matching(revState.name().toLowerCase()));
			empty = false;
		}
		
		if (empty) {
            return factory.matchAll().toPredicate();
        }
        return bool.toPredicate();
    }

//    private Query buildQueryFromParams(QueryBuilder queryBuilder,
//                                       String search,
//                                       SearchFilterVO searchFilter,
//                                       Collection<Integer> apTypeIdTree,
//                                       Collection<Integer> scopeIds,
//                                       ApState.StateApproval state) {
//        BooleanJunction<BooleanJunction> bool = queryBuilder.bool();
//        boolean empty = true;
//
//        if (state != null) {
//            BooleanJunction<BooleanJunction> stateQuery = queryBuilder.bool();
//            stateQuery.should(new WildcardQuery(new Term(STATE, state.name().toLowerCase())));
//            bool.must(stateQuery.createQuery());
//            empty = false;
//        }
//
//        if (CollectionUtils.isNotEmpty(apTypeIdTree)) {
//            BooleanJunction<BooleanJunction> aeTypeQuery = queryBuilder.bool();
//            for (Integer apTypeId : apTypeIdTree) {
//                aeTypeQuery.should(new WildcardQuery(new Term(AP_TYPE_ID, apTypeId.toString().toLowerCase())));
//            }
//            bool.must(aeTypeQuery.createQuery());
//            empty = false;
//        }
//
//        if (CollectionUtils.isNotEmpty(scopeIds)) {
//            BooleanJunction<BooleanJunction> scopeQuery = queryBuilder.bool();
//            for (Integer scopeId : scopeIds) {
//                scopeQuery.should(new WildcardQuery(new Term(SCOPE_ID, scopeId.toString().toLowerCase())));
//            }
//            bool.must(scopeQuery.createQuery());
//            empty = false;
//        }
//
//        FulltextCondFactory fcf = new FulltextCondFactory(getElzaSearchConfig());
//
//        if (searchFilter != null) {
//            if (StringUtils.isNotEmpty(searchFilter.getCode())) {
//                Integer apId = Integer.parseInt(searchFilter.getCode());
//                NumericRangeQuery<Integer> apIdQuery = NumericRangeQuery
//                        .newIntRange(ApCachedAccessPoint.FIELD_ACCESSPOINT_ID,
//                                     apId, apId, true, true);
//                bool.must(apIdQuery);
//                empty = false;
//            }
//
//            if (StringUtils.isNotEmpty(searchFilter.getUser())) {
//                bool.must(new WildcardQuery(new Term(DATA + SEPARATOR + USERNAME, STAR + searchFilter.getUser().toLowerCase() + STAR)));
//                empty = false;
//            }
//
//            if (searchFilter.getArea() != Area.ENTITY_CODE) {
//                // prepare fulltext query
//                Query q = process(queryBuilder, searchFilter, fcf);
//                if (q != null) {
//                    bool.must(q);
//                    empty = false;
//                }
//            }
//
//        } else {
//            // prepare fulltext query
//            if (StringUtils.isNotEmpty(search)) {
//                List<String> keyWords = getKeyWordsFromSearch(search);
//                for (String keyWord : keyWords) {
//                    bool.must(processIndexCondDef(queryBuilder, keyWord, null, fcf));
//                    empty = false;
//                }
//            }
//        }
//
//        if (empty) {
//            return queryBuilder.all().createQuery();
//        }
//        return bool.createQuery();
//    }

    /**
     * Return prepared query
     * 
     * @param queryBuilder
     * @param searchFilterVO
     * @param fcf
     * @return Might return null if empty query
     */
//    @Nullable
//    private Query process(QueryBuilder queryBuilder, SearchFilterVO searchFilterVO, FulltextCondFactory fcf) {
//        StaticDataProvider sdp = staticDataService.getData();
//        String search = searchFilterVO.getSearch();
//        Area area = searchFilterVO.getArea();
//        if (area == null) {
//            area = Area.ALL_NAMES;
//        }
//        BooleanJunction<BooleanJunction> searchQuery = queryBuilder.bool();
//
//        if (StringUtils.isNotEmpty(search)) {
//            List<String> keyWords = getKeyWordsFromSearch(search);
//            RulPartType defaultPartType = sdp.getDefaultPartType();
//            for (String keyWord : keyWords) {
//                String partTypeCode;
//                boolean onlyMainPart = false;
//                switch (area) {
//                    case PREFER_NAMES:
//                        partTypeCode = PREFIX_PREF;
//                        if (searchFilterVO.getOnlyMainPart() != null && searchFilterVO.getOnlyMainPart()) {
//                            onlyMainPart = true;
//                        }
//                        break;
//                    case ALL_PARTS:
//                        partTypeCode = null;
//                        break;
//                    case ALL_NAMES:
//                        partTypeCode = defaultPartType.getCode().toLowerCase();
//                        if (searchFilterVO.getOnlyMainPart() != null && searchFilterVO.getOnlyMainPart()) {
//                            onlyMainPart = true;
//                        }
//                        break;
//                    default:
//                        throw new NotImplementedException("Neimplementovaný stav oblasti: " + area);
//                }
//                if (onlyMainPart) {
//                    searchQuery.must(processValueCondDef(sdp, queryBuilder, keyWord,
//                                                         "NM_MAIN", null, partTypeCode,
//                                                         fcf));
//                } else {
//                    searchQuery.must(processIndexCondDef(queryBuilder, keyWord, partTypeCode, fcf));
//                }
//            }
//        }
//        if (CollectionUtils.isNotEmpty(searchFilterVO.getExtFilters())) {
//            for (ExtensionFilterVO ext : searchFilterVO.getExtFilters()) {
//                Validate.notNull(ext.getItemTypeId());
//                ItemType itemType = sdp.getItemTypeById(ext.getItemTypeId());
//                RulItemSpec itemSpec;
//                if(ext.getItemSpecId() != null) {
//                    itemSpec = sdp.getItemSpecById(ext.getItemSpecId());
//                } else {
//                    itemSpec = null;
//                    if (ext.getValue() == null) {
//                        // specification nor value defined -> skip this condition
//                        // note: this is probably incorrect, exception should be thrown for invalid condition
//                        continue;
//                    }
//                }
//                String value;
//                if (ext.getValue() != null) {
//                    value = ext.getValue().toString();
//                } else {
//                    value = null;
//                }
//                searchQuery.must(processValueCondDef(queryBuilder, value,
//                                                     itemType.getEntity(), itemSpec,
//                                                     ext.getPartTypeCode().toLowerCase(), fcf));
//            }
//        }
//        if (CollectionUtils.isNotEmpty(searchFilterVO.getRelFilters())) {
//            for (RelationFilterVO rel : searchFilterVO.getRelFilters()) {
//                if (rel.getCode() != null) {
//                    Query query;
//                    if (rel.getRelTypeId() != null) {
//                        ItemType itemType = sdp.getItemTypeById(rel.getRelTypeId());
//                        RulItemSpec itemSpec;
//                        if (rel.getRelSpecId() != null) {
//                            itemSpec = sdp.getItemSpecById(rel.getRelSpecId());
//                        } else {
//                            itemSpec = null;
//                        }
//                        query = processValueCondDef(queryBuilder, rel.getCode().toString(),
//                                                    itemType.getEntity(), itemSpec, null, fcf);
//                    } else {
//                        query = processRecordRefCond(queryBuilder, rel.getCode(), fcf);
//                    }
//                    searchQuery.must(query);
//                }
//            }
//        }
//        if (StringUtils.isNotEmpty(searchFilterVO.getCreation())) {
//            ArrDataUnitdate arrDataUnitdate = UnitDateConvertor.convertToUnitDate(searchFilterVO.getCreation(), new ArrDataUnitdate());
//            String intervalCreation = arrDataUnitdate.getValueFrom() + DEFAULT_INTERVAL_DELIMITER + arrDataUnitdate.getValueTo();
//            searchQuery.must(processValueCondDef(sdp, queryBuilder, intervalCreation.toLowerCase(),
//                                                 "CRE_DATE", null,
//                                                 "PT_CRE", fcf));
//        }
//        if (StringUtils.isNotEmpty(searchFilterVO.getExtinction())) {
//            ArrDataUnitdate arrDataUnitdate = UnitDateConvertor.convertToUnitDate(searchFilterVO.getExtinction(), new ArrDataUnitdate());
//            String intervalExtinction = arrDataUnitdate.getValueFrom() + DEFAULT_INTERVAL_DELIMITER + arrDataUnitdate.getValueTo();
//            searchQuery.must(processValueCondDef(sdp, queryBuilder, intervalExtinction.toLowerCase(),
//                                                 "EXT_DATE", null,
//                                                 "PT_EXT", fcf));
//        }
//
//        if (searchQuery.isEmpty()) {
//            return null;
//        } else {
//            return searchQuery.createQuery();
//        }
//    }

    @Nullable
    private SearchPredicate process(SearchPredicateFactory factory, SearchFilterVO searchFilterVO) {
    	StaticDataProvider sdp = staticDataService.getData();
    	String search = searchFilterVO.getSearch();
    	Area area = searchFilterVO.getArea();
    	if (area == null) {
    		area = Area.ALL_NAMES;
    	}
    	BooleanPredicateClausesStep<?> bool = factory.bool();
    	boolean empty = true;

    	if (StringUtils.isNotEmpty(search)) {
    		RulPartType defaultPartType = sdp.getDefaultPartType();
    		List<String> keyWords = getKeyWordsFromSearch(search);
    		for (String keyWord : keyWords) {
    			String partTypeCode;
    			boolean onlyMainPart = false;
    			switch (area) {
                  case PREFER_NAMES:
                      partTypeCode = PREFIX_PREF;
                      if (searchFilterVO.getOnlyMainPart() != null && searchFilterVO.getOnlyMainPart()) {
                          onlyMainPart = true;
                      }
                      break;
                  case ALL_PARTS:
                      partTypeCode = null;
                      break;
                  case ALL_NAMES:
                      partTypeCode = defaultPartType.getCode().toLowerCase();
                      if (searchFilterVO.getOnlyMainPart() != null && searchFilterVO.getOnlyMainPart()) {
                          onlyMainPart = true;
                      }
                      break;
                  default:
                      throw new NotImplementedException("Neimplementovaný stav oblasti: " + area);
    			}
    			if (onlyMainPart) {
    				bool.must(processValueCondDef(sdp, factory, keyWord, NM_MAIN.toUpperCase(), null, partTypeCode));
    			} else {
    				bool.must(processIndexCondDef(factory, keyWord, partTypeCode));
    			}
    		}
    		empty = false;
    	}
    	if (CollectionUtils.isNotEmpty(searchFilterVO.getExtFilters())) {
    		// TODO
    	}
    	if (CollectionUtils.isNotEmpty(searchFilterVO.getRelFilters())) {
    		// TODO
    	}
    	if (StringUtils.isNotEmpty(searchFilterVO.getCreation())) {
    		// TODO
    	}
    	if (StringUtils.isNotEmpty(searchFilterVO.getExtinction())) {
    		// TODO
    	}

    	if (empty) {
    		return null;
    	}
    	return bool.toPredicate();
    }

    private SearchPredicate processValueCondDef(StaticDataProvider sdp, SearchPredicateFactory factory, String value,
    											String itemTypeCode, String itemSpecCode, String partTypeCode) {
    	Objects.requireNonNull(itemTypeCode);

    	RulItemType itemType = sdp.getItemType(itemTypeCode);
    	RulItemSpec itemSpec = null;
    	if (itemSpecCode != null) {
    		itemSpec = sdp.getItemSpec(itemSpecCode);
    	}

    	return processValueCondDef(factory, value, itemType, itemSpec, partTypeCode);
	}

	private SearchPredicate processValueCondDef(SearchPredicateFactory factory, String value,
												RulItemType itemType, RulItemSpec itemSpec, String partTypeCode) {
		if (itemType == null) {
			throw new SystemException("Missing itemType", BaseCode.INVALID_STATE);
		}

		BooleanPredicateClausesStep<?> bool = factory.bool();
		String fieldName = "";
		if (StringUtils.isNotEmpty(partTypeCode)) {
			if (partTypeCode.equals(PREFIX_PREF)) {
				fieldName = PREFIX_PREF + SEPARATOR;
			}
		}
		fieldName += itemType.getCode().toLowerCase();

        DataType dataType = DataType.fromId(itemType.getDataTypeId());
        boolean wildcard = !(dataType == DataType.INT || dataType == DataType.RECORD_REF || dataType == DataType.BIT);

        if (itemSpec != null) {
            String fieldSpecName = fieldName + SEPARATOR + itemSpec.getCode();

            if (value == null) {
                value = itemSpec.getCode();
                //bool.must(new WildcardQuery(new Term(DATA + SEPARATOR + fieldSpecName.toString().toLowerCase(), value.toLowerCase())));
            } else {
                if (StringUtils.isEmpty(partTypeCode) || !partTypeCode.equals(PREFIX_PREF)) {
                    //boost o preferovaný item
                    //fcf.addWildcardQuery(valueQuery, PREFIX_PREF + SEPARATOR + itemType.getCode().toLowerCase()
                    //        + SEPARATOR +
                    //        itemSpec.getCode().toLowerCase(), value.toLowerCase(), true, true, wildcard);
                }

                //item
                //BooleanJunction<BooleanJunction> transQuery = queryBuilder.bool();
                //transQuery.minimumShouldMatchNumber(1);
                //fcf.addWildcardQuery(transQuery, fieldSpecName.toString().toLowerCase(), value.toLowerCase(), true, false, wildcard);

                //valueQuery.must(transQuery.createQuery());
                //fcf.addExactQuery(valueQuery, fieldSpecName.toString().toLowerCase(), value.toLowerCase(), DATA + SEPARATOR);
            }

        } else {
            if (StringUtils.isEmpty(partTypeCode) || !partTypeCode.equals(PREFIX_PREF)) {
                //boost o preferovaný item
                //fcf.addWildcardQuery(valueQuery, PREFIX_PREF + SEPARATOR + itemType.getCode().toLowerCase(),
                //                     value.toLowerCase(), true, true, wildcard);
            }

            //item
            //BooleanJunction<BooleanJunction> transQuery = queryBuilder.bool();
            //transQuery.minimumShouldMatchNumber(1);
            //fcf.addWildcardQuery(transQuery, fieldName.toString().toLowerCase(), value.toLowerCase(), true, false, wildcard);

            //valueQuery.must(transQuery.createQuery());
            //fcf.addExactQuery(valueQuery, fieldName.toString().toLowerCase(), value.toLowerCase(), DATA + SEPARATOR);
        }

        return bool.toPredicate();
	}
    
    private SearchPredicate processIndexCondDef(SearchPredicateFactory factory, String value, String partTypeCode) {
        BooleanPredicateClausesStep<?> bool = factory.bool();

        String fieldName = "";
        String itemFieldName = "";
        if (StringUtils.isNotEmpty(partTypeCode)) {
        	fieldName = partTypeCode;
        	if (partTypeCode.equals(PREFIX_PREF)) {
                itemFieldName = partTypeCode + SEPARATOR;
            }
        }

        // boost o accessPointId
        boostExactQuery(factory, bool, FIELD_ACCESSPOINT_ID, value, false); 

        if (StringUtils.isEmpty(partTypeCode) || !partTypeCode.equals(PREFIX_PREF)) {
            // boost o preferované indexi a jména
	        boostWildcardQuery(factory, bool, PREFIX_PREF + INDEX, value, true, true);
	        boostWildcardQuery(factory, bool, PREFIX_PREF + SEPARATOR + NM_MAIN, value, true, true);
	        boostWildcardQuery(factory, bool, PREFIX_PREF + SEPARATOR + NM_MINOR, value, true, true);
        }

        // boost hlavního a minor jména
        boostWildcardQuery(factory, bool, itemFieldName + NM_MAIN, value, true, true);
        boostWildcardQuery(factory, bool, itemFieldName + NM_MINOR, value, true, true);

        // index
        fieldName += INDEX;
        boostWildcardQuery(factory, bool, fieldName, value, true, false);
        boostExactQuery(factory, bool, fieldName, value, true);

        return bool.toPredicate();
    }

    private void boostWildcardQuery(SearchPredicateFactory factory, BooleanPredicateClausesStep<?> step, String fieldName, String value, boolean trans, boolean exact) {
    	float boost = 1.0f;
    	Float boostExact = null;
    	Float boostTransExact = null;
    	SettingIndexSearch.Field sisField = getFieldSearchConfigByName(fieldName);
    	if (sisField != null && sisField.getBoost() != null) {
    		boost = sisField.getBoost();
    		boostExact = sisField.getBoostExact();
    		boostTransExact = sisField.getBoostTransExact();
    	}
    	String wildCardValue = STAR + value.toLowerCase() + STAR;

    	step.should(factory.wildcard().field(addPrefix(fieldName)).matching(wildCardValue).boost(boost).toPredicate());
    	if (trans) {
    		step.should(factory.wildcard().field(addPrefix(fieldName) + ANALYZED).matching(wildCardValue).boost(boost).toPredicate());
    	}
    	if (exact) {
    		boostExactQuery(factory, step, fieldName, value, boostExact, boostTransExact);
    	}
    }

    private void boostExactQuery(SearchPredicateFactory factory, BooleanPredicateClausesStep<?> step, String fieldName, String value, Float boostExact, Float boostTransExact) {
    	if (boostExact != null) {
    		step.should(factory.wildcard().field(addPrefix(fieldName)).boost(boostExact).matching(value).toPredicate());
    	}
    	if (boostTransExact != null) {
    		step.should(factory.wildcard().field(addPrefix(fieldName) + ANALYZED).boost(boostTransExact).matching(value).toPredicate());
    	}
    }

//    private void boostExactQuery(SearchPredicateFactory factory, BooleanPredicateClausesStep<?> step, String fieldName, String value) {
//    	SettingIndexSearch.Field sisField = getFieldSearchConfigByName(fieldName);
//    	if (sisField != null) { 
//    		Float boostExact = sisField.getBoostExact();
//    		if (boostExact != null) {
//    			step.should(factory.match().field(fieldName).boost(boostExact).matching(value).toPredicate());
//    		}
//    	}
//    }

    private void boostExactQuery(SearchPredicateFactory factory, BooleanPredicateClausesStep<?> step, String fieldName, String value, boolean prefix) {
    	SettingIndexSearch.Field sisField = getFieldSearchConfigByName(fieldName);
    	if (sisField != null) { 
    		Float boostExact = sisField.getBoostExact();
    		if (boostExact != null) {
    			step.should(factory.wildcard().field(addPrefix(fieldName, prefix)).boost(boostExact).matching(value).toPredicate());
    		}
            Float boostTransExact = sisField.getBoostTransExact();
            if (boostTransExact != null) {
            	step.should(factory.wildcard().field(addPrefix(fieldName, prefix) + ANALYZED).boost(boostTransExact).matching(value).toPredicate());
            }
    	}
    }

    private static String addPrefix(String fieldName, boolean prefix) {
    	if (prefix) {
    		return addPrefix(fieldName);
    	}
    	return fieldName;
    }

	private static String addPrefix(String fieldName) {
    	return fieldName.startsWith("_")? DATA + fieldName : DATA + SEPARATOR + fieldName;
    }

//    private Query processIndexCondDef(QueryBuilder queryBuilder, String value, String partTypeCode,
//                                      FulltextCondFactory fcf) {
//        String valueLowerCase = value.toLowerCase();
//
//        BooleanJunction<BooleanJunction> indexQuery = queryBuilder.bool();
//        StringBuilder fieldName = new StringBuilder();
//        StringBuilder itemFieldName = new StringBuilder();
//        if (StringUtils.isNotEmpty(partTypeCode)) {
//            fieldName.append(partTypeCode).append(SEPARATOR);
//
//            if (partTypeCode.equals(PREFIX_PREF)) {
//                itemFieldName.append(partTypeCode).append(SEPARATOR);
//            }
//        }
//
//        if (StringUtils.isEmpty(partTypeCode) || !partTypeCode.equals(PREFIX_PREF)) {
//            // boost o preferované indexi a jména
//            fcf.addWildcardQuery(indexQuery, PREFIX_PREF + SEPARATOR + INDEX, valueLowerCase, true, true, true);
//            fcf.addWildcardQuery(indexQuery, PREFIX_PREF + SEPARATOR + NM_MAIN, valueLowerCase, true, true, true);
//            fcf.addWildcardQuery(indexQuery, PREFIX_PREF + SEPARATOR + NM_MINOR, valueLowerCase, true, true, true);
//        }
//        fieldName.append(INDEX);
//
//        //boost hlavního jména
//        fcf.addWildcardQuery(indexQuery, itemFieldName.toString().toLowerCase() + NM_MAIN, valueLowerCase, true, true, true);
//
//        //boost minor jména
//        fcf.addWildcardQuery(indexQuery, itemFieldName.toString().toLowerCase() + NM_MINOR, valueLowerCase, true, true, true);
//
//        //index
//        BooleanJunction<BooleanJunction> transQuery = queryBuilder.bool();
//        transQuery.minimumShouldMatchNumber(1);
//        fcf.addWildcardQuery(transQuery, fieldName.toString().toLowerCase(), valueLowerCase, true, false, true);
//        try {
//            // accessPointId
//            int accessPointId = Integer.parseInt(valueLowerCase);
//            fcf.addExactQuery(transQuery, ApCachedAccessPoint.FIELD_ACCESSPOINT_ID, accessPointId);
//
//        } catch (Exception e) {
//
//        }
//
//        indexQuery.must(transQuery.createQuery());
//        fcf.addExactQuery(indexQuery, fieldName.toString().toLowerCase(), valueLowerCase, DATA + SEPARATOR);
//        return indexQuery.createQuery();
//    }

//    private Query processValueCondDef(StaticDataProvider sdp,
//                                      QueryBuilder queryBuilder, String value,
//                                      String itemTypeCode, String itemSpecCode,
//                                      String partTypeCode, FulltextCondFactory fcf) {
//        Validate.notNull(itemTypeCode);
//
//        RulItemType itemType = sdp.getItemType(itemTypeCode);
//
//        RulItemSpec itemSpec;
//        if (itemSpecCode != null) {
//            itemSpec = sdp.getItemSpec(itemSpecCode);
//        } else {
//            itemSpec = null;
//        }
//
//        return processValueCondDef(queryBuilder, value, itemType, itemSpec, partTypeCode, fcf);
//    }

//    private Query processRecordRefCond(QueryBuilder queryBuilder, Integer code, FulltextCondFactory fcf) {
//
//        BooleanJunction<BooleanJunction> valueQuery = queryBuilder.bool();
//
//        // add boost if needed
//        fcf.addExactQuery(valueQuery, ApCachedAccessPointClassBridge.REL_AP_ID.toLowerCase(), code);
//
//        return valueQuery.createQuery();
//    }

//    private Query processValueCondDef(QueryBuilder queryBuilder, String value,
//                                      RulItemType itemType, RulItemSpec itemSpec,
//                                      String partTypeCode, FulltextCondFactory fcf) {
//        if (itemType == null) {
//            throw new SystemException("Missing itemType", BaseCode.INVALID_STATE);
//        }
//
//        BooleanJunction<BooleanJunction> valueQuery = queryBuilder.bool();
//        StringBuilder fieldName = new StringBuilder();
//        if (StringUtils.isNotEmpty(partTypeCode)) {
//            if (partTypeCode.equals(PREFIX_PREF)) {
//                fieldName.append(PREFIX_PREF).append(SEPARATOR);
//            }
//        }
//        fieldName.append(itemType.getCode());
//
//        DataType dataType = DataType.fromId(itemType.getDataTypeId());
//        boolean wildcard = !(dataType == DataType.INT || dataType == DataType.RECORD_REF || dataType == DataType.BIT);
//
//        if (itemSpec != null) {
//            StringBuilder fieldSpecName = new StringBuilder(fieldName.toString());
//            fieldSpecName.append(SEPARATOR).append(itemSpec.getCode());
//
//            if (value == null) {
//                value = itemSpec.getCode();
//                valueQuery.must(new WildcardQuery(new Term(DATA + SEPARATOR + fieldSpecName.toString().toLowerCase(), value.toLowerCase())));
//            } else {
//                if (StringUtils.isEmpty(partTypeCode) || !partTypeCode.equals(PREFIX_PREF)) {
//                    //boost o preferovaný item
//                    fcf.addWildcardQuery(valueQuery, PREFIX_PREF + SEPARATOR + itemType.getCode().toLowerCase()
//                            + SEPARATOR +
//                            itemSpec.getCode().toLowerCase(), value.toLowerCase(), true, true, wildcard);
//                }
//
//                //item
//                BooleanJunction<BooleanJunction> transQuery = queryBuilder.bool();
//                transQuery.minimumShouldMatchNumber(1);
//                fcf.addWildcardQuery(transQuery, fieldSpecName.toString().toLowerCase(), value.toLowerCase(), true,
//                                     false, wildcard);
//
//                valueQuery.must(transQuery.createQuery());
//                fcf.addExactQuery(valueQuery, fieldSpecName.toString().toLowerCase(), value.toLowerCase(), DATA
//                        + SEPARATOR);
//            }
//
//        } else {
//            if (StringUtils.isEmpty(partTypeCode) || !partTypeCode.equals(PREFIX_PREF)) {
//                //boost o preferovaný item
//                fcf.addWildcardQuery(valueQuery, PREFIX_PREF + SEPARATOR + itemType.getCode().toLowerCase(),
//                                     value.toLowerCase(), true, true, wildcard);
//            }
//
//            //item
//            BooleanJunction<BooleanJunction> transQuery = queryBuilder.bool();
//            transQuery.minimumShouldMatchNumber(1);
//            fcf.addWildcardQuery(transQuery, fieldName.toString().toLowerCase(), value.toLowerCase(), true, false, wildcard);
//
//            valueQuery.must(transQuery.createQuery());
//            fcf.addExactQuery(valueQuery, fieldName.toString().toLowerCase(), value.toLowerCase(), DATA + SEPARATOR);
//        }
//
//        return valueQuery.createQuery();
//    }

    static private String removeDiacritic(String value) {
        char[] chars = new char[512];
        final int maxSizeNeeded = 4 * value.length();
        if (chars.length < maxSizeNeeded) {
            chars = new char[ArrayUtil.oversize(maxSizeNeeded, RamUsageEstimator.QUERY_DEFAULT_RAM_BYTES_USED)];
        }
        ASCIIFoldingFilter.foldToASCII(value.toCharArray(), 0, chars, 0, value.length());

        return String.valueOf(chars).trim();
    }

    /**
     * Return field definition
     * 
     * @param fields
     * @param name
     * @return
     */
    @Nullable
    private SettingIndexSearch.Field getFieldSearchConfigByName(String name) {
    	SettingIndexSearch sis = getElzaSearchConfig();
        if (sis == null || CollectionUtils.isEmpty(sis.getFields())) {
            return null;
        }
        if (name.startsWith(DATA + SEPARATOR)) {
        	name = name.substring((DATA + SEPARATOR).length());
        }
        if (name.startsWith(SEPARATOR)) {
        	name = name.substring(SEPARATOR.length());
        }
        for (SettingIndexSearch.Field field : sis.getFields()) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        return null;
    }
    
    @Nullable
    private SettingIndexSearch getElzaSearchConfig() {
        UISettings.SettingsType indexSearch = UISettings.SettingsType.INDEX_SEARCH;
        List<UISettings> uiSettings = settingsService.getGlobalSettings(indexSearch.toString(), indexSearch.getEntityType());
        if (CollectionUtils.isNotEmpty(uiSettings)) {
            return SettingIndexSearch.newInstance(uiSettings.get(0));
        }
        return null;
    }

    private List<String> getKeyWordsFromSearch(String search) {
        List<String> keyWords = new ArrayList<>();
        Pattern pattern = Pattern.compile("[^\\s,;\"]+|\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(search);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                keyWords.add(matcher.group(1));
            } else {
                keyWords.add(matcher.group());
            }
        }
        return keyWords;
    }
}
