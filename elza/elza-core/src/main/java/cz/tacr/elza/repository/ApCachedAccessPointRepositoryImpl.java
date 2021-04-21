package cz.tacr.elza.repository;

import cz.tacr.elza.controller.vo.Area;
import cz.tacr.elza.controller.vo.ExtensionFilterVO;
import cz.tacr.elza.controller.vo.RelationFilterVO;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.core.data.StaticDataService;
import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.packageimport.xml.SettingIndexSearch;
import cz.tacr.elza.repository.vo.ApCachedAccessPointResult;
import cz.tacr.elza.service.SettingsService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.RamUsageEstimator;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cz.tacr.elza.domain.ApCachedAccessPoint.DATA;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.AP_TYPE_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.INDEX;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.NM_MAIN;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.NM_MINOR;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.PREFIX_PREF;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.SCOPE_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.SEPARATOR;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.SORT;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.STATE;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.TRANS;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.USERNAME;

public class ApCachedAccessPointRepositoryImpl implements ApCachedAccessPointRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private StaticDataService staticDataService;

    @Autowired
    private SettingsService settingsService;

    public static final String STAR = "*";

    private FullTextEntityManager getFullTextEntityManager() {
        return Search.getFullTextEntityManager(entityManager);
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
     * Vytvoří hibernate jpa query z lucene query.
     *
     * @param query lucene query
     * @return hibernate jpa query
     */
    private FullTextQuery createFullTextQuery(Query query, final Class<?> entityClass) {
        return getFullTextEntityManager().createFullTextQuery(query, entityClass);
    }

    private Analyzer getAnalyzer() {
        return getFullTextEntityManager().getSearchFactory().getAnalyzer("cz");
    }

    @Override
    public ApCachedAccessPointResult findApCachedAccessPointisByQuery(String search,
                                                                      SearchFilterVO searchFilter,
                                                                      Set<Integer> apTypeIdTree,
                                                                      Set<Integer> scopeIds,
                                                                      ApState.StateApproval state,
                                                                      Integer from,
                                                                      Integer count,
                                                                      StaticDataProvider sdp) {

        QueryBuilder queryBuilder = createQueryBuilder(ApCachedAccessPoint.class);
        Query query = buildQueryFromParams(queryBuilder, search, searchFilter, apTypeIdTree, scopeIds, state);
        SortField[] sortFields = new SortField[2];
        sortFields[0] = new SortField(null, SortField.Type.SCORE);
        sortFields[1] = new SortField(DATA + SEPARATOR + PREFIX_PREF + SEPARATOR + INDEX + SEPARATOR + SORT, SortField.Type.STRING);

        Sort sort = new Sort(sortFields);

        FullTextQuery fullTextQuery = createFullTextQuery(query, ApCachedAccessPoint.class);
        fullTextQuery.setFirstResult(from);
        fullTextQuery.setMaxResults(count);
        fullTextQuery.setSort(sort);

        return new ApCachedAccessPointResult(fullTextQuery.getResultSize(), fullTextQuery.getResultList());
    }

    private Query buildQueryFromParams(QueryBuilder queryBuilder,
                                       String search,
                                       SearchFilterVO searchFilter,
                                       Set<Integer> apTypeIdTree,
                                       Set<Integer> scopeIds,
                                       ApState.StateApproval state) {
        BooleanJunction<BooleanJunction> bool = queryBuilder.bool();
        boolean empty = true;

        if (state != null) {
            BooleanJunction<BooleanJunction> stateQuery = queryBuilder.bool();
            stateQuery.should(new WildcardQuery(new Term(DATA + SEPARATOR + STATE, state.name().toLowerCase())));
            bool.must(stateQuery.createQuery());
            empty = false;
        }

        if (CollectionUtils.isNotEmpty(apTypeIdTree)) {
            BooleanJunction<BooleanJunction> aeTypeQuery = queryBuilder.bool();
            for (Integer apTypeId : apTypeIdTree) {
                aeTypeQuery.should(new WildcardQuery(new Term(DATA + SEPARATOR + AP_TYPE_ID, apTypeId.toString().toLowerCase())));
            }
            bool.must(aeTypeQuery.createQuery());
            empty = false;
        }

        if (CollectionUtils.isNotEmpty(scopeIds)) {
            BooleanJunction<BooleanJunction> scopeQuery = queryBuilder.bool();
            for (Integer scopeId : scopeIds) {
                scopeQuery.should(new WildcardQuery(new Term(DATA + SEPARATOR + SCOPE_ID, scopeId.toString().toLowerCase())));
            }
            bool.must(scopeQuery.createQuery());
            empty = false;
        }

        if (searchFilter != null) {
            if (StringUtils.isNotEmpty(searchFilter.getCode())) {
                BooleanJunction<BooleanJunction> codeQuery = queryBuilder.bool();
                codeQuery.should(new WildcardQuery(new Term(DATA + SEPARATOR + CachedAccessPoint.ACCESS_POINT_ID, searchFilter.getCode().toLowerCase())));
                bool.must(codeQuery.createQuery());
                empty = false;
            }

            if (StringUtils.isNotEmpty(searchFilter.getUser())) {
                bool.must(new WildcardQuery(new Term(DATA + SEPARATOR + USERNAME, STAR + searchFilter.getUser().toLowerCase() + STAR)));
                empty = false;
            }

            if (searchFilter.getArea() != Area.ENTITY_CODE) {
                bool.must(process(queryBuilder, searchFilter));
                empty = false;
            }

        } else {
            if (StringUtils.isNotEmpty(search)) {
                List<String> keyWords = getKeyWordsFromSearch(search);
                for (String keyWord : keyWords) {
                    bool.must(processIndexCondDef(queryBuilder, keyWord, null));
                    empty = false;
                }
            }
        }

        if (empty) {
            return queryBuilder.all().createQuery();
        }
        return bool.createQuery();
    }

    private Query process(QueryBuilder queryBuilder, SearchFilterVO searchFilterVO) {
        StaticDataProvider sdp = staticDataService.getData();
        String search = searchFilterVO.getSearch();
        Area area = searchFilterVO.getArea();
        BooleanJunction<BooleanJunction> searchQuery = queryBuilder.bool();

        if (StringUtils.isNotEmpty(search)) {
            List<String> keyWords = getKeyWordsFromSearch(search);
            RulPartType defaultPartType = sdp.getDefaultPartType();
            for (String keyWord : keyWords) {
                String partTypeCode;
                switch (area) {
                    case PREFER_NAMES:
                        partTypeCode = PREFIX_PREF;
                        break;
                    case ALL_PARTS:
                        partTypeCode = null;
                        break;
                    case ALL_NAMES:
                        partTypeCode = defaultPartType.getCode().toLowerCase();
                        break;
                    default:
                        throw new NotImplementedException("Neimplementovaný stav oblasti: " + area);
                }
                if (searchFilterVO.getOnlyMainPart() && !area.equals(Area.ALL_PARTS)) {
                    searchQuery.must(processValueCondDef(queryBuilder, keyWord, "NM_MAIN", null, partTypeCode));
                } else {
                    searchQuery.must(processIndexCondDef(queryBuilder, keyWord, partTypeCode));
                }
            }
        }
        if (CollectionUtils.isNotEmpty(searchFilterVO.getExtFilters())) {
            for (ExtensionFilterVO ext : searchFilterVO.getExtFilters()) {
                String itemTypeCode = ext.getItemTypeId() != null ? sdp.getItemTypeById(ext.getItemTypeId()).getCode().toLowerCase() : null;
                String itemSpecCode = ext.getItemSpecId() != null ? sdp.getItemSpecById(ext.getItemSpecId()).getCode().toLowerCase() : null;
                searchQuery.must(processValueCondDef(queryBuilder, String.valueOf(ext.getValue()), itemTypeCode,
                        itemSpecCode, ext.getPartTypeCode().toLowerCase()));
            }
        }
        if (CollectionUtils.isNotEmpty(searchFilterVO.getRelFilters())) {
            for (RelationFilterVO rel : searchFilterVO.getRelFilters()) {
                if (rel.getCode() != null) {
                    String itemTypeCode = rel.getRelTypeId() != null ? sdp.getItemTypeById(rel.getRelTypeId()).getCode().toLowerCase() : null;
                    String itemSpecCode = rel.getRelSpecId() != null ? sdp.getItemSpecById(rel.getRelSpecId()).getCode().toLowerCase() : null;
                    searchQuery.must(processValueCondDef(queryBuilder, String.valueOf(rel.getCode()), itemTypeCode, itemSpecCode, null));
                }
            }
        }
        if (StringUtils.isNotEmpty(searchFilterVO.getCreation())) {
            ArrDataUnitdate arrDataUnitdate = UnitDateConvertor.convertToUnitDate(searchFilterVO.getCreation(), new ArrDataUnitdate());
            String intervalCreation = arrDataUnitdate.getValueFrom() + UnitDateConvertor.DEFAULT_INTERVAL_DELIMITER + arrDataUnitdate.getValueTo();
            searchQuery.must(processValueCondDef(queryBuilder, intervalCreation.toLowerCase(), "CRE_DATE", null, "PT_CRE"));
        }
        if (StringUtils.isNotEmpty(searchFilterVO.getExtinction())) {
            ArrDataUnitdate arrDataUnitdate = UnitDateConvertor.convertToUnitDate(searchFilterVO.getExtinction(), new ArrDataUnitdate());
            String intervalExtinction = arrDataUnitdate.getValueFrom() + UnitDateConvertor.DEFAULT_INTERVAL_DELIMITER + arrDataUnitdate.getValueTo();
            searchQuery.must(processValueCondDef(queryBuilder, intervalExtinction.toLowerCase(),  "EXT_DATE", null, "PT_EXT"));
        }


        return searchQuery.createQuery();
    }

    private Query processIndexCondDef(QueryBuilder queryBuilder, String value, String partTypeCode) {
        BooleanJunction<BooleanJunction> indexQuery = queryBuilder.bool();
        StringBuilder fieldName = new StringBuilder();
        StringBuilder itemFieldName = new StringBuilder();
        if (StringUtils.isNotEmpty(partTypeCode)) {
            fieldName.append(partTypeCode).append(SEPARATOR);

            if (partTypeCode.equals(PREFIX_PREF)) {
                itemFieldName.append(partTypeCode).append(SEPARATOR);
            }
        }

        if (StringUtils.isEmpty(partTypeCode) || !partTypeCode.equals(PREFIX_PREF)) {
            // boost o preferované indexi a jména
            boostWildcardQuery(indexQuery, PREFIX_PREF + SEPARATOR + INDEX, value.toLowerCase(), true, true);
            boostWildcardQuery(indexQuery, PREFIX_PREF + SEPARATOR + NM_MAIN, value.toLowerCase(), true, true);
            boostWildcardQuery(indexQuery, PREFIX_PREF + SEPARATOR + NM_MINOR, value.toLowerCase(), true, true);
        }
        fieldName.append(INDEX);

        //boost hlavního jména
        boostWildcardQuery(indexQuery, itemFieldName.toString().toLowerCase() + NM_MAIN, value.toLowerCase(), true, true);

        //boost minor jména
        boostWildcardQuery(indexQuery, itemFieldName.toString().toLowerCase() + NM_MINOR, value.toLowerCase(), true, true);

        //index
        BooleanJunction<BooleanJunction> transQuery = queryBuilder.bool();
        transQuery.minimumShouldMatchNumber(1);
        boostWildcardQuery(transQuery, fieldName.toString().toLowerCase(), value.toLowerCase(), true, false);
        //code
        boostWildcardQuery(transQuery, CachedAccessPoint.ACCESS_POINT_ID, value.toLowerCase(), false, true);

        indexQuery.must(transQuery.createQuery());
        boostExactQuery(indexQuery, fieldName.toString().toLowerCase(), value.toLowerCase());
        return indexQuery.createQuery();
    }

    private Query processValueCondDef(QueryBuilder queryBuilder, String value, String itemTypeCode, String itemSpecCode, String partTypeCode) {
        BooleanJunction<BooleanJunction> valueQuery = queryBuilder.bool();
        StringBuilder fieldName = new StringBuilder();
        if (StringUtils.isNotEmpty(partTypeCode)) {
            if (partTypeCode.equals(PREFIX_PREF)) {
                fieldName.append(PREFIX_PREF).append(SEPARATOR);
            }
        }
        fieldName.append(itemTypeCode);

        if (StringUtils.isNotEmpty(itemSpecCode)) {
            StringBuilder fieldSpecName = new StringBuilder(fieldName.toString());
            fieldSpecName.append(SEPARATOR).append(itemSpecCode);

            if (value == null) {
                value = itemSpecCode;
                valueQuery.must(new WildcardQuery(new Term(DATA + SEPARATOR + fieldSpecName.toString().toLowerCase(), value.toLowerCase())));
            } else {
                if (StringUtils.isEmpty(partTypeCode) || !partTypeCode.equals(PREFIX_PREF)) {
                    //boost o preferovaný item
                    boostWildcardQuery(valueQuery, PREFIX_PREF + SEPARATOR + itemTypeCode.toLowerCase() + SEPARATOR +
                            itemSpecCode.toLowerCase(), value.toLowerCase(), true, true);
                }

                //item
                BooleanJunction<BooleanJunction> transQuery = queryBuilder.bool();
                transQuery.minimumShouldMatchNumber(1);
                boostWildcardQuery(transQuery, fieldSpecName.toString().toLowerCase(), value.toLowerCase(), true, false);

                valueQuery.must(transQuery.createQuery());
                boostExactQuery(valueQuery, fieldSpecName.toString().toLowerCase(), value.toLowerCase());
            }

        } else {
            if (StringUtils.isEmpty(partTypeCode) || !partTypeCode.equals(PREFIX_PREF)) {
                //boost o preferovaný item
                boostWildcardQuery(valueQuery, PREFIX_PREF + SEPARATOR + itemTypeCode.toLowerCase(), value.toLowerCase(), true, true);
            }

            //item
            BooleanJunction<BooleanJunction> transQuery = queryBuilder.bool();
            transQuery.minimumShouldMatchNumber(1);
            boostWildcardQuery(transQuery, fieldName.toString().toLowerCase(), value.toLowerCase(), true, false);

            valueQuery.must(transQuery.createQuery());
            boostExactQuery(valueQuery, fieldName.toString().toLowerCase(), value.toLowerCase());
        }

        return valueQuery.createQuery();
    }

    private void boostWildcardQuery(BooleanJunction<BooleanJunction> query, String fieldName, String value, boolean trans, boolean exact) {
        float boost = 1.0f;
        SettingIndexSearch elzaSearchConfig = getElzaSearchConfig();
        if (elzaSearchConfig != null && elzaSearchConfig.getFields() != null) {
            SettingIndexSearch.Field fieldSearchConfig = getFieldSearchConfigByName(elzaSearchConfig.getFields(), fieldName);
            if (fieldSearchConfig != null && fieldSearchConfig.getBoost() != null) {
                boost = fieldSearchConfig.getBoost();
            }
        }

        query.should(new BoostQuery(new WildcardQuery(new Term(DATA + SEPARATOR + fieldName, STAR + value + STAR)), boost));
        if (trans) {
            query.should(new BoostQuery(parseTransQuery(DATA + SEPARATOR + fieldName + SEPARATOR + TRANS, STAR + value + STAR), boost));
        }
        if (exact) {
            boostExactQuery(query, fieldName, value);
        }
    }

    private void boostExactQuery(BooleanJunction<BooleanJunction> query, String fieldName, String value) {
        Float boost = null;
        Float boostTrans = null;
        SettingIndexSearch elzaSearchConfig = getElzaSearchConfig();
        if (elzaSearchConfig != null && elzaSearchConfig.getFields() != null) {
            SettingIndexSearch.Field fieldSearchConfig = getFieldSearchConfigByName(elzaSearchConfig.getFields(), fieldName);
            if (fieldSearchConfig != null) {
                boost = fieldSearchConfig.getBoostExact();
                boostTrans = fieldSearchConfig.getBoostTransExact();
            }
        }

        if (boostTrans != null) {
            query.should(new BoostQuery(new WildcardQuery(new Term(DATA + SEPARATOR + fieldName + SEPARATOR + TRANS, removeDiacritic(value))), boostTrans));
        }

        if (boost != null) {
            query.should(new BoostQuery(new WildcardQuery(new Term(DATA + SEPARATOR + fieldName, value)), boost));
        }
    }

    private String removeDiacritic(String value) {
        char[] chars = new char[512];
        final int maxSizeNeeded = 4 * value.length();
        if (chars.length < maxSizeNeeded) {
            chars = new char[ArrayUtil.oversize(maxSizeNeeded, RamUsageEstimator.NUM_BYTES_CHAR)];
        }
        ASCIIFoldingFilter.foldToASCII(value.toCharArray(), 0, chars, 0, value.length());

        return String.valueOf(chars).trim();
    }

    @Nullable
    private SettingIndexSearch.Field getFieldSearchConfigByName(List<SettingIndexSearch.Field> fields, String name) {
        if (CollectionUtils.isNotEmpty(fields)) {
            for (SettingIndexSearch.Field field : fields) {
                if (field.getName().equals(name)) {
                    return field;
                }
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

    private Query parseTransQuery(String fieldName, String value) {
        try {
            QueryParser queryParser = new AnalyzingQueryParser(fieldName, getAnalyzer());
            queryParser.setAllowLeadingWildcard(true);
            return queryParser.parse(value);
        } catch (ParseException e) {
            return new WildcardQuery(new Term(fieldName, value));
        }
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
