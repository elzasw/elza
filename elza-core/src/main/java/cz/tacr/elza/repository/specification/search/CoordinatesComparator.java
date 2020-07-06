package cz.tacr.elza.repository.specification.search;


import cz.tacr.cam.client.controller.vo.QueryComparator;
import cz.tacr.elza.core.data.DataType;

import javax.persistence.criteria.Predicate;

public class CoordinatesComparator implements Comparator {

    private final Ctx ctx;

    public CoordinatesComparator(final Ctx ctx) {
        this.ctx = ctx;
    }

    @Override
    public Predicate toPredicate(final QueryComparator comparator, final String value) {
        throw new IllegalArgumentException(unimplementedMessage(comparator, DataType.COORDINATES));
    }
}
