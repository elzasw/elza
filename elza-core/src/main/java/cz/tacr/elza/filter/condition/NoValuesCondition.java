package cz.tacr.elza.filter.condition;

import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 * Podmínka na uzly bez hodnoty daného typu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 2. 5. 2016
 */
public class NoValuesCondition implements HibernateDescItemCondition {

    @Override
    public Query createHibernateQuery(final EntityManager entityManager, final Integer fundId, final Integer descItemTypeId, final Integer lockChangeId) {
        Query query;
        if (lockChangeId == null) {
            String sql = "select n.node_id from arr_node n join arr_level l on n.node_id = l.node_id left join arr_desc_item di on n.node_id = di.node_id "
                    + "where di.desc_item_type_id is null and n.fund_id = :fundId and l.delete_change_id is null "
                    + "UNION "
                    + "select n.node_id  from arr_node n join arr_level l on n.node_id = l.node_id left join arr_desc_item di on n.node_id = di.node_id "
                    + "where n.fund_id = :fundId and l.delete_change_id is null and n.node_id not in (select node_id from arr_desc_item where desc_item_type_id = :descItemTypeId)";

            query = entityManager.createNativeQuery(sql);
        } else {
            String sql = "select n.node_id from arr_node n join arr_level l on n.node_id = l.node_id left join arr_desc_item di on n.node_id = di.node_id "
                    + "where di.desc_item_type_id is null and n.fund_id = :fundId and l.create_change_id < :lockChangeId and (l.delete_change_id is null or l.delete_change_id > :lockChangeId) "
                    + "UNION "
                    + "select n.node_id  from arr_node n join arr_level l on n.node_id = l.node_id left join arr_desc_item di on n.node_id = di.node_id "
                    + "where n.fund_id = :fundId and l.create_change_id < :lockChangeId and (l.delete_change_id is null or l.delete_change_id > :lockChangeId) and n.node_id not in (select node_id from arr_desc_item where desc_item_type_id = :descItemTypeId)";

            query = entityManager.createNativeQuery(sql);
            query.setParameter("lockChangeId", lockChangeId);
        }
        query.setParameter("descItemTypeId", descItemTypeId);
        query.setParameter("fundId", fundId);

        return query;
    }
}
