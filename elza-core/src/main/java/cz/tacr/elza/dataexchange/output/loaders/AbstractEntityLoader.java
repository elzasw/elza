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
import javax.persistence.criteria.FetchParent;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.Validate;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;

import cz.tacr.elza.common.db.HibernateUtils;

/**
 * Abstract implementation for entity batch loader.
 */
public abstract class AbstractEntityLoader<RES> extends AbstractBatchLoader<Object, RES> {

    private final Class<?> entityClass;

    private final String queryEntityIdPath;

    private final EntityManager em;

    protected AbstractEntityLoader(Class<?> entityClass,
                                   String queryEntityIdPath,
                                   EntityManager em,
                                   int batchSize) {
        super(batchSize);
        this.entityClass = Validate.notNull(entityClass);
        this.queryEntityIdPath = Validate.notNull(queryEntityIdPath);
        this.em = Validate.notNull(em);

    }

    @Override
    protected final void processBatch(ArrayList<BatchEntry> entries) {
        Map<Object, List<BatchEntry>> idLookup = getQueryEntityIdLookup(entries);

        CriteriaQuery<Tuple> cq = createCriteriaQuery(idLookup.keySet());
        Query<Tuple> q = createHibernateQuery(cq);

        try (ScrollableResults results = q.scroll(ScrollMode.FORWARD_ONLY)) {
            while (results.next()) {
                Object queryEntityId = results.get(0);
                Object entity = results.get(1);

                // TODO: replace detach for stateless session
                em.detach(entity);
                // can be initialized (detached) proxy
                entity = HibernateUtils.unproxy(entity);

                for (BatchEntry entry : idLookup.get(queryEntityId)) {
                    RES result = createResult(entity);
                    entry.addResult(result);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected RES createResult(Object entity) {
        return (RES) entity;
    }

    protected void setEntityFetch(FetchParent<?, ?> baseEntity) {
    }

    private Map<Object, List<BatchEntry>> getQueryEntityIdLookup(Collection<BatchEntry> entries) {
        Map<Object, List<BatchEntry>> map = new HashMap<>(entries.size());

        for (BatchEntry entry : entries) {
            Object id = entry.getRequest();
            List<BatchEntry> group = map.get(id);
            if (group == null) {
                map.put(id, group = new ArrayList<>());
            }
            group.add(entry);
        }
        return map;
    }

    private <T> Query<T> createHibernateQuery(CriteriaQuery<T> criteriaQuery) {
        Session session = em.unwrap(Session.class);
        Query<T> query = session.createQuery(criteriaQuery);
        query.setCacheable(false);
        query.setReadOnly(true);
        return query;
    }

    private CriteriaQuery<Tuple> createCriteriaQuery(Set<Object> queryEntityIds) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<?> root = cq.from(entityClass);
        setEntityFetch(root);

        Path<?> idPath = getPath(root, queryEntityIdPath);
        cq.where(idPath.in(queryEntityIds));

        cq.multiselect(idPath, root);

        return cq;
    }

    /**
     * Builds attribute path for JPA API. FK or joined entities are accessible through dot notation.
     *
     * @param root base entity
     * @param path to attribute
     * @return Simple or compound path to attribute.
     */
    public static Path<?> getPath(Root<?> root, String path) {
        final String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return root.get(path);
        }
        Path<?> last = root.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            last = last.get(parts[i]);
        }
        return last;
    }
}
