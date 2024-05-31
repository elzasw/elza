package cz.tacr.elza.repository;

import static cz.tacr.elza.domain.ApCachedAccessPoint.DATA;
import static cz.tacr.elza.domain.ApCachedAccessPoint.FIELD_ACCESSPOINT_ID;
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
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBinder.REL_AP_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBinder.NORM_FROM;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointBinder.NORM_TO;

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
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.common.db.QueryResults;
import cz.tacr.elza.controller.vo.Area;
import cz.tacr.elza.controller.vo.ExtensionFilterVO;
import cz.tacr.elza.controller.vo.RelationFilterVO;
import cz.tacr.elza.controller.vo.SearchFilterVO;
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

    private static final String DATA_CRE_DATE = DATA + SEPARATOR + "cre_date";
    private static final String DATA_EXT_DATE = DATA + SEPARATOR + "ext_date";

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
        //SearchScope<ApCachedAccessPoint> scope = session.scope(ApCachedAccessPoint.class);
        //SortField sortField = new SortField(DATA + PREFIX_PREF + INDEX + SORTABLE, SortField.Type.STRING);

		SearchResult<ApCachedAccessPoint> result = session.search(ApCachedAccessPoint.class)
				.where(predicate)
                //.sort(scope.sort().field(sortField.getField()).desc().toSort())
                //.sort(SearchSortFactory::score)
                //.sort(f -> f.composite(b -> {
                //    b.add(f.field(sortField.getField()));
                //}))
                .fetch(from, count);

		// počet všech záznamů dle podmínky
		// pozor: pokud to nefunguje správně, musíme znovu vygenerovat indexové soubory /lucene/indexes
		Long hitCount = result.total().hitCount();

		return new QueryResults<ApCachedAccessPoint>(hitCount.intValue(), result.hits());
    }

    private SearchPredicate buildQueryFromParams(SearchPredicateFactory factory, 
    											 String search,
    											 SearchFilterVO searchFilter,
    											 Collection<Integer> apTypeIdTree,
    											 Collection<Integer> scopeIds,
    											 ApState.StateApproval state,
    											 RevStateApproval revState) {
        BooleanPredicateClausesStep<?> bool = factory.bool();

		if (searchFilter != null) {
			// TODO je třeba dále projednat podmínky
			if (StringUtils.isNotEmpty(searchFilter.getUser())) {
				bool.should(factory.wildcard().field(USERNAME).matching(wildcardValue(searchFilter.getUser())));
			}
			if (searchFilter.getArea() != Area.ENTITY_CODE) {
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

		if (state != null) {
			bool.must(factory.match().field(STATE).matching(state.name().toLowerCase()));
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
    private SearchPredicate process(SearchPredicateFactory factory, SearchFilterVO searchFilterVO) {
    	StaticDataProvider sdp = staticDataService.getData();
    	String search = searchFilterVO.getSearch();
    	Area area = searchFilterVO.getArea();
    	if (area == null) {
    		area = Area.ALL_NAMES;
    	}
    	BooleanPredicateClausesStep<?> bool = factory.bool();

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
    				bool.must(processValueCondDef(factory, keyWord, sdp.getItemType(NM_MAIN.toUpperCase()), null, partTypeCode));
    			} else {
    				bool.must(processIndexCondDef(factory, keyWord, partTypeCode));
    			}
    		}
    	}
    	if (CollectionUtils.isNotEmpty(searchFilterVO.getExtFilters())) {
    		for (ExtensionFilterVO ext : searchFilterVO.getExtFilters()) {
    			Objects.requireNonNull(ext.getItemTypeId());
    			RulItemType itemType = sdp.getItemType(ext.getItemTypeId());
    			RulItemSpec itemSpec;
    			if(ext.getItemSpecId() != null) {
    				itemSpec = sdp.getItemSpecById(ext.getItemSpecId());
    			} else {
    				itemSpec = null;
    				if (ext.getValue() == null) {
    					// specification nor value defined -> skip this condition
    					// note: this is probably incorrect, exception should be thrown for invalid condition
    					continue;
    				}
    			}
    			String value = null;
    			if (ext.getValue() != null) {
    				value = ext.getValue().toString();
    			}
    			bool.must(processValueCondDef(factory, value, itemType, itemSpec, ext.getPartTypeCode()));
    		}
    	}
    	if (CollectionUtils.isNotEmpty(searchFilterVO.getRelFilters())) {
    		for (RelationFilterVO rel : searchFilterVO.getRelFilters()) {
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
    					relPred.should(processValueCondDef(factory, rel.getCode().toString(), itemType, itemSpec, null));
    				} else {
    					relPred.should(factory.match().field(REL_AP_ID).matching(rel.getCode()));
    				}
    				bool.must(relPred);
    			}
    		}
    	}
    	if (StringUtils.isNotEmpty(searchFilterVO.getCreation())) {
    		ArrDataUnitdate creDate = UnitDateConvertor.convertToUnitDate(searchFilterVO.getCreation(), new ArrDataUnitdate());
    		bool.must(factory.range().field(DATA_CRE_DATE + NORM_FROM).atMost(creDate.getNormalizedFrom()))
    			.must(factory.range().field(DATA_CRE_DATE + NORM_TO).atLeast(creDate.getNormalizedTo()));
    	}
    	if (StringUtils.isNotEmpty(searchFilterVO.getExtinction())) {
    		ArrDataUnitdate extDate = UnitDateConvertor.convertToUnitDate(searchFilterVO.getExtinction(), new ArrDataUnitdate());
    		bool.must(factory.range().field(DATA_EXT_DATE + NORM_FROM).atMost(extDate.getNormalizedFrom()))
    			.must(factory.range().field(DATA_EXT_DATE + NORM_TO).atLeast(extDate.getNormalizedTo()));
    	}

    	if (!bool.hasClause()) {
    		return null;
    	}
    	return bool.toPredicate();
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
		String itemTypeCode = itemType.getCode().toLowerCase();

        if (itemSpec != null) {
        	String itemSpecCode = itemSpec.getCode().toLowerCase();
            fieldName += SEPARATOR + itemSpecCode;

            if (value == null) {
                value = itemSpec.getCode().toLowerCase();
                bool.should(factory.match().field(addDataPrefix(fieldName)).matching(value));
            } else {
                if (StringUtils.isEmpty(partTypeCode) || !partTypeCode.equals(PREFIX_PREF)) {
                    // boost o preferovaný item
                	boostWildcardQuery(factory, bool, 
                					   PREFIX_PREF + SEPARATOR + itemTypeCode + SEPARATOR + itemSpecCode,
                				       wildcardValue(value), true, true);
                }
                boostWildcardQuery(factory, bool, addDataPrefix(fieldName), wildcardValue(value), true, true);
            }

        } else {
            if (StringUtils.isEmpty(partTypeCode) || !partTypeCode.equals(PREFIX_PREF)) {
                // boost o preferovaný item
            	boostWildcardQuery(factory, bool, PREFIX_PREF + SEPARATOR + itemTypeCode, wildcardValue(value), true, true);
            }
            boostWildcardQuery(factory, bool, addDataPrefix(fieldName), wildcardValue(value), true, true);
        }

        return bool.toPredicate();
	}

    private SearchPredicate processIndexCondDef(SearchPredicateFactory factory, String value, String partTypeCode) {
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

    private void boostWildcardQuery(SearchPredicateFactory factory, BooleanPredicateClausesStep<?> step, String fieldName, 
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

    	step.should(factory.wildcard().field(addDataPrefix(fieldName)).matching(wildcardValue(value)).boost(boost));
    	if (trans) {
    		step.should(factory.wildcard().field(addDataPrefix(fieldName) + ANALYZED).matching(wildcardValue(value)).boost(boost));
    	}
    	if (exact) {
    		boostExactQuery(factory, step, fieldName, value, boostExact, boostTransExact);
    	}
    }

    private void boostExactQuery(SearchPredicateFactory factory, BooleanPredicateClausesStep<?> step, String fieldName, 
    		                     String value, Float boostExact, Float boostTransExact) {
    	if (boostExact != null) {
    		step.should(factory.wildcard().field(addDataPrefix(fieldName)).matching(value).boost(boostExact));
    	}
    	if (boostTransExact != null) {
    		step.should(factory.wildcard().field(addDataPrefix(fieldName) + ANALYZED).matching(value).boost(boostTransExact));
    	}
    }

    private void boostExactQuery(SearchPredicateFactory factory, BooleanPredicateClausesStep<?> step, String fieldName, String value, boolean prefix) {
    	SettingIndexSearch.Field sisField = getFieldSearchConfigByName(fieldName);
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
