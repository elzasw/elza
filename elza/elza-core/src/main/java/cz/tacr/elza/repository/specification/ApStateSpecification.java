package cz.tacr.elza.repository.specification;

import cz.tacr.cam.client.controller.vo.QueryComparator;
import cz.tacr.elza.controller.vo.Area;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulDataType;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.repository.specification.search.BitComparator;
import cz.tacr.elza.repository.specification.search.Comparator;
import cz.tacr.elza.repository.specification.search.CoordinatesComparator;
import cz.tacr.elza.repository.specification.search.Ctx;
import cz.tacr.elza.repository.specification.search.IntegerComparator;
import cz.tacr.elza.repository.specification.search.LinkComparator;
import cz.tacr.elza.repository.specification.search.NullComparator;
import cz.tacr.elza.repository.specification.search.RecordRefComparator;
import cz.tacr.elza.repository.specification.search.StringComparator;
import cz.tacr.elza.repository.specification.search.TextComparator;
import cz.tacr.elza.repository.specification.search.UnitdateComparator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ApStateSpecification implements Specification<ApState> {

    private SearchFilterVO searchFilterVO;
    private StaticDataProvider sdp;

    public ApStateSpecification(final SearchFilterVO searchFilterVO, final StaticDataProvider sdp) {
        this.searchFilterVO = searchFilterVO;
        this.sdp = sdp;
    }


    @Override
    public Predicate toPredicate(Root<ApState> stateRoot, CriteriaQuery<?> q, CriteriaBuilder cb) {
        Ctx ctx = new Ctx(cb, q);
        ctx.setStateRoot(stateRoot);

        Join<ApState, ApAccessPoint> accessPointJoin = stateRoot.join(ApState.FIELD_ACCESS_POINT, JoinType.INNER);
        Join<ApAccessPoint, ApPart> preferredPartJoin = accessPointJoin.join(ApAccessPoint.FIELD_PREFFERED_PART, JoinType.INNER);
        ctx.setAccessPointJoin(accessPointJoin);
        ctx.setPreferredPartJoin(preferredPartJoin);

        Predicate condition = cb.conjunction();

//        String user = params.getUser();
//        if (StringUtils.isNotEmpty(user)) {
//            Join<AeRevision, AeChange> aeChangeJoin = aeRevisionRoot.join(AeRevision.CREATE_CHANGE, JoinType.INNER);
//            condition = cb.and(condition, cb.like(cb.lower(aeChangeJoin.get(AeChange.USERNAME)), "%" + user.toLowerCase() + "%"));
//        }

        // typ archivní entity
        List<Integer> aeTypeIdList =  searchFilterVO.getAeTypeIds();
        if (CollectionUtils.isNotEmpty(aeTypeIdList)) {
            condition = cb.and(condition, stateRoot.get(ApState.FIELD_AP_TYPE_ID).in(aeTypeIdList));
        }

//        // omezení dle vyjmenovaných stavů
//        List<QueryParamsDef.StateEnum> states = params.getState();
//        if (CollectionUtils.isNotEmpty(states)) {
//            condition = cb.and(condition, aeRevisionRoot.get(AeRevision.STATE).in(convertStates(states)));
//        }

        // omezení dle konkrétních archivních entit
        String code = searchFilterVO.getCode();
        if (StringUtils.isNotEmpty(code)) {
            try {
                Integer id = Integer.parseInt(code);
                condition = cb.and(condition, accessPointJoin.get(ApAccessPoint.FIELD_ACCESS_POINT_ID).in(id));
            } catch (NumberFormatException e) {

            }
        }

        // pouze aktuální state
        condition = cb.and(condition, cb.isNull(stateRoot.get(ApState.FIELD_DELETE_CHANGE_ID)));

        condition = cb.and(condition, process(cb.conjunction(), ctx));

        // atribut, podle kterého se výsledek seřadí - dle "sort value" preferovaného jména archivní entity
        //Path<ApPart> sortingBySortValue = ctx.getPreferredPartJoin().get(ApPart.VALUE);
        // každý nález ae pouze jednou
        //q.groupBy(stateRoot, sortingBySortValue);
        // seřazení
        //.orderBy(cb.asc(sortingBySortValue));


        return condition;
    }

    private Predicate process(Predicate condition, Ctx ctx) {
        CriteriaBuilder cb = ctx.cb;
        String search = searchFilterVO.getSearch();
        Area area = searchFilterVO.getArea();
        if (area != Area.ENTITY_CODE) {
            if (StringUtils.isNotEmpty(search)) {
                List<String> keyWords = getKeyWordsFromSearch(search);

                Predicate and = cb.conjunction();
                RulPartType defaultPartType = sdp.getDefaultPartType();
                for (String keyWord : keyWords) {
                    String partTypeCode;
                    boolean prefPart = false;
                    switch (area) {
                        case PREFER_NAMES:
                            partTypeCode = defaultPartType.getCode();
                            prefPart = true;
                            break;
                        case ALL_PARTS:
                            partTypeCode = null;
                            break;
                        case ALL_NAMES:
                            partTypeCode = defaultPartType.getCode();
                            break;
                        default:
                            throw new NotImplementedException("Neimplementovaný stav oblasti: " + area);
                    }
                    if (searchFilterVO.getOnlyMainPart() && !area.equals(Area.ALL_PARTS)) {
                        and = processValueCondDef(ctx, and, keyWord, partTypeCode, "NM_MAIN", null, QueryComparator.CONTAIN, prefPart);
                    } else {
                        and = processIndexCondDef(ctx, and, keyWord, partTypeCode, prefPart);
                    }
                }
                condition = cb.and(condition, and);
            }
        }

        return condition;
    }

    private Predicate processValueCondDef(final Ctx ctx, final Predicate condition, final String value,
                                          final String partTypeCode, final String itemTypeCode, final String itemSpecCode,
                                          final QueryComparator comparator, final boolean prefPart) {
        CriteriaBuilder cb = ctx.cb;

        Predicate and = cb.conjunction();
        ctx.resetApItemRoot();

        String dataTypeCode;
        if (StringUtils.isNotEmpty(itemTypeCode)) {
            dataTypeCode = validateItemType(itemTypeCode);
            and = cb.and(and, ctx.getItemTypeJoin().get(RulItemType.CODE).in(itemTypeCode));
        } else {
            throw new IllegalArgumentException("Musí být vyplněn alespoň jeden typ prvku popisu v hodnotové podmínce");
        }

        if (StringUtils.isNotEmpty(itemSpecCode)) {
            validateItemSpec(itemSpecCode);
            and = cb.and(and, ctx.getItemSpecJoin().get(RulItemSpec.CODE).in(itemSpecCode));
        }

        if (partTypeCode != null) {
            Join<ApItem, ApPart> itemPartJoin = ctx.getItemPartJoin();
            if (prefPart) {
                itemPartJoin.on(cb.equal(itemPartJoin.get(ApPart.PART_ID), ctx.getAccessPointJoin().get(ApAccessPoint.FIELD_PREFFERED_PART_ID)));
                and = cb.and(and, cb.equal(ctx.getPartTypeJoin().get(RulPartType.CODE), partTypeCode));
            } else {
                and = cb.and(and, cb.equal(ctx.getPartTypeJoin().get(RulPartType.CODE), partTypeCode));
            }
        }

        return cb.and(condition,
                and,
                processValueComparator(ctx, comparator, dataTypeCode, value));
    }

    private Predicate processIndexCondDef(final Ctx ctx, final Predicate condition, final String value, final String partTypeCode, final boolean prefPart) {
        CriteriaBuilder cb = ctx.cb;

        ctx.resetApIndexRoot();
        Predicate and = cb.conjunction();

        if (partTypeCode != null) {
            Join<ApIndex, ApPart> indexPartJoin = ctx.getIndexPartJoin();
            if (prefPart) {
                indexPartJoin.on(cb.equal(indexPartJoin.get(ApPart.PART_ID), ctx.getAccessPointJoin().get(ApAccessPoint.FIELD_PREFFERED_PART_ID)));
                and = cb.and(and, cb.equal(ctx.getPartTypeJoin().get(RulPartType.CODE), partTypeCode));
            } else {
                and = cb.and(and, cb.equal(ctx.getPartTypeJoin().get(RulPartType.CODE), partTypeCode));
            }
        }
        Join<ApPart, ApIndex> apIndexRoot = ctx.getApIndexRoot();

        return cb.and(condition, and, cb.equal(apIndexRoot.get(ApIndex.INDEX_TYPE), "DISPLAY_NAME"),
                createPredicateIndexComp(ctx, QueryComparator.CONTAIN, value));
    }

    private Predicate createPredicateIndexComp(final Ctx ctx, QueryComparator comparator, String value) {
        CriteriaBuilder cb = ctx.cb;
        Join<ApPart, ApIndex> aeIndexRoot = ctx.getApIndexRoot();
        switch (comparator) {
            case EQ:
                return cb.equal(cb.lower(aeIndexRoot.get(ApIndex.VALUE)), value.toLowerCase());
            case CONTAIN:
                return cb.like(cb.lower(aeIndexRoot.get(ApIndex.VALUE)), "%" + value.toLowerCase() + "%");
            case START_WITH:
                return cb.like(cb.lower(aeIndexRoot.get(ApIndex.VALUE)), value.toLowerCase() + "%");
            case END_WITH:
                return cb.like(cb.lower(aeIndexRoot.get(ApIndex.VALUE)), "%" + value.toLowerCase());
            default:
                throw new IllegalArgumentException("Není možné v indexu použít comparator: " + comparator);
        }
    }

    private String validateItemType(String itemTypeCode) {
        RulItemType rulItemType = sdp.getItemType(itemTypeCode);
        RulDataType dataType = rulItemType.getDataType();
        return dataType.getCode();
    }

    private void validateItemSpec(String itemSpecCode) {
        sdp.getItemSpec(itemSpecCode);
    }

    private Predicate processValueComparator(final Ctx ctx, final QueryComparator comparator, final String dataTypeCode, final String value) {
        Comparator cmp;
        switch (DataType.fromCode(dataTypeCode)) {
            case TEXT:
                cmp = new TextComparator(ctx);
                break;
            case STRING:
                cmp = new StringComparator(ctx);
                break;
            case BIT:
                cmp = new BitComparator(ctx);
                break;
            case INT:
                cmp = new IntegerComparator(ctx);
                break;
            case COORDINATES:
                cmp = new CoordinatesComparator(ctx);
                break;
            case URI_REF:
                cmp = new LinkComparator(ctx);
                break;
            case RECORD_REF:
                cmp = new RecordRefComparator(ctx);
                break;
            case UNITDATE:
                cmp = new UnitdateComparator(ctx);
                break;
            case ENUM:
                cmp = new NullComparator(ctx);
                break;
            default:
                throw new IllegalArgumentException("Neplatný datový typ: " + dataTypeCode);
        }
        return cmp.toPredicate(comparator, value);
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
