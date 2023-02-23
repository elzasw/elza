package cz.tacr.elza.repository.specification.search;


import cz.tacr.cam.client.controller.vo.QueryComparator;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrDataRecordRef;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class RecordRefComparator implements Comparator {

    private final Ctx ctx;

    public RecordRefComparator(final Ctx ctx) {
        this.ctx = ctx;
    }

    @Override
    public Predicate toPredicate(final QueryComparator comparator, final String value) {
        CriteriaBuilder cb = ctx.cb;
        Join<ApItem, ArrDataRecordRef> dataJoin = cb.treat(ctx.getApItemRoot().join(ApItem.FIELD_DATA, JoinType.INNER), ArrDataRecordRef.class);
        Join<ArrDataRecordRef, ApAccessPoint> recordJoin = dataJoin.join(ArrDataRecordRef.FIELD_RECORD, JoinType.INNER);
        Integer code = Integer.parseInt(value);
        if (comparator == QueryComparator.EQ || comparator == QueryComparator.CONTAIN) {
            return cb.equal(recordJoin.get(ApAccessPoint.FIELD_ACCESS_POINT_ID), code);
        }
        throw new IllegalArgumentException(unimplementedMessage(comparator, DataType.RECORD_REF));
    }
}
