package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulArrangementExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;

/**
 * Implementace repository pro {@link RulArrangementExtension} - Custom.
 *
 * @since 23.10.2017
 */
@Component
public class ArrangementExtensionRepositoryImpl implements ArrangementExtensionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LevelRepository levelRepository;

    @Override
    public List<RulArrangementExtension> findByNodeIdToRoot(final Integer nodeId) {
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");

        String sql_nodes = "WITH " + levelRepository.getRecursivePart() + " treeData(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position) AS (SELECT t.* FROM arr_level t WHERE t.node_id = :nodeId UNION ALL SELECT t.* FROM arr_level t JOIN treeData td ON td.node_id_parent = t.node_id) " +
                "SELECT DISTINCT n.node_id FROM treeData t JOIN arr_node n ON n.node_id = t.node_id WHERE t.delete_change_id IS NULL";

        String sql = "SELECT DISTINCT ae.* FROM arr_node_extension ne JOIN rul_arrangement_extension ae ON ae.arrangement_extension_id = ne.arrangement_extension_id WHERE ne.node_id IN" +
                " (" + sql_nodes + " ) ORDER BY ae.name";

        Query query = entityManager.createNativeQuery(sql, RulArrangementExtension.class);
        query.setParameter("nodeId", nodeId);

        return query.getResultList();
    }
}
