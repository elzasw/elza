package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApChange;
import cz.tacr.elza.domain.ApName;
import cz.tacr.elza.domain.ApScope;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repository jmen pristupovych bodu.
 *
 * @author <a href="mailto:vojtech.fric@marbes.cz">Vojtěch Frič</a>
 */
@Repository
public interface ApNameRepository extends ElzaJpaRepository<ApName, Integer> {

    @Query("SELECT name FROM ap_name name WHERE name.accessPoint = ?1 and name.deleteChangeId is null ORDER BY name.preferredName DESC")
    List<ApName> findByAccessPoint(ApAccessPoint accessPoint);

    @Query("SELECT name FROM ap_name name WHERE name.accessPoint = ?1 and name.preferredName = true and name.deleteChangeId is null")
    ApName findPreferredNameByAccessPoint(ApAccessPoint accessPoint);

    @Modifying
    @Query("UPDATE ap_name name SET name.deleteChange=?2 WHERE name.accessPointId IN ?1 AND name.deleteChangeId IS NULL")
    int invalidateByAccessPointIdIn(Collection<Integer> apIds, ApChange deleteChange);

    @Query("SELECT COUNT(n) FROM ap_name n JOIN n.accessPoint ap WHERE ap.scope = :scope AND LOWER(n.fullName) = LOWER(:fullName) AND n.deleteChangeId IS NULL")
    int countUniqueName(@Param("fullName") String fullName, @Param("scope") ApScope scope);
}
