package cz.tacr.elza.dataexchange.output.loaders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import cz.tacr.elza.domain.ApItem;
import org.apache.commons.lang3.Validate;
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

    protected AbstractEntityLoader(Class<? extends ENT> entityClass,
            String entityIdPath,
            EntityManager em,
            int batchSize) {
        super(batchSize);
        this.entityClass = Validate.notNull(entityClass);
        this.entityIdPath = Validate.notNull(entityIdPath);
        this.em = Validate.notNull(em);

    }

    @Override
    protected void processItemBatch(ArrayList<BatchEntry> entries) {
        Map<Object, List<BatchEntry>> entityIdLookup = getEntityIdLookup(entries);
        CriteriaQuery<Tuple> cq = createCriteriaItemQuery(entityIdLookup.keySet());

        Query<Tuple> q = createHibernateQuery(cq);

        try (ScrollableResults results = q.scroll(ScrollMode.FORWARD_ONLY)) {
            while (results.next()) {
                Tuple tuple = (Tuple) results.get(0);
                Object entityId = tuple.get(0);
                Object entity = tuple.get(1);

                // TODO: replace detach for stateless session
                em.detach(entity);
                // can be initialized (detached) proxy
                entity = HibernateUtils.unproxy(entity);

                for (BatchEntry entry : entityIdLookup.get(entityId)) {
                    RES result = createResult(entity);
                    entry.setResult(result);
                }
            }
        }

    }

    @Override
    protected final void processBatch(ArrayList<BatchEntry> entries) {
        Map<Object, List<BatchEntry>> entityIdLookup = getEntityIdLookup(entries);

        CriteriaQuery<Tuple> cq = createCriteriaQuery(entityIdLookup.keySet());

        Query<Tuple> q = createHibernateQuery(cq);

        try (ScrollableResults results = q.scroll(ScrollMode.FORWARD_ONLY)) {
            while (results.next()) {
                Tuple tuple = (Tuple) results.get(0);
                Object entityId = tuple.get(0);
                Object entity = tuple.get(1);

                // TODO: replace detach for stateless session
                em.detach(entity);
                // can be initialized (detached) proxy
                entity = HibernateUtils.unproxy(entity);

                for (BatchEntry entry : entityIdLookup.get(entityId)) {
                    RES result = createResult(entity);
                    entry.setResult(result);
                }
            }
        }
    }

    /**
     * Use entity as a result
     *
     * Override this method if result is same as entity
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
     */
    protected Predicate createQueryCondition(Path<? extends ENT> root, CriteriaBuilder cb) {
        return null;
    }

    /**
     * Groups requests with same id. Key set is used for query as IN search. Map of
     * values is used as lookup for result.
     */
    private Map<Object, List<BatchEntry>> getEntityIdLookup(Collection<BatchEntry> entries) {
        Map<Object, List<BatchEntry>> lookup = new HashMap<>(entries.size());
        for (BatchEntry entry : entries) {
            Object id = entry.getRequest();
            List<BatchEntry> group = lookup.get(id);
            if (group == null) {
                lookup.put(id, group = new ArrayList<>());
            }
            group.add(entry);
        }
        return lookup;
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
        Predicate cond = createQueryCondition(root, cb);
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

    private CriteriaQuery<Tuple> createCriteriaItemQuery(Set<Object> entityIds) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<? extends ENT> root = cq.from(entityClass);

        root.fetch(ApItem.FIELD_DATA);

        // prepare where
        Path<?> jpaPath = getJpaPath(root, entityIdPath);
        Predicate cond = createQueryCondition(root, cb);
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
