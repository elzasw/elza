package cz.tacr.elza.repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.BatchState;
import cz.tacr.elza.domain.ImpBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ImpBatchRepository extends JpaRepository<ImpBatch, Integer> {

    List<ImpBatch> findByStateIn(Collection<BatchState> states);

    @Query("SELECT b FROM cz.tacr.elza.domain.ImpBatch b "
         + " WHERE b.state IN :states "
         + "   AND b.keepFiles = false "
         + "   AND b.filesDeletedAt IS NULL "
         + "   AND b.lastStateChangeAt < :threshold")
    List<ImpBatch> findEligibleForCleanup(@Param("states") Collection<BatchState> states,
                                          @Param("threshold") OffsetDateTime threshold);
}
