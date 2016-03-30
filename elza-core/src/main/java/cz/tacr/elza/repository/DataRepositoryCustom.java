package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import cz.tacr.elza.domain.ArrData;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.RulDescItemType;


/**
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 03.02.2016
 */
public interface DataRepositoryCustom {

    List<ArrData> findDescItemsByNodeIds(Set<Integer> nodeIds,
                                         Set<RulDescItemType> descItemTypes,
                                         ArrFundVersion version);

    List<ArrData> findByDataIdsAndVersionFetchSpecification(Set<Integer> nodeIds,
            Set<RulDescItemType> descItemTypes,
            ArrFundVersion version);


    /**
     * Najde seznam hodnot atributů obsahujících hledaný text.
     * @param nodes seznam uzlů, ve kterých hledáme
     * @param descItemType typ atributu
     * @param text hledaný text
     * @param <T>
     * @return seznam hodnot atributů
     */
    <T extends ArrData> List<T> findByNodesContainingText(Collection<ArrNode> nodes, RulDescItemType descItemType,
                                                          String text);

}
