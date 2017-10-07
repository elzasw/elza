package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.RegScope;

/**
 * @since 18.07.2017
 */
public interface ScopeRepositoryCustom {

	List<RegScope> findScopesBySubtreeNodeIds(Collection<Integer> nodeIds, boolean ignoreRootNode);

}
