package cz.tacr.elza.repository;

import cz.tacr.elza.common.db.QueryResults;
import cz.tacr.elza.controller.vo.SearchFilterVO;
import cz.tacr.elza.core.data.StaticDataProvider;
import cz.tacr.elza.domain.ApCachedAccessPoint;
import cz.tacr.elza.domain.ApState;
import cz.tacr.elza.domain.RevStateApproval;

import java.util.Collection;

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
                                                                       Collection<Integer> scopeIds,
                                                                       ApState.StateApproval state,
                                                                       RevStateApproval revState,
                                                                       Integer from, Integer count,
                                                                       StaticDataProvider sdp);
}
