package cz.tacr.elza.repository.specification.search;

import cz.tacr.cam.v1.client.controller.vo.QueryComparator;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUnitdate;
import cz.tacr.elza.domain.converter.UnitDateConverter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class UnitdateComparator implements Comparator {

    private final Ctx ctx;

    public UnitdateComparator(final Ctx ctx) {
        this.ctx = ctx;
    }

    @Override
    public Predicate toPredicate(final QueryComparator comparator, final String value) {
        CriteriaBuilder cb = ctx.cb;
        ArrDataUnitdate data = UnitDateConverter.convertToUnitDate(value, new ArrDataUnitdate());
        Long normalizedFrom = data.getNormalizedFrom();
        Long normalizedTo = data.getNormalizedTo();
        Join<ApItem, ArrData> dataJoin = ctx.getApItemRoot().join(ApItem.FIELD_DATA, JoinType.INNER);
        switch (comparator) {
            case CT_EQ:
                return cb.and(
                        cb.equal(cb.treat(dataJoin, ArrDataUnitdate.class).get(ArrDataUnitdate.NORMALIZED_FROM), normalizedFrom),
                        cb.equal(cb.treat(dataJoin, ArrDataUnitdate.class).get(ArrDataUnitdate.NORMALIZED_TO), normalizedTo)
                );
            case CT_GT:
                return cb.greaterThan(cb.treat(dataJoin, ArrDataUnitdate.class).get(ArrDataUnitdate.NORMALIZED_FROM), normalizedTo);
            case CT_GTE:
                return cb.greaterThanOrEqualTo(cb.treat(dataJoin, ArrDataUnitdate.class).get(ArrDataUnitdate.NORMALIZED_FROM), normalizedTo);
            case CT_LT:
                return cb.lessThan(cb.treat(dataJoin, ArrDataUnitdate.class).get(ArrDataUnitdate.NORMALIZED_TO), normalizedFrom);
            case CT_LTE:
                return cb.lessThanOrEqualTo(cb.treat(dataJoin, ArrDataUnitdate.class).get(ArrDataUnitdate.NORMALIZED_TO), normalizedFrom);
            case CT_CONTAIN:
                return cb.or(
                        cb.and(
                                cb.greaterThanOrEqualTo(cb.treat(dataJoin, ArrDataUnitdate.class).get(ArrDataUnitdate.NORMALIZED_FROM), normalizedFrom),
                                cb.lessThanOrEqualTo(cb.treat(dataJoin, ArrDataUnitdate.class).get(ArrDataUnitdate.NORMALIZED_FROM), normalizedTo)
                        ),
                        cb.and(
                                cb.greaterThanOrEqualTo(cb.treat(dataJoin, ArrDataUnitdate.class).get(ArrDataUnitdate.NORMALIZED_TO), normalizedFrom),
                                cb.lessThanOrEqualTo(cb.treat(dataJoin, ArrDataUnitdate.class).get(ArrDataUnitdate.NORMALIZED_TO), normalizedTo)
                        ),
                        cb.and(
                                cb.greaterThanOrEqualTo(cb.treat(dataJoin, ArrDataUnitdate.class).get(ArrDataUnitdate.NORMALIZED_TO), normalizedTo),
                                cb.lessThanOrEqualTo(cb.treat(dataJoin, ArrDataUnitdate.class).get(ArrDataUnitdate.NORMALIZED_FROM), normalizedFrom)
                        )
                );
            default:
                throw new IllegalArgumentException(unimplementedMessage(comparator, DataType.UNITDATE));
        }
    }
}
