package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrStructureData;
import cz.tacr.elza.domain.UsrUser;
import org.apache.commons.lang.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.Map;

/**
 * Rozšířené repository pro {@link StructureDataRepository}.
 *
 * @since 10.11.2017
 */
public class StructureDataRepositoryImpl implements StructureDataRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

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
    public FilteredResult<ArrStructureData> findStructureData(final Integer structureTypeId, final int fundId, final String search, final Boolean assignable, final int firstResult, final int maxResults) {
        TypedQuery data = buildStructureDataFindQuery(true, search, structureTypeId, fundId, assignable, firstResult, maxResults);
        TypedQuery count = buildStructureDataFindQuery(false, search, structureTypeId, fundId, assignable, firstResult, maxResults);
        return new FilteredResult<>(firstResult, maxResults, ((Number) count.getSingleResult()).longValue(), data.getResultList());
    }
}
