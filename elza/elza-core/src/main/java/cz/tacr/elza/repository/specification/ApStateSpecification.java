package cz.tacr.elza.repository.specification;

import cz.tacr.elza.controller.vo.Area;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApPart;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.ApType;
import cz.tacr.elza.domain.RulPartType;
import cz.tacr.elza.repository.specification.search.Ctx;
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

    public ApStateSpecification(final SearchFilterVO searchFilterVO) {
        this.searchFilterVO = searchFilterVO;
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

        // typ archivní entity
        List<Integer> aeTypeIdList =  searchFilterVO.getAeTypeIds();
        if (CollectionUtils.isNotEmpty(aeTypeIdList)) {
            condition = cb.and(condition, stateRoot.get(ApState.FIELD_AP_TYPE_ID).in(aeTypeIdList));
        }

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
                StaticDataProvider sdp = StaticDataProvider.getInstance();
                RulPartType defaultPartType = sdp.getDefaultPartType();
                for (String keyWord : keyWords) {
                    Predicate cond = processValueCondDef(ctx, keyWord);
                    switch (area) {
                        case PREFER_NAMES:
                            and = cb.and(and, processPartCondDef(ctx, cond, defaultPartType.getCode(), true));
                            break;
                        case ALL_PARTS:
                            and = cb.and(and, cond);
                            break;
                        case ALL_NAMES:
                            and = cb.and(and, processPartCondDef(ctx, cond, defaultPartType.getCode(), false));
                            break;
                        default:
                            throw new NotImplementedException("Neimplementovaný stav oblasti: " + area);
                    }
                }
                condition = cb.and(condition, and);
            }
        }

        return condition;
    }

    private Predicate processValueCondDef(Ctx ctx, String keyWord) {
        return null;
    }

    private Predicate processPartCondDef(Ctx ctx, Predicate cond, String partTypeCode, boolean b) {
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
