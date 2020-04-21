package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrAsyncRequest;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.AsyncTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface ArrAsyncRequestRepository extends JpaRepository<ArrAsyncRequest, Integer> {

    @Query("SELECT aar FROM arr_async_request aar JOIN FETCH aar.node WHERE aar.type = :type ORDER BY aar.priority")
    List<ArrAsyncRequest> findNodeRequestsByPriorityWithLimit(@Param(value = "type") AsyncTypeEnum type, Pageable pageable);

    @Query("SELECT aar FROM arr_async_request aar WHERE aar.type = :type ORDER BY aar.priority")
    List<ArrAsyncRequest> findRequestsByPriorityWithLimit(@Param(value = "type") AsyncTypeEnum type, Pageable pageable);

    @Query("SELECT aar FROM arr_async_request aar JOIN FETCH aar.node WHERE aar.type = :type AND aar.asyncRequestId NOT IN (:loadedRequestIds) ORDER BY aar.priority")
    List<ArrAsyncRequest> findNodeRequestsByPriorityAndRequestsWithLimit(@Param(value = "type") AsyncTypeEnum type, @Param(value= "loadedRequestIds") Collection<Long> loadedRequestIds, Pageable pageable);

    @Modifying
    @Query("DELETE FROM arr_async_request aar WHERE aar.node.nodeId = :nodeId ")
    void deleteByNodeId(@Param(value="nodeId") Integer nodeId);

    @Modifying
    @Query("DELETE FROM arr_async_request aar WHERE aar.bulkAction.bulkActionRunId = :bulkActionId ")
    void deleteByBulkActionRunId(@Param(value="bulkActionId") Integer bulkActionId);

    @Modifying
    @Query("DELETE FROM arr_async_request aar WHERE aar.output.outputId = :outputId")
    void deleteByOutputId(@Param(value="outputId") Integer outputId);

    @Modifying
    @Query("DELETE FROM arr_async_request aar WHERE aar.asyncRequestId = :requestId")
    void deleteByRequestId(@Param(value="requestId") Long requestId);
}


