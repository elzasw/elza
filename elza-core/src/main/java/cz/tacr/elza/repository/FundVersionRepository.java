package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrLevel;


/**
 * @author by Ondřej Buriánek, burianek@marbes.cz.
 * @since 22.7.15
 */
@Repository
public interface FundVersionRepository extends ElzaJpaRepository<ArrFundVersion, Integer> {

    @Query(value = "select v from arr_fund_version v join v.createChange ch join v.fund fa where fa.fundId = :fundId order by ch.changeDate asc")
    List<ArrFundVersion> findVersionsByFundIdOrderByCreateDateAsc(@Param(value = "fundId") Integer fundId);


    @Query(value = "select v from arr_fund_version v join v.fund fa where fa.fundId = :fundId and v.lockChange is null")
    ArrFundVersion findByFundIdAndLockChangeIsNull(@Param(value = "fundId") Integer fundId);


    ArrFundVersion findTopByRootNode(ArrNode node);


    @Query(value = "SELECT v FROM arr_fund_version v join fetch v.fund fa join fetch v.arrangementType at join fetch at.ruleSet join fetch v.createChange left join fetch v.lockChange order by fa.name asc, v.createChange.changeId desc")
    List<ArrFundVersion> findAllFetchFunds();


    @Override
    default String getClassName() {
        return ArrFundVersion.class.getSimpleName();
    }
}
