package cz.tacr.elza.repository.specification.search;


import cz.tacr.cam.client.controller.vo.QueryComparator;

import javax.persistence.criteria.Predicate;

public class NullComparator implements Comparator {

    private final Ctx ctx;

    public NullComparator(final Ctx ctx) {
        this.ctx = ctx;
    }

    @Override
    public Predicate toPredicate(final QueryComparator comparator, final String value) {
        return ctx.cb.conjunction();
    }
}
