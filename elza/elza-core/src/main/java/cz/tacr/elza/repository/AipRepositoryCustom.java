package cz.tacr.elza.repository;

import cz.tacr.elza.controller.vo.SearchParams;
import cz.tacr.elza.domain.DaAip;

public interface AipRepositoryCustom {

    FilteredResult<DaAip> findAipsByFilter(SearchParams params);
}
