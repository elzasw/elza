package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ApRevisionRepository extends JpaRepository<ApRevision, Integer> {

    @Query("SELECT r FROM ap_revision r WHERE r.state = :state AND r.deleteChange IS NULL")
    ApRevision findByState(@Param("state") ApState state);
}
