package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutput;
import cz.tacr.elza.domain.ArrOutput.OutputState;

/**
 * Custom respozitory pro výstup.
 */
public interface OutputRepositoryCustom {

    /**
     * Searches outputs defined for specified nodes.
     *
     * @param currentStates state filter, null allows all output states
     */
    List<ArrOutput> findOutputsByNodes(ArrFundVersion fundVersion,
                                       Collection<Integer> nodeIds,
                                       OutputState... currentStates);
}
