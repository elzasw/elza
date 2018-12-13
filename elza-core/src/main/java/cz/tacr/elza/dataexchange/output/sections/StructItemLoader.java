package cz.tacr.elza.dataexchange.output.sections;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Root;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;

public class StructItemLoader extends AbstractEntityLoader<ArrStructuredItem> {

    Join<ArrStructuredItem, RulItemType> joinItemType;
    Join<ArrStructuredItem, RulItemSpec> joinItemSpec;

    public StructItemLoader(EntityManager em, int batchSize) {
        super(ArrStructuredItem.class, ArrStructuredItem.STRUCT_OBJ_FK, em, batchSize);
    }

    @Override
    protected void buildExtendedQuery(Root<?> root, CriteriaBuilder cb) {
        root.fetch(ArrStructuredItem.FIELD_DATA);
        joinItemType = root.join(ArrItem.FIELD_ITEM_TYPE);
        joinItemSpec = root.join(ArrItem.FIELD_ITEM_SPEC, JoinType.LEFT);
    }

    private List<Order> createQueryOrderBy(CriteriaBuilder cb) {
        List<Order> orderList = new ArrayList<>();
        orderList.add(cb.asc(joinItemType.get(RulItemType.FIELD_VIEW_ORDER)));
        orderList.add(cb.asc(joinItemType.get(RulItemSpec.FIELD_VIEW_ORDER)));
        orderList.add(cb.asc(joinItemType.get(ArrItem.FIELD_POSITION)));
        return orderList;
    }
}
