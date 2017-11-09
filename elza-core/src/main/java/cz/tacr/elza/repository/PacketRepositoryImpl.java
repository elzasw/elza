package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

import cz.tacr.elza.core.DatabaseType;
import cz.tacr.elza.core.RecursiveQueryBuilder;
import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulPacketType;

/**
 * Implementace repository pro obaly.
 */
@Component
public class PacketRepositoryImpl implements PacketRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<ArrPacket> findPacketByTextAndType(final String searchRecord, final Integer registerTypeId,
                                         final Integer firstResult, final Integer maxResults) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ArrPacket> query = builder.createQuery(ArrPacket.class);
        Root<ArrPacket> packet = query.from(ArrPacket.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, registerTypeId, packet, builder);

        Order order = builder.asc(packet.get(ArrPacket.PACKET_ID));
        query.select(packet).where(condition).orderBy(order).distinct(true);

        return entityManager.createQuery(query)
                .setFirstResult(firstResult)
                .setMaxResults(maxResults)
                .getResultList();
    }

    @Override
    public long findPacketByTextAndTypeCount(final String searchRecord, final Integer registerTypeId) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<ArrPacket> record = query.from(ArrPacket.class);

        Predicate condition = preparefindRegRecordByTextAndType(searchRecord, registerTypeId, record, builder);

        query.select(builder.countDistinct(record)).where(condition);

        return entityManager.createQuery(query)
                .getSingleResult();
    }

    @Override
    public List<ArrPacket> findPackets(final ArrFund fund, final Integer limit, final String text, final ArrPacket.State state) {
        String like = "";
        if (text != null) {
            like = " upper(p.storageNumber) LIKE CONCAT('%', upper(:text), '%') AND ";
        }
        String hql = "SELECT p FROM arr_packet p WHERE p.fund = :fund AND " + like + " p.state = :state ORDER BY p.storageNumber ASC";

        Query query = entityManager.createQuery(hql);
        query.setMaxResults(limit);
        query.setParameter("fund", fund);
        if (text != null) {
            query.setParameter("text", text);
        }
        query.setParameter("state", state);

        return query.getResultList();
    }

	@SuppressWarnings("unchecked")
	@Override
    public List<ArrPacket> findPacketsBySubtreeNodeIds(final Collection<Integer> nodeIds, final boolean ignoreRootNodes) {
        Validate.notEmpty(nodeIds);

        RecursiveQueryBuilder<ArrPacket> rqBuilder = DatabaseType.getCurrent().createRecursiveQueryBuilder(ArrPacket.class);

        rqBuilder.addSqlPart("SELECT p.* FROM arr_packet p WHERE p.packet_id IN (")

        .addSqlPart("SELECT dpr.packet_id FROM arr_data_packet_ref dpr ")
        .addSqlPart("JOIN arr_packet ap ON ap.packet_id = dpr.packet_id WHERE dpr.data_id IN (")

        .addSqlPart("SELECT d.data_id FROM arr_item i JOIN arr_data d ON d.data_id = i.data_id ")
        .addSqlPart("JOIN arr_desc_item di ON di.item_id = i.item_id ")
        .addSqlPart("WHERE i.delete_change_id IS NULL AND d.data_type_id = 11 AND di.node_id IN (")

        .addSqlPart("WITH RECURSIVE treeData(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position) AS ")
        .addSqlPart("(SELECT t.* FROM arr_level t WHERE t.node_id IN (:nodeIds) ")
        .addSqlPart("UNION ALL ")
        .addSqlPart("SELECT t.* FROM arr_level t JOIN treeData td ON td.node_id = t.node_id_parent) ")

        .addSqlPart("SELECT DISTINCT n.node_id FROM treeData t JOIN arr_node n ON n.node_id = t.node_id ")
        .addSqlPart("WHERE t.delete_change_id IS NULL");
        if (ignoreRootNodes) {
            rqBuilder.addSqlPart(" AND n.node_id NOT IN (:nodeIds)");
        }

        rqBuilder.addSqlPart(")))");

        rqBuilder.prepareQuery(entityManager);
        rqBuilder.setParameter("nodeIds", nodeIds);

		return rqBuilder.getQuery().getResultList();
    }

    /**
     * Připraví dotaz pro nalezení záznamů obalů.
     *
     * @param searchRecord      hledaný řetězec, může být null
     * @param packetTypeId       typ záznamu
     * @param builder           buider pro vytváření podmínek
     * @return                  výsledné podmínky pro dotaz
     */
    private Predicate preparefindRegRecordByTextAndType(final String searchRecord, final Integer packetTypeId,
                        final Root<ArrPacket> packet, final CriteriaBuilder builder) {

        final String searchString = (searchRecord != null ? searchRecord.toLowerCase() : null);

        Join<Object, Object> partyType = packet.join(ArrPacket.PACKET_TYPE, JoinType.LEFT);

        String searchValue = "%" + searchString + "%";

        Predicate condition = builder.notEqual(packet.get(ArrPacket.INVALID_PACKET), Boolean.TRUE);
        if (searchString != null) {
            condition = builder.and(condition, builder.like(builder.lower(packet.get(ArrPacket.STORAGE_NUMBER)), searchValue));
        }

        if (packetTypeId != null) {
            Predicate conditionType = builder.equal(partyType.get(RulPacketType.PACKET_TYPE_ID), packetTypeId);
            condition = builder.and(condition, conditionType);
        }

        return condition;
    }
}
