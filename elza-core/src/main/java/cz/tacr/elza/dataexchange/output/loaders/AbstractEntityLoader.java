package cz.tacr.elza.dataexchange.output.loaders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.FetchParent;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.Validate;

import cz.tacr.elza.common.db.HibernateUtils;

/**
 * Abstract implementation for entity batch loader.
 */
public abstract class AbstractEntityLoader<REQ_ID, ENTITY> extends AbstractBatchLoader<REQ_ID, ENTITY> {

    private final Class<ENTITY> entityClass;

    private final String entityRequestIdPath;

    private final EntityManager em;

    public AbstractEntityLoader(Class<ENTITY> entityClass, String entityRequestIdPath, EntityManager em, int batchSize) {
        super(batchSize);
        this.entityClass = Validate.notNull(entityClass);
        this.entityRequestIdPath = Validate.notNull(entityRequestIdPath);
        this.em = Validate.notNull(em);
    }

    @Override
    protected final void processBatch(ArrayList<BatchEntry> entries) {
        Map<REQ_ID, List<BatchEntry>> requestIdMap = getRequestIdMap(entries);

        TypedQuery<Tuple> query = createQuery(requestIdMap.keySet());

        List<Tuple> results = query.getResultList();

        for (Tuple result : results) {
            Object requestId = result.get(0);
            ENTITY entity = result.get(1, entityClass);

            // TODO: replace detach for stateless session
            em.detach(entity);
            // if referenced before can be proxy (after query always initialized)
            entity = HibernateUtils.unproxy(entity);

            for (BatchEntry entry : requestIdMap.get(requestId)) {
                entry.addResult(entity);
            }
        }
    }

    protected void addFetches(FetchParent<?, ENTITY> parent) {
    }

    private Map<REQ_ID, List<BatchEntry>> getRequestIdMap(Collection<BatchEntry> entries) {
        Map<REQ_ID, List<BatchEntry>> map = new HashMap<>(entries.size());

        for (BatchEntry entry : entries) {
            REQ_ID requestId = entry.getRequest();
            List<BatchEntry> group = map.get(requestId);
            if (group == null) {
                map.put(requestId, group = new ArrayList<>());
            }
            group.add(entry);
        }
        return map;
    }

    private TypedQuery<Tuple> createQuery(Set<REQ_ID> requestIds) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<ENTITY> root = cq.from(entityClass);
        addFetches(root);

        Path<REQ_ID> reqIdPath = getPath(root, entityRequestIdPath);
        cq.where(reqIdPath.in(requestIds));

        cq.multiselect(reqIdPath, root);

        return em.createQuery(cq);
    }

    /**
     * Builds attribute path for JPA API. FK or joined entities are accessible through dot notation.
     *
     * @param root base entity
     * @param path to attribute
     * @return Simple or compound path to attribute.
     */
    @SuppressWarnings("unchecked")
    public static <T> Path<T> getPath(Root<?> root, String path) {
        final String[] parts = path.split("\\.");
        if (parts.length == 0) {
            return root.get(path);
        }
        Path<?> last = root.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            last = last.get(parts[i]);
        }
        return (Path<T>) last;
    }
}
