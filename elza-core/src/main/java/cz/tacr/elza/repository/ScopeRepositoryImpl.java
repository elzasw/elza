package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RegScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


/**
 * Implementation of DataRepositoryCustom
 *
 * @since 03.02.2016
 */
public class ScopeRepositoryImpl implements ScopeRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LevelRepository levelRepository;

    @Override
    public Set<RegScope> findScopesBySubtreeNodeIds(final Collection<Integer> nodeIds, final boolean ignoreRootNode) {
        Assert.notEmpty(nodeIds, "Identifikátor JP musí být vyplněn");

        String sql_nodes = "WITH " + levelRepository.getRecursivePart() + " treeData(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position) AS (SELECT t.* FROM arr_level t WHERE t.node_id IN (:nodeIds) UNION ALL SELECT t.* FROM arr_level t JOIN treeData td ON td.node_id = t.node_id_parent) " +
                "SELECT DISTINCT n.node_id FROM treeData t JOIN arr_node n ON n.node_id = t.node_id WHERE t.delete_change_id IS NULL";

        if (ignoreRootNode) {
            sql_nodes += " AND n.node_id NOT IN (:nodeIds)";
        }

        String sql = "SELECT rs.* FROM reg_record r JOIN reg_scope rs ON r.scope_id = rs.scope_id WHERE r.record_id IN" +
                " (" +
                "  SELECT p.record_id FROM arr_data_party_ref dpf JOIN par_party p ON p.party_id = dpf.party_id WHERE dpf.data_id IN (SELECT d.data_id FROM arr_data d JOIN arr_item i ON d.item_id = i.item_id JOIN arr_desc_item di ON di.item_id = i.item_id WHERE i.delete_change_id IS NULL AND d.data_type_id = 8 AND di.node_id IN (" + sql_nodes + "))" +
                "  UNION" +
                "  SELECT drf.record_id FROM arr_data_record_ref drf WHERE drf.data_id IN (SELECT d.data_id FROM arr_data d JOIN arr_item i ON d.item_id = i.item_id JOIN arr_desc_item di ON di.item_id = i.item_id WHERE i.delete_change_id IS NULL AND d.data_type_id = 9 AND di.node_id IN (" + sql_nodes + "))" +
                "  UNION" +
                "  SELECT nr.record_id FROM arr_node_register nr WHERE nr.delete_change_id IS NULL AND nr.node_id IN (" + sql_nodes + ")" +
                " )";

        Query query = entityManager.createNativeQuery(sql, RegScope.class);
        query.setParameter("nodeIds", nodeIds);

        return new HashSet<>(query.getResultList());
    }

}
