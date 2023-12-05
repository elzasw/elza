package cz.tacr.elza.repository;

import cz.tacr.elza.common.ObjectListIterator;
import cz.tacr.elza.domain.ArrDataRecordRef;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.RulItemType;

import org.springframework.beans.factory.annotation.Autowired;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Implementation of DataRecordRefRepositoryCustom
 *
 * @since 4. 2. 2016
 */
public class DataRecordRefRepositoryImpl implements DataRecordRefRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<ArrDataRecordRef> findByDataIdsAndVersionFetchRecord(Set<Integer> dataIds, final Set<RulItemType> itemTypes, ArrFundVersion version) {
        return findByDataIdsAndVersionFetchRecord(dataIds, itemTypes, version.getLockChange() == null ? null : version.getLockChange().getChangeId());
    }

    @Override
    public List<ArrDataRecordRef> findByDataIdsAndVersionFetchRecord(final Set<Integer> dataIds, final Set<RulItemType> itemTypes, final Integer changeId) {
        String hql = "SELECT d FROM arr_data_record_ref d JOIN FETCH d.item di JOIN FETCH di.node n JOIN FETCH di.itemType dit JOIN FETCH d.record r WHERE ";
        if (changeId == null) {
            hql += "di.deleteChange IS NULL ";
        } else {
            hql += "di.createChange.changeId < :changeId AND (di.deleteChange IS NULL OR di.deleteChange.changeId > :changeId) ";
        }

        hql += "AND di.itemType IN (:itemTypes) AND d.dataId IN (:dataIds)";

        List<ArrDataRecordRef> result = new LinkedList<>();
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
