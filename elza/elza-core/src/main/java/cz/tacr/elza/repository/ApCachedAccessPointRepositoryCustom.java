package cz.tacr.elza.repository;

import cz.tacr.elza.common.db.QueryResults;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.domain.ApState;

import java.util.Set;

public interface ApCachedAccessPointRepositoryCustom {

    QueryResults<ApCachedAccessPoint> findApCachedAccessPointisByQuery(String search, SearchFilterVO searchFilter, Set<Integer> apTypeIdTree, Set<Integer> scopeIds,
                                                               ApState.StateApproval state, Integer from, Integer count, StaticDataProvider sdp);
}
