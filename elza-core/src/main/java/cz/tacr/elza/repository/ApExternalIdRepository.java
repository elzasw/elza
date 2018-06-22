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

    @Query("SELECT extId FROM ap_external_id extId WHERE extId.accessPoint = ?1 and extId.deleteChange is null")
    ApExternalId findApExternalIdByAccessPoint(ApAccessPoint accessPoint);

    /**
     * Searches external ids and its APs by type code and values.
     * 
     * @return External id and AP projections.
     */
    @Query("SELECT eid.accessPoint as accessPoint, eid.value as value "
            + "FROM ap_external_id eid JOIN ap_external_id_type eidType "
            + "WHERE eidType.code=?1 AND eid.value IN ?2 AND eid.deleteChange IS NULL")
    List<ApExternalIdInfo> findInfoByExternalIdTypeCodeAndValuesIn(String typeCode, Collection<String> values);

    @Modifying
    @Query("UPDATE ap_external_id eid SET eid.deleteChange=?2 WHERE eid.accessPointId IN ?1 AND eid.deleteChange IS NULL")
    void deleteByAccessPointIdIn(Collection<Integer> apIds, ApChange deleteChange);
}
