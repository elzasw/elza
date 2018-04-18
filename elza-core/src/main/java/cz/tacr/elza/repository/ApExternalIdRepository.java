package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApExternalId;
import org.springframework.data.jpa.repository.Query;

public interface ApExternalIdRepository extends ElzaJpaRepository<ApExternalId, Integer> {
    @Query("SELECT extId FROM ap_external_id extId WHERE extId.accessPoint = ?1 and extId.deleteChange = null")
    ApExternalId findApExternalIdByAccessPoint(ApAccessPoint accessPoint);
}
