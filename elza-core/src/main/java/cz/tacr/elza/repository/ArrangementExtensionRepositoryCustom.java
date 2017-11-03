package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RulArrangementExtension;

import java.util.List;

/**
 * Custom repository pro {@link ArrangementExtensionRepository}
 *
 * @since 23.10.2017
 */
public interface ArrangementExtensionRepositoryCustom {

    /**
     * Vyhledání nadřízených definicí řídících pravidel od JP k root JP, seřazený podle názvu. (tzn. bez pravidel JP)
     *
     * @param nodeId JP ke které hledáme
     * @return nalezené definice
     */
    List<RulArrangementExtension> findByNodeIdToRoot(Integer nodeId);

}
