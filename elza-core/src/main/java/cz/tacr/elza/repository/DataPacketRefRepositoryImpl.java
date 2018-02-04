package cz.tacr.elza.repository;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulItemType;

public class DataPacketRefRepositoryImpl implements DataPacketRefRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<ArrDataPacketRef> findByDataIdsAndVersionFetchPacket(Set<Integer> dataIds, final Set<RulItemType> itemTypes, ArrFundVersion version) {
        return findByDataIdsAndVersionFetchPacket(dataIds, itemTypes, version.getLockChange() == null ? null : version.getLockChange().getChangeId());
    }

    @Override
    public List<ArrDataPacketRef> findByDataIdsAndVersionFetchPacket(final Set<Integer> dataIds, final Set<RulItemType> itemTypes, final Integer changeId) {
        String hql = "SELECT d FROM arr_data_packet_ref d JOIN FETCH d.item di JOIN FETCH di.node n JOIN FETCH di.itemType dit JOIN FETCH d.packet p LEFT JOIN FETCH p.packetType pt  WHERE ";
        if (changeId == null) {
            hql += "di.deleteChange IS NULL ";
        } else {
            hql += "di.createChange.changeId < :changeId AND (di.deleteChange IS NULL OR di.deleteChange.changeId > :changeId) ";
        }

        hql += "AND di.itemType IN (:itemTypes) AND d.dataId IN (:dataIds)";

        List<ArrDataPacketRef> result = new LinkedList<>();
        ObjectListIterator<Integer> nodeIdsIterator = new ObjectListIterator<Integer>(dataIds);
        while (nodeIdsIterator.hasNext()) {
            Query query = entityManager.createQuery(hql);

            if (changeId != null) {
                query.setParameter("changeId", changeId);
            }

            query.setParameter("itemTypes", itemTypes);
            query.setParameter("dataIds", nodeIdsIterator.next());

            result.addAll(query.getResultList());
        }

        return result;
    }

    @Override
    public int countInFundVersionByPacketIds(final List<Integer> packetIds, final ArrFundVersion version) {
        /*String hql = "SELECT count(d) FROM arr_data_packet_ref d " +
                "JOIN d.packet p JOIN d.item di JOIN di.node n JOIN n.fund f JOIN f.versions v " +
                "WHERE v = :version AND p.packetId IN :packetIds";*/

        String hql = "SELECT count(di.data) FROM arr_item di " +
                "JOIN di.data d JOIN d.packet p JOIN di.node n JOIN n.fund f JOIN f.versions v " +
                "WHERE v = :version AND p.packetId IN :packetIds";

        Query query = entityManager.createQuery(hql);
        query.setParameter("packetIds", packetIds);
        query.setParameter("version", version);
        return query.getFirstResult();
    }

    @Override
    public List<ArrPacket> findUsePacketsByPacketIds(final List<Integer> packetIds) {
        List<ArrPacket> result = new ArrayList<>();
        ObjectListIterator<Integer> nodeIdsIterator = new ObjectListIterator<>(packetIds);
        while (nodeIdsIterator.hasNext()) {
            String hql = "SELECT DISTINCT p FROM arr_data_packet_ref d " +
                    "JOIN d.packet p " +
                    "WHERE p.packetId IN :packetIds";

            Query query = entityManager.createQuery(hql);
            query.setParameter("packetIds", nodeIdsIterator.next());
            result.addAll(query.getResultList());
        }
        return result;
    }
}
