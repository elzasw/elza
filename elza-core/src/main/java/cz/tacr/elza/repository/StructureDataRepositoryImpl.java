package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.common.db.DatabaseType;
import cz.tacr.elza.common.db.RecursiveQueryBuilder;
import cz.tacr.elza.domain.ArrStructureData;

/**
 * Rozšířené repository pro {@link StructureDataRepository}.
 *
 * @since 10.11.2017
 */
public class StructureDataRepositoryImpl implements StructureDataRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LevelRepository levelRepository;

    private TypedQuery buildStructureDataFindQuery(final boolean dataQuery, String search, int structureTypeId, int fundId, Boolean assignable, int firstResult, int maxResults) {

        // Podmínky hledání
        StringBuilder conds = new StringBuilder();
        Map<String, Object> parameters = new HashMap<>();

        StringBuilder query = new StringBuilder();
        query.append("FROM arr_structure_data sd");

        query.append(" WHERE sd.structureTypeId = :structureTypeId AND sd.fundId = :fundId");
        parameters.put("structureTypeId", structureTypeId);
        parameters.put("fundId", fundId);

        if (!StringUtils.isEmpty(search)) {
            conds.append(" AND LOWER(sd.value) LIKE :search");
            parameters.put("search", "%" + search.toLowerCase() + "%");
        }

        // bez tempových
        conds.append(" AND sd.state <> :state");
        parameters.put("state", ArrStructureData.State.TEMP);

        // pouze nesmazané
        conds.append(" AND sd.deleteChange IS NULL");

        if (assignable != null) {
            conds.append(" AND sd.assignable = :assignable");
            parameters.put("assignable", assignable);
        }

        // Připojení podmínek ke query
        if (conds.length() > 0) {
            query.append(conds.toString());
        }

        TypedQuery q;
        if (dataQuery) {
            String dataQueryStr = "SELECT sd " + query.toString() + " ORDER BY sd.value";
            q = entityManager.createQuery(dataQueryStr, ArrStructureData.class);
        } else {
            String countQueryStr = "SELECT COUNT(sd) " + query.toString();
            q = entityManager.createQuery(countQueryStr, Number.class);
        }

        parameters.forEach(q::setParameter);

        if (dataQuery) {
            q.setFirstResult(firstResult);
            if (maxResults >= 0) {
                q.setMaxResults(maxResults);
            }
        }

        return q;
    }

    @Override
    public FilteredResult<ArrStructureData> findStructureData(final Integer structureTypeId, final int fundId,
            final String search, final Boolean assignable,
            final int firstResult, final int maxResults) {
        TypedQuery data = buildStructureDataFindQuery(true, search, structureTypeId, fundId, assignable, firstResult, maxResults);
        TypedQuery count = buildStructureDataFindQuery(false, search, structureTypeId, fundId, assignable, firstResult, maxResults);
        return new FilteredResult<>(firstResult, maxResults,
                ((Number) count.getSingleResult()).intValue(), data.getResultList());
    }

    @Override
    public List<ArrStructureData> findStructureDataBySubtreeNodeIds(final Collection<Integer> nodeIds,
            final boolean ignoreRootNodes) {
        Validate.notEmpty(nodeIds);

        RecursiveQueryBuilder<ArrStructureData> rqBuilder = DatabaseType.getCurrent()
                .createRecursiveQueryBuilder(ArrStructureData.class);

        rqBuilder.addSqlPart("SELECT p.* FROM arr_structure_data p WHERE p.structure_data_id IN (")

                .addSqlPart("SELECT dpr.structure_data_id FROM arr_data_structure_ref dpr ")
                .addSqlPart(
                        "JOIN arr_structure_data ap ON ap.structure_data_id = dpr.structure_data_id WHERE dpr.data_id IN (")

                .addSqlPart("SELECT d.data_id FROM arr_item i JOIN arr_data d ON d.data_id = i.data_id ")
                .addSqlPart("JOIN arr_desc_item di ON di.item_id = i.item_id ")
                .addSqlPart("WHERE i.delete_change_id IS NULL AND d.data_type_id = 11 AND di.node_id IN (")

                .addSqlPart(
                        "WITH RECURSIVE treeData(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position) AS ")
                .addSqlPart("(SELECT t.* FROM arr_level t WHERE t.node_id IN (:nodeIds) ")
                .addSqlPart("UNION ALL ")
                .addSqlPart("SELECT t.* FROM arr_level t JOIN treeData td ON td.node_id = t.node_id_parent) ")

                .addSqlPart("SELECT DISTINCT n.node_id FROM treeData t JOIN arr_node n ON n.node_id = t.node_id ")
                .addSqlPart("WHERE t.delete_change_id IS NULL");
        if (ignoreRootNodes) {
            rqBuilder.addSqlPart(" AND n.node_id NOT IN (:nodeIds)");
        }

        rqBuilder.addSqlPart(")))");

        rqBuilder.prepareQuery(entityManager);
        rqBuilder.setParameter("nodeIds", nodeIds);
        return rqBuilder.getQuery().getResultList();

    }

}
