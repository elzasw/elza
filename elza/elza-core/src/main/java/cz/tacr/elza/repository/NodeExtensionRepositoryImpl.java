package cz.tacr.elza.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.common.db.DatabaseType;
import cz.tacr.elza.common.db.RecursiveQueryBuilder;
import cz.tacr.elza.domain.ArrNodeExtension;

/**
 * Implementace repository pro {@link ArrNodeExtension} - Custom.
 *
 * @since 23.10.2017
 */
@Component
public class NodeExtensionRepositoryImpl implements NodeExtensionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    // TODO: Rewrite query without path
    @Override
    public List<ArrNodeExtension> findAllByNodeIdFromRoot(final Integer nodeId) {
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");

        RecursiveQueryBuilder<ArrNodeExtension> rqBuilder = DatabaseType.getCurrent()
                .createRecursiveQueryBuilder(ArrNodeExtension.class);

        String sql_nodes = "WITH RECURSIVE treeData(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position, path) AS"
                +
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

        rqBuilder.addSqlPart(sql);
        rqBuilder.prepareQuery(entityManager);
        rqBuilder.setParameter("nodeId", nodeId);

        Query query = rqBuilder.getQuery();
        return query.getResultList();
    }
}
