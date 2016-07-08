package cz.tacr.elza.filter.condition;

import javax.persistence.EntityManager;
import javax.persistence.Query;

/**
 * Podmínka na uzly bez specifikace daného typu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 3. 5. 2016
 */
public class NoSpecsCondition implements HibernateDescItemCondition {

    @Override
    public Query createHibernateQuery(final EntityManager entityManager, final Integer fundId, final Integer descItemTypeId,
            final Integer lockChangeId) {
        Query query;
        if (lockChangeId == null) {
            String sql = "select n.node_id from arr_node n join arr_level l on n.node_id = l.node_id left join arr_desc_item di on n.node_id = di.node_id left join arr_item it on di.item_id = it.item_id left join rul_item_spec s on it.item_spec_id = s.item_spec_id "
                    + "where (it.item_type_id is null OR s.item_spec_id is null) and n.fund_id = :fundId and l.delete_change_id is null "
                    + "UNION "
                    + "select n.node_id  from arr_node n join arr_level l on n.node_id = l.node_id left join arr_desc_item di on n.node_id = di.node_id left join arr_item it on di.item_id = it.item_id left join rul_item_spec s on it.item_spec_id = s.item_spec_id "
                    + "where n.fund_id = :fundId l.delete_change_id is null and n.node_id not in (select di2.node_id from arr_desc_item di2 join arr_item it2 on di2.item_id = it2.item_id where it2.item_type_id = :descItemTypeId and it2.item_spec_id is not null)";


            query = entityManager.createNativeQuery(sql);
        } else {
            String sql = "select n.node_id from arr_node n join arr_level l on n.node_id = l.node_id left join arr_desc_item di on n.node_id = di.node_id left join arr_item it on di.item_id = it.item_id left join rul_item_spec s on it.item_spec_id = s.item_spec_id "
                    + "where (it.item_type_id is null OR s.item_spec_id is null)and n.fund_id = :fundId and l.create_change_id < :lockChangeId and (l.delete_change_id is null or l.delete_change_id > :lockChangeId) "
                    + "UNION "
                    + "select n.node_id  from arr_node n join arr_level l on n.node_id = l.node_id left join arr_desc_item di on n.node_id = di.node_id left join arr_item it on di.item_id = it.item_id left join rul_item_spec s on it.item_spec_id = s.item_spec_id "
                    + "where n.fund_id = :fundId and l.create_change_id < :lockChangeId and (l.delete_change_id is null or l.delete_change_id > :lockChangeId) and n.node_id not in (select di2.node_id from arr_desc_item di2 join arr_item it2 on di2.item_id = it2.item_id where it2.item_type_id = :descItemTypeId and it2.item_spec_id is not null)";

            query = entityManager.createNativeQuery(sql);
            query.setParameter("lockChangeId", lockChangeId);
        }
        query.setParameter("descItemTypeId", descItemTypeId);
        query.setParameter("fundId", fundId);

        return query;
    }

}
