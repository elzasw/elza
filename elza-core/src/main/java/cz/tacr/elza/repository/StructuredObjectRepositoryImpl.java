package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;

import cz.tacr.elza.common.db.DatabaseType;
import cz.tacr.elza.common.db.RecursiveQueryBuilder;
import cz.tacr.elza.domain.ArrStructuredObject;

/**
 * Rozšířené repository pro {@link StructuredObjectRepository}.
 *
 * @since 10.11.2017
 */
public class StructuredObjectRepositoryImpl implements StructuredObjectRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    private Predicate createStructObjSearchCond(Path<ArrStructuredObject> path,
                                                String search,
                                                int structuredTypeId,
                                                int fundId,
                                                Boolean assignable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        Predicate typeEqual = cb.equal(path.get("structuredTypeId"), structuredTypeId);
        Predicate fundEqual = cb.equal(path.get("fundId"), fundId);
        Predicate notTemp = cb.notEqual(path.get("state"), ArrStructuredObject.State.TEMP);
        Predicate notDeleted = cb.isNull(path.get("deleteChange"));

        Predicate cond = cb.and(typeEqual, fundEqual, notTemp, notDeleted);

        if (StringUtils.isNotEmpty(search)) {
            String searchExp = '%' + search.toLowerCase() + '%';
            Predicate searchLike = cb.like(cb.lower(path.get("value")), searchExp);
            cond = cb.and(cond, searchLike);
        }

        if (assignable != null) {
            Predicate assignableEqual = cb.equal(path.get("assignable"), assignable);
            cond = cb.and(cond, assignableEqual);
        }

        return cond;
    }

    private TypedQuery<Long> createStructObjCountQuery(String search, int structureTypeId, int fundId, Boolean assignable) {

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);

        Root<ArrStructuredObject> root = cq.from(ArrStructuredObject.class);
        Predicate cond = createStructObjSearchCond(root, search, structureTypeId, fundId, assignable);

        cq.select(cb.count(root));
        cq.where(cond);

        return em.createQuery(cq);
    }

    private TypedQuery<ArrStructuredObject> createStructObjSearchQuery(String search,
                                                                    int structureTypeId,
                                                                    int fundId,
                                                                    Boolean assignable,
                                                                    int firstResult,
                                                                    int maxResults) {
        Validate.isTrue(firstResult >= 0);
        Validate.isTrue(maxResults >= 0);

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ArrStructuredObject> cq = cb.createQuery(ArrStructuredObject.class);

        Root<ArrStructuredObject> root = cq.from(ArrStructuredObject.class);
        Predicate cond = createStructObjSearchCond(root, search, structureTypeId, fundId, assignable);

        cq.where(cond);
        cq.orderBy(cb.asc(root.get("value")));

        TypedQuery<ArrStructuredObject> q = em.createQuery(cq);
        q.setFirstResult(firstResult);
        q.setMaxResults(maxResults);

        return q;
    }

    @Override
    public FilteredResult<ArrStructuredObject> findStructureData(final Integer structuredTypeId,
                                                              final int fundId,
                                                              final String search,
                                                              final Boolean assignable,
                                                              final int firstResult,
                                                              final int maxResults) {
        TypedQuery<Long> countQuery = createStructObjCountQuery(search, structuredTypeId, fundId, assignable);
        TypedQuery<ArrStructuredObject> objQuery = createStructObjSearchQuery(search, structuredTypeId, fundId, assignable, firstResult,
                maxResults);
        int count = countQuery.getSingleResult().intValue();
        List<ArrStructuredObject> objList = objQuery.getResultList();
        return new FilteredResult<>(firstResult, maxResults, count, objList);
    }

    @Override
    public List<ArrStructuredObject> findStructureDataBySubtreeNodeIds(final Collection<Integer> nodeIds,
                                                                       final boolean ignoreRootNodes) {
        Validate.notEmpty(nodeIds);

        RecursiveQueryBuilder<ArrStructuredObject> rqBuilder = DatabaseType.getCurrent()
                .createRecursiveQueryBuilder(ArrStructuredObject.class);

        rqBuilder.addSqlPart("SELECT p.* FROM arr_structured_object p WHERE p.structured_object_id IN (")

                .addSqlPart("SELECT dpr.structured_object_id FROM arr_data_structure_ref dpr ")
                .addSqlPart(
                        "JOIN arr_structured_object ap ON ap.structured_object_id = dpr.structured_object_id WHERE dpr.data_id IN (")

                .addSqlPart("SELECT d.data_id FROM arr_item i JOIN arr_data d ON d.data_id = i.data_id ")
                .addSqlPart("JOIN arr_desc_item di ON di.item_id = i.item_id ")
                .addSqlPart("WHERE i.delete_change_id IS NULL AND d.data_type_id = 11 AND di.node_id IN (")

                .addSqlPart(
                        "WITH RECURSIVE treeData(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position) AS ")
                .addSqlPart("(SELECT t.* FROM arr_level t WHERE t.node_id IN (:nodeIds) ").addSqlPart("UNION ALL ")
                .addSqlPart("SELECT t.* FROM arr_level t JOIN treeData td ON td.node_id = t.node_id_parent) ")

                .addSqlPart("SELECT DISTINCT n.node_id FROM treeData t JOIN arr_node n ON n.node_id = t.node_id ")
                .addSqlPart("WHERE t.delete_change_id IS NULL");
        if (ignoreRootNodes) {
            rqBuilder.addSqlPart(" AND n.node_id NOT IN (:nodeIds)");
        }

        rqBuilder.addSqlPart(")))");

        rqBuilder.prepareQuery(em);
        rqBuilder.setParameter("nodeIds", nodeIds);
        return rqBuilder.getQuery().getResultList();

    }
}