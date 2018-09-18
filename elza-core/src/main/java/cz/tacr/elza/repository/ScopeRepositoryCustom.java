package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

import cz.tacr.elza.domain.ApScope;

/**
 * @since 18.07.2017
 */
public interface ScopeRepositoryCustom {

	List<ApScope> findScopesBySubtreeNodeIds(Collection<Integer> nodeIds, boolean ignoreRootNode);

}
