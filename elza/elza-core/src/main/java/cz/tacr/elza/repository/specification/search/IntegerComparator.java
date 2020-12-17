package cz.tacr.elza.repository.specification.search;



import cz.tacr.cam.client.controller.vo.QueryComparator;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataInteger;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;

public class IntegerComparator implements Comparator {

    private final Ctx ctx;

    public IntegerComparator(final Ctx ctx) {
        this.ctx = ctx;
    }

    @Override
    public Predicate toPredicate(final QueryComparator comparator, final String value) {
        CriteriaBuilder cb = ctx.cb;
        Join<ApItem, ArrData> dataJoin = ctx.getApItemRoot().join(ApItem.FIELD_DATA, JoinType.INNER);
        int numberValue = Integer.parseInt(value);
        switch (comparator) {
            case EQ:
                return cb.equal(cb.lower(cb.treat(dataJoin, ArrDataInteger.class).get(ArrDataInteger.INTEGER_VALUE)), numberValue);
            case GT:
                return cb.greaterThan(cb.treat(dataJoin, ArrDataInteger.class).get(ArrDataInteger.INTEGER_VALUE), numberValue);
            case GTE:
                return cb.greaterThanOrEqualTo(cb.treat(dataJoin, ArrDataInteger.class).get(ArrDataInteger.INTEGER_VALUE), numberValue);
            case LT:
                return cb.lessThan(cb.treat(dataJoin, ArrDataInteger.class).get(ArrDataInteger.INTEGER_VALUE), numberValue);
            case LTE:
                return cb.lessThanOrEqualTo(cb.treat(dataJoin, ArrDataInteger.class).get(ArrDataInteger.INTEGER_VALUE), numberValue);
            default:
                throw new IllegalArgumentException(unimplementedMessage(comparator, DataType.INT));
        }
    }
}
