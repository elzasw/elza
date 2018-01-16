package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.domain.ArrOutputDefinition.OutputState;

/**
 * Custom respozitory pro výstup.
 */
public interface OutputDefinitionRepositoryCustom {

    /**
     * Searches outputs defined for specified nodes.
     *
     * @param currentStates state filter, null allows all output states
     */
    List<ArrOutputDefinition> findOutputsByNodes(ArrFundVersion fundVersion,
                                                 Collection<Integer> nodeIds,
                                                 OutputState... currentStates);
}
