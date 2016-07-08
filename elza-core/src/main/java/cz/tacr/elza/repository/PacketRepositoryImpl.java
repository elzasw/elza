package cz.tacr.elza.repository;

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

import cz.tacr.elza.domain.ArrFund;
import org.springframework.stereotype.Component;

import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulPacketType;

/**
 * Implementace repository pro obaly.
 *
 * @author <a href="mailto:martin.kuzel@marbes.cz">Martin Kužel</a>
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
