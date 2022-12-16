package cz.tacr.elza.repository.specification;

import static cz.tacr.elza.domain.convertor.UnitDateConvertorConsts.DEFAULT_INTERVAL_DELIMITER;
import static cz.tacr.elza.groovy.GroovyResult.DISPLAY_NAME_LOWER;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.data.jpa.domain.Specification;

import cz.tacr.cam.client.controller.vo.QueryComparator;
import cz.tacr.elza.controller.vo.Area;
import cz.tacr.elza.controller.vo.ExtensionFilterVO;
import cz.tacr.elza.controller.vo.RelationFilterVO;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApIndex;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.RevStateApproval;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.domain.UsrUser;
import cz.tacr.elza.domain.convertor.UnitDateConvertor;
import cz.tacr.elza.exception.BusinessException;
import cz.tacr.elza.exception.codes.BaseCode;
import cz.tacr.elza.repository.specification.search.BitComparator;
import cz.tacr.elza.repository.specification.search.Comparator;
import cz.tacr.elza.repository.specification.search.CoordinatesComparator;
import cz.tacr.elza.repository.specification.search.Ctx;
import cz.tacr.elza.repository.specification.search.DateComparator;
import cz.tacr.elza.repository.specification.search.DecimalComparator;
import cz.tacr.elza.repository.specification.search.FileRefComparator;
import cz.tacr.elza.repository.specification.search.IntegerComparator;
import cz.tacr.elza.repository.specification.search.JsonTableComparator;
import cz.tacr.elza.repository.specification.search.LinkComparator;
import cz.tacr.elza.repository.specification.search.NullComparator;
import cz.tacr.elza.repository.specification.search.RecordRefComparator;
import cz.tacr.elza.repository.specification.search.StringComparator;
import cz.tacr.elza.repository.specification.search.StructuredComparator;
import cz.tacr.elza.repository.specification.search.TextComparator;
import cz.tacr.elza.repository.specification.search.UnitIdComparator;
import cz.tacr.elza.repository.specification.search.UnitdateComparator;

public class ApStateSpecification implements Specification<ApState> {

    private SearchFilterVO searchFilterVO;
    private Set<Integer> apTypeIdTree;
    private Set<Integer> scopeIds;
    private ApState.StateApproval state;
    private RevStateApproval revState;
    private StaticDataProvider sdp;

    public ApStateSpecification(final SearchFilterVO searchFilterVO, Set<Integer> apTypeIdTree, Set<Integer> scopeIds,
                                ApState.StateApproval state, RevStateApproval revState, final StaticDataProvider sdp) {
        this.searchFilterVO = searchFilterVO;
        this.apTypeIdTree = apTypeIdTree;
        this.scopeIds = scopeIds;
        this.state = state;
        this.revState = revState;
        this.sdp = sdp;
    }

    @Override
    public Predicate toPredicate(Root<ApState> stateRoot, CriteriaQuery<?> q, CriteriaBuilder cb) {
        Ctx ctx = new Ctx(cb, q);
        q.distinct(true);
        ctx.setStateRoot(stateRoot);

        Join<ApState, ApAccessPoint> accessPointJoin = stateRoot.join(ApState.FIELD_ACCESS_POINT, JoinType.INNER);
        Join<ApAccessPoint, ApPart> preferredPartJoin = accessPointJoin.join(ApAccessPoint.FIELD_PREFFERED_PART, JoinType.INNER);
        ctx.setAccessPointJoin(accessPointJoin);
        ctx.setPreferredPartJoin(preferredPartJoin);

        Predicate condition = cb.conjunction();

        // omezení dle oblasti
        Validate.isTrue(!scopeIds.isEmpty());
        condition = cb.and(condition, stateRoot.get(ApState.FIELD_SCOPE_ID).in(scopeIds));

        // typ archivní entity
        if (CollectionUtils.isNotEmpty(apTypeIdTree)) {
            condition = cb.and(condition, stateRoot.get(ApState.FIELD_AP_TYPE_ID).in(apTypeIdTree));
        }

        // omezení dle stavu
        if (state != null) {
            condition = cb.and(condition, stateRoot.get(ApState.FIELD_STATE_APPROVAL).in(state));
        }

        if (revState != null) {
            Root<ApRevision> revisionRoot = q.from(ApRevision.class);
            Join<ApRevision, ApState> revisionApStateJoin = revisionRoot.join(ApRevision.FIELD_STATE, JoinType.INNER);

            condition = cb.and(condition,
                    cb.equal(stateRoot.get(ApState.FIELD_STATE_ID), revisionApStateJoin.get(ApState.FIELD_STATE_ID)),
                    cb.isNull(revisionRoot.get(ApRevision.FIELD_DELETE_CHANGE_ID)),
                    revisionRoot.get(ApRevision.FIELD_STATE_APPROVAL).in(revState));
        }

        // pouze aktuální state
        condition = cb.and(condition, cb.isNull(stateRoot.get(ApState.FIELD_DELETE_CHANGE_ID)));

        if (searchFilterVO != null) {
            String user = searchFilterVO.getUser();
            if (StringUtils.isNotEmpty(user)) {
                Join<ApState, ApChange> apChangeJoin = stateRoot.join(ApState.FIELD_CREATE_CHANGE, JoinType.INNER);
                condition = cb.and(condition, cb.like(cb.lower(apChangeJoin.get(ApChange.USER).get(UsrUser.FIELD_USERNAME)), "%" + user.toLowerCase() + "%"));
            }

            // omezení dle konkrétních archivních entit
            String code = searchFilterVO.getCode();
            if (StringUtils.isNotEmpty(code)) {
                try {
                    Integer id = Integer.parseInt(code);
                    condition = cb.and(condition, accessPointJoin.get(ApAccessPoint.FIELD_ACCESS_POINT_ID).in(id));
                } catch (NumberFormatException e) {

                }
            }

            condition = cb.and(condition, process(cb.conjunction(), ctx));
        }

        Join<ApIndex, ApPart> indexJoin = preferredPartJoin.join(ApPart.INDICES, JoinType.INNER);
        indexJoin.on(cb.equal(indexJoin.get(ApIndex.INDEX_TYPE), DISPLAY_NAME_LOWER));
        Path<String> accessPointName = indexJoin.get(ApIndex.VALUE);
        q.orderBy(cb.asc(accessPointName));

        return condition;
    }

