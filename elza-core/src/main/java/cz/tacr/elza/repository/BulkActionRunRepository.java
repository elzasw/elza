package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrBulkActionRun;


@Repository
public interface BulkActionRunRepository extends JpaRepository<ArrBulkActionRun, Integer> {

    @Query(value = "SELECT ba FROM arr_bulk_action_run ba JOIN ba.version v WHERE v.findingAidVersionId = :faVersionId")
    List<ArrBulkActionRun> findByFaVersionId(@Param(value = "faVersionId") Integer faVersionId);


    @Query(value = "SELECT ba FROM arr_bulk_action_run ba JOIN ba.version v WHERE v.findingAidVersionId = :faVersionId AND ba.bulkActionCode = :code")
    List<ArrBulkActionRun> findByFaVersionIdAndBulkActionCode(@Param(value = "faVersionId") final Integer faVersionId,
                                                             @Param(value = "code") final String code);
}
