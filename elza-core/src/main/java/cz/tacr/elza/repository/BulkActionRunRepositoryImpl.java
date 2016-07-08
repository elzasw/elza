package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementace {@link BulkActionRunRepositoryCustom}
 *
 * @author Martin Šlapa
 * @since 07.07.2016
 */
public class BulkActionRunRepositoryImpl implements BulkActionRunRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private BulkActionRunRepository bulkActionRunRepository;

    @Override
    public List<ArrBulkActionRun> findBulkActionsByNodes(@NotNull final ArrFundVersion fundVersion,
                                                         @NotEmpty final Set<ArrNode> nodes,
                                                         @NotEmpty final ArrBulkActionRun.State... states) {
        List<Integer> nodeIds = nodes.stream().map(ArrNode::getNodeId).collect(Collectors.toList());

        String sql = "SELECT k.bulk_action_run_id FROM arr_bulk_action_run k JOIN " +
                "(SELECT ar.bulk_action_code, MAX(ar.change_id) AS change_id FROM arr_bulk_action_run ar JOIN " +
                "(SELECT no.bulk_action_run_id FROM arr_bulk_action_node no WHERE no.node_id IN (:nodeIds) GROUP BY no.bulk_action_run_id HAVING count(*) = :count) x ON ar.bulk_action_run_id = x.bulk_action_run_id JOIN " +
                "(SELECT DISTINCT no.bulk_action_run_id FROM arr_bulk_action_node no GROUP BY no.bulk_action_run_id HAVING count(*) = :count) y ON ar.bulk_action_run_id = y.bulk_action_run_id " +
                "WHERE ar.state IN (:states) GROUP BY ar.bulk_action_code) l ON k.bulk_action_code = l.bulk_action_code AND k.change_id = l.change_id WHERE k.fund_version_id = :fundVersionId";

        javax.persistence.Query query = entityManager.createNativeQuery(sql);
        query.setParameter("count", nodes.size());
        query.setParameter("nodeIds", nodeIds);
        query.setParameter("fundVersionId", fundVersion.getFundVersionId());
        query.setParameter("states", Arrays.asList(states).stream().map(ArrBulkActionRun.State::name).collect(Collectors.toList()));

        return bulkActionRunRepository.findAll(query.getResultList());
    }

}
