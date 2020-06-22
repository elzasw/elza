package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.projection.ApExternalIdInfo;

public interface ApBindingRepository extends ElzaJpaRepository<ApBinding, Integer> {

    @Query("SELECT eid FROM ap_external_id eid WHERE eid.accessPoint = ?1 and eid.deleteChangeId is null")
    List<ApBinding> findByAccessPoint(ApAccessPoint accessPoint);

    @Query("SELECT eid FROM ap_external_id eid WHERE eid.accessPoint IN :accessPoints and eid.deleteChangeId IS NULL")
    List<ApBinding> findByAccessPoints(@Param("accessPoints") Collection<ApAccessPoint> accessPoints);

    /**
     * Searches external ids and its APs by type code and values.
     *
     * @return External id and AP projections.
     */
    @Query("SELECT new cz.tacr.elza.domain.projection.ApExternalIdInfo(eid.value, ap.accessPointId, ap.uuid, s.stateId, s.scopeId, s.apTypeId)" +
            " FROM ap_external_id eid" +
            " JOIN eid.accessPoint ap" +
            " JOIN ap_state s on s.accessPointId = ap.accessPointId" +
            " WHERE eid.externalIdTypeId = :typeId" +
            " AND eid.value IN :values" +
            " AND eid.deleteChangeId IS NULL" +
            " AND s.deleteChangeId IS NULL")
    List<ApExternalIdInfo> findActiveInfoByTypeIdAndValues(@Param("typeId") Integer typeId, @Param("values") Collection<String> values);

    @Modifying
    @Query("UPDATE ap_external_id eid SET eid.deleteChange=?2 WHERE eid.accessPointId IN ?1 AND eid.deleteChangeId IS NULL")
    void invalidateByAccessPointIdIn(Collection<Integer> apIds, ApChange deleteChange);
}
