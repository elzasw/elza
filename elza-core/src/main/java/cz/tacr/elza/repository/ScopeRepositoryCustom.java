package cz.tacr.elza.repository;

import java.util.Collection;
import java.util.List;

/**
 * @since 18.07.2017
 */
public interface ScopeRepositoryCustom {

    List<Integer> findScopeIdsBySubtreeNodeIds(Collection<Integer> nodeIds, boolean ignoreRootNode);

}
