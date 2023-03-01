package cz.tacr.elza.repository;

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
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.UISettings;
import cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.packageimport.xml.SettingIndexSearch;
import cz.tacr.elza.service.SettingsService;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.ASCIIFoldingFilter;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.analyzing.AnalyzingQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.util.ArrayUtil;
import org.apache.lucene.util.RamUsageEstimator;
import org.hibernate.search.backend.lucene.search.predicate.impl.AbstractLuceneSearchPredicate;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneNumericRangePredicate;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateFieldStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateMatchingStep;
import org.hibernate.search.engine.search.predicate.dsl.RangePredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.hibernate.search.util.common.data.Range;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.annotation.Nullable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collection;
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

import static cz.tacr.elza.domain.convertor.UnitDateConvertorConsts.DEFAULT_INTERVAL_DELIMITER;

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

//        private Query parseTransQuery(String fieldName, String value) {
//            try {
//                QueryParser queryParser = new AnalyzingQueryParser(fieldName, getAnalyzer());
//                queryParser.setAllowLeadingWildcard(true);
//                return queryParser.parse(value);
//            } catch (ParseException e) {
//                return new WildcardQuery(new Term(fieldName, value));
//            }
//        }

        private void addWildcardQuery(SearchPredicateFactory factory, BooleanPredicateClausesStep<?> step, String fieldName, String value,
                                      boolean trans, boolean exact, boolean wildcard) {
            float boost = 1.0f;
            SettingIndexSearch.Field fieldSearchConfig = getFieldSearchConfigByName(fieldName);
            if (fieldSearchConfig != null && fieldSearchConfig.getBoost() != null) {
                boost = fieldSearchConfig.getBoost();
            }
            String wildCardValue = value;

            if (wildcard) {
                wildCardValue = STAR + value + STAR;
            }

            step.should(factory.wildcard().field(ApCachedAccessPointClassBridge.toLuceneName(DATA + SEPARATOR + fieldName)).boost(boost).matching(wildCardValue).toPredicate());
            if (trans) {
                step.should(factory.wildcard().field(ApCachedAccessPointClassBridge.toLuceneName(DATA + SEPARATOR + fieldName + SEPARATOR + TRANS)).boost(boost).matching(wildCardValue).toPredicate());
            }
            if (exact) {
                addExactQuery(factory, step, fieldName, value, DATA + SEPARATOR);
            }
        }

        /**
         *
         * @param fieldName
         *            Final field name
         * @param value
         */
        private void addExactQuery(SearchPredicateFactory factory, BooleanPredicateClausesStep<?> step, String fieldName, String value,
                                   String fieldPrefix) {
            Float boost = null;
            Float boostTrans = null;

            SettingIndexSearch.Field fieldSearchConfig = getFieldSearchConfigByName(fieldName);
            if (fieldSearchConfig != null) {
                boost = fieldSearchConfig.getBoostExact();
                boostTrans = fieldSearchConfig.getBoostTransExact();
            }

            if (boostTrans != null) {
                String fldName = (fieldPrefix != null) ? (fieldPrefix + fieldName + SEPARATOR + TRANS)
                        : fieldName + SEPARATOR + TRANS;

                String transLitValue = removeDiacritic(value);
                step.should(factory.wildcard().field(ApCachedAccessPointClassBridge.toLuceneName(fldName)).boost(boostTrans).matching(transLitValue).toPredicate());
            }

            if (boost != null) {
                String fldName = (fieldPrefix != null) ? (fieldPrefix + fieldName) : fieldName;
                step.should(factory.wildcard().field(ApCachedAccessPointClassBridge.toLuceneName(fldName)).boost(boost).matching(value).toPredicate());            }
        }

        public void addExactQuery(SearchPredicateFactory factory, BooleanPredicateClausesStep<?> transStep, String fieldName,
                                  int accessPointId) {

            RangePredicateOptionsStep<?> range = factory.range().field(ApCachedAccessPointClassBridge.toLuceneName(fieldName)).range(Range.between(accessPointId, accessPointId));

            SettingIndexSearch.Field fieldSearchConfig = getFieldSearchConfigByName(fieldName);
            if (fieldSearchConfig != null) {
                Float boost = fieldSearchConfig.getBoostExact();
                if (boost != null) {
                    range = factory.range().field(ApCachedAccessPointClassBridge.toLuceneName(fieldName)).range(Range.between(accessPointId, accessPointId)).boost(boost);
                }
            }
            transStep.should(range);
        }
    }


    private SearchSession getSearchSession() {
        return Search.session(entityManager);
    }

    /**
     * Vytvoří query builder pro danou třídu.
     *
     * @param entityClass třída
     *
     * @return query builder
     */
    private SearchPredicateFactory createSearchPredicateFactory(final Class<?> entityClass) {
        return getSearchSession().scope(entityClass).predicate();
    }

    @Override
    public QueryResults<ApCachedAccessPoint> findApCachedAccessPointisByQuery(String search,
                                                                              SearchFilterVO searchFilter,
                                                                              Collection<Integer> apTypeIdTree,
                                                                              Collection<Integer> scopeIds,
                                                                              ApState.StateApproval state,
                                                                              Integer from,
                                                                              Integer count,
                                                                              StaticDataProvider sdp) {

        SearchPredicateFactory factory = createSearchPredicateFactory(ApCachedAccessPoint.class);

        SearchPredicate predicate = buildQueryFromParams(factory, search, searchFilter, apTypeIdTree, scopeIds, state);
        SortField sortField = new SortField(ApCachedAccessPointClassBridge.toLuceneName(DATA + SEPARATOR + PREFIX_PREF + SEPARATOR + INDEX + SEPARATOR + SORT), SortField.Type.STRING);
        List<ApCachedAccessPoint> apCachedAccessPoints = getSearchSession().search(ApCachedAccessPoint.class).where(predicate).sort(SearchSortFactory::score)
                .sort(f -> f.composite(b -> {
                    b.add(f.field(sortField.getField()));
                })).fetchHits(from, count);

        return new QueryResults<ApCachedAccessPoint>(apCachedAccessPoints.size(), apCachedAccessPoints);
    }

    private SearchPredicate buildQueryFromParams(SearchPredicateFactory factory,
                                                 String search,
                                                 SearchFilterVO searchFilter,
                                                 Collection<Integer> apTypeIdTree,
                                                 Collection<Integer> scopeIds,
                                                 ApState.StateApproval state) {
        BooleanPredicateClausesStep<?> bool = factory.bool();
        boolean empty = true;

        if (state != null) {
            BooleanPredicateClausesStep<?> stateStep = factory.bool();
            stateStep.should(factory.wildcard().field(ApCachedAccessPointClassBridge.toLuceneName(STATE)).matching(state.name().toLowerCase()).toPredicate());
            bool.must(stateStep.toPredicate());
            empty = false;
        }

        if (CollectionUtils.isNotEmpty(apTypeIdTree)) {
            BooleanPredicateClausesStep<?> aeTypeStep = factory.bool();
            for (Integer apTypeId : apTypeIdTree) {
                aeTypeStep.should(factory.wildcard().field(ApCachedAccessPointClassBridge.toLuceneName(AP_TYPE_ID)).matching(apTypeId.toString().toLowerCase()).toPredicate());
            }
            bool.must(aeTypeStep.toPredicate());
            empty = false;
        }

        if (CollectionUtils.isNotEmpty(scopeIds)) {
            BooleanPredicateClausesStep<?> scopeStep = factory.bool();
            for (Integer scopeId : scopeIds) {
                scopeStep.should(factory.wildcard().field(ApCachedAccessPointClassBridge.toLuceneName(SCOPE_ID)).matching(scopeId.toString().toLowerCase()).toPredicate());
            }
            bool.must(scopeStep.toPredicate());
            empty = false;
        }

        FulltextCondFactory fcf = new FulltextCondFactory(getElzaSearchConfig());

        if (searchFilter != null) {
            if (StringUtils.isNotEmpty(searchFilter.getCode())) {
//                Integer apId = Integer.parseInt(searchFilter.getCode()); //TODO pasek
//                NumericRangeQuery<Integer> apIdQuery = NumericRangeQuery
//                        .newIntRange(ApCachedAccessPoint.FIELD_ACCESSPOINT_ID,
//                                     apId, apId, true, true);
//                bool.must(apIdQuery);
//                empty = false;
            }

            if (StringUtils.isNotEmpty(searchFilter.getUser())) {
                bool.must(factory.wildcard().field(ApCachedAccessPointClassBridge.toLuceneName(DATA + SEPARATOR + USERNAME)).matching(STAR + searchFilter.getUser().toLowerCase() + STAR).toPredicate());                empty = false;
            }

            if (searchFilter.getArea() != Area.ENTITY_CODE) {
                // prepare fulltext query
                SearchPredicate p = process(factory, searchFilter, fcf);
                if (p != null) {
                    bool.must(p);
                    empty = false;
                }
            }

        } else {
            // prepare fulltext query
            if (StringUtils.isNotEmpty(search)) {
                List<String> keyWords = getKeyWordsFromSearch(search);
                for (String keyWord : keyWords) {
                    bool.must(processIndexCondDef(factory, keyWord, null, fcf));
                    empty = false;
                }
            }
        }

        if (empty) {
            return factory.matchAll().toPredicate();
        }
        return bool.toPredicate();
    }

    /**
     * Return prepared query
     *
     * @param factory
     * @param searchFilterVO
     * @param fcf
     * @return Might return null if empty query
     */
    @Nullable
    private SearchPredicate process(SearchPredicateFactory factory, SearchFilterVO searchFilterVO, FulltextCondFactory fcf) {
        StaticDataProvider sdp = staticDataService.getData();
        String search = searchFilterVO.getSearch();
        Area area = searchFilterVO.getArea();
        if (area == null) {
            area = Area.ALL_NAMES;
        }
        BooleanPredicateClausesStep searchStep = factory.bool();

        if (StringUtils.isNotEmpty(search)) {
            List<String> keyWords = getKeyWordsFromSearch(search);
            RulPartType defaultPartType = sdp.getDefaultPartType();
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
                    searchStep.must(processValueCondDef(sdp, factory, keyWord,
                                                         "NM_MAIN", null, partTypeCode,
                                                         fcf));
                } else {
                    searchStep.must(processIndexCondDef(factory, keyWord, partTypeCode, fcf));
                }
            }
        }
        if (CollectionUtils.isNotEmpty(searchFilterVO.getExtFilters())) {
            for (ExtensionFilterVO ext : searchFilterVO.getExtFilters()) {
                Validate.notNull(ext.getItemTypeId());
                ItemType itemType = sdp.getItemTypeById(ext.getItemTypeId());
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
                String value;
                if (ext.getValue() != null) {
                    value = ext.getValue().toString();
                } else {
                    value = null;
                }
                searchStep.must(processValueCondDef(factory, value,
                                                     itemType.getEntity(), itemSpec,
                                                     ext.getPartTypeCode().toLowerCase(), fcf));
            }
        }
        if (CollectionUtils.isNotEmpty(searchFilterVO.getRelFilters())) {
            for (RelationFilterVO rel : searchFilterVO.getRelFilters()) {
                if (rel.getCode() != null) {
                    Validate.notNull(rel.getRelTypeId());
                    ItemType itemType = sdp.getItemTypeById(rel.getRelTypeId());
                    RulItemSpec itemSpec;
                    if (rel.getRelSpecId() != null) {
                        itemSpec = sdp.getItemSpecById(rel.getRelSpecId());
                    } else {
                        itemSpec = null;
                    }
                    searchStep.must(processValueCondDef(factory, rel.getCode().toString(),
                                                         itemType.getEntity(), itemSpec, null, fcf));
                }
            }
        }
        if (StringUtils.isNotEmpty(searchFilterVO.getCreation())) {
            ArrDataUnitdate arrDataUnitdate = UnitDateConvertor.convertToUnitDate(searchFilterVO.getCreation(), new ArrDataUnitdate());
            String intervalCreation = arrDataUnitdate.getValueFrom() + DEFAULT_INTERVAL_DELIMITER + arrDataUnitdate.getValueTo();
            searchStep.must(processValueCondDef(sdp, factory, intervalCreation.toLowerCase(),
                                                 "CRE_DATE", null,
                                                 "PT_CRE", fcf));
        }
        if (StringUtils.isNotEmpty(searchFilterVO.getExtinction())) {
            ArrDataUnitdate arrDataUnitdate = UnitDateConvertor.convertToUnitDate(searchFilterVO.getExtinction(), new ArrDataUnitdate());
            String intervalExtinction = arrDataUnitdate.getValueFrom() + DEFAULT_INTERVAL_DELIMITER + arrDataUnitdate.getValueTo();
            searchStep.must(processValueCondDef(sdp, factory, intervalExtinction.toLowerCase(),
                                                 "EXT_DATE", null,
                                                 "PT_EXT", fcf));
        }

        return searchStep.toPredicate();
    }

    private SearchPredicate processIndexCondDef(SearchPredicateFactory factory, String value, String partTypeCode,
                                      FulltextCondFactory fcf) {
        String valueLowerCase = value.toLowerCase();

        BooleanPredicateClausesStep<?> indexStep = factory.bool();
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
            fcf.addWildcardQuery(factory, indexStep, PREFIX_PREF + SEPARATOR + INDEX, valueLowerCase, true, true, true);
            fcf.addWildcardQuery(factory, indexStep, PREFIX_PREF + SEPARATOR + NM_MAIN, valueLowerCase, true, true, true);
            fcf.addWildcardQuery(factory, indexStep, PREFIX_PREF + SEPARATOR + NM_MINOR, valueLowerCase, true, true, true);
        }
        fieldName.append(INDEX);

        //boost hlavního jména
        fcf.addWildcardQuery(factory, indexStep, itemFieldName.toString().toLowerCase() + NM_MAIN, valueLowerCase, true, true, true);

        //boost minor jména
        fcf.addWildcardQuery(factory, indexStep, itemFieldName.toString().toLowerCase() + NM_MINOR, valueLowerCase, true, true, true);

        //index
        BooleanPredicateClausesStep<?> transStep = factory.bool();
        transStep.minimumShouldMatchNumber(1);
        fcf.addWildcardQuery(factory, transStep, fieldName.toString().toLowerCase(), valueLowerCase, true, false, true);
        try {
            // accessPointId
            int accessPointId = Integer.parseInt(valueLowerCase);
            fcf.addExactQuery(factory, transStep, ApCachedAccessPoint.FIELD_ACCESSPOINT_ID, accessPointId);

        } catch (Exception e) {

        }

        indexStep.must(transStep.toPredicate());
        fcf.addExactQuery(factory, indexStep, fieldName.toString().toLowerCase(), valueLowerCase, DATA + SEPARATOR);
        return indexStep.toPredicate();
    }

    private SearchPredicate processValueCondDef(StaticDataProvider sdp,
                                      SearchPredicateFactory factory, String value,
                                      String itemTypeCode, String itemSpecCode,
                                      String partTypeCode, FulltextCondFactory fcf) {
        Validate.notNull(itemTypeCode);

        RulItemType itemType = sdp.getItemType(itemTypeCode);

        RulItemSpec itemSpec;
        if (itemSpecCode != null) {
            itemSpec = sdp.getItemSpec(itemSpecCode);
        } else {
            itemSpec = null;
        }

        return processValueCondDef(factory, value, itemType, itemSpec, partTypeCode, fcf);
    }

    private SearchPredicate processValueCondDef(SearchPredicateFactory factory, String value,
                                      RulItemType itemType, RulItemSpec itemSpec,
                                      String partTypeCode, FulltextCondFactory fcf) {
        if (itemType == null) {
            throw new SystemException("Missing itemType", BaseCode.INVALID_STATE);
        }

        BooleanPredicateClausesStep<?> valueStep = factory.bool();
        StringBuilder fieldName = new StringBuilder();
        if (StringUtils.isNotEmpty(partTypeCode)) {
            if (partTypeCode.equals(PREFIX_PREF)) {
                fieldName.append(PREFIX_PREF).append(SEPARATOR);
            }
        }
        fieldName.append(itemType.getCode());

        DataType dataType = DataType.fromId(itemType.getDataTypeId());
        boolean wildcard = !(dataType == DataType.INT || dataType == DataType.RECORD_REF || dataType == DataType.BIT);

        if (itemSpec != null) {
            StringBuilder fieldSpecName = new StringBuilder(fieldName.toString());
            fieldSpecName.append(SEPARATOR).append(itemSpec.getCode());

            if (value == null) {
                value = itemSpec.getCode();
                valueStep.must(factory.wildcard().field(ApCachedAccessPointClassBridge.toLuceneName(DATA + SEPARATOR + fieldSpecName.toString().toLowerCase())).matching(value.toLowerCase()).toPredicate());
            } else {
                if (StringUtils.isEmpty(partTypeCode) || !partTypeCode.equals(PREFIX_PREF)) {
                    //boost o preferovaný item
                    fcf.addWildcardQuery(factory, valueStep, PREFIX_PREF + SEPARATOR + itemType.getCode().toLowerCase()
                            + SEPARATOR +
                            itemSpec.getCode().toLowerCase(), value.toLowerCase(), true, true, wildcard);
                }

                //item
                BooleanPredicateClausesStep<?> transStep = factory.bool();
                transStep.minimumShouldMatchNumber(1);
                fcf.addWildcardQuery(factory, transStep, fieldSpecName.toString().toLowerCase(), value.toLowerCase(), true,
                                     false, wildcard);

                valueStep.must(transStep.toPredicate());
                fcf.addExactQuery(factory, valueStep, fieldSpecName.toString().toLowerCase(), value.toLowerCase(), DATA
                        + SEPARATOR);
            }

        } else {
            if (StringUtils.isEmpty(partTypeCode) || !partTypeCode.equals(PREFIX_PREF)) {
                //boost o preferovaný item
                fcf.addWildcardQuery(factory, valueStep, PREFIX_PREF + SEPARATOR + itemType.getCode().toLowerCase(),
                                     value.toLowerCase(), true, true, wildcard);
            }

            //item
            BooleanPredicateClausesStep<?> transStep = factory.bool();
            transStep.minimumShouldMatchNumber(1);
            fcf.addWildcardQuery(factory, transStep, fieldName.toString().toLowerCase(), value.toLowerCase(), true, false, wildcard);

            valueStep.must(transStep.toPredicate());
            fcf.addExactQuery(factory, valueStep, fieldName.toString().toLowerCase(), value.toLowerCase(), DATA + SEPARATOR);
        }

        return valueStep.toPredicate();
    }


    static private String removeDiacritic(String value) {
        char[] chars = new char[512];
        final int maxSizeNeeded = 4 * value.length();
        if (chars.length < maxSizeNeeded) {
            chars = new char[ArrayUtil.oversize(maxSizeNeeded, Character.BYTES)];
        }
        ASCIIFoldingFilter.foldToASCII(value.toCharArray(), 0, chars, 0, value.length());

        return String.valueOf(chars).trim();
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
