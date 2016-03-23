package cz.tacr.elza.repository;

import java.util.List;

import cz.tacr.elza.api.vo.RelatedNodeDirection;
import cz.tacr.elza.domain.ArrFundVersion;
import cz.tacr.elza.domain.ArrNode;


/**
 * Rozšíření repozitáře {@link NodeRepository}.
 *
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 23.11.2015
 */
public interface NodeRepositoryCustom {

    /**
     * Najde všechny uzly v daném směru prohledávání.
     *
     * @param node      uzel, od kterého prohledáváme
     * @param version   verze, ve které prohledáváme
     * @param direction směr, kterým prohledáváme strom
     * @return všechny uzly v daném směru prohledávání
     */
    List<ArrNode> findNodesByDirection(ArrNode node, ArrFundVersion version,
                                       RelatedNodeDirection direction);

    List<ArrNode> findByFulltextAndVersionLockChangeId(String text, Integer lockChangeId);

    List<ArrNode> findByNodeConformityIsNull();
}
