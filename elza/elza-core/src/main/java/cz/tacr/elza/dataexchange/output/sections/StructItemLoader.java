package cz.tacr.elza.dataexchange.output.sections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
import cz.tacr.elza.dataexchange.output.loaders.AbstractBatchLoader;
import cz.tacr.elza.dataexchange.output.loaders.AbstractEntityLoader;
import cz.tacr.elza.domain.ArrItem;
import cz.tacr.elza.domain.ArrStructuredItem;
import cz.tacr.elza.domain.RulItemSpec;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.service.DataService;

public class StructItemLoader extends AbstractBatchLoader<Integer, ArrStructuredItem> {

    Join<ArrStructuredItem, RulItemType> joinItemType;
    Join<ArrStructuredItem, RulItemSpec> joinItemSpec;
    
    final EntityManager em;
    final DataService dataService;

    public StructItemLoader(final DataService dataService, 
    		final EntityManager em, 
    		final int batchSize) {    	
        super(batchSize);
        this.em = em;
        this.dataService = dataService;
    }

	@Override
	protected void processBatch(List<AbstractBatchLoader<Integer, ArrStructuredItem>.BatchEntry> entries) {
		Map<Integer, List<BatchEntry>> entityIdLookup = getEntityIdLookup(entries);
		CriteriaQuery<ArrStructuredItem> cq = prepareQuery(entityIdLookup);
		List<ArrStructuredItem> result = em.createQuery(cq).getResultList();
		dataService.findItemsWithData(result);
		// store results
		for(ArrStructuredItem si: result) {
			var list = entityIdLookup.get(si.getStructuredObjectId());
			Objects.requireNonNull(list, () -> "Cannot find list for entity id " + si.getStructuredObjectId());
			list.forEach(entry -> entry.setResult(si));
		}
	}
    private CriteriaQuery<ArrStructuredItem> prepareQuery(
			Map<Integer, List<AbstractBatchLoader<Integer, ArrStructuredItem>.BatchEntry>> entityIdLookup) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ArrStructuredItem> cq = cb.createQuery(ArrStructuredItem.class);

        Root<? extends ArrStructuredItem> root = cq.from(ArrStructuredItem.class);
    	// Cannot fetch field data directly because of inheritance and many tables would be needed
        // root.fetch(ArrStructuredItem.FIELD_DATA);
        joinItemType = root.join(ArrItem.FIELD_ITEM_TYPE);
        joinItemSpec = root.join(ArrItem.FIELD_ITEM_SPEC, JoinType.LEFT);

        // prepare where
        Path<?> jpaPath = root.get(ArrStructuredItem.STRUCT_OBJ_FK);
        Predicate cond = root.get(ArrItem.FIELD_DELETE_CHANGE_ID).isNull();
        cond = cb.and(jpaPath.in(entityIdLookup.keySet()), cond);
        cq.where(cond);
        List<Order> order = createQueryOrderBy(root, cb);
        if (order != null) {
            cq.orderBy(order);
        }
        //cq.multiselect(jpaPath, root);
		return cq;
	}

    protected List<Order> createQueryOrderBy(Root<? extends ArrStructuredItem> root, CriteriaBuilder cb) {
        List<Order> orderList = new ArrayList<>();
        orderList.add(cb.asc(joinItemType.get(RulItemType.FIELD_VIEW_ORDER)));
        orderList.add(cb.asc(joinItemType.get(RulItemSpec.FIELD_VIEW_ORDER)));
        orderList.add(cb.asc(root.get(ArrItem.FIELD_POSITION)));
        return orderList;
    }
}
