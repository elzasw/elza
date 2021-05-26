package cz.tacr.elza.repository;

import cz.tacr.elza.common.db.QueryResults;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.domain.ApState;

import java.util.Collection;
import java.util.Set;

public interface ApCachedAccessPointRepositoryCustom {

    /**
     * Search all related accesspoints
     * 
     * @param search
     * @param searchFilter
     * @param apTypeIdTree
     * @param scopeIds
     * @param state
     *            might be null
     * @param from
     * @param count
     * @param sdp
     * @return
     */
    QueryResults<ApCachedAccessPoint> findApCachedAccessPointisByQuery(String search,
                                                                       SearchFilterVO searchFilter,
                                                                       Collection<Integer> apTypeIdTree,
                                                                       Set<Integer> scopeIds,
                                                                       ApState.StateApproval state, Integer from,
                                                                       Integer count, StaticDataProvider sdp);
}
