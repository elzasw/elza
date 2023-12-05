package cz.tacr.elza.repository.specification.search;


import cz.tacr.cam.client.controller.vo.QueryComparator;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataText;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class TextComparator implements Comparator {

    private final Ctx ctx;

    public TextComparator(final Ctx ctx) {
        this.ctx = ctx;
    }

    @Override
    public Predicate toPredicate(final QueryComparator comparator, final String value) {
        CriteriaBuilder cb = ctx.cb;
        Join<ApItem, ArrData> dataJoin = ctx.getApItemRoot().join(ApItem.FIELD_DATA, JoinType.INNER);
        String lowerValue = value.toLowerCase();
        switch (comparator) {
            case EQ:
                return cb.equal(cb.lower(cb.treat(dataJoin, ArrDataText.class).get(ArrDataText.TEXT_VALUE)), lowerValue);
            case CONTAIN:
                return cb.like(cb.lower(cb.treat(dataJoin, ArrDataText.class).get(ArrDataText.TEXT_VALUE)), "%" + lowerValue + "%");
            case START_WITH:
                return cb.like(cb.lower(cb.treat(dataJoin, ArrDataText.class).get(ArrDataText.TEXT_VALUE)), lowerValue + "%");
            case END_WITH:
                return cb.like(cb.lower(cb.treat(dataJoin, ArrDataText.class).get(ArrDataText.TEXT_VALUE)), "%" + lowerValue);
            default:
                throw new IllegalArgumentException(unimplementedMessage(comparator, DataType.TEXT));
        }
    }
}
