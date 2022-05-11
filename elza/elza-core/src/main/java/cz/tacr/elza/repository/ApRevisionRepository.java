package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApState;

@Repository
public interface ApRevisionRepository extends JpaRepository<ApRevision, Integer> {

    @Query("SELECT r FROM ap_revision r WHERE r.state = :state AND r.deleteChange IS NULL")
    ApRevision findByState(@Param("state") ApState state);

    @Query("SELECT r FROM ap_revision r WHERE r.state IN :state AND r.deleteChange IS NULL")
    List<ApRevision> findAllByStateIn(@Param("state") List<ApState> apStates);
}
