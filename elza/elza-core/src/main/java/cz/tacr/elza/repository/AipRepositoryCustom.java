package cz.tacr.elza.repository;

import cz.tacr.elza.domain.ArrAip;

public interface AipRepositoryCustom {

    FilteredResult<ArrAip> findAips(String search, Integer firstResult, Integer maxResults);
}
