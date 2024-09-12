package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaSyncQueueItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


@Repository
public interface DaSyncQueueItemRepository extends JpaRepository<DaSyncQueueItem, Integer> {

    @Query("SELECT i FROM da_sync_queue_item i WHERE i.state IN :states ORDER BY i.syncQueueItemId")
    Page<DaSyncQueueItem> findByStates(@Param("states") Collection<DaSyncQueueItem.QueueItemState> states, Pageable pageable);

    List<DaSyncQueueItem> findByCodeInAndDigitalRepositoryAndStateIn(List<String> codes,
                                                                     ArrDigitalRepository digitalRepository,
                                                                     Collection<DaSyncQueueItem.QueueItemState> states);
}
