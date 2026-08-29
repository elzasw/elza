package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrDigitalRepository;
import cz.tacr.elza.domain.DaAip;
import cz.tacr.elza.domain.DaSyncQueueItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;


@Repository
public interface DaSyncQueueItemRepository extends JpaRepository<DaSyncQueueItem, Integer> {

    @Query("SELECT i FROM da_sync_queue_item i WHERE i.state IN :states and i.active = true ORDER BY i.syncQueueItemId")
    Page<DaSyncQueueItem> findByStates(@Param("states") Collection<DaSyncQueueItem.QueueItemState> states, Pageable pageable);

    @Modifying
    @Query("UPDATE da_sync_queue_item i SET i.active = false " +
            "WHERE i.code = :code " +
            "AND i.digitalRepository = :digitalRepository " +
            "AND i.state IN :states " +
            "AND i.active IS TRUE")
    void updateActiveByCodeAndDigitalRepositoryAndStateInAndActiveIsTrue(@Param("code") String code,
                                                                         @Param("digitalRepository") ArrDigitalRepository digitalRepository,
                                                                         @Param("states") Collection<DaSyncQueueItem.QueueItemState> states);

    DaSyncQueueItem findByAipAndStateInAndActiveIsTrue(DaAip aip, Collection<DaSyncQueueItem.QueueItemState> states);

    /**
     * Pairs (aipId, actionItemId) of the action items the given queue items are carrying out.
     *
     * A projection rather than the entities: the processor works with queue items read in an
     * earlier transaction, where navigating their associations is no longer possible.
     */
    @Query("SELECT q.aipActionItem.aip.aipId, q.aipActionItem.aipActionItemId FROM da_sync_queue_item q"
            + " WHERE q.syncQueueItemId IN :queueItemIds AND q.aipActionItem IS NOT NULL")
    List<Object[]> findAipAndActionItemIds(@Param("queueItemIds") Collection<Integer> queueItemIds);
}
