package cz.tacr.elza.repository;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import cz.tacr.elza.domain.ArrFundVersion;
import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.domain.ArrDataPacketRef;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.utils.ObjectListIterator;

public class DataPacketRefRepositoryImpl implements DataPacketRefRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<ArrDataPacketRef> findByDataIdsAndVersionFetchPacket(Set<Integer> dataIds, final Set<RulDescItemType> descItemTypes, ArrFundVersion version) {
        String hql = "SELECT d FROM arr_data_packet_ref d JOIN FETCH d.descItem di JOIN FETCH di.node n JOIN FETCH di.descItemType dit JOIN FETCH d.packet p JOIN FETCH p.packetType pt  WHERE ";
        if (version.getLockChange() == null) {
            hql += "di.deleteChange IS NULL ";
        } else {
            hql += "di.createChange < :lockChange AND (di.deleteChange IS NULL OR di.deleteChange > :lockChange) ";
        }

        hql += "AND di.descItemType IN (:descItemTypes) AND d.dataId IN (:dataIds)";


        Query query = entityManager.createQuery(hql);

        if (version.getLockChange() != null) {
            query.setParameter("lockChange", version.getLockChange());
        }

        query.setParameter("descItemTypes", descItemTypes);

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
                "JOIN d.packet p JOIN d.descItem di JOIN di.node n JOIN n.fund f JOIN f.versions v " +
                "WHERE v = :version AND p.packetId IN :packetIds";

        Query query = entityManager.createQuery(hql);
        query.setParameter("packetIds", packetIds);
        query.setParameter("version", version);
        return query.getFirstResult();
    }
}
