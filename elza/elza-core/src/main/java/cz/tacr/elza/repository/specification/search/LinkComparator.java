package cz.tacr.elza.repository.specification.search;


import cz.tacr.cam.client.controller.vo.QueryComparator;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataUriRef;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;

public class LinkComparator implements Comparator {

    private final Ctx ctx;

    public LinkComparator(final Ctx ctx) {
        this.ctx = ctx;
    }

    @Override
    public Predicate toPredicate(final QueryComparator comparator, final String value) {
        CriteriaBuilder cb = ctx.cb;
        Join<ApItem, ArrData> dataJoin = ctx.getApItemRoot().join(ApItem.FIELD_DATA, JoinType.INNER);
        String lowerValue = value.toLowerCase();
        switch (comparator) {
            case EQ:
                return cb.or(
                        cb.equal(cb.lower(cb.treat(dataJoin, ArrDataUriRef.class).get(ArrDataUriRef.URI_REF_VALUE)), lowerValue),
                        cb.equal(cb.lower(cb.treat(dataJoin, ArrDataUriRef.class).get(ArrDataUriRef.DESCRIPTION)), lowerValue)
                );
            case CONTAIN:
                return cb.or(
                        cb.like(cb.lower(cb.treat(dataJoin, ArrDataUriRef.class).get(ArrDataUriRef.URI_REF_VALUE)), "%" + lowerValue + "%"),
                        cb.like(cb.lower(cb.treat(dataJoin, ArrDataUriRef.class).get(ArrDataUriRef.DESCRIPTION)), "%" + lowerValue + "%")
                );
            case START_WITH:
                return cb.or(
                        cb.like(cb.lower(cb.treat(dataJoin, ArrDataUriRef.class).get(ArrDataUriRef.URI_REF_VALUE)), lowerValue + "%"),
                        cb.like(cb.lower(cb.treat(dataJoin, ArrDataUriRef.class).get(ArrDataUriRef.DESCRIPTION)), lowerValue + "%")
                );
            case END_WITH:
                return cb.or(
                        cb.like(cb.lower(cb.treat(dataJoin, ArrDataUriRef.class).get(ArrDataUriRef.URI_REF_VALUE)), "%" + lowerValue),
                        cb.like(cb.lower(cb.treat(dataJoin, ArrDataUriRef.class).get(ArrDataUriRef.DESCRIPTION)), "%" + lowerValue)
                );
            default:
                throw new IllegalArgumentException(unimplementedMessage(comparator, DataType.URI_REF));
        }
    }
}
