package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrBulkActionRun.State;
import cz.tacr.elza.domain.ArrFund;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface BulkActionRunRepository extends JpaRepository<ArrBulkActionRun, Integer>, BulkActionRunRepositoryCustom {

    /**
     * Vrátí všechny hromadné akce daného FA
     * @param fundVersionId id verze FA
     * @return list hromadných ackí
     */
    @Query(value = "SELECT ba FROM arr_bulk_action_run ba JOIN ba.fundVersion v WHERE v.fundVersionId = :fundVersionId")
    List<ArrBulkActionRun> findByFundVersionId(@Param(value = "fundVersionId") Integer fundVersionId);

    @Query(value = "SELECT ba FROM arr_bulk_action_run ba JOIN ba.fundVersion v WHERE v.fundVersionId = :fundVersionId ORDER BY ba.bulkActionRunId DESC")
    List<ArrBulkActionRun> findByFundVersionId(@Param(value = "fundVersionId") Integer fundVersionId, Pageable pageable);

    /**
     * Vrátí všechny hromadné akce verze FA daného CODE
     *
     * @param fundVersionId id verze FA
     * @return list hromadných ackí
     */
    @Query(value = "SELECT ba FROM arr_bulk_action_run ba JOIN ba.fundVersion v WHERE v.fundVersionId = :fundVersionId AND ba.state = :state")
    List<ArrBulkActionRun> findByFundVersionIdAndState(@Param(value = "fundVersionId") final Integer fundVersionId,
                                                       @Param(value = "state") final State state);
    @Query(value = "SELECT ba FROM arr_bulk_action_run ba JOIN ba.fundVersion v WHERE v.fundVersionId = :fundVersionId AND ba.state = :state")
    List<ArrBulkActionRun> findByFundVersionIdAndState(@Param(value = "fundVersionId") final Integer fundVersionId,
                                                       @Param(value = "state") final State state, Pageable pageable);

    @Query(value = "SELECT ba FROM arr_bulk_action_run ba JOIN ba.fundVersion v WHERE v.fundVersionId = :fundVersionId AND ba.bulkActionCode = :code")
    List<ArrBulkActionRun> findByFundVersionIdAndBulkActionCode(@Param(value = "fundVersionId") final Integer fundVersionId,
                                                                @Param(value = "code") final String code);

    @Query(value = "SELECT ba.bulkActionRunId from arr_bulk_action_run ba WHERE ba.state = :state GROUP BY ba.fundVersion,ba.bulkActionRunId ORDER BY ba.bulkActionRunId ASC")
    List<Integer> findIdByStateGroupByFundOrderById(@Param(value = "state") final State state);

    @Modifying
    @Query("UPDATE arr_bulk_action_run ba SET ba.state = :toState WHERE ba.state = :fromState")
    int updateFromStateToState(@Param("fromState") final State fromState, @Param("toState") final State toState);

    List<ArrBulkActionRun> findByState(@Param(value = "state") final State state);
    
    void deleteByFundVersionFund(ArrFund fund);

    @Modifying
    @Query("DELETE FROM arr_bulk_action_run ba WHERE ba.fundVersion IN (SELECT fv FROM arr_fund_version fv WHERE fv.fund = ?1)")
    void deleteByFund(ArrFund fund);
}
