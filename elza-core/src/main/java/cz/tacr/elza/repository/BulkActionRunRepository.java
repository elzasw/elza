package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrBulkActionRun;


@Repository
public interface BulkActionRunRepository extends JpaRepository<ArrBulkActionRun, Integer> {

    @Query(value = "SELECT ba FROM arr_bulk_action_run ba JOIN ba.fundVersion v WHERE v.fundVersionId = :fundVersionId")
    List<ArrBulkActionRun> findByFundVersionId(@Param(value = "fundVersionId") Integer fundVersionId);


    @Query(value = "SELECT ba FROM arr_bulk_action_run ba JOIN ba.fundVersion v WHERE v.fundVersionId = :fundVersionId AND ba.bulkActionCode = :code")
    List<ArrBulkActionRun> findByFundVersionIdAndBulkActionCode(@Param(value = "fundVersionId") final Integer fundVersionId,
                                                                @Param(value = "code") final String code);
}
