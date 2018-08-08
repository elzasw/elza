package cz.tacr.elza.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApDescription;

@Repository
public interface ApDescriptionRepository extends ElzaJpaRepository<ApDescription, Integer> {

    @Query("SELECT dsc FROM ap_description dsc WHERE dsc.accessPoint = ?1 and dsc.deleteChangeId is null")
    ApDescription findByAccessPoint(ApAccessPoint accessPoint);

    @Modifying
    @Query("UPDATE ap_description dsc SET dsc.deleteChange=?2 WHERE dsc.accessPointId IN ?1 AND dsc.deleteChangeId IS NULL")
    void invalidateByAccessPointIdIn(Collection<Integer> apIds, ApChange deleteChange);
}
