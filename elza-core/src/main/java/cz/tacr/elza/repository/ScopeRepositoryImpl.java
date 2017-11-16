package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.Validate;
import org.hibernate.query.NativeQuery;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.core.DatabaseType;
import cz.tacr.elza.core.RecursiveQueryBuilder;
import cz.tacr.elza.domain.RegScope;


/**
 * Implementation of DataRepositoryCustom
 *
 * @since 03.02.2016
 */
public class ScopeRepositoryImpl implements ScopeRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    /*
	  There are several options how the query should/could be constructed.
	
	  Base idea: multiple subqueries
	    - parties connected to data connected to nodes
	    - registers connected to data connected to nodes
	    - registers connected to nodes
	
	  Integrate all queries above into one larger query. This should be more
	  efficient because CTE will be evaluated only once. Example of the final query:
	
	WITH RECURSIVE treeData(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position) AS
	(
	
	SELECT t.* FROM arr_level t WHERE t.node_id IN (5538)
	UNION ALL
	SELECT t.* FROM arr_level t JOIN treeData td ON td.node_id = t.node_id_parent AND t.delete_change_id IS NULL
	)
	SELECT distinct s.* FROM treeData t
	JOIN arr_desc_item di ON di.node_id = t.node_id
	JOIN arr_item it ON it.item_id = di.item_id
	JOIN arr_data_party_ref dp ON it.data_id = dp.data_id
	JOIN par_party p ON p.party_id = dp.party_id
	JOIN reg_record r ON r.record_id = p.record_id
	JOIN reg_scope s ON s.scope_id = r.scope_id
	UNION 
	SELECT distinct s.* FROM treeData t
	JOIN arr_desc_item di ON di.node_id = t.node_id
	JOIN arr_item it ON it.item_id = di.item_id
	JOIN arr_data_record_ref dr ON it.data_id = dr.data_id
	JOIN reg_record r ON r.record_id = dr.record_id
	JOIN reg_scope s ON s.scope_id = r.scope_id
	UNION
	SELECT distinct s.* FROM treeData t
	JOIN arr_node_register nr ON nr.node_id = t.node_id
	JOIN reg_record r ON r.record_id = nr.record_id
	JOIN reg_scope s ON s.scope_id = r.scope_id	
	 */

	static String FIND_SCOPE_PART1 = "WITH RECURSIVE treeData(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position) AS \n"
	        + "(\n"
	        + "  SELECT t.* FROM arr_level t WHERE t.node_id IN (:nodeIds) \n"
	        + "  UNION ALL\n"
	        + "  SELECT t.* FROM arr_level t JOIN treeData td ON td.node_id = t.node_id_parent AND t.delete_change_id IS NULL \n"
	        + ")\n"
	        + "SELECT distinct s.* FROM treeData t \n"
	        + "JOIN arr_desc_item di ON di.node_id = t.node_id \n"
	        + "JOIN arr_item it ON it.item_id = di.item_id \n"
	        + "JOIN arr_data_party_ref dp ON it.data_id = dp.data_id \n"
	        + "JOIN par_party p ON p.party_id = dp.party_id \n"
	        + "JOIN reg_record r ON r.record_id = p.record_id \n"
	        + "JOIN reg_scope s ON s.scope_id = r.scope_id \n";
	static String FIND_SCOPE_PART2 = "UNION \n"
	        + "SELECT distinct s.* FROM treeData t \n"
	        + "JOIN arr_desc_item di ON di.node_id = t.node_id \n"
	        + "JOIN arr_item it ON it.item_id = di.item_id \n"
	        + "JOIN arr_data_record_ref dr ON it.data_id = dr.data_id \n"
	        + "JOIN reg_record r ON r.record_id = dr.record_id \n"
	        + "JOIN reg_scope s ON s.scope_id = r.scope_id \n";
	static String FIND_SCOPE_PART3 = "UNION \n"
	        + "SELECT distinct s.* FROM treeData t \n"
	        + "JOIN arr_node_register nr ON nr.node_id = t.node_id \n"
	        + "JOIN reg_record r ON r.record_id = nr.record_id \n"
	        + "JOIN reg_scope s ON s.scope_id = r.scope_id \n";

	static String FIND_SCOPE_NOT_INCLUDE_ROOT = "WHERE t.node_id NOT IN (:nodeIds) \n";

	@Override
    public List<RegScope> findScopesBySubtreeNodeIds(final Collection<Integer> nodeIds, final boolean ignoreRootNode) {
        Validate.isTrue(nodeIds.size() > 0);

        RecursiveQueryBuilder<RegScope> rqBuilder = DatabaseType.getCurrent().createRecursiveQueryBuilder(RegScope.class);

		rqBuilder.addSqlPart(FIND_SCOPE_PART1);

        if (ignoreRootNode) {
            rqBuilder.addSqlPart(FIND_SCOPE_NOT_INCLUDE_ROOT);
        }

		rqBuilder.addSqlPart(FIND_SCOPE_PART2);
        if (ignoreRootNode) {
            rqBuilder.addSqlPart(FIND_SCOPE_NOT_INCLUDE_ROOT);
        }

		rqBuilder.addSqlPart(FIND_SCOPE_PART3);
        if (ignoreRootNode) {
            rqBuilder.addSqlPart(FIND_SCOPE_NOT_INCLUDE_ROOT);
		}

        rqBuilder.prepareQuery(entityManager);
        rqBuilder.setParameter("nodeIds", nodeIds);

		NativeQuery<RegScope> q = rqBuilder.getQuery();
		return q.getResultList();
    }

}
