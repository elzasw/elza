package cz.tacr.elza.dataexchange.output.sections;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;

public class StructItemLoader extends AbstractEntityLoader<ArrStructuredItem, ArrStructuredItem> {

    Join<ArrStructuredItem, RulItemType> joinItemType;
    Join<ArrStructuredItem, RulItemSpec> joinItemSpec;

    public StructItemLoader(EntityManager em, int batchSize) {
        super(ArrStructuredItem.class, ArrStructuredItem.STRUCT_OBJ_FK, em, batchSize);
    }

    @Override
    protected void buildExtendedQuery(Root<? extends ArrStructuredItem> root, CriteriaBuilder cb) {
        root.fetch(ArrStructuredItem.FIELD_DATA);
        joinItemType = root.join(ArrItem.FIELD_ITEM_TYPE);
        joinItemSpec = root.join(ArrItem.FIELD_ITEM_SPEC, JoinType.LEFT);
    }

    @Override
    protected Predicate createQueryCondition(CriteriaQuery<Tuple> cq,
                                             Path<? extends ArrStructuredItem> root, CriteriaBuilder cb) {
        return root.get(ArrItem.FIELD_DELETE_CHANGE_ID).isNull();
    }

    @Override
    protected List<Order> createQueryOrderBy(Root<? extends ArrStructuredItem> root, CriteriaBuilder cb) {
        List<Order> orderList = new ArrayList<>();
        orderList.add(cb.asc(joinItemType.get(RulItemType.FIELD_VIEW_ORDER)));
        orderList.add(cb.asc(joinItemType.get(RulItemSpec.FIELD_VIEW_ORDER)));
        orderList.add(cb.asc(root.get(ArrItem.FIELD_POSITION)));
        return orderList;
    }
}
