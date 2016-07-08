package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrOutputDefinition;
import cz.tacr.elza.api.ArrOutputDefinition.OutputState;

import java.util.List;
import java.util.Set;


/**
 * Custom respozitory pro výstup.
 *
 * @author Martin Šlapa
 * @since 01.04.2016
 */
public interface OutputDefinitionRepositoryCustom {

    /**
     * Vyhledá outputy podle uzlů - striktně.
     *
     * @param fundVersion   verze as
     * @param nodes         seznam nodů
     * @param states        stavy, které se vyhledávají (null pro všechny)
     * @return seznam outputů
     */
    List<ArrOutputDefinition> findOutputsByNodes(final ArrFundVersion fundVersion,
                                                 final Set<ArrNode> nodes,
                                                 final OutputState... states);

}
