package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFund;
import cz.tacr.elza.domain.ArrFundVersion;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


/**
 *
 * @since 22.7.15
 */
@Repository
public interface FundVersionRepository extends ElzaJpaRepository<ArrFundVersion, Integer> {

    @Query(value = "select v from arr_fund_version v join v.createChange ch join v.fund fa where fa.fundId = :fundId order by ch.changeDate desc")
    List<ArrFundVersion> findVersionsByFundIdOrderByCreateDateDesc(@Param(value = "fundId") Integer fundId);

    @Query(value = "select v from arr_fund_version v join v.fund fa where fa.fundId = :fundId and v.lockChange is null")
    ArrFundVersion findByFundIdAndLockChangeIsNull(@Param(value = "fundId") Integer fundId);

    @Query(value = "SELECT v FROM arr_fund_version v join fetch v.fund fa join fetch v.createChange left join fetch v.lockChange order by fa.name asc, v.createChange.changeId desc")
    List<ArrFundVersion> findAllFetchFunds();

    @Query(value = "SELECT v FROM arr_fund_version v JOIN FETCH v.fund f WHERE v.lockChange IS NULL")
    List<ArrFundVersion> findAllOpenVersion();

    @Query(value = "select v from arr_fund_version v JOIN FETCH v.fund fa where fa.fundId in :fundIds and v.lockChange is null")
    List<ArrFundVersion> findByFundIdsAndLockChangeIsNull(@Param(value = "fundIds") Collection<Integer> fundIds);

    /**
     * Fetches fund, fund institution and lockChange for dataexchange export.
     */
    @Query(value = "SELECT v FROM arr_fund_version v JOIN FETCH v.fund f JOIN FETCH f.institution i LEFT JOIN FETCH v.lockChange lc WHERE v.fundVersionId = :fundVersionId")
    ArrFundVersion findByIdWithFetchForExport(@Param(value = "fundVersionId") Integer fundVersionId);

    @Query(value = "SELECT v FROM arr_fund_version v JOIN FETCH v.fund f WHERE v.fundVersionId = :fundVersionId")
    ArrFundVersion findByIdWithFetchFund(@Param(value = "fundVersionId") Integer fundVersionId);

    /**
     * Delete fund versions by fund
     *
     * @param fund
     */
    @Modifying
    @Query("DELETE FROM arr_fund_version fv WHERE fv.fund = ?1")
    void deleteByFund(ArrFund fund);

    @Modifying
    @Query("DELETE FROM arr_fund_version fv WHERE fv.fund = ?1  and fv.rootNodeId is null")
    void deleteByFundNotRootNode(ArrFund fund);

    @Query("SELECT fv FROM arr_fund_version fv JOIN FETCH fv.fund f WHERE fv.lockChange IS NULL AND f IN (SELECT n.fund FROM arr_node n WHERE n.nodeId IN :nodeIds)")
    List<ArrFundVersion> findVersionsByNodeIds(@Param("nodeIds") Collection<Integer> nodeIds);
}