    private Predicate process(Predicate condition, Ctx ctx) {
        CriteriaBuilder cb = ctx.cb;
        String search = searchFilterVO.getSearch();
        Area area = searchFilterVO.getArea();
        if (area != Area.ENTITY_CODE) {
            Predicate and = cb.conjunction();
            if (StringUtils.isNotEmpty(search)) {
                List<String> keyWords = getKeyWordsFromSearch(search);
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
            }
            if (CollectionUtils.isNotEmpty(searchFilterVO.getExtFilters())) {
                for (ExtensionFilterVO ext : searchFilterVO.getExtFilters()) {
                    String itemTypeCode = ext.getItemTypeId() != null ? sdp.getItemTypeById(ext.getItemTypeId()).getCode() : null;
                    String itemSpecCode = ext.getItemSpecId() != null ? sdp.getItemSpecById(ext.getItemSpecId()).getCode() : null;
                    and = processValueCondDef(ctx, and, String.valueOf(ext.getValue()), ext.getPartTypeCode(), itemTypeCode,
                            itemSpecCode, QueryComparator.CONTAIN, false);
                }
            }
            if (CollectionUtils.isNotEmpty(searchFilterVO.getRelFilters())) {
                for (RelationFilterVO rel : searchFilterVO.getRelFilters()) {
                    if (rel.getCode() != null) {
                        // relation without item type
                        if(rel.getRelTypeId()==null) {
                            and = processValueCondDef(ctx, and, String.valueOf(rel.getCode()),
                                                      DataType.RECORD_REF, QueryComparator.EQ);
                        } else {
                            String itemTypeCode = rel.getRelTypeId() != null ? sdp.getItemTypeById(rel.getRelTypeId()).getCode() : null;                                                
                            String itemSpecCode = rel.getRelSpecId() != null ? sdp.getItemSpecById(rel.getRelSpecId()).getCode() : null;
                            and = processValueCondDef(ctx, and, String.valueOf(rel.getCode()), null, itemTypeCode,
                                itemSpecCode, QueryComparator.EQ, false);
                        }
                    }
                }
            }
            if (StringUtils.isNotEmpty(searchFilterVO.getCreation())) {
                ArrDataUnitdate arrDataUnitdate = UnitDateConvertor.convertToUnitDate(searchFilterVO.getCreation(), new ArrDataUnitdate());
                String intervalCreation = arrDataUnitdate.getValueFrom() + DEFAULT_INTERVAL_DELIMITER + arrDataUnitdate.getValueTo();
                and = processValueCondDef(ctx, and, intervalCreation, "PT_CRE", "CRE_DATE", null, QueryComparator.CONTAIN, false);
            }
            if (StringUtils.isNotEmpty(searchFilterVO.getExtinction())) {
                ArrDataUnitdate arrDataUnitdate = UnitDateConvertor.convertToUnitDate(searchFilterVO.getExtinction(), new ArrDataUnitdate());
                String intervalExtinction = arrDataUnitdate.getValueFrom() + DEFAULT_INTERVAL_DELIMITER + arrDataUnitdate.getValueTo();
                and = processValueCondDef(ctx, and, intervalExtinction, "PT_EXT", "EXT_DATE", null, QueryComparator.CONTAIN, false);
            }
            condition = cb.and(condition, and);
        }

        return condition;
    }

