package cz.tacr.elza.repository;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.domain.ArrDataPartyRef;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.RulItemType;

import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DataPartyRefRepositoryImpl implements DataPartyRefRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<ArrDataPartyRef> findByDataIdsAndVersionFetchPartyRecord(Set<Integer> dataIds, final Set<RulItemType> itemTypes, ArrFundVersion version) {
        return findByDataIdsAndVersionFetchPartyRecord(dataIds, itemTypes, version.getLockChange() == null ? null : version.getLockChange().getChangeId());
    }

    @Override
    public List<ArrDataPartyRef> findByDataIdsAndVersionFetchPartyRecord(final Set<Integer> dataIds, final Set<RulItemType> itemTypes, final Integer changeId) {
        String hql = "SELECT d FROM arr_data_party_ref d JOIN FETCH d.item di JOIN FETCH di.node n JOIN FETCH di.itemType dit JOIN FETCH d.party party JOIN FETCH party.accessPoint r WHERE ";
        if (changeId == null) {
            hql += "di.deleteChange IS NULL ";
        } else {
            hql += "di.createChange.changeId < :changeId AND (di.deleteChange IS NULL OR di.deleteChange.changeId > :changeId) ";
        }

        hql += "AND di.itemType IN (:itemTypes) AND d.dataId IN (:dataIds)";


        List<ArrDataPartyRef> result = new LinkedList<>();
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
}
