package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrNodeExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Collections;
import java.util.List;

/**
 * Implementace repository pro {@link ArrNodeExtension} - Custom.
 *
 * @since 23.10.2017
 */
@Component
public class NodeExtensionRepositoryImpl implements NodeExtensionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LevelRepository levelRepository;

    @Override
    public List<ArrNodeExtension> findAllByNodeIdFromRoot(final Integer nodeId) {
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");

        String sql_nodes = "WITH " + levelRepository.getRecursivePart() + " treeData(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position, path) AS" +
                " (" +
                "  (" +
                "   SELECT t.*, '000001' AS path" +
                "   FROM arr_level t" +
                "   WHERE t.node_id = :nodeId AND t.delete_change_id IS NULL" +
                "  )" +
                "  UNION ALL" +
                "  (" +
                "   SELECT t.*, CONCAT(td.path, '.', RIGHT(CONCAT('000000', t.position), 6)) AS deep" +
                "   FROM arr_level t JOIN treeData td ON td.node_id_parent = t.node_id" +
                "   WHERE t.delete_change_id IS NULL" +
                "  )" +
                " )" +
                " SELECT t.* FROM treeData t JOIN arr_node n ON n.node_id = t.node_id WHERE t.delete_change_id IS NULL ORDER BY t.path DESC";

        String sql = "SELECT ne.* FROM (" + sql_nodes + ") n JOIN arr_node_extension ne ON n.node_id = ne.node_id JOIN rul_arrangement_extension ae ON ae.arrangement_extension_id = ne.arrangement_extension_id" +
                " WHERE ne.delete_change_id IS NULL ORDER BY n.path DESC, ne.node_extension_id ASC";

        Query query = entityManager.createNativeQuery(sql, ArrNodeExtension.class);
        query.setParameter("nodeId", nodeId);

        return query.getResultList();
    }
}