    /**
     * Podminka pro hledani shody na zaklade hodnoty bez znalosti itemType
     * 
     * @param ctx
     * @param condition
     * @param value
     * @param dataType
     * @param comparator
     * @return
     */
    private Predicate processValueCondDef(final Ctx ctx, final Predicate condition,
                                          final String value,
                                          final DataType dataType,
                                          final QueryComparator comparator) {
        CriteriaBuilder cb = ctx.cb;

        Predicate and = cb.conjunction();
        ctx.resetApItemRoot();

        // zajimaji nas jen platne apItem
        and = cb.and(and, cb.isNull(ctx.getApItemRoot().get(ApItem.DELETE_CHANGE_ID)));

        return cb.and(condition,
                      and,
                      processValueComparator(ctx, comparator, dataType, value));
    }

    private Predicate processValueCondDef(final Ctx ctx, final Predicate condition, final String value,
                                          final String partTypeCode,
                                          final String itemTypeCode,
                                          final String itemSpecCode,
                                          final QueryComparator comparator, final boolean prefPart) {
        CriteriaBuilder cb = ctx.cb;

        Predicate and = cb.conjunction();
        ctx.resetApItemRoot();

        // zajimaji nas jen platne apItem
        and = cb.and(and, cb.isNull(ctx.getApItemRoot().get(ApItem.DELETE_CHANGE_ID)));

        if (StringUtils.isEmpty(itemTypeCode)) {
            throw new BusinessException("ItemType is null", BaseCode.INVALID_STATE);
        }
        RulItemType rulItemType = sdp.getItemType(itemTypeCode);
        DataType dataType = DataType.fromId(rulItemType.getDataTypeId());
        and = cb.and(and, ctx.getItemTypeJoin().get(RulItemType.CODE).in(itemTypeCode));

        if (StringUtils.isNotEmpty(itemSpecCode)) {
            validateItemSpec(itemSpecCode);
            and = cb.and(and, ctx.getItemSpecJoin().get(RulItemSpec.CODE).in(itemSpecCode));
        }

        if (partTypeCode != null) {
            addPartTypeCondForItem(ctx, cb, and, prefPart, partTypeCode);
        }

        return cb.and(condition,
                      and,
                      processValueComparator(ctx, comparator, dataType, value));
    }

    private void addPartTypeCondForItem(Ctx ctx, CriteriaBuilder cb, Predicate and,
                                        boolean prefPart, String partTypeCode) {
        Join<ApItem, ApPart> itemPartJoin = ctx.getItemPartJoin();
        if (prefPart) {
            itemPartJoin.on(cb.equal(itemPartJoin.get(ApPart.PART_ID), ctx.getAccessPointJoin().get(
                                                                                                    ApAccessPoint.FIELD_PREFFERED_PART_ID)));
            and = cb.and(and, cb.equal(ctx.getPartTypeJoin().get(RulPartType.CODE), partTypeCode));
        } else {
            and = cb.and(and, cb.equal(ctx.getPartTypeJoin().get(RulPartType.CODE), partTypeCode));
            // zajimaji nas jen nesmazane part
            and = cb.and(and, cb.isNull(itemPartJoin.get(ApPart.DELETE_CHANGE_ID)));
        }
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

    private void validateItemSpec(String itemSpecCode) {
        sdp.getItemSpec(itemSpecCode);
    }

    private Predicate processValueComparator(final Ctx ctx, final QueryComparator comparator, 
                                             final DataType dataType, final String value) {
        Comparator cmp;
        switch (dataType) {
            case FORMATTED_TEXT:
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
            case UNITID:
                cmp = new UnitIdComparator(ctx);
                break;
            case DECIMAL:
                cmp = new DecimalComparator(ctx);
                break;
            case STRUCTURED:
                cmp = new StructuredComparator(ctx);
                break;
            case FILE_REF:
                cmp = new FileRefComparator(ctx);
                break;
            case JSON_TABLE:
                cmp = new JsonTableComparator(ctx);
                break;
            case DATE:
                cmp = new DateComparator(ctx);
                break;
            default:
                throw new IllegalArgumentException("Neplatný datový typ: " + dataType);
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
