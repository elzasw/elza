package cz.tacr.elza.repository;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import cz.tacr.elza.common.db.DatabaseType;
import cz.tacr.elza.common.db.RecursiveQueryBuilder;
import cz.tacr.elza.domain.RulArrangementExtension;

/**
 * Implementace repository pro {@link RulArrangementExtension} - Custom.
 *
 * @since 23.10.2017
 */
@Component
public class ArrangementExtensionRepositoryImpl implements ArrangementExtensionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<RulArrangementExtension> findByNodeIdToRoot(final Integer nodeId) {
        Assert.notNull(nodeId, "Identifikátor JP musí být vyplněn");

        RecursiveQueryBuilder<RulArrangementExtension> queryBuilder = DatabaseType.getCurrent()
                .createRecursiveQueryBuilder(RulArrangementExtension.class);

        queryBuilder.addSqlPart("SELECT DISTINCT ae.* FROM arr_node_extension ne ");
        queryBuilder.addSqlPart(
                "JOIN rul_arrangement_extension ae ON ae.arrangement_extension_id = ne.arrangement_extension_id ");
        queryBuilder.addSqlPart("WHERE ne.node_id IN ( ");
        queryBuilder.addSqlPart(
                " WITH RECURSIVE treeData(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position) AS ");
        queryBuilder.addSqlPart(" ( SELECT t.* FROM arr_level t WHERE t.node_id = :nodeId ");
        queryBuilder.addSqlPart("   UNION ALL ");
        queryBuilder.addSqlPart("   SELECT t.* FROM arr_level t JOIN treeData td ON td.node_id_parent = t.node_id ");
        queryBuilder.addSqlPart(" ) ");
        queryBuilder.addSqlPart(
                " SELECT DISTINCT n.node_id FROM treeData t JOIN arr_node n ON n.node_id = t.node_id WHERE t.delete_change_id IS NULL ");
        queryBuilder.addSqlPart(") AND ne.node_id <> :nodeId AND ne.delete_change_id IS NULL ORDER BY ae.name");

        queryBuilder.prepareQuery(entityManager);
        queryBuilder.setParameter("nodeId", nodeId);
        return queryBuilder.getQuery()
                .getResultList();
    }
}
