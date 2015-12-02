package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import cz.tacr.elza.domain.ArrDescItem;
import cz.tacr.elza.domain.ArrNode;
import cz.tacr.elza.domain.ArrChange;


/**
 * Rozšířený repozitář pro {@link DescItemRepository}.
 * @author Tomáš Kubový [<a href="mailto:tomas.kubovy@marbes.cz">tomas.kubovy@marbes.cz</a>]
 * @since 2.12.2015
 */
public interface DescItemRepositoryCustom {

    List<ArrDescItem> findByNodes(Collection<ArrNode> nodes, @Nullable ArrChange lockChange);
}
