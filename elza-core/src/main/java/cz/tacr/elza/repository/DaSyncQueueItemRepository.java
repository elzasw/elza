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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;


@Repository
public interface DaSyncQueueItemRepository extends JpaRepository<DaSyncQueueItem, Integer> {

    /**
     * Pending items in the order they should be processed: items whose download failed go
     * after the ones that never failed (and the more failures, the later), so a repeatedly
     * failing download is still retried every time the queue runs dry but never starves the
     * items behind it.
     */
    @Query("SELECT i FROM da_sync_queue_item i WHERE i.state IN :states and i.active = true"
            + " AND (i.nextAttemptAt IS NULL OR i.nextAttemptAt <= :now)"
            + " AND i.digitalRepository.externalSystemId NOT IN :busyRepositoryIds"
            + " ORDER BY i.attemptCount, i.syncQueueItemId")
    Page<DaSyncQueueItem> findDueByStates(@Param("states") Collection<DaSyncQueueItem.QueueItemState> states,
                                          @Param("now") OffsetDateTime now,
                                          @Param("busyRepositoryIds") Collection<Integer> busyRepositoryIds,
                                          Pageable pageable);

    /** Repositories with a batch of the given state in flight - one batch per direction at a time. */
    @Query("SELECT DISTINCT i.digitalRepository.externalSystemId FROM da_sync_queue_item i"
            + " WHERE i.state = :state AND i.active = true")
    List<Integer> findRepositoriesWithState(@Param("state") DaSyncQueueItem.QueueItemState state);

    /** Items of a batch in flight whose next question about it is due, the longest waiting first. */
    @Query("SELECT i FROM da_sync_queue_item i WHERE i.state = :state AND i.active = true"
            + " AND (i.nextAttemptAt IS NULL OR i.nextAttemptAt <= :now)"
            + " ORDER BY i.nextAttemptAt, i.syncQueueItemId")
    Page<DaSyncQueueItem> findDueInFlight(@Param("state") DaSyncQueueItem.QueueItemState state,
                                          @Param("now") OffsetDateTime now, Pageable pageable);

    List<DaSyncQueueItem> findByBatchIdAndStateAndActiveIsTrueOrderBySyncQueueItemId(String batchId,
                                                                                    DaSyncQueueItem.QueueItemState state);

    /** When the earliest of the waiting items is to be taken; null when none waits for a time. */
    @Query("SELECT MIN(i.nextAttemptAt) FROM da_sync_queue_item i WHERE i.state IN :states AND i.active = true")
    OffsetDateTime findEarliestAttempt(@Param("states") Collection<DaSyncQueueItem.QueueItemState> states);

    /** Active items of the AIPs in the given states. */
    @Query("SELECT i FROM da_sync_queue_item i WHERE i.aip.aipId IN :aipIds AND i.state IN :states"
            + " AND i.active = true")
    List<DaSyncQueueItem> findActiveByAipsAndStates(@Param("aipIds") Collection<Integer> aipIds,
                                                    @Param("states") Collection<DaSyncQueueItem.QueueItemState> states);

    /**
     * Action items carried by the AIP's active queue items in the given states - what a newly
     * queued request for the same AIP supersedes.
     *
     * A projection rather than the entities: only the identifiers are needed, and holding the queue
     * items in the session would leave it with rows the deactivation below changes without it.
     */
    @Query("SELECT i.aipActionItem.aipActionItemId FROM da_sync_queue_item i"
            + " WHERE i.code = :code"
            + " AND i.digitalRepository = :digitalRepository"
            + " AND i.state IN :states"
            + " AND i.active IS TRUE"
            + " AND i.aipActionItem IS NOT NULL")
    List<Integer> findActionItemIdsToSupersede(@Param("code") String code,
                                               @Param("digitalRepository") ArrDigitalRepository digitalRepository,
                                               @Param("states") Collection<DaSyncQueueItem.QueueItemState> states);

    @Modifying
    @Query("UPDATE da_sync_queue_item i SET i.active = false " +
            "WHERE i.code = :code " +
            "AND i.digitalRepository = :digitalRepository " +
            "AND i.state IN :states " +
            "AND i.active IS TRUE")
    void updateActiveByCodeAndDigitalRepositoryAndStateInAndActiveIsTrue(@Param("code") String code,
                                                                         @Param("digitalRepository") ArrDigitalRepository digitalRepository,
                                                                         @Param("states") Collection<DaSyncQueueItem.QueueItemState> states);

    /**
     * The active request of the AIP among the given states - the newest one. There should be one at
     * most, but a race used to leave two active (see {@link AipRepository#lockByIds}); reading the
     * newest keeps such data readable instead of failing every list of AIPs.
     */
    DaSyncQueueItem findFirstByAipAndStateInAndActiveIsTrueOrderBySyncQueueItemIdDesc(DaAip aip,
                                                                                   Collection<DaSyncQueueItem.QueueItemState> states);

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
