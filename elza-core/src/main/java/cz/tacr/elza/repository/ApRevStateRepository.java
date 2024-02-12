package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApRevState;
import cz.tacr.elza.domain.ApRevision;
import cz.tacr.elza.domain.ApState;

@Repository
public interface ApRevStateRepository extends ElzaJpaRepository<ApRevState, Integer>, JpaSpecificationExecutor<ApRevState> {

    @Query("SELECT rs FROM ap_rev_state rs JOIN FETCH rs.revision rev WHERE rev = :revision AND rs.deleteChange IS NULL")
    ApRevState findLastRevState(@Param("revision") ApRevision revision);

    @Query("SELECT rs FROM ap_rev_state rs JOIN FETCH rs.revision rev WHERE rev.state = :state AND rs.deleteChange IS NULL")
    ApRevState findByState(@Param("state") ApState state);

    @Query("SELECT rs FROM ap_rev_state rs JOIN FETCH rs.revision rev WHERE rev.state in :states AND rs.deleteChange IS NULL")
    List<ApRevState> findByStates(@Param("states") List<ApState> state);
}
