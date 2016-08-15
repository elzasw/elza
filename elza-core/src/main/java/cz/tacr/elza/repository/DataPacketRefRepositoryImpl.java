package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrPacket;
import cz.tacr.elza.domain.RulItemType;
import cz.tacr.elza.utils.ObjectListIterator;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DataPacketRefRepositoryImpl implements DataPacketRefRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<ArrDataPacketRef> findByDataIdsAndVersionFetchPacket(Set<Integer> dataIds, final Set<RulItemType> itemTypes, ArrFundVersion version) {
        String hql = "SELECT d FROM arr_data_packet_ref d JOIN FETCH d.item di JOIN FETCH di.node n JOIN FETCH di.itemType dit JOIN FETCH d.packet p JOIN FETCH p.packetType pt  WHERE ";
        if (version.getLockChange() == null) {
            hql += "di.deleteChange IS NULL ";
        } else {
            hql += "di.createChange < :lockChange AND (di.deleteChange IS NULL OR di.deleteChange > :lockChange) ";
        }

        hql += "AND di.itemType IN (:itemTypes) AND d.dataId IN (:dataIds)";


        Query query = entityManager.createQuery(hql);

        if (version.getLockChange() != null) {
            query.setParameter("lockChange", version.getLockChange());
        }

        query.setParameter("itemTypes", itemTypes);

        List<ArrDataPacketRef> result = new LinkedList<>();
        ObjectListIterator<Integer> nodeIdsIterator = new ObjectListIterator<Integer>(dataIds);
        while (nodeIdsIterator.hasNext()) {
            query.setParameter("dataIds", nodeIdsIterator.next());

            result.addAll(query.getResultList());
        }

        return result;
    }

    @Override
    public int countInFundVersionByPacketIds(final List<Integer> packetIds, final ArrFundVersion version) {
        String hql = "SELECT count(d) FROM arr_data_packet_ref d " +
                "JOIN d.packet p JOIN d.item di JOIN di.node n JOIN n.fund f JOIN f.versions v " +
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
