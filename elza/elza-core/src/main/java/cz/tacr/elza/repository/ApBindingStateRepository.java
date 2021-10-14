package cz.tacr.elza.repository;

import cz.tacr.elza.domain.*;
import cz.tacr.elza.domain.projection.ApExternalIdInfo;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApBindingStateRepository extends ElzaJpaRepository<ApBindingState, Integer> {

    @Query("SELECT COUNT(bs) FROM ap_binding_state bs WHERE bs.accessPoint = ?1 and bs.deleteChangeId is null")
    int countByAccessPoint(ApAccessPoint accessPoint);

    @Query("SELECT bis FROM ap_binding_state bis JOIN bis.binding WHERE bis.accessPoint = ?1 and bis.deleteChangeId is null")
    List<ApBindingState> findByAccessPoint(ApAccessPoint accessPoint);

    @Query("SELECT bis FROM ap_binding_state bis JOIN FETCH bis.binding b JOIN FETCH b.apExternalSystem WHERE bis.accessPoint IN :accessPoints AND bis.deleteChangeId IS NULL")
    List<ApBindingState> findByAccessPoints(@Param("accessPoints") Collection<ApAccessPoint> accessPoints);

    @Query("SELECT bis FROM ap_binding_state bis WHERE bis.binding = :binding AND bis.deleteChangeId IS NULL")
    Optional<ApBindingState> findActiveByBinding(@Param("binding") ApBinding binding);

    @Query("SELECT bis FROM ap_binding_state bis JOIN FETCH bis.binding bin WHERE bis.accessPoint IN :accessPoints AND bin.apExternalSystem = :externalSystem AND bis.deleteChangeId IS NULL")
    List<ApBindingState> findByAccessPointsAndExternalSystem(@Param("accessPoints") Collection<ApAccessPoint> accessPoints, @Param("externalSystem") ApExternalSystem externalSystem);

    @Query("SELECT bis FROM ap_binding_state bis JOIN FETCH bis.binding bin WHERE bis.accessPoint = :accessPoint AND bis.deleteChangeId IS NULL AND bin.apExternalSystem = :externalSystem")
    ApBindingState findByAccessPointAndExternalSystem(@Param("accessPoint") ApAccessPoint accessPoint, @Param("externalSystem") ApExternalSystem externalSystem);

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

    @Query("SELECT bis FROM ap_binding_state bis JOIN bis.accessPoint WHERE bis.deleteChangeId IS NULL AND bis.binding IN :bidings")
    List<ApBindingState> findByBindings(@Param("bidings") Collection<ApBinding> bs);

    void deleteByBinding(ApBinding binding);
}
