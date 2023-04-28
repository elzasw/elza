package cz.tacr.elza.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ArrAsyncRequest;
import cz.tacr.elza.domain.AsyncTypeEnum;

@Repository
public interface ArrAsyncRequestRepository extends CrudRepository<ArrAsyncRequest, Long> {

    /**
     * Return page of requests
     * 
     * Requests are ordered by priority and id.
     * 
     * @param type
     * @param pageable
     * @return
     */
    @Query("SELECT aar FROM arr_async_request aar WHERE aar.type = :type ORDER BY aar.priority, aar.asyncRequestId")
    List<ArrAsyncRequest> findRequestsByPriorityWithLimit(@Param(value = "type") AsyncTypeEnum type, Pageable pageable);

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


