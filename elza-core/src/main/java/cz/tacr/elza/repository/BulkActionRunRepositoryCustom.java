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
     * @param fundVersionId
     * @param nodeIds not-empty
     * @param state When null then action with any state can be returned.
     * @return Collection of actions executed exactly on specified nodes.
     */
    List<ArrBulkActionRun> findBulkActionsByNodes(int fundVersionId, Collection<Integer> nodeIds, ArrBulkActionRun.State state);

}
