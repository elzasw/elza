package cz.tacr.elza.filter.condition;

import java.util.Objects;

import cz.tacr.elza.common.db.DatabaseType;
import cz.tacr.elza.exception.SystemException;
import cz.tacr.elza.exception.codes.BaseCode;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Podmínka pro souřadnice — geometrie záznamu (její obálka) leží uvnitř zadané oblasti.
 */
public class SubsetCoordinatesDescItemCondition implements HibernateDescItemCondition {

    private final String inputWkt;

    public SubsetCoordinatesDescItemCondition(final String inputWkt) {
        Objects.requireNonNull(inputWkt);
        this.inputWkt = inputWkt;
    }

    @Override
    public Query createHibernateQuery(final EntityManager entityManager,
                                      final Integer fundId,
                                      final Integer descItemTypeId,
                                      final Integer lockChangeId) {
    	if (!DatabaseType.isPostgres()) {
    		throw new SystemException("Operation requires PostgreSQL/PostGIS. Current database: " + DatabaseType.getCurrent(),
    			BaseCode.SYSTEM_ERROR).set("databaseType", DatabaseType.getCurrent());
    	}

        StringBuilder sb = new StringBuilder()
                .append("select distinct n.node_id ")
                .append("from arr_node n ")
                .append("join arr_level l on n.node_id = l.node_id ")
                .append("join arr_desc_item di on n.node_id = di.node_id ")
                .append("join arr_item it on di.item_id = it.item_id ")
                .append("join arr_data_coordinates co on it.data_id = co.data_id ")
                .append("where n.fund_id = :fundId ")
                .append("and it.item_type_id = :descItemTypeId ")
                .append("and ST_Covers(co.coordinates_value, ")
                .append("              ST_GeomFromText(:inputWkt)) ");

        if (lockChangeId == null) {
            sb.append("and l.delete_change_id is null ")
              .append("and it.delete_change_id is null ");
        } else {
            sb.append("and l.create_change_id < :lockChangeId ")
              .append("and (l.delete_change_id is null or l.delete_change_id > :lockChangeId) ")
              .append("and it.create_change_id < :lockChangeId ")
              .append("and (it.delete_change_id is null or it.delete_change_id > :lockChangeId) ");
        }

        Query query = entityManager.createNativeQuery(sb.toString());
        query.setParameter("fundId", fundId);
        query.setParameter("descItemTypeId", descItemTypeId);
        query.setParameter("inputWkt", inputWkt);
        if (lockChangeId != null) {
            query.setParameter("lockChangeId", lockChangeId);
        }
        return query;
    }
}