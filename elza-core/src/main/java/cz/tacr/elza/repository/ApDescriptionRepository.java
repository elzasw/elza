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

    @Query("SELECT dsc FROM ap_description dsc WHERE dsc.accessPoint = ?1 and dsc.deleteChange is null")
    ApDescription findApDescriptionByAccessPoint(ApAccessPoint accessPoint);

    @Modifying
    @Query("UPDATE ap_description desc SET desc.deleteChange=?2 WHERE desc.accessPointId IN ?1 AND desc.deleteChange IS NULL")
    void deleteByAccessPointIdIn(Collection<Integer> apIds, ApChange deleteChange);
}
