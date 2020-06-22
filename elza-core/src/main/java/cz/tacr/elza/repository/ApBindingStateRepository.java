package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBindingState;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.projection.ApExternalIdInfo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ApBindingStateRepository extends ElzaJpaRepository<ApBindingState, Integer> {

    @Query("SELECT bis FROM ap_binding_state bis JOIN bis.binding WHERE bis.accessPoint = ?1 and bis.deleteChangeId is null")
    List<ApBindingState> findByAccessPoint(ApAccessPoint accessPoint);

    @Query("SELECT bis FROM ap_binding_state bis JOIN bis.binding WHERE bis.accessPoint IN :accessPoints and bis.deleteChangeId IS NULL")
    List<ApBindingState> findByAccessPoints(@Param("accessPoints") Collection<ApAccessPoint> accessPoints);

    @Modifying
    @Query("UPDATE ap_binding_state bis SET bis.deleteChange=?2 WHERE bis.accessPointId IN ?1 AND bis.deleteChangeId IS NULL")
    void invalidateByAccessPointIdIn(Collection<Integer> apIds, ApChange deleteChange);

    /**
     * Searches external ids and its APs by type code and values.
     *
     * @return External id and AP projections.
     */
    @Query("SELECT new cz.tacr.elza.domain.projection.ApExternalIdInfo(bin.value, ap.accessPointId, ap.uuid, s.stateId, s.scopeId, s.apTypeId)" +
            " FROM ap_binding_state bis" +
            " JOIN bis.binding bin" +
            " JOIN bin.apExternalSystem aes" +
            " JOIN bis.accessPoint ap" +
            " JOIN ap_state s on s.accessPointId = ap.accessPointId" +
            " WHERE aes.externalSystemId = :typeId" +
            " AND bin.value IN :values" +
            " AND bis.deleteChangeId IS NULL" +
            " AND s.deleteChangeId IS NULL")
    List<ApExternalIdInfo> findActiveInfoByTypeIdAndValues(@Param("typeId") Integer typeId, @Param("values") Collection<String> values);
}
