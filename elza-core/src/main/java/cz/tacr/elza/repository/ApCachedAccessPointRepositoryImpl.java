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
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.service.cache.CachedAccessPoint;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;

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
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.PREFIX_PREF;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.SCOPE_ID;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.SEPARATOR;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.STATE;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.TRANS;
import static cz.tacr.elza.domain.bridge.ApCachedAccessPointClassBridge.USERNAME;

public class ApCachedAccessPointRepositoryImpl implements ApCachedAccessPointRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private StaticDataService staticDataService;

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

    @Override
    public List<ApCachedAccessPoint> findApCachedAccessPointisByQuery(String search,
                                                                      SearchFilterVO searchFilter,
                                                                      Set<Integer> apTypeIdTree,
                                                                      Set<Integer> scopeIds,
                                                                      ApState.StateApproval state,
                                                                      Integer from,
                                                                      Integer count,
                                                                      StaticDataProvider sdp) {

        QueryBuilder queryBuilder = createQueryBuilder(ApCachedAccessPoint.class);
        Query query = buildQueryFromParams(queryBuilder, search, searchFilter, apTypeIdTree, scopeIds, state);
        Sort sort = queryBuilder.sort().byScore().createSort();

        FullTextQuery fullTextQuery = createFullTextQuery(query, ApCachedAccessPoint.class);
        fullTextQuery.setFirstResult(from);
        fullTextQuery.setMaxResults(count);
        fullTextQuery.setSort(sort);

        return fullTextQuery.getResultList();
    }

    private Query buildQueryFromParams(QueryBuilder queryBuilder,
                                       String search,
                                       SearchFilterVO searchFilter,
                                       Set<Integer> apTypeIdTree,
                                       Set<Integer> scopeIds,
                                       ApState.StateApproval state) {
        BooleanJunction<BooleanJunction> bool = queryBuilder.bool();

        if (state != null) {
            BooleanJunction<BooleanJunction> stateQuery = queryBuilder.bool();
            stateQuery.should(new TermQuery(new Term(DATA + SEPARATOR + STATE, state.name().toLowerCase())));
            bool.must(stateQuery.createQuery());
        }

        if (CollectionUtils.isNotEmpty(apTypeIdTree)) {
            BooleanJunction<BooleanJunction> aeTypeQuery = queryBuilder.bool();
            for (Integer apTypeId : apTypeIdTree) {
                aeTypeQuery.should(new TermQuery(new Term(DATA + SEPARATOR + AP_TYPE_ID, apTypeId.toString().toLowerCase())));
            }
            bool.must(aeTypeQuery.createQuery());
        }

        if (CollectionUtils.isNotEmpty(scopeIds)) {
            BooleanJunction<BooleanJunction> scopeQuery = queryBuilder.bool();
            for (Integer scopeId : scopeIds) {
                scopeQuery.should(new TermQuery(new Term(DATA + SEPARATOR + SCOPE_ID, scopeId.toString().toLowerCase())));
            }
            bool.must(scopeQuery.createQuery());
        }

        if (searchFilter != null) {
            if (StringUtils.isNotEmpty(searchFilter.getCode())) {
                BooleanJunction<BooleanJunction> codeQuery = queryBuilder.bool();
                codeQuery.should(new TermQuery(new Term(DATA + SEPARATOR + CachedAccessPoint.ACCESS_POINT_ID, searchFilter.getCode().toLowerCase())));
                bool.must(codeQuery.createQuery());
            }

            if (StringUtils.isNotEmpty(searchFilter.getUser())) {
                bool.must(new WildcardQuery(new Term(DATA + SEPARATOR + USERNAME, STAR + searchFilter.getUser().toLowerCase() + STAR)));
            }

            if (searchFilter.getArea() != Area.ENTITY_CODE) {
                bool.must(process(queryBuilder, searchFilter));
            }

        } else {
            if (StringUtils.isNotEmpty(search)) {
                List<String> keyWords = getKeyWordsFromSearch(search);
                for (String keyWord : keyWords) {
                    bool.must(processIndexCondDef(queryBuilder, keyWord, null));
                }
            }
        }

        if (bool == null) {
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
        StringBuilder fieldName = new StringBuilder(DATA + SEPARATOR);
        StringBuilder itemFieldName = new StringBuilder(DATA + SEPARATOR);
        if (StringUtils.isNotEmpty(partTypeCode)) {
            fieldName.append(partTypeCode).append(SEPARATOR);

            if (partTypeCode.equals(PREFIX_PREF)) {
                itemFieldName.append(partTypeCode).append(SEPARATOR);
            }
        }
        fieldName.append(INDEX);
        itemFieldName.append(NM_MAIN);
        indexQuery.should(new WildcardQuery(new Term(itemFieldName.toString().toLowerCase(), STAR + value.toLowerCase() + STAR)));
        indexQuery.should(new WildcardQuery(new Term(itemFieldName.toString().toLowerCase() + SEPARATOR + TRANS, STAR + value.toLowerCase() + STAR)));

        BooleanJunction<BooleanJunction> transQuery = queryBuilder.bool();
        transQuery.minimumShouldMatchNumber(1);
        transQuery.should(new WildcardQuery(new Term(fieldName.toString().toLowerCase() + SEPARATOR + TRANS, STAR + value.toLowerCase() + STAR)));
        transQuery.should(new WildcardQuery(new Term(fieldName.toString().toLowerCase(), STAR + value.toLowerCase() + STAR)));

        indexQuery.must(transQuery.createQuery());
        return indexQuery.createQuery();
    }

    private Query processValueCondDef(QueryBuilder queryBuilder, String value, String itemTypeCode, String itemSpecCode, String partTypeCode) {
        BooleanJunction<BooleanJunction> valueQuery = queryBuilder.bool();
        StringBuilder fieldName = new StringBuilder(DATA + SEPARATOR);
        if (StringUtils.isNotEmpty(partTypeCode)) {
            if (partTypeCode.equals(PREFIX_PREF)) {
                fieldName.append(PREFIX_PREF).append(SEPARATOR);
            }
        }
        fieldName.append(itemTypeCode);

        if (StringUtils.isNotEmpty(itemSpecCode)) {
            fieldName.append(SEPARATOR).append(itemSpecCode);

            if (value == null) {
                value = itemSpecCode;
            }
            valueQuery.must(new TermQuery(new Term(fieldName.toString().toLowerCase(), value.toLowerCase())));
        } else {
            valueQuery.minimumShouldMatchNumber(1);
            valueQuery.should(new WildcardQuery(new Term(fieldName.toString().toLowerCase(), STAR + value.toLowerCase() + STAR)));
            valueQuery.should(new WildcardQuery(new Term(fieldName.toString().toLowerCase() + SEPARATOR + TRANS, STAR + value.toLowerCase() + STAR)));
        }

        return valueQuery.createQuery();
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
