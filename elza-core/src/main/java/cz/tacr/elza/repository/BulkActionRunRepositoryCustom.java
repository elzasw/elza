package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ArrBulkActionRun;

/**
 * Custom respozitory pro hromadné akce.
 */
public interface BulkActionRunRepositoryCustom {

    /**
     * Searches latest executions of bulk actions for specified node ids.
     *
     * Returns the latest run for each combination of the action and the output filter it was
     * computed with ({@link ArrBulkActionRun#getOutputFilterCode()}), so a single action may have
     * several runs in the result.
     *
     * @param fundVersionId
     * @param nodeIds not-empty
     * @param state When null then action with any state can be returned.
     * @return Collection of actions executed exactly on specified nodes.
     */
    List<ArrBulkActionRun> findBulkActionsByNodes(int fundVersionId, Collection<Integer> nodeIds, ArrBulkActionRun.State state);

}
