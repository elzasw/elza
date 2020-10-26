package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ExtSyncsQueueItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExtSyncsQueueItemRepository extends ElzaJpaRepository<ExtSyncsQueueItem, Integer>, ExtSyncsQueueItemRepositoryCustom{

    @Query("SELECT COUNT(i) FROM ext_syncs_queue_item i WHERE i.state = :state")
    int countByState(@Param("state") ExtSyncsQueueItem.ExtAsyncQueueState state);

    @Query("SELECT i FROM ext_syncs_queue_item i WHERE i.state = :state")
    Page<ExtSyncsQueueItem> findByState(@Param("state") ExtSyncsQueueItem.ExtAsyncQueueState state, Pageable pageable);
}
