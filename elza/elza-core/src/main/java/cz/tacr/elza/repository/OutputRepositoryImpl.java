package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.apache.commons.lang3.Validate;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutput.OutputState;

/**
 * Implementace {@link OutputRepositoryCustom}.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Component
public class OutputRepositoryImpl implements OutputRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<ArrOutput> findOutputsByNodes(ArrFundVersion fundVersion,
                                              Collection<Integer> nodeIds,
                                              OutputState... currentStates) {
        Validate.notEmpty(nodeIds);

        Integer lockChangeId = fundVersion.getLockChangeId();

        // lock change condition
        String sql1 = " JOIN arr_level l ON l.node_id = no.node_id" + (
                lockChangeId == null
                        ? " WHERE l.delete_change_id is null AND no.delete_change_id is null"
                        : " WHERE no.create_change_id < :changeId AND (l.delete_change_id is null OR l.delete_change_id > :changeId) AND (no.delete_change_id is null OR no.delete_change_id > :changeId)"
        );

        String sql2 = "SELECT o.* FROM arr_output o" +
                " JOIN (SELECT no.output_id FROM arr_node_output no" + sql1 + " AND no.node_id in (:nodeIds) GROUP BY no.output_id HAVING count(*) = :count) c1" +
                " ON c1.output_id = o.output_id" +
                " JOIN (SELECT no.output_id FROM arr_node_output no" + sql1 + " GROUP BY no.output_id HAVING count(*) = :count) c2" +
                " ON c2.output_id = o.output_id" +
                " WHERE o.fund_id = :fundId";

        if (currentStates != null) {
            sql2 += " AND o.state IN :states";
        }

        Session session = em.unwrap(Session.class);

        NativeQuery<ArrOutput> query = session.createNativeQuery(sql2, ArrOutput.class);

        query.setParameter("count", nodeIds.size());
        query.setParameter("nodeIds", nodeIds);
        query.setParameter("fundId", fundVersion.getFundId());

        if (lockChangeId != null) {
            query.setParameter("changeId", lockChangeId);
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
