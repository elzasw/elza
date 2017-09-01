package cz.tacr.elza.repository;

import cz.tacr.elza.domain.RegScope;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @since 18.07.2017
 */
public interface ScopeRepositoryCustom {

    Set<RegScope> findScopesBySubtreeNodeIds(Collection<Integer> nodeIds, boolean ignoreRootNode);

}
