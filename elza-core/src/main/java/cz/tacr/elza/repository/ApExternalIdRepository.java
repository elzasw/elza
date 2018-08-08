package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalId;
import cz.tacr.elza.domain.projection.ApExternalIdInfo;

public interface ApExternalIdRepository extends ElzaJpaRepository<ApExternalId, Integer> {

    @Query("SELECT eid FROM ap_external_id eid WHERE eid.accessPoint = ?1 and eid.deleteChangeId is null")
    List<ApExternalId> findByAccessPoint(ApAccessPoint accessPoint);

    /**
     * Searches external ids and its APs by type code and values.
     *
     * @return External id and AP projections.
     */
    @Query("SELECT eid.accessPoint as accessPoint, eid.value as value FROM ap_external_id eid "
            + "WHERE eid.externalIdTypeId=?1 AND eid.value IN ?2 AND eid.deleteChangeId IS NULL")
    List<ApExternalIdInfo> findInfoByExternalIdTypeIdAndValuesIn(Integer typeId, Collection<String> values);

    @Modifying
    @Query("UPDATE ap_external_id eid SET eid.deleteChange=?2 WHERE eid.accessPointId IN ?1 AND eid.deleteChangeId IS NULL")
    void invalidateByAccessPointIdIn(Collection<Integer> apIds, ApChange deleteChange);
}
