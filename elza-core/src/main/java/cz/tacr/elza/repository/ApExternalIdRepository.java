package cz.tacr.elza.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApExternalId;

public interface ApExternalIdRepository extends ElzaJpaRepository<ApExternalId, Integer> {

    @Query("SELECT extId FROM ap_external_id extId WHERE extId.accessPoint = ?1 and extId.deleteChange is null")
    ApExternalId findApExternalIdByAccessPoint(ApAccessPoint accessPoint);

    @Modifying
    @Query("UPDATE ap_external_id eid SET eid.deleteChange=?2 WHERE eid.accessPointId IN ?1 AND eid.deleteChange IS NULL")
    void deleteAllByAccessPointIdIn(Collection<Integer> apIds, ApChange deleteChange);
}
