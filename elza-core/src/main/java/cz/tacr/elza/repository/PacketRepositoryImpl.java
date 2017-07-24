package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

import cz.tacr.elza.domain.ArrFund;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulPacketType;
import org.springframework.util.Assert;

/**
 * Implementace repository pro obaly.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
 */
@Component
public class PacketRepositoryImpl implements PacketRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private LevelRepository levelRepository;

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

    @Override
    public Set<ArrPacket> findPacketsBySubtreeNodeIds(final Collection<Integer> nodeIds, final boolean ignoreRootNodes) {
        Assert.notEmpty(nodeIds, "Identifikátor JP musí být vyplněn");

        String sql_nodes = "WITH " + levelRepository.getRecursivePart() + " treeData(level_id, create_change_id, delete_change_id, node_id, node_id_parent, position) AS (SELECT t.* FROM arr_level t WHERE t.node_id IN (:nodeIds) UNION ALL SELECT t.* FROM arr_level t JOIN treeData td ON td.node_id = t.node_id_parent) " +
                "SELECT DISTINCT n.node_id FROM treeData t JOIN arr_node n ON n.node_id = t.node_id WHERE t.delete_change_id IS NULL";

        if (ignoreRootNodes) {
            sql_nodes += " AND n.node_id NOT IN (:nodeIds)";
        }

        String sql = "SELECT p.* FROM arr_packet p WHERE p.packet_id IN" +
                " (" +
                "  SELECT dpr.packet_id FROM arr_data_packet_ref dpr JOIN arr_packet ap ON ap.packet_id = dpr.packet_id WHERE dpr.data_id IN (SELECT d.data_id FROM arr_data d JOIN arr_item i ON d.item_id = i.item_id JOIN arr_desc_item di ON di.item_id = i.item_id WHERE i.delete_change_id IS NULL AND d.data_type_id = 11 AND di.node_id IN (" + sql_nodes + "))" +
                " )";

        Query query = entityManager.createNativeQuery(sql, ArrPacket.class);
        query.setParameter("nodeIds", nodeIds);

        return new HashSet<>(query.getResultList());
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
