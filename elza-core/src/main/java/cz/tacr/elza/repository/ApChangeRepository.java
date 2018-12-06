package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ApChange;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApChangeRepository extends ElzaJpaRepository<ApChange, Integer> {

    @Query("SELECT DISTINCT ap.createChange FROM ap_access_point ap WHERE ap.state = 'TEMP'")
    List<ApChange> findTempChange();
}
