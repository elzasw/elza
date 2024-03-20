package cz.tacr.elza.filter.condition;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Má vyplněnu nějakou hodnotu.
 *
 * @author Jiří Vaněk [jiri.vanek@marbes.cz]
 * @since 14. 4. 2016
 * @update Sergey Iryupin
 * @since 20. 3. 2024
 */
public class UndefinedDescItemCondition implements HibernateDescItemCondition {

    @Override
    public Query createHibernateQuery(final EntityManager entityManager, 
    								  final Integer fundId, 
    								  final Integer descItemTypeId,
    								  final Integer lockChangeId) {
        StringBuffer sb = new StringBuffer()
                .append("select distinct n.node_id ") // zajímají nás id nodů
                .append("from arr_node n ")
                .append("join arr_level l on n.node_id = l.node_id ")
                .append("left join arr_desc_item di on n.node_id = di.node_id ")
                .append("left join arr_item it on di.item_id = it.item_id ")
                .append("where n.fund_id = :fundId and it.data_id IS NULL and it.item_type_id = :descItemTypeId and it.delete_change_id is null ");

        if (lockChangeId == null) {
            sb.append("and l.delete_change_id is null "); // v otevřené verzi
        } else {
            sb.append("and l.create_change_id < :lockChangeId and (l.delete_change_id is null or l.delete_change_id > :lockChangeId) "); // v uzavřené verzi
        }

        Query query = entityManager.createNativeQuery(sb.toString());
        if (lockChangeId != null) {
            query.setParameter("lockChangeId", lockChangeId);
        }
        query.setParameter("descItemTypeId", descItemTypeId);
        query.setParameter("fundId", fundId);

        return query;
    }
}
