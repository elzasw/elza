package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang.Validate;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputDefinition.OutputState;

/**
 * Implementace {@link OutputDefinitionRepositoryCustom}.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Component
public class OutputDefinitionRepositoryImpl implements OutputDefinitionRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ArrOutputDefinition> findOutputsByNodes(ArrFundVersion fundVersion,
                                                        Collection<Integer> nodeIds,
                                                        OutputState... currentStates) {
        Validate.notEmpty(nodeIds);

        // lock change condition
        String sql1 = "JOIN arr_level l ON l.node_id = no.node_id WHERE ";
        if (fundVersion.getLockChangeId() == null) {
            sql1 += "l.delete_change_id is null AND no.delete_change_id is null";
        } else {
            sql1 += "no.create_change_id < :changeId AND (l.delete_change_id is null or l.delete_change_id > :changeId) AND (no.delete_change_id is null or no.delete_change_id > :changeId)";
        }

        String sql2 = "SELECT od.* FROM arr_output_definition od" +
                " JOIN (SELECT no.output_definition_id FROM arr_node_output no " + sql1 + " AND no.node_id in (:nodeIds) GROUP BY no.output_definition_id HAVING count(*) = :count) c1" +
                    " ON c1.output_definition_id = od.output_definition_id" +
                " JOIN (SELECT no.output_definition_id FROM arr_node_output no " + sql1 + " GROUP BY no.output_definition_id HAVING count(*) = :count) c2" +
                    " ON c2.output_definition_id = od.output_definition_id" +
                " WHERE od.fund_id = :fundId";

        if (currentStates != null) {
            sql2 += " AND od.state IN :states";
        }

        Session session = em.unwrap(Session.class);

        NativeQuery<ArrOutputDefinition> query = session.createNativeQuery(sql2, ArrOutputDefinition.class);

        query.setParameter("count", nodeIds.size());
        query.setParameter("nodeIds", nodeIds);
        query.setParameter("fundId", fundVersion.getFundId());

        if (fundVersion.getLockChangeId() != null) {
            query.setParameter("changeId", fundVersion.getLockChangeId());
        }

        if (currentStates != null) {
            List<String> stateNames = new ArrayList<>(currentStates.length);
            for (OutputState cs : currentStates) {
                stateNames.add(cs.name());
            }
            query.setParameter("states", stateNames);
        }

		return query.getResultList();
    }
}
