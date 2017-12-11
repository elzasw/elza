package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.domain.ArrBulkActionRun;

/**
 * Custom respozitory pro hromadné akce.
 *
 * @author Martin Šlapa
 * @since 07.07.2016
 */
public interface BulkActionRunRepositoryCustom {

    /**
     * Vyhledá poslední spuštěné hromadné akce podle uzlů - striktně.
     *
     * @param fundVersion   verze as
     * @param nodes         seznam nodů
     * @param states        stavy, které se vyhledávají
     * @return seznam hromadných akcí
     */
    List<ArrBulkActionRun> findBulkActionsByNodes(int fundVersionId, List<Integer> nodeIds, ArrBulkActionRun.State... states);

}
