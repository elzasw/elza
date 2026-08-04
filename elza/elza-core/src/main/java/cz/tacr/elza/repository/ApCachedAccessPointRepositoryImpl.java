package cz.tacr.elza.repository;

import static cz.tacr.elza.domain.ApCachedAccessPoint.DATA;
import static cz.tacr.elza.domain.ApCachedAccessPoint.FIELD_ACCESSPOINT_ID;
import static cz.tacr.elza.domain.ArrDescItem.NORM_FROM;
import static cz.tacr.elza.domain.ArrDescItem.NORM_TO;
import static cz.tacr.elza.domain.ArrDescItem.REL_AP_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.AP_TYPE_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.INDEX;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.NM_MAIN;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.NM_MINOR;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.PREFIX_PREF;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.SCOPE_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.SEPARATOR;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.STATE;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.VALIDATION_RESULT;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.REV_STATE;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.USERNAME;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBridge.ASSIGNED_TO;
import static cz.tacr.elza.domain.bridge.LuceneAnalyzerConfigurer.ANALYZED;
import static cz.tacr.elza.domain.bridge.LuceneAnalyzerConfigurer.SORTABLE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.common.db.QueryResults;
import cz.tacr.elza.controller.vo.ApSearchArea;
import cz.tacr.elza.controller.vo.ApSearchByItemWithValue;
import cz.tacr.elza.controller.vo.ApSearchByRelation;
import cz.tacr.elza.controller.vo.ApAdvanceSearchFilter;
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
import cz.tacr.elza.domain.converter.UnitDateConverter;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.ArrangementCode;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.packageimport.xml.SettingIndexSearch;
import cz.tacr.elza.service.SettingsService;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class ApCachedAccessPointRepositoryImpl implements ApCachedAccessPointRepositoryCustom {

    private static final Logger log = LoggerFactory.getLogger(ApCachedAccessPointRepositoryImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private SettingsService settingsService;

    public static final String STAR = "*";

    private static final String DATA_CRE_DATE = DATA + SEPARATOR + "cre_date";
    private static final String DATA_EXT_DATE = DATA + SEPARATOR + "ext_date";

    @Override
    public QueryResults<ApCachedAccessPoint> findApCachedAccessPointisByQuery(String search,
                                                                              ApAdvanceSearchFilter searchFilter,
                                                                              Collection<Integer> apTypeIdTree,
                                                                              Collection<Integer> scopeIds,
                                                                              Collection<ApState.StateApproval> states,
                                                                              RevStateApproval revState,
                                                                              Integer from,
                                                                              Integer count,
                                                                              StaticDataProvider sdp) {
    	SearchSession session = Search.session(entityManager);
        SearchPredicateFactory factory = session.scope(ApCachedAccessPoint.class).predicate();

        if (log.isTraceEnabled()) {
            log.trace("Search query params: search='{}', apTypeIdTree={}, scopeIds={}, states={}, revState={}, from={}, count={}",
                      search, apTypeIdTree, scopeIds, states, revState, from, count);
            logSearchConfig();
        }

        SearchPredicate predicate = buildQueryFromParams(factory, search, searchFilter, apTypeIdTree, scopeIds, states, revState);

		SearchResult<ApCachedAccessPoint> result = session.search(ApCachedAccessPoint.class)
				.where(predicate)
                .sort(f -> f.composite(b -> {
                    b.add(f.score());
                    b.add(f.field(DATA + SEPARATOR + PREFIX_PREF + SEPARATOR + INDEX + SORTABLE).asc());
                }))
                .fetch(from, count);

		// počet všech záznamů dle podmínky
		// pozor: pokud to nefunguje správně, musíme znovu vygenerovat indexové soubory /lucene/indexes
		Long hitCount = result.total().hitCount();

		if (log.isTraceEnabled()) {
		    log.trace("Search results: totalHitCount={}, returnedHits={}", hitCount, result.hits().size());
		    // Fetch scores for up to first 100 results using score projection
		    try {
		        int scoreLimit = Math.min(100, hitCount.intValue());
		        if (scoreLimit > 0) {
		            SearchResult<List<?>> scoreResult = session.search(ApCachedAccessPoint.class)
		                .select(f -> f.composite().from(f.score(), f.id()).asList())
		                .where(predicate)
		                .sort(f -> f.composite(b -> {
		                    b.add(f.score());
		                    b.add(f.field(DATA + SEPARATOR + PREFIX_PREF + SEPARATOR + INDEX + SORTABLE).asc());
		                }))
		                .fetch(0, scoreLimit);
		            StringBuilder sb = new StringBuilder("Score details (rank: entityId=score):\n");
		            int rank = 1;
		            for (List<?> hit : scoreResult.hits()) {
		                Float score = (Float) hit.get(0);
		                Object entityId = hit.get(1);
		                sb.append(String.format("  %3d: id=%-10s score=%.6f%n", rank++, entityId, score));
		            }
		            log.trace(sb.toString());
		        }
		    } catch (Exception e) {
		        log.trace("Failed to fetch score details for debugging", e);
		    }
		}

		return new QueryResults<ApCachedAccessPoint>(hitCount.intValue(), result.hits());
    }

    private SearchPredicate buildQueryFromParams(SearchPredicateFactory factory,
    											 String search,
    											 ApAdvanceSearchFilter searchFilter,
    											 Collection<Integer> apTypeIdTree,
    											 Collection<Integer> scopeIds,
    											 Collection<ApState.StateApproval> states,
    											 RevStateApproval revState) {
        BooleanPredicateClausesStep<?> bool = factory.bool();

		if (searchFilter != null) {
			// vyhledávání podle ID entity
			if (StringUtils.isNotEmpty(searchFilter.getCode())) {
				bool.must(factory.match().field(FIELD_ACCESSPOINT_ID).matching(searchFilter.getCode()));
			}
			// vyhledávání podle uživatelského jména, které provedl poslední změnu stavu
			if (StringUtils.isNotEmpty(searchFilter.getUser())) {
				bool.must(factory.wildcard().field(USERNAME).matching(wildcardValue(searchFilter.getUser())));
			}
			// vyhledávání podle ID přiřazeného uživatele
			if (searchFilter.getAssignedTo() != null) {
				bool.must(factory.match().field(ASSIGNED_TO).matching(searchFilter.getAssignedTo()));
			}
			// vyhledávání podle výsledků validace: ok | error
			if (searchFilter.getValidationResult() != null) {
				bool.must(factory.match().field(VALIDATION_RESULT).matching(searchFilter.getValidationResult()));
			}
			if (searchFilter.getArea() != ApSearchArea.ENTITY_CODE) {
				SearchPredicate sp = process(factory, searchFilter);
				if (sp != null) {
					bool.must(sp);
				}
			}
		} else {
	        if (search != null) {
	        	List<String> keyWords = getKeyWordsFromSearch(search);
	        	for (String keyWord : keyWords) {
	        		bool.must(processIndexCondDef(factory, keyWord, null));
	        	}
	        	// BM25 boost for full search string on analyzed fields
	        	// Unlike wildcard queries (constant score), match() uses BM25 with field-length
	        	// normalization - shorter preferred names containing all search terms score higher.
	        	// This ensures entities whose name closely matches the search rank above
	        	// sub-entities with longer names.
	        	addFullTextBoost(factory, bool, search, null);
	        }
		}

		if (CollectionUtils.isNotEmpty(apTypeIdTree)) {
			BooleanPredicateClausesStep<?> aeTypeBool = factory.bool();
			for (Integer typeId : apTypeIdTree) {
				aeTypeBool.should(factory.match().field(AP_TYPE_ID).matching(typeId.toString()));
			}
			bool.must(aeTypeBool);
		}

		if (CollectionUtils.isNotEmpty(scopeIds)) {
			BooleanPredicateClausesStep<?> scopeBool = factory.bool();
			for (Integer scope : scopeIds) {
				scopeBool.should(factory.match().field(SCOPE_ID).matching(scope.toString()));
			}
			bool.must(scopeBool);
		}

		if (CollectionUtils.isNotEmpty(states)) {
			BooleanPredicateClausesStep<?> stateBool = factory.bool();
			for (ApState.StateApproval state : states) {
				stateBool.should(factory.match().field(STATE).matching(state.name().toLowerCase()));
			}
			bool.must(stateBool);
		}

		if (revState != null) {
			bool.must(factory.match().field(REV_STATE).matching(revState.name().toLowerCase()));
		}
		
		if (!bool.hasClause()) {
            return factory.matchAll().toPredicate();
        }
        return bool.toPredicate();
    }

    /**
     * Return prepared predicate
     * 
     * @param factory
     * @param searchFilterVO
     * @return null if BooleanPredicateClausesStep has no Clause
     */
    @Nullable
    private SearchPredicate process(SearchPredicateFactory factory, ApAdvanceSearchFilter searchFilterVO) {
    	StaticDataProvider sdp = staticDataService.getData();
    	String search = searchFilterVO.getSearch();
    	ApSearchArea area = searchFilterVO.getArea();
    	if (area == null) {
    		area = ApSearchArea.ALL_NAMES;
    	}
    	BooleanPredicateClausesStep<?> bool = factory.bool();

    	if (StringUtils.isNotEmpty(search)) {
    		boolean onlyMainPart = (searchFilterVO.getOnlyMainPart() != null && searchFilterVO.getOnlyMainPart());
    		RulPartType defaultPartType = sdp.getDefaultPartType();
    		List<String> keyWords = getKeyWordsFromSearch(search);
    		String partTypeCode = null;
    		for (String keyWord : keyWords) {
    			switch (area) {
                  case PREFER_NAMES:
                      partTypeCode = PREFIX_PREF;
                      break;
                  case ALL_PARTS:
                	  // s takovou volbou zaškrtávací políčko onlyMainPart ignorujeme
                      onlyMainPart = false;
                      partTypeCode = null;
                      break;
                  case ALL_NAMES:
                      partTypeCode = defaultPartType.getCode().toLowerCase();
                      break;
                  default:
                      throw new NotImplementedException("Neimplementovaný stav oblasti: " + area);
    			}
    			if (onlyMainPart) {
    				bool.must(processValueCondDef(factory, keyWord, sdp.getItemType(NM_MAIN.toUpperCase()), null, area == ApSearchArea.PREFER_NAMES));
    			} else {
    				bool.must(processIndexCondDef(factory, keyWord, partTypeCode));
    			}
    		}
    		// BM25 boost for full search string - see addFullTextBoost
    		if (!onlyMainPart) {
    		    addFullTextBoost(factory, bool, search, partTypeCode);
    		}
    	}
    	if (CollectionUtils.isNotEmpty(searchFilterVO.getExtFilters())) {
    		for (ApSearchByItemWithValue ext : searchFilterVO.getExtFilters()) {
    			Objects.requireNonNull(ext.getItemTypeId());
    			RulItemType itemType = sdp.getItemType(ext.getItemTypeId());
    			// ext.getValue() is JsonNullable<Object>; unwrap to a plain nullable value
    			Object rawValue = ext.getValue() != null && ext.getValue().isPresent() ? ext.getValue().get() : null;
    			RulItemSpec itemSpec;
    			if(ext.getItemSpecId() != null) {
    				itemSpec = sdp.getItemSpecById(ext.getItemSpecId());
    			} else {
    				itemSpec = null;
    				if (rawValue == null) {
    					// specification nor value defined -> skip this condition
    					// note: this is probably incorrect, exception should be thrown for invalid condition
    					continue;
    				}
    			}
    			String value = rawValue != null ? rawValue.toString() : null;
    			// nelze limitovat cast v niz se hleda
    			if(StringUtils.isNotEmpty(ext.getPartTypeCode()) && value != null) {
    				throw new BusinessException("Vyhledávací dotaz Lucene nelze omezit na typ ApPart", ArrangementCode.REQUEST_INVALID_STATE)
    					.set("partTypeCode", ext.getPartTypeCode());
    			}
    			bool.must(processValueCondDef(factory, value, itemType, itemSpec, false));
    		}
    	}
    	if (CollectionUtils.isNotEmpty(searchFilterVO.getRelFilters())) {
    		for (ApSearchByRelation rel : searchFilterVO.getRelFilters()) {
    			if (rel.getCode() != null) {
    				BooleanPredicateClausesStep<?> relPred = factory.bool();
    				if (rel.getRelTypeId() != null) {
    					RulItemType itemType = sdp.getItemType(rel.getRelTypeId());
    					RulItemSpec itemSpec;
    					if (rel.getRelSpecId() != null) {
    						itemSpec = sdp.getItemSpecById(rel.getRelSpecId());
    					} else {
    						itemSpec = null;
    					}
    					relPred.should(processValueCondDef(factory, rel.getCode().toString(), itemType, itemSpec, false));
    				} else {
    					relPred.should(factory.match().field(REL_AP_ID).matching(rel.getCode()));
    				}
    				bool.must(relPred);
    			}
    		}
    	}
    	if (StringUtils.isNotEmpty(searchFilterVO.getCreation())) {
    		ArrDataUnitdate creDate = UnitDateConverter.convertToUnitDate(searchFilterVO.getCreation(), new ArrDataUnitdate());
    		bool.must(factory.range().field(DATA_CRE_DATE + SEPARATOR + NORM_FROM).atMost(creDate.getNormalizedFrom()))
    			.must(factory.range().field(DATA_CRE_DATE + SEPARATOR + NORM_TO).atLeast(creDate.getNormalizedTo()));
    	}
    	if (StringUtils.isNotEmpty(searchFilterVO.getExtinction())) {
    		ArrDataUnitdate extDate = UnitDateConverter.convertToUnitDate(searchFilterVO.getExtinction(), new ArrDataUnitdate());
    		bool.must(factory.range().field(DATA_EXT_DATE + SEPARATOR + NORM_FROM).atMost(extDate.getNormalizedFrom()))
    			.must(factory.range().field(DATA_EXT_DATE + SEPARATOR + NORM_TO).atLeast(extDate.getNormalizedTo()));
    	}

    	if (!bool.hasClause()) {
    		return null;
    	}
    	return bool.toPredicate();
    }

	private SearchPredicate processValueCondDef(SearchPredicateFactory factory, 
												String value,
												RulItemType itemType, 
												RulItemSpec itemSpec, 
												boolean onlyPrefPart) {
		if (itemType == null) {
			throw new SystemException("Missing itemType", BaseCode.INVALID_STATE);
		}

		BooleanPredicateClausesStep<?> bool = factory.bool();
		String fieldName = "";
		if (onlyPrefPart) {
			fieldName = PREFIX_PREF + SEPARATOR;
		}
		fieldName += itemType.getCode().toLowerCase();
		String itemTypeCode = itemType.getCode().toLowerCase();

        if (itemSpec != null) {
        	String itemSpecCode = itemSpec.getCode().toLowerCase();
            fieldName += SEPARATOR + itemSpecCode;

            if (value == null) {
                value = itemSpec.getCode().toLowerCase();
                bool.should(factory.match().field(addDataPrefix(fieldName)).matching(value));
            } else {
                if (!onlyPrefPart) {
                    // boost o preferovaný item
                	boostWildcardQuery(factory, bool, 
                					   PREFIX_PREF + SEPARATOR + itemTypeCode + SEPARATOR + itemSpecCode,
                				       wildcardValue(value), true, true);
                }
                boostWildcardQuery(factory, bool, fieldName, wildcardValue(value), true, true);
            }

        } else {
            if (!onlyPrefPart) {
                // boost o preferovaný item
            	boostWildcardQuery(factory, bool, PREFIX_PREF + SEPARATOR + itemTypeCode, wildcardValue(value), true, true);
            }
            boostWildcardQuery(factory, bool, fieldName, wildcardValue(value), true, true);
        }

        return bool.toPredicate();
	}

    /**
     * Přidání BM25 skórování pro celý vyhledávací řetězec na analyzovaných polích.
     * Na rozdíl od wildcard dotazů (konstantní skóre), match() používá BM25 s normalizací
     * délky pole - kratší preferovaná jména obsahující hledané výrazy získají vyšší skóre.
     * Řeší problém, kdy entity bez vedlejší části jména (nm_minor) byly řazeny níže
     * než podřízené entity s nm_minor odpovídajícím hledaným výrazům.
     *
     * Váha se bere z konfigurace pole (boost-fulltext). Pokud není nastavena,
     * použije se násobek stávající hodnoty boost.
     */
    private void addFullTextBoost(SearchPredicateFactory factory,
                                   BooleanPredicateClausesStep<?> bool,
                                   String search,
                                   String partTypeCode) {
        String searchLower = search.toLowerCase();

        // Boost on pref_index_analyzed - BM25 field-length normalization favors shorter preferred names
        addFullTextFieldBoost(factory, bool, PREFIX_PREF + SEPARATOR + INDEX, searchLower);

        // Boost on pref_nm_main_analyzed
        addFullTextFieldBoost(factory, bool, PREFIX_PREF + SEPARATOR + NM_MAIN, searchLower);

        if (StringUtils.isEmpty(partTypeCode) || !partTypeCode.equals(PREFIX_PREF)) {
            addFullTextFieldBoost(factory, bool, INDEX, searchLower);
        }
    }

    /**
     * Multiplier applied to existing boost value when boost-fulltext is not configured.
     */
    private static final float DEFAULT_FULLTEXT_BOOST_MULTIPLIER = 4.0f;

    private void addFullTextFieldBoost(SearchPredicateFactory factory,
                                        BooleanPredicateClausesStep<?> bool,
                                        String fieldName,
                                        String searchLower) {
        SettingIndexSearch.Field sisField = getFieldSearchConfigByName(fieldName);
        Float fulltextBoost = null;
        if (sisField != null) {
            fulltextBoost = sisField.getBoostFulltext();
            if (fulltextBoost == null && sisField.getBoost() != null) {
                // fallback: derive from existing boost value
                fulltextBoost = sisField.getBoost() * DEFAULT_FULLTEXT_BOOST_MULTIPLIER;
            }
        }
        if (fulltextBoost == null || fulltextBoost <= 0f) {
            return;
        }

        String resolvedField = addDataPrefix(fieldName) + ANALYZED;
        bool.should(factory.match().field(resolvedField).matching(searchLower).boost(fulltextBoost));

        if (log.isTraceEnabled()) {
            log.trace("addFullTextFieldBoost: field='{}' (resolved='{}'), search='{}', boostFulltext={}, fromConfig={}",
                      fieldName, resolvedField, searchLower, fulltextBoost,
                      sisField != null && sisField.getBoostFulltext() != null);
        }
    }

    private SearchPredicate processIndexCondDef(SearchPredicateFactory factory,
    											String value,
    											String partTypeCode) {
        if (log.isTraceEnabled()) {
            log.trace("processIndexCondDef: value='{}', partTypeCode='{}'", value, partTypeCode);
        }
        BooleanPredicateClausesStep<?> bool = factory.bool();

        String fieldName = "";
        String itemFieldName = "";
        if (StringUtils.isNotEmpty(partTypeCode)) {
        	fieldName = partTypeCode + SEPARATOR;
        	if (partTypeCode.equals(PREFIX_PREF)) {
        		// pref_
                itemFieldName = partTypeCode + SEPARATOR;
            }
        }

        // boost o accessPointId
        boostExactQuery(factory, bool, FIELD_ACCESSPOINT_ID, value, false); 

        if (StringUtils.isEmpty(partTypeCode) || !partTypeCode.equals(PREFIX_PREF)) {
            // boost o preferované indexi a jména
	        boostWildcardQuery(factory, bool, PREFIX_PREF + SEPARATOR + INDEX, value, true, true);
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

    private void boostWildcardQuery(SearchPredicateFactory factory,
    								BooleanPredicateClausesStep<?> step,
    								String fieldName,
    								String value, boolean trans, boolean exact) {
    	float boost = 1.0f;
    	Float boostExact = null;
    	Float boostTransExact = null;
    	SettingIndexSearch.Field sisField = getFieldSearchConfigByName(fieldName);
    	if (sisField != null && sisField.getBoost() != null) {
    		boost = sisField.getBoost();
    		boostExact = sisField.getBoostExact();
    		boostTransExact = sisField.getBoostTransExact();
    	}

    	if (log.isTraceEnabled()) {
    	    log.trace("boostWildcardQuery: field='{}' (resolved='{}'), value='{}', boost={}, boostExact={}, boostTransExact={}, trans={}, exact={}, configFound={}",
    	              fieldName, addDataPrefix(fieldName), value, boost, boostExact, boostTransExact, trans, exact, sisField != null);
    	}

    	step.should(factory.wildcard().field(addDataPrefix(fieldName)).matching(wildcardValue(value)).boost(boost));
    	if (trans) {
    		step.should(factory.wildcard().field(addDataPrefix(fieldName) + ANALYZED).matching(wildcardValue(value)).boost(boost));
    	}
    	if (exact) {
    		boostExactQuery(factory, step, fieldName, value, boostExact, boostTransExact);
    	}
    }

    private void boostExactQuery(SearchPredicateFactory factory,
    							 BooleanPredicateClausesStep<?> step, String fieldName,
    		                     String value,
    		                     Float boostExact, Float boostTransExact) {
    	if (log.isTraceEnabled()) {
    	    log.trace("boostExactQuery: field='{}', value='{}', boostExact={}, boostTransExact={}",
    	              fieldName, value, boostExact, boostTransExact);
    	}
    	if (boostExact != null) {
    		step.should(factory.wildcard().field(addDataPrefix(fieldName)).matching(value).boost(boostExact));
    	}
    	if (boostTransExact != null) {
    		step.should(factory.wildcard().field(addDataPrefix(fieldName) + ANALYZED).matching(value).boost(boostTransExact));
    	}
    }

    private void boostExactQuery(SearchPredicateFactory factory,
    		                     BooleanPredicateClausesStep<?> step,
    		                     String fieldName,
    		                     String value, boolean prefix) {
    	SettingIndexSearch.Field sisField = getFieldSearchConfigByName(fieldName);
    	if (log.isTraceEnabled()) {
    	    log.trace("boostExactQuery(prefix={}): field='{}' (resolved='{}'), value='{}', configFound={}, boostExact={}, boostTransExact={}",
    	              prefix, fieldName, addDataPrefix(fieldName, prefix), value, sisField != null,
    	              sisField != null ? sisField.getBoostExact() : null,
    	              sisField != null ? sisField.getBoostTransExact() : null);
    	}
    	if (sisField != null) {
    		Float boostExact = sisField.getBoostExact();
    		if (boostExact != null) {
    			step.should(factory.wildcard().field(addDataPrefix(fieldName, prefix)).matching(value).boost(boostExact));
    		}
            Float boostTransExact = sisField.getBoostTransExact();
            if (boostTransExact != null) {
            	step.should(factory.wildcard().field(addDataPrefix(fieldName, prefix) + ANALYZED).matching(value).boost(boostTransExact));
            }
    	}
    }

    private static String wildcardValue(String value) {
    	return STAR + value.toLowerCase() + STAR;
    }

    private static String addDataPrefix(String fieldName, boolean prefix) {
    	if (prefix) {
    		return addDataPrefix(fieldName);
    	}
    	return fieldName;
    }

	private static String addDataPrefix(String fieldName) {
    	return DATA + (fieldName.startsWith("_")? "" : SEPARATOR) + fieldName;
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

    private void logSearchConfig() {
        SettingIndexSearch sis = getElzaSearchConfig();
        if (sis == null || CollectionUtils.isEmpty(sis.getFields())) {
            log.trace("Search config (INDEX_SEARCH): not configured or no fields defined");
            return;
        }
        StringBuilder sb = new StringBuilder("Search config (INDEX_SEARCH) fields:\n");
        for (SettingIndexSearch.Field field : sis.getFields()) {
            sb.append(String.format("  field='%s', boost=%s, boostExact=%s, boostTransExact=%s, boostFulltext=%s, transliterate=%s%n",
                                    field.getName(), field.getBoost(), field.getBoostExact(),
                                    field.getBoostTransExact(), field.getBoostFulltext(), field.getTransliterate()));
        }
        log.trace(sb.toString());
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
