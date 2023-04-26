package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import cz.tacr.elza.domain.ApAccessPoint;
import cz.tacr.elza.domain.ApBinding;
import cz.tacr.elza.domain.ApExternalSystem;
import cz.tacr.elza.domain.ExtSyncsQueueItem;
import cz.tacr.elza.domain.ExtSyncsQueueItem.ExtAsyncQueueState;

@Repository
public interface ExtSyncsQueueItemRepository extends ElzaJpaRepository<ExtSyncsQueueItem, Integer>, ExtSyncsQueueItemRepositoryCustom{

    /*
    @Query("SELECT COUNT(i) FROM ext_syncs_queue_item i WHERE i.state = :state")
    int countByState(@Param("state") ExtSyncsQueueItem.ExtAsyncQueueState state);
    */

    ExtSyncsQueueItem findFirstByStateOrderByExtSyncsQueueItemId(ExtAsyncQueueState state);

    @Query("SELECT i FROM ext_syncs_queue_item i WHERE i.state = :state ORDER BY i.extSyncsQueueItemId")
    Page<ExtSyncsQueueItem> findByState(@Param("state") ExtAsyncQueueState state, Pageable pageable);

    @Query("SELECT i FROM ext_syncs_queue_item i WHERE i.state IN :states ORDER BY i.extSyncsQueueItemId")
    Page<ExtSyncsQueueItem> findByStates(@Param("states") Collection<ExtAsyncQueueState> states, Pageable pageable);

    @Query("SELECT COUNT(i) FROM ext_syncs_queue_item i WHERE i.accessPoint = :accessPoint AND i.externalSystem = :extSystem AND i.state = :state")
    int countByAccesPointAndExternalSystemAndState(@Param("accessPoint") ApAccessPoint accessPoint, @Param("extSystem") ApExternalSystem extSystem, @Param("state") ExtSyncsQueueItem.ExtAsyncQueueState state);

    @Modifying
    int deleteByAccessPoint(ApAccessPoint accessPoint);

    @Modifying
    int deleteByBinding(ApBinding binding);

    @Query("SELECT i FROM ext_syncs_queue_item i WHERE i.accessPointId = :accessPointId AND i.externalSystemId = :extSystemId AND i.state IN :states ORDER BY i.extSyncsQueueItemId")
    List<ExtSyncsQueueItem> findByApExtSystAndState(@Param("accessPointId") Integer accessPointId,
                                                    @Param("extSystemId") Integer externalSystemId,
                                                    @Param("states") Collection<ExtAsyncQueueState> asList);
}
