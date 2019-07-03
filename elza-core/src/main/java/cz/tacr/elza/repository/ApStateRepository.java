package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApState;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ApStateRepository extends ElzaJpaRepository<ApState, Integer> {
    @Query("SELECT s FROM ap_state s WHERE s.accessPoint = ?1 AND s.deleteChangeId IS NULL")
    ApState findByAccessPoint(ApAccessPoint accessPoint);

    @Query("SELECT s FROM ap_state s WHERE s.accessPoint IN :accessPoints AND s.deleteChangeId IS NULL")
    List<ApState> findByAccessPoints(@Param("accessPoints") Collection<ApAccessPoint> accessPoints);

    @Modifying
    @Query("DELETE FROM ap_state s WHERE s.stateId IN (SELECT i.stateId FROM ap_state i JOIN i.accessPoint ap WHERE ap.state = 'TEMP' AND i.accessPoint = :accessPoint)")
    void removeTemp(@Param("accessPoint") ApAccessPoint accessPoint);
}
