package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import cz.tacr.elza.domain.ArrBulkActionNode;
import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrFund;

/**
 * Repository k {@link ArrBulkActionNode}
 *
 * @author Martin Šlapa
 * @since 04.04.2016
 */
public interface BulkActionNodeRepository extends JpaRepository<ArrBulkActionNode, Integer> {

    @Query("SELECT node.nodeId FROM arr_bulk_action_node an JOIN an.node node WHERE an.bulkActionRun = :bulkActionRun")
    List<Integer> findNodeIdsByBulkActionRun(@Param(value = "bulkActionRun") final ArrBulkActionRun bulkActionRun);

    @Modifying
    @Query("DELETE FROM arr_bulk_action_node an WHERE an.bulkActionRun = :bulkActionRun")
    void deleteByBulkAction(@Param(value = "bulkActionRun") final ArrBulkActionRun action);

    void deleteByNodeFund(ArrFund fund);
}
