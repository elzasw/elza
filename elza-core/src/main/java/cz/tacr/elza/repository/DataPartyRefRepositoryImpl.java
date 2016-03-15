package cz.tacr.elza.repository;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;

import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.RulDescItemType;
import cz.tacr.elza.utils.ObjectListIterator;

public class DataPartyRefRepositoryImpl implements DataPartyRefRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<ArrDataPartyRef> findByDataIdsAndVersionFetchPartyRecord(Set<Integer> dataIds, final Set<RulDescItemType> descItemTypes, ArrFundVersion version) {
        String hql = "SELECT d FROM arr_data_party_ref d JOIN FETCH d.descItem di JOIN FETCH di.node n JOIN FETCH di.descItemType dit JOIN FETCH d.party party JOIN FETCH party.record r WHERE ";
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

        List<ArrDataPartyRef> result = new LinkedList<>();
        ObjectListIterator<Integer> nodeIdsIterator = new ObjectListIterator<Integer>(dataIds);
        while (nodeIdsIterator.hasNext()) {
            query.setParameter("dataIds", nodeIdsIterator.next());

            result.addAll(query.getResultList());
        }

        return result;
    }
}
