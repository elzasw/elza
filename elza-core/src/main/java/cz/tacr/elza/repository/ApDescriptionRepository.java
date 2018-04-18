package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApDescription;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ApDescriptionRepository extends ElzaJpaRepository<ApDescription, Integer> {
    @Query("SELECT dsc FROM ap_description dsc WHERE dsc.accessPoint = ?1 and dsc.deleteChange = null")
    ApDescription findApDescriptionByAccessPoint(ApAccessPoint accessPoint);
}
