package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrChange;
import cz.tacr.elza.domain.ArrFund;


/**
 * Respozitory pro číslo změny.
 * 
 * @since 22.7.15
 */
@Repository
public interface ChangeRepository extends ElzaJpaRepository<ArrChange, Integer> {


    @Modifying
    @Query("DELETE FROM arr_change c WHERE c.primaryNode IN (SELECT n FROM arr_node n WHERE n.fund = ?1)")
    void deleteByPrimaryNodeFund(ArrFund fund);

    @Query("SELECT c.changeId FROM arr_change c WHERE c.primaryNodeId IN :nodeIds")
    List<Integer> findChangesIdsByPrimaryNodeIds(@Param("nodeIds") Collection<Integer> nodeIds);

    @Modifying
    @Query("DELETE FROM arr_change c WHERE c.primaryNodeId IN ?1")
    void deleteByPrimaryNodeIds(List<Integer> deleteNodeIds);

    @Modifying
    @Query("DELETE FROM arr_change c WHERE c.primaryNodeId IN :deleteNodeIds AND c.changeId <> :ignoreChangeId")
    void deleteByPrimaryNodeIdsWithIgnoredId(@Param("deleteNodeIds") List<Integer> deleteNodeIds, @Param("ignoreChangeId") Integer ignoreChangeId);

    @Modifying
    @Query("DELETE FROM arr_change c WHERE c.changeId IN :changeIds")
    void deleteAllByIds(@Param("changeIds") Collection<Integer> changeIds);
}
