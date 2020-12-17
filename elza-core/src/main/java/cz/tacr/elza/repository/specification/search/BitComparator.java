package cz.tacr.elza.repository.specification.search;

import cz.tacr.cam.client.controller.vo.QueryComparator;
import cz.tacr.elza.core.data.DataType;
import cz.tacr.elza.domain.ApItem;
import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrDataBit;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;

public class BitComparator implements Comparator {

    private final Ctx ctx;

    public BitComparator(final Ctx ctx) {
        this.ctx = ctx;
    }

    private static boolean parseBool(final String value) {
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Nebyla zadána hodnota pro vyhodnocení bool výrazu");
        }
        switch (value) {
            case "no":
            case "ne":
            case "0":
            case "false":
                return false;
            case "yes":
            case "ano":
            case "1":
            case "true":
                return true;
            default:
                throw new IllegalArgumentException("Neplatná hodnota pro vyhodnocení bool výrazu: " + value);
        }
    }

    @Override
    public Predicate toPredicate(final QueryComparator comparator, final String value) {
        CriteriaBuilder cb = ctx.cb;
        Join<ApItem, ArrData> dataJoin = ctx.getApItemRoot().join(ApItem.FIELD_DATA, JoinType.INNER);
        boolean booleanValue = parseBool(value.toLowerCase());
        if (comparator == QueryComparator.EQ) {
            return cb.equal(cb.lower(cb.treat(dataJoin, ArrDataBit.class).get(ArrDataBit.BIT_VALUE)), booleanValue);
        }
        throw new IllegalArgumentException(unimplementedMessage(comparator, DataType.BIT));
    }
}
