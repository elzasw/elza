package cz.tacr.elza.repository;

import cz.tacr.elza.api.ArrOutputDefinition.OutputState;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrOutputDefinition;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementace {@link OutputDefinitionRepositoryCustom}.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
@Component
public class OutputDefinitionRepositoryImpl implements OutputDefinitionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private OutputDefinitionRepository outputDefinitionRepository;

    @Override
    public List<ArrOutputDefinition> findOutputsByNodes(@NotNull final ArrFundVersion fundVersion,
                                                        @NotEmpty final Set<ArrNode> nodes,
                                                        @Nullable final OutputState... states) {

        List<Integer> nodeIds = nodes.stream().map(ArrNode::getNodeId).collect(Collectors.toList());

        String sql = "SELECT x.output_definition_id FROM " +
                "(" +
                "SELECT no.output_definition_id FROM arr_node_output no WHERE no.delete_change_id is null GROUP BY no.output_definition_id HAVING count(*) = :count" +
                ") x JOIN " +
                "(" +
                "SELECT DISTINCT no.output_definition_id FROM arr_node_output no WHERE no.node_id in (:nodeIds) and no.delete_change_id is null GROUP BY no.output_definition_id HAVING count(*) = :count" +
                ") z ON x.output_definition_id = z.output_definition_id " +
                " JOIN arr_output_definition od ON od.output_definition_id = x.output_definition_id WHERE od.fund_id = :fundId";

        if (states != null && states.length > 0) {
            sql += " AND od.state IN :states";
        }

        javax.persistence.Query query = entityManager.createNativeQuery(sql);
        query.setParameter("count", nodes.size());
        query.setParameter("nodeIds", nodeIds);
        query.setParameter("fundId", fundVersion.getFund().getFundId());

        if (states != null && states.length > 0) {
            query.setParameter("states", Arrays.asList(states).stream().map(OutputState::name).collect(Collectors.toList()));
        }

        return outputDefinitionRepository.findAll(query.getResultList());
    }
}
