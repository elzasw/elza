package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.DaAipAction;
import cz.tacr.elza.domain.DaAipActionItem;
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

    /** Type of the action and the AIP of one item, without loading either of them. */
    @Query("SELECT i.aipAction.actionType, i.aip.aipId FROM da_aip_action_item i"
            + " WHERE i.aipActionItemId = :itemId")
    List<Object[]> findActionTypeAndAip(@Param("itemId") Integer itemId);
}
