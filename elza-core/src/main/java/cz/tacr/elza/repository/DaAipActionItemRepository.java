package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.api.DaAipActionItemState;
import cz.tacr.elza.domain.DaAipAction;
import cz.tacr.elza.domain.DaAipActionItem;
import cz.tacr.elza.domain.DaSyncQueueItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DaAipActionItemRepository extends JpaRepository<DaAipActionItem, Integer> {

    List<DaAipActionItem> findByAipActionOrderByAipActionItemId(DaAipAction aipAction);


    /**
     * Pairs (aipId, itemId) of the items of one action.
     *
     * A projection rather than the entities: the caller runs outside the transaction the items
     * were read in, where navigating their associations is not possible.
     */
    @Query("SELECT i.aip.aipId, i.aipActionItemId FROM da_aip_action_item i"
            + " WHERE i.aipAction.aipActionId = :actionId ORDER BY i.aipActionItemId")
    List<Object[]> findAipAndItemIds(@Param("actionId") Integer actionId);

    /**
     * Items of an action that nothing can carry out any more: not done, with no queue item left to
     * bring them on and no request left in the asynchronous queue.
     *
     * A queue item counts only while it is active and still waiting to be processed; once it is
     * deactivated or has reached a terminal state, the processors never read it again.
     */
    @Query("SELECT i FROM da_aip_action_item i WHERE i.state IN :openStates"
            + " AND NOT EXISTS (SELECT q FROM da_sync_queue_item q WHERE q.aipActionItem = i"
            + "                  AND q.active = true AND q.state IN :pendingStates)"
            + " AND NOT EXISTS (SELECT r FROM arr_async_request r WHERE r.aipActionItem = i)"
            + " ORDER BY i.aipActionItemId")
    List<DaAipActionItem> findWithoutCarrier(@Param("openStates") Collection<DaAipActionItemState> openStates,
                                             @Param("pendingStates") Collection<DaSyncQueueItem.QueueItemState> pendingStates);

    /** What a step needs about its action and its AIP, without loading either of them. */
    @Query("SELECT i.aipAction.actionType, i.aip.aipId, i.aipAction.params, i.state"
            + " FROM da_aip_action_item i WHERE i.aipActionItemId = :itemId")
    List<Object[]> findActionTypeAndAip(@Param("itemId") Integer itemId);
}
