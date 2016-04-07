package cz.tacr.elza.repository;

import java.util.List;
import java.util.Set;

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

    /**
     * Najde uzly s danou hodnotou.
     *
     * @param text hledaná hodnota
     * @param fundId id fondu do kterého uzly patří
     * @param lockChangeId id verze ve které se má hledat, může být null
     *
     * @return množina id uzlů odopovídající hledané hodnotě
     */
    Set<Integer> findByFulltextAndVersionLockChangeId(String text, Integer fundId, Integer lockChangeId);

    List<ArrNode> findByNodeConformityIsNull();
}
