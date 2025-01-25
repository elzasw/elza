package cz.tacr.elza.dataexchange.output.loaders;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;

import cz.tacr.elza.common.db.HibernateUtils;

/**
 * Abstract implementation for entity batch loader.
 */
public abstract class AbstractEntityLoader<RES, ENT> extends AbstractBatchLoader<Object, RES> {

    private final Class<? extends ENT> entityClass;

    private final String entityIdPath;

    private final EntityManager em;
    
    protected AbstractEntityLoader(final Class<? extends ENT> entityClass,
    		final String entityIdPath,
            final EntityManager em,
            final int batchSize) {
        super(batchSize);
        this.entityClass = Objects.requireNonNull(entityClass);
        this.entityIdPath = Objects.requireNonNull(entityIdPath);
        this.em = Objects.requireNonNull(em);
    }
    
    private void storeResult(Object entityId, Object entity, Map<Object, List<BatchEntry>> entityIdLookup) {
		// can be initialized (detached) proxy
		entity = HibernateUtils.unproxy(entity);
    	
    	for (BatchEntry entry : entityIdLookup.get(entityId)) {
    		RES result = createResult(entity);                    
    		entry.setResult(result);
    	}		
	}

    @Override
    protected final void processBatch(List<BatchEntry> entries) {
        Map<Object, List<BatchEntry>> entityIdLookup = getEntityIdLookup(entries);

        CriteriaQuery<Tuple> cq = createCriteriaQuery(entityIdLookup.keySet());

        Query<Tuple> q = createHibernateQuery(cq);

        try (ScrollableResults<Tuple> results = q.scroll(ScrollMode.FORWARD_ONLY)) {						
			while (results.next()) {
				Tuple tuple = results.get();
				Object entityId = tuple.get(0);
				Object entity = tuple.get(1);

				// TODO: replace detach for stateless session
				em.detach(entity);
				
				storeResult(entityId, entity, entityIdLookup);
			}
        }
	}

	/**
     * Use entity as a result
     *
     * Override this method if result is not same as entity
     * or entity has to be adjusted/modified.
     *
     * @param entity
     * @return
     */
    @SuppressWarnings("unchecked")
    protected RES createResult(Object entity) {
        return (RES) entity;
    }

    /**
     * Sets additional fetches to root entity. Default implementation is empty.
     */
    protected void buildExtendedQuery(Root<? extends ENT> root, CriteriaBuilder cb) {
    }

    /**
     * Creates query condition which is used as conjunction with id search. Default
     * implementation returns null.
     *
     * @param criteriaQuery
     */
    protected Predicate createQueryCondition(CriteriaQuery<Tuple> criteriaQuery,
                                             Path<? extends ENT> root,
                                             CriteriaBuilder cb) {
        return null;
    }

    private <T> Query<T> createHibernateQuery(CriteriaQuery<T> criteriaQuery) {
        Session session = em.unwrap(Session.class);
        Query<T> query = session.createQuery(criteriaQuery);
        query.setCacheable(false);
        query.setReadOnly(true);
        return query;
    }

    private CriteriaQuery<Tuple> createCriteriaQuery(Set<Object> entityIds) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<? extends ENT> root = cq.from(entityClass);
        buildExtendedQuery(root, cb);

        // prepare where
        Path<?> jpaPath = getJpaPath(root, entityIdPath);
        Predicate cond = createQueryCondition(cq, root, cb);
        if (cond != null) {
            cond = cb.and(jpaPath.in(entityIds), cond);
        } else {
            cond = jpaPath.in(entityIds);
        }
        cq.where(cond);
        List<Order> order = createQueryOrderBy(root, cb);
        if (order != null) {
            cq.orderBy(order);
        }

        cq.multiselect(jpaPath, root);

        return cq;
    }

    protected List<Order> createQueryOrderBy(Root<? extends ENT> root, CriteriaBuilder cb) {
        return null;
    }

    /**
     * Builds attribute path for JPA API. FK or joined entities are accessible
     * through dot notation.
     *
     * @param root
     *            base entity
     * @param jpaPath
     *            to attribute
     * @return Simple or compound path to attribute.
     */
    public static Path<?> getJpaPath(Root<?> root, String jpaPath) {
        final String[] parts = jpaPath.split("\\.");
        if (parts.length == 0) {
            return root.get(jpaPath);
        }
        Path<?> last = root.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            last = last.get(parts[i]);
        }
        return last;
    }
}
