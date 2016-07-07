package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrBulkActionRun;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;

import java.util.List;
import java.util.Set;

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
    List<ArrBulkActionRun> findBulkActionsByNodes(ArrFundVersion fundVersion, Set<ArrNode> nodes, cz.tacr.elza.api.ArrBulkActionRun.State... states);

}
